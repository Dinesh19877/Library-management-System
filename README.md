Library Management System
A simple, command-line based Library Management System built with Java and MySQL. This application provides core functionalities for managing books, users, and the borrowing/returning process through a console interface.
Features
Book Management
Add a new book or increase the quantity of an existing book.
Completely remove a book from the library.
View a list of all books with their availability.
User Management
Add a new user or update an existing user's details.
View a list of all registered users.
View detailed information for a specific user, including their currently borrowed books.
Borrowing & Returning
Issue a book to a user, with checks for borrow limits and book availability.
Process the return of a borrowed book.
Search & Discovery
Find all books written by a specific author.
Find the author(s) of a specific book title.
Technologies Used
Language: Java
Database: MySQL
API: Java Database Connectivity (JDBC)
Prerequisites
Before you begin, ensure you have the following installed on your system:
Java Development Kit (JDK): Version 11 or newer (to support text blocks """).
MySQL Server: The database used to store all library data.
MySQL Connector/J: The official JDBC driver for MySQL. You will need to download the JAR file.
Setup and Installation
Follow these steps to get the application running on your local machine.
1. Database Setup
First, you need to create the database and the required tables.
Open your MySQL client (e.g., MySQL Command Line, MySQL Workbench).
Create a new database named librarydb.
code
SQL
CREATE DATABASE librarydb;
Use the new database.
code
SQL
USE librarydb;
Run the following SQL scripts to create the necessary tables:
users Table:
code
SQL
CREATE TABLE users (
    user_id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    borrow_limit INT NOT NULL DEFAULT 5,
    total_borrowed INT NOT NULL DEFAULT 0
);
books Table:
code
SQL
CREATE TABLE books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    availability INT NOT NULL,
    UNIQUE KEY uk_title_author (title, author)
);
borrowed_books Table:
code
SQL
CREATE TABLE borrowed_books (
    borrow_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    borrowed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    returned_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE
);
2. Configure DB Connection
Open the library.java file and navigate to the Db class. Update the USER and PASS constants with your MySQL username and password.
code
Java
// ===================== DB Connection Manager =====================
class Db {
    private static final String URL  = "jdbc:mysql://localhost:3306/librarydb?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "your_mysql_username"; // <-- CHANGE THIS
    private static final String PASS = "your_mysql_password"; // <-- CHANGE THIS
    // ...
}
3. Compile and Run
Place the downloaded MySQL Connector/J JAR file in the same directory as your library.java file.
Open a terminal or command prompt in that directory.
Compile the Java code. Be sure to include the JDBC driver in the classpath.
On Windows:
code
Bash
javac -cp .;mysql-connector-j-8.x.x.jar library.java
On macOS/Linux:
code
Bash
javac -cp .:mysql-connector-j-8.x.x.jar library.java
(Note: Replace mysql-connector-j-8.x.x.jar with the actual filename of your driver)
Run the application, again including the driver in the classpath.
On Windows:
code
Bash
java -cp .;mysql-connector-j-8.x.x.jar library
On macOS/Linux:
code
Bash
java -cp .:mysql-connector-j-8.x.x.jar library
How to Use
Once the application is running, you will be presented with a command-line menu. Enter the number corresponding to the action you wish to perform and follow the on-screen prompts.
code
Code
Connected to Database Successfully!

Choose Options -->
1 : Add/Increase Book
2 : Remove Book
3 : Add/Update User
4 : Display All Books
5 : Display All Users
6 : Display Individual User
7 : Find Books by Author
8 : Find Author(s) by Book Title
9 : Borrow a Book
10: Return a Book
11: Exit
Choice:
Any errors, such as a book not being available or a user exceeding their borrow limit, will be displayed directly in the console.
Code Overview
The application is structured into several logical components within a single file for simplicity:
library Class: The main class that contains the main method. It is responsible for displaying the user menu and handling user input.
LibraryService Class: The service layer that contains all the business logic. All database operations (SQL queries) are encapsulated within this class's methods. It handles everything from adding books to processing returns.
Db Class: A simple connection manager responsible for establishing and providing a singleton connection to the MySQL database.
Custom Exceptions: A set of custom RuntimeException classes (e.g., BookNotAvailableException, BorrowedLimitExceedException) are used to handle specific error conditions gracefully.
Models: The Book and User classes are defined as simple POJOs, though the current implementation primarily uses direct ResultSet handling instead of object mapping.
