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

   private static final String API_URL = "http://localhost:8082/api/books";

    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final String authHeader; // null si pas de credentials -> pas d'auth envoyée

    public BookApiService() {
        String username = null;
        String password = null;

        try (InputStream input = getClass().getResourceAsStream("/config.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                username = prop.getProperty("api.username");
                password = prop.getProperty("api.password");
            }
        } catch (Exception ex) {
            System.err.println("Erreur lecture config.properties : " + ex.getMessage());
        }

        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            this.authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes());
        } else {
            this.authHeader = null; // pas d'auth -> GET public
        }
    }

    public HttpClient getClient() {
        return client;
    }

    // Requête GET sans auth (publique)
    private HttpRequest getPublicBooksRequest() {
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .GET()
                .build();
    }

    // Requête avec auth pour POST/PUT/DELETE
    public HttpRequest.Builder authenticatedRequest(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json");

        if (authHeader != null) {
            builder.header("Authorization", authHeader);
        }
        return builder;
    }

    public void loadBooksAsync(Consumer<ObservableList<Book>> onSuccess,
                               Runnable onError) {

        HttpRequest request = getPublicBooksRequest(); // GET public, sans auth

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

    // Méthode pour POST/PUT/DELETE avec auth (si credentials présents)
    public void sendAuthenticatedRequest(HttpRequest.Builder originalBuilder,
                                         Runnable onSuccess,
                                         Runnable onError) {
        // On reconstruit la requête avec l'auth
        HttpRequest.Builder builder = authenticatedRequest(originalBuilder.build().uri())
                .method(originalBuilder.build().method(), 
                        originalBuilder.build().bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));

        originalBuilder.build().headers().map().forEach((key, values) -> 
            values.forEach(value -> builder.header(key, value))
        );

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