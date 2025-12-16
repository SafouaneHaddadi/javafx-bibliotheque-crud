package be.condorcet.javafx.model;

public class Borrow {
    
    private Long id;
    private String borrowDate; // On utilise String pour simplifier la réception du JSON (évite les erreurs de parsing de dates)
    private String returnDate;
    private Book book;         // L'API renvoie souvent l'objet Livre complet à l'intérieur de l'emprunt
    // private User borrower;  // On peut l'ajouter si ton API renvoie l'utilisateur, mais souvent inutile côté front si c'est "moi"

    public Borrow() {
    }

    public Borrow(Long id, String borrowDate, String returnDate, Book book) {
        this.id = id;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
        this.book = book;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(String borrowDate) {
        this.borrowDate = borrowDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    @Override
    public String toString() {
        return "Emprunt n°" + id + " : " + book.getTitle() + " (le " + borrowDate + ")";
    }
}