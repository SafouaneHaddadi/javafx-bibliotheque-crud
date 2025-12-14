package be.condorcet.javafx.service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import be.condorcet.javafx.model.Book;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class BookApiService {

    private static final String API_URL = "http://host.docker.internal:8082/api/books";

    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final String authHeader;

    public BookApiService() {
        String username;
        String password;

        try (InputStream input = getClass().getResourceAsStream("/config.properties")) {
            if (input == null) {
                throw new RuntimeException("Fichier config.properties non trouvé. "
                        + "Créez-le avec api.username et api.password pour l'authentification.");
            }

            Properties prop = new Properties();
            prop.load(input);

            username = prop.getProperty("api.username");
            password = prop.getProperty("api.password");

            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new RuntimeException("api.username ou api.password manquant ou vide dans config.properties");
            }

        } catch (Exception ex) {
            throw new RuntimeException("Impossible de charger les credentials : " + ex.getMessage(), ex);
        }

        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
    }

    public HttpClient getClient() {
        return client;
    }

    public HttpRequest.Builder authenticatedRequest(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json");
    }

    public void loadBooksAsync(Consumer<ObservableList<Book>> onSuccess,
                               Runnable onError) {

        HttpRequest request = authenticatedRequest(URI.create(API_URL))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(json -> {
                    var type = new TypeToken<List<Book>>(){}.getType();
                    return gson.fromJson(json, type);
                })
                .thenAccept(rawList -> Platform.runLater(() -> {
                    @SuppressWarnings("unchecked")
                    List<Book> bookList = (List<Book>) rawList;
                    ObservableList<Book> observableBooks = FXCollections.observableArrayList(bookList);
                    onSuccess.accept(observableBooks);
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(onError);
                    return null;
                });
    }

    // Méthode pour POST/PUT/DELETE
    public void sendAuthenticatedRequest(HttpRequest.Builder builder,
                                         Runnable onSuccess,
                                         Runnable onError) {
        HttpRequest request = builder.build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        onSuccess.run();
                    } else {
                        onError.run();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(onError);
                    return null;
                });
    }
}