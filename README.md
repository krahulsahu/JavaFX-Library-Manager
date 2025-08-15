# ShelfMate — JavaFX Library Manager

- Display a list of books in a TableView with columns: Book ID, Title, Author, Available (boolean).
- Insert a new book with title, author and availability.
- Update an existing book by selecting a row and editing its fields.
- Delete a selected book from the list.
- Search books by title (case-insensitive) and display matching results.
- Refresh the table to show the latest data source contents.
- Sort the visible books by title (case-insensitive).
- Uses JDBC to persist data to a MySQL `Books` table when available.
- Falls back to a seeded in-memory dataset when the database is unreachable so the UI and tests remain functional.
- Pre-seeds the app with sample books (e.g., "1984", "The Great Gatsby") when starting with an empty database or when running without DB.


  to run this project : mvn clean javafx:run

<img width="993" height="788" alt="Screenshot 2025-08-15 101951" src="https://github.com/user-attachments/assets/125a3ed1-8fa8-4d1f-a15d-9528adb7496d" />
<img width="994" height="786" alt="Screenshot 2025-08-15 102028" src="https://github.com/user-attachments/assets/7b4c5f54-d1ee-4368-ae35-4d373856b403" />
<img width="989" height="772" alt="Screenshot 2025-08-15 102203" src="https://github.com/user-attachments/assets/bc8a2b05-d650-498a-86a2-cecba32f20cc" />
<img width="992" height="786" alt="Screenshot 2025-08-15 102217" src="https://github.com/user-attachments/assets/e2d41358-5bdd-4b1d-ae0f-7ce8c600700f" />


