package be.condorcet.javafx;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class App extends Application {

    // IMPORTANT : On reste sur localhost pour éviter les soucis Docker/Windows
    private static final String AUTH_URL = "http://localhost:8082/api/auth";
    
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @Override
    public void start(Stage primaryStage) {
        showLoginScreen(primaryStage);
    }

    private void showLoginScreen(Stage stage) {
        stage.setTitle("Connexion Krousty-Books");

        // Champs
        TextField userField = new TextField();
        userField.setPromptText("Nom d'utilisateur");
        
        PasswordField passField = new PasswordField();
        passField.setPromptText("Mot de passe");

        Button btnEnter = new Button("Entrer (Connexion / Inscription)");
        btnEnter.setDefaultButton(true); // Permet de valider avec la touche Entrée
        
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);

        // Action du bouton
        btnEnter.setOnAction(e -> {
            String u = userField.getText().trim();
            String p = passField.getText().trim();

            if(u.isEmpty() || p.isEmpty()) {
                errorLabel.setText("Remplissez tout svp !");
                return;
            }

            // On désactive le bouton pour éviter le double-clic
            btnEnter.setDisable(true);
            errorLabel.setText("Connexion en cours...");

            // 1. TENTATIVE DE LOGIN
            tryLogin(u, p, stage, errorLabel, btnEnter, () -> {
                // 2. ECHEC LOGIN (401/404) -> TENTATIVE REGISTER
                Platform.runLater(() -> errorLabel.setText("Compte inconnu, création auto..."));
                tryRegister(u, p, stage, errorLabel, btnEnter);
            });
        });

        VBox root = new VBox(15, new Label("Bienvenue !"), userField, passField, btnEnter, errorLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        
        stage.setScene(new Scene(root, 300, 250));
        stage.show();
    }

    // Tente de se connecter (/login)
    private void tryLogin(String username, String password, Stage stage, Label errorLabel, Button btn, Runnable onFail) {
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("password", password);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json)))
                .build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    if (resp.statusCode() == 200) {
                        launchMainApp(stage, username, password);
                    } else {
                        // Si erreur (401 ou 404), on déclenche le plan B (Register)
                        onFail.run();
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        errorLabel.setText("Erreur réseau (Backend éteint ?)");
                        btn.setDisable(false); // <--- IMPORTANT : On réactive le bouton ici !
                    });
                    return null;
                });
    }

    // Tente de créer le compte (/register)
    private void tryRegister(String username, String password, Stage stage, Label errorLabel, Button btn) {
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("password", password);
        json.addProperty("email", username + "@krousty.com");
        json.addProperty("role", "USER"); // Par défaut USER. Change en "ADMIN" manuellement ici si tu veux tester l'admin.

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_URL + "/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(json)))
                .build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    if (resp.statusCode() == 200) {
                        Platform.runLater(() -> launchMainApp(stage, username, password));
                    } else {
                        Platform.runLater(() -> {
                            errorLabel.setText("Erreur création : " + resp.statusCode() + " (Mdp trop court ?)");
                            btn.setDisable(false); // <--- IMPORTANT : On réactive le bouton ici !
                        });
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        errorLabel.setText("Erreur réseau register : " + ex.getMessage());
                        btn.setDisable(false); // <--- IMPORTANT : On réactive le bouton ici !
                    });
                    return null;
                });
    }

    // Lance l'écran principal (Catalogue)
    private void launchMainApp(Stage stage, String username, String password) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/be/condorcet/javafx/view/book-catalog.fxml"));
                Parent root = loader.load();
                
                // On stocke la session
                Session.username = username;
                Session.password = password;

                Scene scene = new Scene(root, 1000, 600);
                // Charge le CSS s'il existe, sinon ignore
                try {
                    scene.getStylesheets().add(getClass().getResource("/be/condorcet/javafx/view/app.css").toExternalForm());
                } catch (Exception e) { System.out.println("Pas de CSS trouvé, tant pis."); }
                
                stage.close();
                Stage mainStage = new Stage();
                mainStage.setTitle("Krousty-Books - Connecté en tant que " + username);
                mainStage.setScene(scene);
                mainStage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    public static class Session {
        public static String username;
        public static String password;
    }

    public static void main(String[] args) {
        launch(args);
    }
}