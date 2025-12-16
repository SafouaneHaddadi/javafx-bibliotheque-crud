package be.condorcet.javafx.view;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

import be.condorcet.javafx.App;
import be.condorcet.javafx.model.Book; // Nécessaire pour App.Session
import be.condorcet.javafx.model.Borrow;
import be.condorcet.javafx.model.User;
import be.condorcet.javafx.service.BookApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class BookCatalogController {

    @FXML private Label titleLabel;
    @FXML private ListView<Book> bookListView;
    
    // Champs d'ajout
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;
    @FXML private Button addButton;
    
    @FXML private Label messageLabel;

    private final BookApiService apiService = new BookApiService();
    private final Gson gson = new Gson();
    private static final String DEFAULT_IMAGE_URL = "https://via.placeholder.com/300x450?text=No+Image";

    // Vérifie si l'utilisateur connecté est "admin" (insensible à la casse)
    private boolean isAdmin = "admin".equalsIgnoreCase(App.Session.username);

    @FXML
    public void initialize() {
        if (titleLabel != null) {
            // On utilise une valeur par défaut si la session est vide
            String name = (App.Session.username != null) ? App.Session.username : "Utilisateur";
            titleLabel.setText("Bienvenue " + name + (isAdmin ? " (Mode Admin)" : ""));
            injectTopMenu(); 
        }
        
       // Cacher la zone admin si pas admin
        if (!isAdmin) {
            // 1. On cache les boutons au cas où
            if (addButton != null) addButton.setVisible(false);
            
            // 2. L'ASTUCE ULTIME :
            // On récupère le "Parent" du champ Titre (c'est à dire la VBox de droite)
            // et on cache TOUT le panneau d'un coup !
            if (titleField != null && titleField.getParent() != null) {
                titleField.getParent().setVisible(false); // Rend invisible
                titleField.getParent().setManaged(false); // Libère l'espace (le tableau prendra toute la largeur)
            }
        }

        if (messageLabel != null) messageLabel.setText("Chargement...");

        // Configuration de la liste
        bookListView.setCellFactory(lv -> new ListCell<Book>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);

                if (empty || book == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Info du livre
                    Label label = new Label(book.getTitle() + " - " + book.getAuthor() + " (Stock: " + book.getStock() + ")");
                    label.setPrefWidth(320);

                    // Bouton EMPRUNTER (Pour tout le monde)
                    Button borrowBtn = new Button("Emprunter");
                    borrowBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
                    
                    if (book.getStock() <= 0) {
                        borrowBtn.setDisable(true);
                        borrowBtn.setText("Indisponible");
                        borrowBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
                    }
                    
                    borrowBtn.setOnAction(e -> {
                        borrowBtn.setDisable(true);
                        borrowBtn.setText("...");
                        apiService.borrowBook(book.getId(), 
                            b -> { 
                                showSuccess("Livre emprunté !");
                                loadBooks(); // Rafraîchir pour le stock
                            },
                            () -> { 
                                showError("Erreur stock ou serveur");
                                borrowBtn.setDisable(false);
                                borrowBtn.setText("Emprunter");
                            }
                        );
                    });

                    HBox hbox = new HBox(10, label, borrowBtn);
                    hbox.setStyle("-fx-alignment: center-left; -fx-padding: 5;");

                    // Boutons ADMIN (Modifier / Supprimer)
                    if (isAdmin) {
                        Button editBtn = new Button("Edit");
                        editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        editBtn.setOnAction(e -> openEditDialog(book));
                        
                        Button delBtn = new Button("X");
                        delBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                        delBtn.setOnAction(e -> confirmAndDelete(book));
                        
                        hbox.getChildren().addAll(editBtn, delBtn);
                    }

                    setGraphic(hbox);
                }
            }
        });

        loadBooks();
    }

    // --- LE HACK : Injecter les boutons sans toucher au FXML ---
    private void injectTopMenu() {
        HBox topMenu = new HBox(15);
        topMenu.setPadding(new Insets(10));
        topMenu.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 0 0 1 0;");
        
        Button btnBorrows = new Button("📚 Mes Emprunts");
        btnBorrows.setOnAction(e -> onMyBorrowsClick());
        topMenu.getChildren().add(btnBorrows);

        if (isAdmin) {
            Button btnUsers = new Button("👥 Gérer Utilisateurs");
            btnUsers.setOnAction(e -> onUsersClick());
            topMenu.getChildren().add(btnUsers);
        }

        // On essaie d'insérer ce menu juste après le titre
        if (titleLabel.getParent() instanceof VBox) {
            VBox root = (VBox) titleLabel.getParent();
            // Index 1 = Juste après le premier élément (souvent le titre)
            try {
                root.getChildren().add(1, topMenu);
            } catch (Exception e) {
                // Si ça rate (ex: layout complexe), on ajoute tout en bas pour être sûr
                root.getChildren().add(topMenu);
            }
        }
    }

    // --- ACTIONS DU NOUVEAU MENU ---

    public void onMyBorrowsClick() {
        Stage stage = new Stage();
        stage.setTitle("Mes Emprunts");
        stage.initModality(Modality.APPLICATION_MODAL); // Bloque la fenêtre derrière
        
        ListView<String> list = new ListView<>();
        
        apiService.loadMyBorrows(borrows -> {
            if (borrows.isEmpty()) list.getItems().add("Aucun emprunt en cours.");
            for (Borrow b : borrows) {
                String bookTitle = (b.getBook() != null) ? b.getBook().getTitle() : "Livre inconnu";
                list.getItems().add(b.getBorrowDate() + " : " + bookTitle);
            }
        }, () -> list.getItems().add("Erreur lors du chargement."));

        Scene scene = new Scene(new VBox(list), 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    public void onUsersClick() {
        Stage stage = new Stage();
        stage.setTitle("Utilisateurs Inscrits");
        stage.initModality(Modality.APPLICATION_MODAL);
        
        ListView<String> list = new ListView<>();
        
        apiService.loadAllUsers(users -> {
            for (User u : users) {
                list.getItems().add("ID " + u.getId() + " : " + u.getUsername() + " (" + u.getRole() + ")");
            }
        }, () -> list.getItems().add("Erreur chargement users."));
        
        Label hint = new Label("Info: Suppression impossible (API manquante)");
        hint.setPadding(new Insets(5));
        
        VBox root = new VBox(5, list, hint);
        root.setPadding(new Insets(10));
        
        Scene scene = new Scene(root, 300, 400);
        stage.setScene(scene);
        stage.show();
    }

    // --- FONCTIONS CLASSIQUES (Load, Add, Delete...) ---

    private void loadBooks() {
        apiService.loadBooksAsync(
            books -> bookListView.setItems(books),
            () -> showError("Impossible de charger les livres (API éteinte ?)")
        );
    }

    @FXML
    private void addBook() {
        if (!isAdmin) return; // Sécurité

        String title = titleField.getText().trim();
        String author = authorField.getText().trim();

        if (title.isEmpty() || author.isEmpty()) {
            showError("Titre et auteur obligatoires !");
            return;
        }

        Book newBook = new Book();
        newBook.setTitle(title);
        newBook.setAuthor(author);
        newBook.setIsbn(isbnField.getText().trim());
        newBook.setImageUrl(DEFAULT_IMAGE_URL);
        newBook.setStock(1); // Stock par défaut

        String json = gson.toJson(newBook);

        // Appel API Manuel (POST)
        var request = apiService.authenticatedRequest(URI.create(apiService.getApiUrl()))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        apiService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        showSuccess("Livre ajouté !");
                        titleField.clear(); authorField.clear(); isbnField.clear();
                        loadBooks();
                    } else {
                        showError("Erreur ajout : " + resp.statusCode());
                    }
                }));
    }

    private void confirmAndDelete(Book book) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + book.getTitle() + " ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) deleteBook(book.getId());
        });
    }

    private void deleteBook(Long id) {
        var request = apiService.authenticatedRequest(URI.create(apiService.getApiUrl() + "/" + id))
                .DELETE()
                .build();

        apiService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() == 200 || resp.statusCode() == 204) {
                        showSuccess("Supprimé.");
                        loadBooks();
                    } else {
                        showError("Erreur delete: " + resp.statusCode());
                    }
                }));
    }

    private void openEditDialog(Book book) {
        Stage dialog = new Stage();
        dialog.initOwner(bookListView.getScene().getWindow());
        dialog.setTitle("Modifier");

        TextField tField = new TextField(book.getTitle());
        TextField aField = new TextField(book.getAuthor());
        TextField iField = new TextField(book.getIsbn());
        TextField sField = new TextField(String.valueOf(book.getStock()));

        Button saveBtn = new Button("Sauvegarder");
        saveBtn.setOnAction(e -> {
            book.setTitle(tField.getText());
            book.setAuthor(aField.getText());
            book.setIsbn(iField.getText());
            try { book.setStock(Integer.parseInt(sField.getText())); } catch(Exception ex){}

            updateBook(book, dialog);
        });

        VBox vbox = new VBox(10, new Label("Titre"), tField, new Label("Auteur"), aField, new Label("ISBN"), iField, new Label("Stock"), sField, saveBtn);
        vbox.setPadding(new Insets(20));
        dialog.setScene(new Scene(vbox, 300, 350));
        dialog.show();
    }

    private void updateBook(Book book, Stage dialog) {
        String json = gson.toJson(book);
        var request = apiService.authenticatedRequest(URI.create(apiService.getApiUrl() + "/" + book.getId()))
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        apiService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() == 200) {
                        showSuccess("Modifié.");
                        loadBooks();
                        dialog.close();
                    } else {
                        showError("Erreur modif: " + resp.statusCode());
                    }
                }));
    }

    private void showSuccess(String msg) { 
        if(messageLabel != null) {
            messageLabel.setText(msg); 
            messageLabel.setStyle("-fx-text-fill: green;");
        }
    }
    
    private void showError(String msg) { 
        if(messageLabel != null) {
            messageLabel.setText(msg); 
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
}