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
import be.condorcet.javafx.model.Borrow; // <--- IMPORTANT : On importe le modèle Emprunt
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class BookApiService {

    // Attention : Si tu es hors Docker, change "host.docker.internal" par "localhost" et vérifie le port (8080 ou 8082)
    private static final String API_URL = "http://localhost:8082/api/books";

    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private String authHeader; // null si pas de credentials -> pas d'auth envoyée

    public BookApiService() {
        // 1. On tente de lire la session créée par App.java
        String username = be.condorcet.javafx.App.Session.username;
        String password = be.condorcet.javafx.App.Session.password;

        // 2. Si la session est vide (ex: tests), on lit le fichier de config (Plan B)
        if (username == null) {
            try (InputStream input = getClass().getResourceAsStream("/config.properties")) {
                if (input != null) {
                    Properties prop = new Properties();
                    prop.load(input);
                    username = prop.getProperty("api.username");
                    password = prop.getProperty("api.password");
                }
            } catch (Exception ex) { }
        }

        // 3. On génère le header d'authentification pour toutes les futures requêtes
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            this.authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes());
        } else {
            this.authHeader = null;
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

    // Méthode générique pour POST/PUT/DELETE avec auth (si on se fiche du retour JSON)
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
                        System.err.println("Erreur API : " + resp.statusCode() + " - " + resp.body());
                        onError.run();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(onError);
                    return null;
                });
    }


    // --- AJOUT CORRECT POUR LES EMPRUNTS ---


    public void borrowBook(Long bookId, Consumer<Borrow> onSuccess, Runnable onError) {
        // 1. On prépare l'URL avec le paramètre ?bookId=... (C'est ce que veut ton @RequestParam)
        // Résultat : http://host.docker.internal:8082/api/borrows?bookId=5
        String url = API_URL.replace("/books", "/borrows") + "?bookId=" + bookId;

        // 2. On construit la requête POST
        // Note : On met "noBody()" car l'info est dans l'URL, pas dans le JSON
        HttpRequest.Builder builder = authenticatedRequest(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpRequest request = builder.build();

        // 3. Envoi et traitement
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    // Ton backend renvoie 200 OK avec l'objet Borrow en JSON
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        try {
                            Borrow createdBorrow = gson.fromJson(resp.body(), Borrow.class);
                            onSuccess.accept(createdBorrow);
                        } catch (Exception e) {
                            // Si le parsing échoue mais que la requête a marché
                            System.err.println("Succès mais erreur lecture JSON : " + e.getMessage());
                            onSuccess.accept(new Borrow());
                        }
                    } else {
                        System.err.println("Erreur Emprunt (Code " + resp.statusCode() + ") : " + resp.body());
                        onError.run();
                    }
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(onError);
                    return null;
                });
    }


    // --- AJOUTS POUR LE LOGIN DYNAMIQUE ---

    // Permet de changer l'utilisateur en cours de route (après le login)
    public void setCredentials(String username, String password) {
        if (username != null && !username.isBlank()) {
            // On recrée le header Basic Auth avec les nouvelles infos
            // Attention: ceci modifie le champ 'final authHeader', il faut retirer le mot-clé 'final' en haut !
            // -> Va en haut du fichier et change : private final String authHeader;  EN  private String authHeader;
            this.authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes());
        }
    }
    
    public String getApiUrl() {
        return API_URL;
    }

    // --- AJOUTS POUR LA GESTION ADMIN & UTILISATEUR ---

    // Récupérer mes emprunts (GET /api/borrows)
    public void loadMyBorrows(Consumer<List<Borrow>> onSuccess, Runnable onError) {
        // L'URL est http://.../api/borrows
        // Attention : vérifie que ton backend écoute bien sur /api/borrows pour le GET
        HttpRequest request = authenticatedRequest(URI.create(API_URL.replace("/books", "/borrows")))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> Platform.runLater(() -> {
                    try {
                        var type = new TypeToken<List<Borrow>>(){}.getType();
                        List<Borrow> list = gson.fromJson(body, type);
                        onSuccess.accept(list);
                    } catch (Exception e) {
                        e.printStackTrace();
                        onError.run();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(onError);
                    return null;
                });
    }

    // Récupérer tous les utilisateurs (GET /api/auth/users)
    // NOTE : Il faut avoir créé la classe User (étape précédente)
    public void loadAllUsers(Consumer<List<be.condorcet.javafx.model.User>> onSuccess, Runnable onError) {
        String authUrl = API_URL.replace("/api/books", "/api/auth/users"); // Hack pour reconstruire l'URL
        
        HttpRequest request = authenticatedRequest(URI.create(authUrl))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> Platform.runLater(() -> {
                    try {
                        var type = new TypeToken<List<be.condorcet.javafx.model.User>>(){}.getType();
                        List<be.condorcet.javafx.model.User> list = gson.fromJson(body, type);
                        onSuccess.accept(list);
                    } catch (Exception e) {
                        e.printStackTrace();
                        onError.run();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(onError);
                    return null;
                });
    }
}