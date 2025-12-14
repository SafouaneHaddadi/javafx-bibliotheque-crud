package be.condorcet.javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/be/condorcet/javafx/view/book-catalog.fxml"));
        Parent root = mainLoader.load(); 
        Scene mainScene = new Scene(root, 1000, 600);
        mainScene.getStylesheets().add(getClass().getResource("/be/condorcet/javafx/view/app.css").toExternalForm());

        primaryStage.setTitle("Ma Bibliothèque – JavaFX + Spring Boot");
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}