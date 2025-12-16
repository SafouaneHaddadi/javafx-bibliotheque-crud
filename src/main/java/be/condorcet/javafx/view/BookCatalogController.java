package be.condorcet.javafx.view;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

import be.condorcet.javafx.model.Book;
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
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;
    @FXML private Button addButton;
    @FXML private Label messageLabel;
    @FXML private TextField genreField;

    private final BookApiService apiService = new BookApiService();
    private final Gson gson = new Gson();
    private static final String DEFAULT_IMAGE_URL = "https://via.placeholder.com/300x450?text=No+Image";

    @FXML
    public void initialize() {
        messageLabel.setText("Chargement des livres...");

        bookListView.setCellFactory(lv -> new ListCell<Book>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);

                if (empty || book == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label label = new Label(book.toString());
                    label.setPrefWidth(400);

                    Button editBtn = new Button("Modifier");
                    editBtn.getStyleClass().add("button-edit");
                    editBtn.setOnAction(e -> openEditDialog(book));

                    Button deleteBtn = new Button("Supprimer");
                    deleteBtn.getStyleClass().add("button-delete");
                    deleteBtn.setOnAction(e -> confirmAndDelete(book));

                    HBox hbox = new HBox(15, label, editBtn, deleteBtn);
                    hbox.setStyle("-fx-alignment: center-left; -fx-padding: 8;");
                    setGraphic(hbox);
                }
            }
        });

        loadBooks();
    }

    private void loadBooks() {
        apiService.loadBooksAsync(
            books -> {
                bookListView.setItems(books);
                showSuccess("Livres chargés depuis Spring Boot !");
            },
            () -> showError("Erreur de connexion à l'API")
        );
    }

    // AJOUT
    @FXML
    private void addBook() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String genre = genreField.getText().trim();

        if (title.isEmpty() || author.isEmpty() || genre.isEmpty()) {
        showError("Titre, auteur et genre sont obligatoires");
        return;
    }

        Book newBook = new Book();
        newBook.setTitle(title);
        newBook.setAuthor(author);
        newBook.setIsbn(isbnField.getText().trim());
        newBook.setGenre(genre);
        newBook.setImageUrl(DEFAULT_IMAGE_URL);

        String json = gson.toJson(newBook);

        var request = apiService.authenticatedRequest(URI.create("http://host.docker.internal:8082/api/books"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        apiService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        showSuccess("Livre ajouté !");
                        titleField.clear();
                        authorField.clear();
                        isbnField.clear();
                        loadBooks();
                    } else {
                        showError("Erreur ajout : " + resp.statusCode());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Erreur réseau : " + ex.getMessage()));
                    return null;
                });
    }

    // MODIFICATION
    private void openEditDialog(Book book) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(bookListView.getScene().getWindow());
        dialog.setTitle("Modifier le livre");

        TextField titleField = new TextField(book.getTitle());
        TextField authorField = new TextField(book.getAuthor());
        TextField isbnField = new TextField(book.getIsbn() != null ? book.getIsbn() : "");

        Button saveBtn = new Button("Sauvegarder");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setOnAction(e -> {
            book.setTitle(titleField.getText().trim());
            book.setAuthor(authorField.getText().trim());
            book.setIsbn(isbnField.getText().trim());

            if (book.getImageUrl() == null || book.getImageUrl().isEmpty()) {
                book.setImageUrl(DEFAULT_IMAGE_URL);
            }

            updateBook(book, dialog);
        });

        VBox vbox = new VBox(10,
            new Label("Titre :"), titleField,
            new Label("Auteur :"), authorField,
            new Label("ISBN :"), isbnField,
            saveBtn
        );
        vbox.setPadding(new Insets(20));

        dialog.setScene(new Scene(vbox, 400, 250));
        dialog.show();
    }

    private void updateBook(Book book, Stage dialog) {
        String json = gson.toJson(book);

        var request = apiService.authenticatedRequest(URI.create("http://host.docker.internal:8082/api/books/" + book.getId()))
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        apiService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() == 200) {
                        showSuccess("Livre modifié !");
                        loadBooks();
                        dialog.close();
                    } else {
                        showError("Erreur modification : " + resp.statusCode());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Erreur réseau : " + ex.getMessage()));
                    return null;
                });
    }

    // SUPPRESSION
    private void confirmAndDelete(Book book) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setContentText("Supprimer " + book.getTitle() + " ?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            deleteBook(book.getId());
        }
    }

    private void deleteBook(Long id) {
        var request = apiService.authenticatedRequest(URI.create("http://host.docker.internal:8082/api/books/" + id))
                .DELETE()
                .build();

        apiService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() == 204 || resp.statusCode() == 200) {
                        showSuccess("Livre supprimé !");
                        loadBooks();
                    } else {
                        showError("Erreur suppression : " + resp.statusCode());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Erreur réseau : " + ex.getMessage()));
                    return null;
                });
    }

    private void showSuccess(String text) {
        messageLabel.getStyleClass().removeAll("message-error");
        messageLabel.getStyleClass().add("message-success");
        messageLabel.setText(text);
    }

    private void showError(String text) {
        messageLabel.getStyleClass().removeAll("message-success");
        messageLabel.getStyleClass().add("message-error");
        messageLabel.setText(text);
    }
}