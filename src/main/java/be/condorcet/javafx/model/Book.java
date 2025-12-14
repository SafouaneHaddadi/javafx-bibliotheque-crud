package be.condorcet.javafx.model;

public class Book {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String genre;
    private int stock;
    private String imageUrl;   
    private String synopsis;   

    // Constructeur vide obligatoire pour Gson
    public Book() {}

    public Book(Long id, String title, String author, String isbn, String genre, int stock, String imageUrl, String synopsis) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.genre = genre;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.synopsis = synopsis;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public String getGenre() { return genre; }
    public int getStock() { return stock; }
    public String getImageUrl() { return imageUrl; }
    public String getSynopsis() { return synopsis; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setStock(int stock) { this.stock = stock; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    @Override
    public String toString() {
        return title + " - " + author + " (" + stock + " disponibles)";
    }
}