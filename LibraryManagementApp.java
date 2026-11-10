import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A single-file console application demonstrating core OOP concepts through
 * a basic library management system.
 */
public class LibraryManagementApp {

    public static void main(String[] args) {
        Library library = new Library();

        // Scanner reads the user's choices and values from the console.
        try (Scanner scanner = new Scanner(System.in)) {
            runMenu(scanner, library);
        }
    }

    /**
     * Runs the interactive menu until the user chooses to exit.
     */
    private static void runMenu(Scanner scanner, Library library) {
        boolean isRunning = true;

        System.out.println("Welcome to the Library Management System!");

        // The loop keeps the application active until option 5 changes the flag.
        while (isRunning) {
            printMenu();
            int choice = readInt(scanner, "Enter your choice: ");

            switch (choice) {
                case 1 -> addBook(scanner, library);
                case 2 -> library.displayAvailableBooks();
                case 3 -> borrowBook(scanner, library);
                case 4 -> returnBook(scanner, library);
                case 5 -> {
                    isRunning = false;
                    System.out.println("Thank you for using the Library Management System. Goodbye!");
                }
                default -> System.out.println("Invalid choice. Please select an option from 1 to 5.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n----- Library Menu -----");
        System.out.println("1. Add Book");
        System.out.println("2. View Available Books");
        System.out.println("3. Borrow Book");
        System.out.println("4. Return Book");
        System.out.println("5. Exit");
    }

    private static void addBook(Scanner scanner, Library library) {
        String bookId = readRequiredText(scanner, "Enter book ID: ");
        String title = readRequiredText(scanner, "Enter book title: ");
        String author = readRequiredText(scanner, "Enter author name: ");

        if (library.addBook(new Book(bookId, title, author))) {
            System.out.println("Book added successfully.");
        } else {
            System.out.println("A book with that ID already exists.");
        }
    }

    private static void borrowBook(Scanner scanner, Library library) {
        String bookId = readRequiredText(scanner, "Enter book ID to borrow: ");
        Member member = findOrRegisterMember(scanner, library);

        if (library.borrowBook(bookId, member)) {
            System.out.println("Book borrowed successfully.");
        }
    }

    private static void returnBook(Scanner scanner, Library library) {
        String bookId = readRequiredText(scanner, "Enter book ID to return: ");
        String userId = readRequiredText(scanner, "Enter your member ID: ");

        if (library.returnBook(bookId, userId)) {
            System.out.println("Book returned successfully.");
        }
    }

    private static Member findOrRegisterMember(Scanner scanner, Library library) {
        String userId = readRequiredText(scanner, "Enter your member ID: ");
        Member member = library.findMember(userId);

        if (member != null) {
            return member;
        }

        String name = readRequiredText(scanner, "New member. Enter your name: ");
        member = new Member(userId, name);
        library.addMember(member);
        System.out.println("Member registered successfully.");
        return member;
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            String input = readRequiredText(scanner, prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static String readRequiredText(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This value cannot be empty.");
        }
    }
}

/**
 * Encapsulates a book's state and validates all values through its setters.
 */
class Book {
    private String bookId;
    private String title;
    private String author;
    private boolean isIssued;
    private String issuedToUserId;

    public Book(String bookId, String title, String author) {
        validateText(bookId, "Book ID");
        validateText(title, "Title");
        validateText(author, "Author");
        this.bookId = bookId.trim();
        this.title = title.trim();
        this.author = author.trim();
        this.isIssued = false;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        validateText(bookId, "Book ID");
        this.bookId = bookId.trim();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        validateText(title, "Title");
        this.title = title.trim();
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        validateText(author, "Author");
        this.author = author.trim();
    }

    public boolean isIssued() {
        return isIssued;
    }

    public String getIssuedToUserId() {
        return issuedToUserId;
    }

    public void issueTo(String userId) {
        if (isIssued) {
            throw new IllegalStateException("The book is already issued.");
        }
        validateText(userId, "User ID");
        issuedToUserId = userId.trim();
        isIssued = true;
    }

    public void returnBook(String userId) {
        if (!isIssued || !issuedToUserId.equals(userId)) {
            throw new IllegalStateException("This book is not borrowed by that member.");
        }
        isIssued = false;
        issuedToUserId = null;
    }

    @Override
    public String toString() {
        return String.format("ID: %s | Title: %s | Author: %s", bookId, title, author);
    }

    private static void validateText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }
}

/**
 * Abstraction for users of the library. Subclasses define their own policy.
 */
abstract class User {
    private final String userId;
    private String name;

    protected User(String userId, String name) {
        validateText(userId, "User ID");
        validateText(name, "Name");
        this.userId = userId.trim();
        this.name = name.trim();
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public final void setName(String name) {
        validateText(name, "Name");
        this.name = name.trim();
    }

    public abstract int getBorrowingLimit();

    public abstract void displayUserDetails();

    private static void validateText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }
}

/**
 * A library member inherits common user data and provides member-specific
 * borrowing behavior, demonstrating inheritance and method overriding.
 */
class Member extends User {
    private final List<String> borrowedBookIds = new ArrayList<>();

    public Member(String userId, String name) {
        super(userId, name);
    }

    @Override
    public int getBorrowingLimit() {
        return 3;
    }

    public boolean canBorrow() {
        return borrowedBookIds.size() < getBorrowingLimit();
    }

    public void addBorrowedBook(String bookId) {
        if (!canBorrow()) {
            throw new IllegalStateException("Borrowing limit reached.");
        }
        borrowedBookIds.add(bookId);
    }

    public void removeBorrowedBook(String bookId) {
        if (!borrowedBookIds.remove(bookId)) {
            throw new IllegalStateException("This member did not borrow that book.");
        }
    }

    @Override
    public void displayUserDetails() {
        System.out.printf("Member ID: %s | Name: %s | Books borrowed: %d/%d%n",
                getUserId(), getName(), borrowedBookIds.size(), getBorrowingLimit());
    }
}

/**
 * Coordinates books and users using ArrayList collections.
 */
class Library {
    private final List<Book> books = new ArrayList<>();
    private final List<Member> members = new ArrayList<>();

    public boolean addBook(Book book) {
        if (book == null || findBook(book.getBookId()) != null) {
            return false;
        }
        books.add(book);
        return true;
    }

    public void addMember(Member member) {
        if (member != null && findMember(member.getUserId()) == null) {
            members.add(member);
        }
    }

    public boolean borrowBook(String bookId, Member member) {
        Book book = findBook(bookId);
        if (book == null) {
            System.out.println("Book not found.");
            return false;
        }
        if (book.isIssued()) {
            System.out.println("That book is already issued.");
            return false;
        }
        if (!member.canBorrow()) {
            System.out.println("Borrowing limit reached. You may borrow up to "
                    + member.getBorrowingLimit() + " books.");
            return false;
        }

        book.issueTo(member.getUserId());
        member.addBorrowedBook(book.getBookId());
        return true;
    }

    public boolean returnBook(String bookId, String userId) {
        Book book = findBook(bookId);
        Member member = findMember(userId);

        if (book == null) {
            System.out.println("Book not found.");
            return false;
        }
        if (member == null) {
            System.out.println("Member not found.");
            return false;
        }

        try {
            book.returnBook(userId);
            member.removeBorrowedBook(book.getBookId());
            return true;
        } catch (IllegalStateException exception) {
            System.out.println(exception.getMessage());
            return false;
        }
    }

    public void displayAvailableBooks() {
        boolean foundAvailableBook = false;
        System.out.println("\n----- Available Books -----");

        for (Book book : books) {
            if (!book.isIssued()) {
                System.out.println(book);
                foundAvailableBook = true;
            }
        }

        if (!foundAvailableBook) {
            System.out.println("No books are currently available.");
        }
    }

    public Book findBook(String bookId) {
        for (Book book : books) {
            if (book.getBookId().equalsIgnoreCase(bookId)) {
                return book;
            }
        }
        return null;
    }

    public Member findMember(String userId) {
        for (Member member : members) {
            if (member.getUserId().equalsIgnoreCase(userId)) {
                return member;
            }
        }
        return null;
    }
}
