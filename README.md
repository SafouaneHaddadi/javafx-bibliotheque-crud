# Application JavaFX - Gestion de bibliothèque

Application desktop JavaFX qui consomme une API REST Spring Boot sécurisée pour effectuer des opérations CRUD sur des livres.

## Fonctionnalités

- Affichage de la liste des livres (GET)
- Ajout d'un livre (POST)
- Modification d'un livre (PUT)
- Suppression d'un livre (DELETE)
- Authentification Basic pour les opérations d'écriture (POST/PUT/DELETE)
- Lecture publique (GET) accessible à tous


## Architecture

- **MVC** strict avec séparation claire :
  - **Model** : `Book.java` (POJO)
  - **Service** : `BookApiService.java` (communication HTTP asynchrone avec Gson)
  - **View** : `book-catalog.fxml` + `app.css`
  - **Controller** : `BookCatalogController.java` (logique d'écran)
  - **Point d'entrée** : `App.java`

- Communication réseau asynchrone (`HttpClient.sendAsync`) pour une interface toujours fluide
- Gestion des erreurs avec messages utilisateur et classes CSS
- Authentification Basic via fichier de configuration local

## Lancement de l'application

1. **Lancer le backend Spring Boot** (port 8082)  
   (Le projet backend est disponible dans un repo séparé (Krousty-Books)

2. **Créer le fichier de configuration local**  
   Dans `src/main/resources/config.properties` (fichier **ignoré par Git**) :

   ```properties
   api.username=X
   api.password=X
    ```
3. **Exécuter l'application JavaFX :**  
```bash
mvn javafx:run
```
## Sécurité 
- Les opérations de lecture (GET) sont publiques
- Les opérations d'écriture (POST/PUT/DELETE) nécessitent l'utilisateur admin
- Les credentials sont stockés dans config.properties 
   
