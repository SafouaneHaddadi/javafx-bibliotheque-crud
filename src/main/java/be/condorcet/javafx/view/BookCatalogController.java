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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    @FXML private TextField genreField;
    @FXML private TextField imageUrlField;
    @FXML private TextArea synopsisArea;
    @FXML private Button addButton;
    @FXML private Label messageLabel;

    private BookApiService apiService;
    private final Gson gson = new Gson();

    private static final String PLACEHOLDER_IMAGE = "https://via.placeholder.com/300x450?text=No+Image";

    public void setApiService(BookApiService apiService) {
        this.apiService = apiService;
    }

    public void loadBooksAfterLogin() {
        messageLabel.setText("Chargement des livres...");
        apiService.loadBooksAsync(
            books -> {
                bookListView.setItems(books);
                showSuccess("Livres chargés ! (" + books.size() + " livres)");
            },
            () -> showError("Erreur de connexion à l'API")
        );
    }

    @FXML
    public void initialize() {
        messageLabel.setText("Chargement des livres...");

        bookListView.setCellFactory(lv -> new ListCell<Book>() {
            private final ImageView imageView = new ImageView();
            private final Label titleLabel = new Label();
            private final Label authorLabel = new Label();
            private final Label genreLabel = new Label();
            private final Label synopsisLabel = new Label();
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");

            {
                imageView.setFitHeight(120);
                imageView.setFitWidth(80);
                imageView.setPreserveRatio(true);

                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");
                authorLabel.setStyle("-fx-font-style: italic; -fx-font-size: 13;");
                genreLabel.setStyle("-fx-font-size: 12;");
                synopsisLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");
                synopsisLabel.setWrapText(true);
                synopsisLabel.setPrefHeight(60);

                editBtn.getStyleClass().add("button-edit");
                deleteBtn.getStyleClass().add("button-delete");
            }

            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);

                if (empty || book == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String imgUrl = book.getImageUrl() != null && !book.getImageUrl().isBlank()
                            ? book.getImageUrl()
                            : PLACEHOLDER_IMAGE;
                    Image image = new Image(imgUrl, true);
                    imageView.setImage(image);

                    titleLabel.setText(book.getTitle());
                    authorLabel.setText("par " + book.getAuthor());
                    genreLabel.setText("Genre : " + book.getGenre());
                    synopsisLabel.setText(book.getSynopsis() != null && !book.getSynopsis().isBlank()
                            ? book.getSynopsis()
                            : "Aucun synopsis disponible");

                    editBtn.setOnAction(e -> openEditDialog(book));
                    deleteBtn.setOnAction(e -> confirmAndDelete(book));

                    VBox infoBox = new VBox(5, titleLabel, authorLabel, genreLabel, synopsisLabel);
                    infoBox.setPrefWidth(400);

                    HBox buttonsBox = new HBox(10, editBtn, deleteBtn);

                    VBox rightBox = new VBox(15, infoBox, buttonsBox);

                    HBox mainBox = new HBox(20, imageView, rightBox);
                    mainBox.setPadding(new Insets(10));
                    mainBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 5;");

                    setGraphic(mainBox);
                }
            }
        });
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
        newBook.setSynopsis(synopsisArea.getText().trim());
        String imageUrl = imageUrlField.getText().trim();
        newBook.setImageUrl(imageUrl.isEmpty() ? PLACEHOLDER_IMAGE : imageUrl);

        String json = gson.toJson(newBook);

        var request = apiService.authenticatedRequest("/api/books")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        apiService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        showSuccess("Livre ajouté !");
                        clearAddFields();
                        loadBooksAfterLogin();
                    } else {
                        showError("Erreur ajout : " + resp.statusCode());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Erreur réseau : " + ex.getMessage()));
                    return null;
                });
    }

    private void clearAddFields() {
        titleField.clear();
        authorField.clear();
        isbnField.clear();
        genreField.clear();
        synopsisArea.clear();
        imageUrlField.clear();
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
        TextField genreField = new TextField(book.getGenre() != null ? book.getGenre() : "");
        TextArea synopsisArea = new TextArea(book.getSynopsis() != null ? book.getSynopsis() : "");
        synopsisArea.setWrapText(true);
        synopsisArea.setPrefRowCount(5);
        TextField imageUrlField = new TextField(book.getImageUrl() != null ? book.getImageUrl() : "");

        Button saveBtn = new Button("Sauvegarder");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setOnAction(e -> {
            book.setTitle(titleField.getText().trim());
            book.setAuthor(authorField.getText().trim());
            book.setIsbn(isbnField.getText().trim());
            book.setGenre(genreField.getText().trim());
            book.setSynopsis(synopsisArea.getText().trim());
            String imageUrl = imageUrlField.getText().trim();
            book.setImageUrl(imageUrl.isEmpty() ? PLACEHOLDER_IMAGE : imageUrl);

            updateBook(book, dialog);
        });

        VBox vbox = new VBox(10,
            new Label("Titre :"), titleField,
            new Label("Auteur :"), authorField,
            new Label("ISBN :"), isbnField,
            new Label("Genre :"), genreField,
            new Label("Synopsis :"), synopsisArea,
            new Label("URL Image :"), imageUrlField,
            saveBtn
        );
        vbox.setPadding(new Insets(20));
        dialog.setScene(new Scene(vbox, 550, 500));
        dialog.show();
    }

    private void updateBook(Book book, Stage dialog) {
        String json = gson.toJson(book);

        var request = apiService.authenticatedRequest("/api/books/" + book.getId())
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        apiService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() == 200) {
                        showSuccess("Livre modifié !");
                        loadBooksAfterLogin();
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
        var request = apiService.authenticatedRequest("/api/books/" + id)
                .DELETE()
                .build();

        apiService.getClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() == 204 || resp.statusCode() == 200) {
                        showSuccess("Livre supprimé !");
                        loadBooksAfterLogin();
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