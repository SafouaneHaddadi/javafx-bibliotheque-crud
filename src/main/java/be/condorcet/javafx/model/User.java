package be.condorcet.javafx.model;

public class User {

    private Long id;
    private String username;
    private String password; // Sera souvent null quand on récupère la liste (sécurité du backend)
    private String email;
    private String role;     // Ex: "ADMIN", "USER"

    // Constructeur vide (Obligatoire pour Gson)
    public User() {
    }

    // Constructeur complet
    public User(Long id, String username, String password, String email, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}