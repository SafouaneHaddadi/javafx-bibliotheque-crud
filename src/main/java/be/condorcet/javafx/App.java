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
        // === FENÊTRE DE CONNEXION STYLÉE ===
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(40));

        Label welcomeLabel = new Label("Bienvenue sur Krousty Books !");
        welcomeLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #212529;");

        Label subtitleLabel = new Label("Veuillez vous connecter pour accéder à votre bibliothèque");
        subtitleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #495057;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username (ex: admin ou Sarah)");
        usernameField.setMaxWidth(300);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setMaxWidth(300);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #d63384; -fx-font-weight: bold;");

        Button loginBtn = new Button("Se connecter");
        loginBtn.getStyleClass().add("button-primary");
        loginBtn.setDefaultButton(true);

        grid.add(welcomeLabel, 0, 0, 2, 1);
        grid.add(subtitleLabel, 0, 1, 2, 1);
        grid.add(new Label("Username :"), 0, 2);
        grid.add(usernameField, 1, 2);
        grid.add(new Label("Mot de passe :"), 0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(loginBtn, 1, 4);
        grid.add(errorLabel, 0, 5, 2, 1);

        Stage loginStage = new Stage();
        loginStage.initModality(Modality.APPLICATION_MODAL);
        loginStage.setTitle("Connexion – Krousty Books");
        Scene loginScene = new Scene(grid, 500, 400);
        loginScene.getStylesheets().add(getClass().getResource("/be/condorcet/javafx/view/app.css").toExternalForm());
        loginStage.setScene(loginScene);

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Veuillez remplir les deux champs");
                return;
            }

            // Test de connexion
            BookApiService testService = new BookApiService(username, password);

            testService.loadBooksAsync(
                books -> Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/be/condorcet/javafx/view/book-catalog.fxml"));
                        Scene mainScene = new Scene(loader.load());

                        BookCatalogController controller = loader.getController();
                        controller.setApiService(new BookApiService(username, password));
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