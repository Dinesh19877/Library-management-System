// LibraryApp.java
// If you need a package, add: package com.ok.javainonevid.oops;

import java.sql.*;
import java.util.*;

// ===================== Custom Exceptions =====================
class BookNotAvailableException extends RuntimeException {
    public BookNotAvailableException(String msg) { super(msg); }
}
class BookNotExistsException extends RuntimeException {
    public BookNotExistsException(String msg) { super(msg); }
}
class BorrowedLimitExceedException extends RuntimeException {
    public BorrowedLimitExceedException(String msg) { super(msg); }
}
class UserIDNotAvailableException extends RuntimeException {
    public UserIDNotAvailableException(String msg) { super(msg); }
}

// ===================== DB Connection Manager =====================
class Db {
    private static final String URL  = "jdbc:mysql://localhost:3306/librarydb?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";          // TODO: change
    private static final String PASS = "MACINTOSH100"; // TODO: change

    private static Connection conn;

    public static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL Driver not found", e);
            }
            conn = DriverManager.getConnection(URL, USER, PASS);
        }
        return conn;
    }
}

// ===================== Models (optional POJOs) =====================
class Book {
    int id;
    String title;
    String author;
    int quantity;
    int availability;
}
class User {
    int userId;
    String name;
    int borrowLimit;
    int totalBorrowed;
}

// ===================== Service Layer (all SQL lives here) =====================
class LibraryService {
    private final Connection con;
    private final Scanner sc = new Scanner(System.in);

    public LibraryService(Connection con) {
        this.con = con;
    }

    // ---------- Users ----------
    public void addUserInteractive() {
        System.out.print("Enter User ID: ");
        int id = nextIntSafe();
        sc.nextLine();
        System.out.print("Enter Name: ");
        String name = sc.nextLine();
        System.out.print("Enter Borrow Limit: ");
        int limit = nextIntSafe();

        addUser(id, name, limit);
        System.out.println("✅ User added/updated.");
    }

    public void addUser(int userId, String name, int borrowLimit) {
        final String sql = """
            INSERT INTO users(user_id, name, borrow_limit, total_borrowed)
            VALUES(?, ?, ?, 0)
            ON DUPLICATE KEY UPDATE name=VALUES(name), borrow_limit=VALUES(borrow_limit)
        """;
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.setString(2, name);
            pst.setInt(3, borrowLimit);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add user", e);
        }
    }

    public void displayUsers() {
        final String sql = "SELECT user_id, name, borrow_limit, total_borrowed FROM users ORDER BY user_id";
        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            System.out.println("\nUsers:");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("User: %s (%d)\n", rs.getString("name"), rs.getInt("user_id"));
                System.out.printf("Borrow Limit: %d | Total Borrowed: %d\n\n",
                        rs.getInt("borrow_limit"), rs.getInt("total_borrowed"));
            }
            if (!any) System.out.println("(no users)\n");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to display users", e);
        }
    }

    public void displayUserInteractive() {
        System.out.print("Enter User ID: ");
        int userId = nextIntSafe();
        displayUser(userId);
    }

    public void displayUser(int userId) {
        final String uSql = "SELECT user_id, name, borrow_limit, total_borrowed FROM users WHERE user_id=?";
        final String bSql = """
            SELECT bb.borrow_id, b.title, b.author, bb.borrowed_at, bb.returned_at
            FROM borrowed_books bb
            JOIN books b ON b.book_id = bb.book_id
            WHERE bb.user_id=? AND bb.returned_at IS NULL
            ORDER BY bb.borrowed_at DESC
        """;
        try (PreparedStatement upst = con.prepareStatement(uSql)) {
            upst.setInt(1, userId);
            try (ResultSet urs = upst.executeQuery()) {
                if (!urs.next()) throw new UserIDNotAvailableException("User not found with this ID");

                System.out.printf("\nUser: %s (%d)\n", urs.getString("name"), urs.getInt("user_id"));
                System.out.printf("Borrow Limit: %d | Total Borrowed: %d\n",
                        urs.getInt("borrow_limit"), urs.getInt("total_borrowed"));
            }
            try (PreparedStatement bpst = con.prepareStatement(bSql)) {
                bpst.setInt(1, userId);
                try (ResultSet brs = bpst.executeQuery()) {
                    System.out.println("Currently Borrowed:");
                    boolean any = false;
                    while (brs.next()) {
                        any = true;
                        System.out.printf("- %s by %s (since %s)\n",
                                brs.getString("title"),
                                brs.getString("author"),
                                brs.getTimestamp("borrowed_at"));
                    }
                    if (!any) System.out.println("(none)");
                    System.out.println();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to display user", e);
        }
    }

    // ---------- Books ----------
    public void addBookInteractive() {
        sc.nextLine();
        System.out.print("Enter Book Title: ");
        String title = sc.nextLine();
        System.out.print("Enter Author: ");
        String author = sc.nextLine();
        System.out.print("Enter Quantity: ");
        int qty = nextIntSafe();

        addOrIncreaseBook(title, author, qty);
    }

    // Add new or increase existing quantity atomically
    public void addOrIncreaseBook(String title, String author, int addQty) {
        final String upsert = """
            INSERT INTO books(title, author, quantity, availability)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity),
                                    availability = availability + VALUES(availability)
        """;
        try (PreparedStatement pst = con.prepareStatement(upsert)) {
            pst.setString(1, title);
            pst.setString(2, author);
            pst.setInt(3, addQty);
            pst.setInt(4, addQty);
            pst.executeUpdate();
            System.out.println("📘 Book added/increased successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add/increase book", e);
        }
    }

    public void removeBookInteractive() {
        sc.nextLine();
        System.out.print("Enter Book Title: ");
        String title = sc.nextLine();
        System.out.print("Enter Author: ");
        String author = sc.nextLine();

        removeBook(title, author);
    }

    public void removeBook(String title, String author) {
        final String find = "SELECT book_id FROM books WHERE title=? AND author=?";
        final String del  = "DELETE FROM books WHERE book_id=?";
        try (PreparedStatement f = con.prepareStatement(find)) {
            f.setString(1, title);
            f.setString(2, author);
            try (ResultSet rs = f.executeQuery()) {
                if (!rs.next()) throw new BookNotExistsException("Book not found to remove.");
                int id = rs.getInt(1);

                try (PreparedStatement d = con.prepareStatement(del)) {
                    d.setInt(1, id);
                    d.executeUpdate();
                }
            }
            System.out.println("🗑️ Book removed.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove book", e);
        }
    }

    public void displayBooks() {
        final String sql = "SELECT title, author, quantity, availability FROM books ORDER BY author, title";
        try (PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            System.out.println("\nLibrary Books:");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("- %s by %s\n", rs.getString("title"), rs.getString("author"));
                System.out.printf("  Available: %d | Total: %d\n\n",
                        rs.getInt("availability"), rs.getInt("quantity"));
            }
            if (!any) System.out.println("(no books)\n");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to display books", e);
        }
    }

    public void findByAuthorInteractive() {
        sc.nextLine();
        System.out.print("Enter Author: ");
        String author = sc.nextLine();
        findByAuthor(author);
    }

    public void findByAuthor(String author) {
        final String sql = "SELECT title, author, quantity, availability FROM books WHERE author=? ORDER BY title";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, author);
            try (ResultSet rs = pst.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("Author: %s | Book: %s\n", rs.getString("author"), rs.getString("title"));
                    System.out.printf("Quantity: %d | Available: %d\n\n",
                            rs.getInt("quantity"), rs.getInt("availability"));
                }
                if (!any) throw new BookNotExistsException("No book is available by this author.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find by author", e);
        }
    }

    public void findAuthorByTitleInteractive() {
        sc.nextLine();
        System.out.print("Enter Book Title: ");
        String title = sc.nextLine();
        findAuthorByTitle(title);
    }

    public void findAuthorByTitle(String title) {
        final String sql = "SELECT title, author, quantity, availability FROM books WHERE title=?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, title);
            try (ResultSet rs = pst.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("Book: %s | Author: %s\n", rs.getString("title"), rs.getString("author"));
                    System.out.printf("Quantity: %d | Available: %d\n\n",
                            rs.getInt("quantity"), rs.getInt("availability"));
                }
                if (!any) throw new BookNotExistsException("No author found with this book title.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find author by title", e);
        }
    }

    // ---------- Availability ----------
    private int getBookIdAndAvailability(String title, String author, int[] outAvail) throws SQLException {
        final String sql = "SELECT book_id, availability FROM books WHERE title=? AND author=?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, title);
            pst.setString(2, author);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) throw new BookNotExistsException("Not available in our library.");
                int id = rs.getInt("book_id");
                outAvail[0] = rs.getInt("availability");
                return id;
            }
        }
    }

    // ---------- Borrow / Return (transactional) ----------
    public void borrowInteractive() {
        System.out.print("Enter User ID: ");
        int userId = nextIntSafe();
        sc.nextLine();
        System.out.print("Enter Book Title: ");
        String title = sc.nextLine();
        System.out.print("Enter Author: ");
        String author = sc.nextLine();
        borrowBook(userId, title, author);
    }

    public void borrowBook(int userId, String title, String author) {
        final String userSql = "SELECT borrow_limit, total_borrowed FROM users WHERE user_id=?";
        final String insertBorrow = "INSERT INTO borrowed_books (user_id, book_id) VALUES (?, ?)";
        final String updateBook = "UPDATE books SET availability = availability - 1 WHERE book_id=? AND availability > 0";
        final String updateUser = "UPDATE users SET total_borrowed = total_borrowed + 1 WHERE user_id=?";

        try {
            con.setAutoCommit(false); // begin

            // Check user
            int limit, total;
            try (PreparedStatement upst = con.prepareStatement(userSql)) {
                upst.setInt(1, userId);
                try (ResultSet urs = upst.executeQuery()) {
                    if (!urs.next()) throw new UserIDNotAvailableException("User not recognized.");
                    limit = urs.getInt("borrow_limit");
                    total = urs.getInt("total_borrowed");
                }
            }
            if (total >= limit) throw new BorrowedLimitExceedException("Return previous books to borrow another.");

            // Check book availability
            int[] avail = new int[1];
            int bookId = getBookIdAndAvailability(title, author, avail);
            if (avail[0] <= 0) throw new BookNotAvailableException("Book is not available to borrow.");

            // Insert borrow
            try (PreparedStatement ib = con.prepareStatement(insertBorrow)) {
                ib.setInt(1, userId);
                ib.setInt(2, bookId);
                ib.executeUpdate();
            }

            // Update availability
            try (PreparedStatement ub = con.prepareStatement(updateBook)) {
                ub.setInt(1, bookId);
                int rows = ub.executeUpdate();
                if (rows == 0) throw new BookNotAvailableException("Race condition: availability just became 0.");
            }

            // Update user's total_borrowed
            try (PreparedStatement uu = con.prepareStatement(updateUser)) {
                uu.setInt(1, userId);
                uu.executeUpdate();
            }

            con.commit();
            System.out.println(" Book issued successfully.\n");
        } catch (Exception e) {
            try { con.rollback(); } catch (SQLException ignored) {}
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Borrow failed", e);
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public void returnInteractive() {
        System.out.print("Enter User ID: ");
        int userId = nextIntSafe();
        sc.nextLine();
        System.out.print("Enter Book Title: ");
        String title = sc.nextLine();
        System.out.print("Enter Author: ");
        String author = sc.nextLine();
        returnBook(userId, title, author);
    }

    public void returnBook(int userId, String title, String author) {
        final String findActive = """
            SELECT bb.borrow_id, b.book_id
            FROM borrowed_books bb
            JOIN books b ON b.book_id = bb.book_id
            WHERE bb.user_id=? AND b.title=? AND b.author=? AND bb.returned_at IS NULL
            ORDER BY bb.borrowed_at ASC LIMIT 1
        """;
        final String markReturned = "UPDATE borrowed_books SET returned_at = CURRENT_TIMESTAMP WHERE borrow_id=?";
        final String incAvail = "UPDATE books SET availability = availability + 1 WHERE book_id=?";
        final String decUser = "UPDATE users SET total_borrowed = GREATEST(total_borrowed - 1, 0) WHERE user_id=?";

        try {
            con.setAutoCommit(false);

            int borrowId, bookId;
            try (PreparedStatement f = con.prepareStatement(findActive)) {
                f.setInt(1, userId);
                f.setString(2, title);
                f.setString(3, author);
                try (ResultSet rs = f.executeQuery()) {
                    if (!rs.next()) throw new BookNotExistsException("This entry is not currently borrowed by the user.");
                    borrowId = rs.getInt("borrow_id");
                    bookId = rs.getInt("book_id");
                }
            }

            try (PreparedStatement mr = con.prepareStatement(markReturned)) {
                mr.setInt(1, borrowId);
                mr.executeUpdate();
            }
            try (PreparedStatement ia = con.prepareStatement(incAvail)) {
                ia.setInt(1, bookId);
                ia.executeUpdate();
            }
            try (PreparedStatement du = con.prepareStatement(decUser)) {
                du.setInt(1, userId);
                du.executeUpdate();
            }

            con.commit();
            System.out.println(" Book returned successfully.\n");
        } catch (Exception e) {
            try { con.rollback(); } catch (SQLException ignored) {}
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Return failed", e);
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    // ---------- Helpers ----------
    private int nextIntSafe() {
        while (!sc.hasNextInt()) {
            System.out.print("Please enter a number: ");
            sc.next();
        }
        return sc.nextInt();
    }
}

// ===================== CLI (menu) =====================
public class library {
    public static void main(String[] args) {
        try (Connection con = Db.getConnection()) {
            System.out.println(" Connected to Database Successfully!");
            LibraryService service = new LibraryService(con);
            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.println("\nChoose Options -->");
                System.out.println("1 : Add/Increase Book");
                System.out.println("2 : Remove Book");
                System.out.println("3 : Add/Update User");
                System.out.println("4 : Display All Books");
                System.out.println("5 : Display All Users");
                System.out.println("6 : Display Individual User");
                System.out.println("7 : Find Books by Author");
                System.out.println("8 : Find Author(s) by Book Title");
                System.out.println("9 : Borrow a Book");
                System.out.println("10: Return a Book");
                System.out.println("11: Exit");
                System.out.print("Choice: ");

                int choice;
                while (!sc.hasNextInt()) { System.out.print("Enter a number: "); sc.next(); }
                choice = sc.nextInt();

                try {
                    switch (choice) {
                        case 1 -> service.addBookInteractive();
                        case 2 -> service.removeBookInteractive();
                        case 3 -> service.addUserInteractive();
                        case 4 -> service.displayBooks();
                        case 5 -> service.displayUsers();
                        case 6 -> service.displayUserInteractive();
                        case 7 -> service.findByAuthorInteractive();
                        case 8 -> service.findAuthorByTitleInteractive();
                        case 9 -> service.borrowInteractive();
                        case 10 -> service.returnInteractive();
                        case 11 -> {
                            System.out.println("👋 Goodbye");
                            return;
                        }
                        default -> System.out.println("Invalid choice.");
                    }
                } catch (RuntimeException ex) {
                    // Show friendly rule-based errors
                    System.out.println(" " + ex.getMessage());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println(" DB connection failed: " + e.getMessage());
        }
    }
}
