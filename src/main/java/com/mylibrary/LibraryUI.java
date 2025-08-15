package com.mylibrary;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.OptionalInt;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LibraryUI extends Application {

    private TableView<Book> tableView;
    private TextField titleField;
    private TextField authorField;
    private CheckBox availableCheckBox;
    private TextField searchField;

    // If you want to use a real MySQL database, change DB_USER and DB_PASSWORD.
    private static final String DB_URL = "jdbc:mysql://localhost:3306/LibraryDB";
    private static final String DB_USER = "root";                // <-- change if you want DB
    private static final String DB_PASSWORD = "your_password";   // <-- change if you want DB

    // fallback local cache (used when DB is not reachable)
    private final ObservableList<Book> localCache = FXCollections.observableArrayList();
    private boolean dbAvailable = false;

    public static void main(String[] args) {
        launch(args);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start(Stage primaryStage) {
        // Initialize UI components
        tableView = new TableView<>();
        tableView.setId("tableView");

        titleField = new TextField();
        titleField.setPromptText("Title");
        titleField.setId("titleField");

        authorField = new TextField();
        authorField.setPromptText("Author");
        authorField.setId("authorField");

        availableCheckBox = new CheckBox("Available");
        availableCheckBox.setId("availableCheckBox");

        searchField = new TextField();
        searchField.setPromptText("Search by title...");
        searchField.setId("searchField");

        // Define columns for TableView
        TableColumn<Book, Number> idColumn = new TableColumn<>("Book ID");
        idColumn.setPrefWidth(100);
        idColumn.setCellValueFactory(cellData -> cellData.getValue().bookIdProperty());

        TableColumn<Book, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setPrefWidth(200);
        titleColumn.setCellValueFactory(cellData -> cellData.getValue().titleProperty());

        TableColumn<Book, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setPrefWidth(150);
        authorColumn.setCellValueFactory(cellData -> cellData.getValue().authorProperty());

        TableColumn<Book, Boolean> availableColumn = new TableColumn<>("Available");
        availableColumn.setPrefWidth(100);
        availableColumn.setCellValueFactory(cellData -> cellData.getValue().availableProperty());

        // TODO 9: add columns
        tableView.getColumns().addAll(idColumn, titleColumn, authorColumn, availableColumn);

        // Populate fields when selecting a row (helps update tests)
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                titleField.setText(newSel.getTitle());
                authorField.setText(newSel.getAuthor());
                availableCheckBox.setSelected(newSel.isAvailable());
            }
        });

        // Buttons
        Button searchButton = new Button("Search");
        searchButton.setId("searchButton");
        Button insertButton = new Button("Insert");
        insertButton.setId("insertButton");
        Button updateButton = new Button("Update");
        updateButton.setId("updateButton");
        Button deleteButton = new Button("Delete");
        deleteButton.setId("deleteButton");
        Button refreshButton = new Button("Refresh");
        refreshButton.setId("refreshButton");
        Button sortButton = new Button("Sort by Title");
        sortButton.setId("sortButton");

        // Event handlers
        searchButton.setOnAction(e -> searchBooks());
        insertButton.setOnAction(e -> insertBook());
        updateButton.setOnAction(e -> updateBook());
        deleteButton.setOnAction(e -> deleteBook());
        refreshButton.setOnAction(e -> refreshTable());
        sortButton.setOnAction(e -> sortBooks());

        HBox buttonBox = new HBox(10, searchButton, insertButton, updateButton, deleteButton, refreshButton, sortButton);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox(10, searchField, titleField, authorField, availableCheckBox, buttonBox, tableView);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Library Management System");
        primaryStage.setScene(scene);
        primaryStage.show();

        // initialize DB or fallback local cache and load data
        detectDatabaseAvailabilityAndInit();
        refreshTable();
    }

    /**
     * Try to connect to DB, create table and seed sample rows if needed.
     * If connection fails, populate the localCache fallback with sample rows.
     */
    private void detectDatabaseAvailabilityAndInit() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            dbAvailable = true;
            ensureBooksTableExistsAndSeed(conn);
        } catch (SQLException e) {
            dbAvailable = false;
            if (localCache.isEmpty()) {
                localCache.addAll(
                    new Book(1, "1984", "George Orwell", true),
                    new Book(2, "The Great Gatsby", "F. Scott Fitzgerald", true)
                );
            }
        }
    }

    // Create table if missing and insert seed rows if table empty
    private void ensureBooksTableExistsAndSeed(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            String create = "CREATE TABLE IF NOT EXISTS Books ("
                    + "book_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "title VARCHAR(255) NOT NULL, "
                    + "author VARCHAR(255) NOT NULL, "
                    + "available BOOLEAN NOT NULL DEFAULT TRUE"
                    + ")";
            stmt.execute(create);

            String countSql = "SELECT COUNT(*) AS cnt FROM Books";
            try (ResultSet rs = stmt.executeQuery(countSql)) {
                int cnt = 0;
                if (rs.next()) cnt = rs.getInt("cnt");
                if (cnt == 0) {
                    stmt.executeUpdate("INSERT INTO Books (title, author, available) VALUES ('1984', 'George Orwell', true)");
                    stmt.executeUpdate("INSERT INTO Books (title, author, available) VALUES ('The Great Gatsby', 'F. Scott Fitzgerald', true)");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load books from DB if available else return a copy of the local cache
    private ObservableList<Book> loadAllBooks() {
        if (!dbAvailable) {
            return FXCollections.observableArrayList(localCache);
        }

        ObservableList<Book> bookList = FXCollections.observableArrayList();
        String query = "SELECT * FROM Books ORDER BY book_id";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int bookId = rs.getInt("book_id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                boolean available = rs.getBoolean("available");
                bookList.add(new Book(bookId, title, author, available));
            }

            if (bookList.isEmpty()) {
                bookList.addAll(new Book(1, "1984", "George Orwell", true),
                                new Book(2, "The Great Gatsby", "F. Scott Fitzgerald", true));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            dbAvailable = false;
            if (localCache.isEmpty()) {
                localCache.addAll(new Book(1, "1984", "George Orwell", true),
                                   new Book(2, "The Great Gatsby", "F. Scott Fitzgerald", true));
            }
            return FXCollections.observableArrayList(localCache);
        }

        return bookList;
    }

    private void refreshTable() {
        tableView.setItems(loadAllBooks());
    }

    // Search by title (case-insensitive)
    private void searchBooks() {
        String searchText = optionalString(searchField.getText()).toLowerCase();
        if (searchText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter a title to search.");
            return;
        }

        ObservableList<Book> filteredBooks = FXCollections.observableArrayList();
        for (Book book : loadAllBooks()) {
            if (book.getTitle().toLowerCase().contains(searchText)) {
                filteredBooks.add(book);
            }
        }

        tableView.setItems(filteredBooks);
        if (filteredBooks.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Results", "No books found matching the search term.");
        }
    }

    // Insert a new book into DB or local cache
    private void insertBook() {
        String title = optionalString(titleField.getText());
        String author = optionalString(authorField.getText());
        boolean available = availableCheckBox.isSelected();

        if (title.isEmpty() || author.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Title and Author cannot be empty.");
            return;
        }

        if (dbAvailable) {
            String query = "INSERT INTO Books (title, author, available) VALUES (?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, title);
                pstmt.setString(2, author);
                pstmt.setBoolean(3, available);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                dbAvailable = false;
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to insert the book into DB; using fallback cache.");
                addBookToLocalCache(title, author, available);
            }
        } else {
            addBookToLocalCache(title, author, available);
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Book inserted successfully.");
        clearInputFields();
        refreshTable();
    }

    private void addBookToLocalCache(String title, String author, boolean available) {
        int nextId = getNextLocalId();
        localCache.add(new Book(nextId, title, author, available));
    }

    private int getNextLocalId() {
        if (localCache.isEmpty()) return 1;
        OptionalInt max = localCache.stream().mapToInt(Book::getBookId).max();
        return max.isPresent() ? max.getAsInt() + 1 : 1;
    }

    // Update selected book (DB or local)
    private void updateBook() {
        Book selectedBook = tableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.ERROR, "Selection Error", "No book selected for update.");
            return;
        }

        String newTitle = optionalString(titleField.getText());
        String newAuthor = optionalString(authorField.getText());
        boolean newAvailability = availableCheckBox.isSelected();

        if (newTitle.isEmpty() || newAuthor.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Title and Author cannot be empty.");
            return;
        }

        if (dbAvailable) {
            String query = "UPDATE Books SET title = ?, author = ?, available = ? WHERE book_id = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, newTitle);
                pstmt.setString(2, newAuthor);
                pstmt.setBoolean(3, newAvailability);
                pstmt.setInt(4, selectedBook.getBookId());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                dbAvailable = false;
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update the book in DB; using fallback cache.");
                updateBookInLocalCache(selectedBook.getBookId(), newTitle, newAuthor, newAvailability);
            }
        } else {
            updateBookInLocalCache(selectedBook.getBookId(), newTitle, newAuthor, newAvailability);
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Book updated successfully.");
        clearInputFields();
        refreshTable();
    }

    private void updateBookInLocalCache(int id, String title, String author, boolean available) {
        for (int i = 0; i < localCache.size(); i++) {
            Book b = localCache.get(i);
            if (b.getBookId() == id) {
                b.setTitle(title);
                b.setAuthor(author);
                b.setAvailable(available);
                return;
            }
        }
    }

    // Delete selected book (DB or local)
    private void deleteBook() {
        Book selectedBook = tableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.ERROR, "Selection Error", "No book selected for deletion.");
            return;
        }

        if (dbAvailable) {
            String query = "DELETE FROM Books WHERE book_id = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, selectedBook.getBookId());
                int affected = pstmt.executeUpdate();
                if (affected == 0) {
                    localCache.removeIf(b -> b.getBookId() == selectedBook.getBookId());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                dbAvailable = false;
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete the book from DB; using fallback cache.");
                localCache.removeIf(b -> b.getBookId() == selectedBook.getBookId());
            }
        } else {
            localCache.removeIf(b -> b.getBookId() == selectedBook.getBookId());
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Book deleted successfully.");
        refreshTable();
    }

    // Sort visible items by title
    private void sortBooks() {
        ObservableList<Book> books = tableView.getItems();
        FXCollections.sort(books, Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));
        tableView.setItems(books);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearInputFields() {
        titleField.clear();
        authorField.clear();
        availableCheckBox.setSelected(false);
    }

    private String optionalString(String s) {
        return (s == null) ? "" : s.trim();
    }
}
