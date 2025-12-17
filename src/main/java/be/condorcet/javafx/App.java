package be.condorcet.javafx;

import be.condorcet.javafx.service.BookApiService;
import be.condorcet.javafx.view.BookCatalogController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // === FENÊTRE DE CONNEXION ===
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username (ex: admin ou Sarah)");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        Button loginBtn = new Button("Se connecter");
        loginBtn.setDefaultButton(true);

        grid.add(new Label("Username :"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Mot de passe :"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(loginBtn, 1, 2);
        grid.add(errorLabel, 1, 3);

        Stage loginStage = new Stage();
        loginStage.initModality(Modality.APPLICATION_MODAL);
        loginStage.setTitle("Connexion à Krousty Books");
        loginStage.setScene(new Scene(grid, 400, 200));

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Veuillez remplir les deux champs");
                return;
            }

            // Test de connexion avec ces identifiants
            BookApiService testService = new BookApiService(username, password);

            testService.loadBooksAsync(
                books -> Platform.runLater(() -> {
                    try {
                        // Connexion réussie → charge l'interface principale
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/be/condorcet/javafx/view/book-catalog.fxml"));
                        Scene mainScene = new Scene(loader.load());

                        BookCatalogController controller = loader.getController();
                        controller.setApiService(new BookApiService(username, password));

                        // Charge les livres après injection du service
                        controller.loadBooksAfterLogin();

                        primaryStage.setScene(mainScene);
                        primaryStage.setTitle("Krousty Books");
                        primaryStage.show();

                        loginStage.close();
                    } catch (Exception ex) {
                        errorLabel.setText("Erreur chargement interface");
                        ex.printStackTrace();
                    }
                }),
                () -> Platform.runLater(() -> errorLabel.setText("Identifiants incorrects ou serveur inaccessible"))
            );
        });

        loginStage.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}