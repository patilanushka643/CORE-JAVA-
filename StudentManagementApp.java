import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Student Management System using JDBC and an MVC-style design.
 *
 * MySQL setup:
 * CREATE DATABASE student_management;
 * USE student_management;
 * CREATE TABLE students (
 *     id INT PRIMARY KEY AUTO_INCREMENT,
 *     name VARCHAR(100) NOT NULL,
 *     email VARCHAR(150) NOT NULL UNIQUE,
 *     department VARCHAR(100) NOT NULL,
 *     gpa DECIMAL(3,2) NOT NULL CHECK (gpa BETWEEN 0.00 AND 4.00)
 * );
 * INSERT INTO students (name, email, department, gpa)
 * VALUES ('Asha Patel', 'asha@example.com', 'Computer Science', 3.80);
 *
 * Required runtime dependency:
 * mysql-connector-j-*.jar
 */
public class StudentManagementApp {

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            System.out.println("MySQL JDBC driver not found.");
            System.out.println("Add mysql-connector-j-*.jar to the VS Code classpath.");
            return;
        }

        StudentDAO studentDAO = new StudentDAOImpl(DatabaseConnection.getInstance());
        StudentController controller = new StudentController(studentDAO);

        try (Scanner scanner = new Scanner(System.in)) {
            new StudentView(scanner, controller).start();
        }
    }
}

/**
 * Model object representing one database record.
 */
class Student {
    private int id;
    private String name;
    private String email;
    private String department;
    private double gpa;

    public Student(String name, String email, String department, double gpa) {
        this(0, name, email, department, gpa);
    }

    public Student(int id, String name, String email, String department, double gpa) {
        setId(id);
        setName(name);
        setEmail(email);
        setDepartment(department);
        setGpa(gpa);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("Student ID cannot be negative.");
        }
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "Name");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        String value = requireText(email, "Email");
        if (!value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        this.email = value;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = requireText(department, "Department");
    }

    public double getGpa() {
        return gpa;
    }

    public void setGpa(double gpa) {
        if (!Double.isFinite(gpa) || gpa < 0.0 || gpa > 4.0) {
            throw new IllegalArgumentException("GPA must be between 0.00 and 4.00.");
        }
        this.gpa = gpa;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }
}

/**
 * DAO contract for persistence operations.
 */
interface StudentDAO {
    Student add(Student student) throws SQLException;

    List<Student> findAll() throws SQLException;

    boolean update(Student student) throws SQLException;

    boolean deleteById(int id) throws SQLException;

    Student findById(int id) throws SQLException;

    List<Student> findByName(String name) throws SQLException;
}

/**
 * Singleton configuration/factory for MySQL connections.
 *
 * Each call creates a fresh connection so DAO try-with-resources blocks can
 * safely close it without leaving a stale connection in the singleton.
 */
final class DatabaseConnection {
    private static final DatabaseConnection INSTANCE = new DatabaseConnection();
    private final String url;
    private final String username;
    private final String password;

    private DatabaseConnection() {
        url = environmentOrDefault("STUDENT_DB_URL",
                "jdbc:mysql://localhost:3306/student_management"
                        + "?useSSL=false&serverTimezone=UTC");
        username = environmentOrDefault("STUDENT_DB_USER", "root");
        password = System.getenv().getOrDefault("STUDENT_DB_PASSWORD", "");
    }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

/**
 * JDBC implementation of the StudentDAO contract.
 */
final class StudentDAOImpl implements StudentDAO {
    private static final String INSERT_SQL =
            "INSERT INTO students (name, email, department, gpa) VALUES (?, ?, ?, ?)";
    private static final String SELECT_ALL_SQL =
            "SELECT id, name, email, department, gpa FROM students ORDER BY id";
    private static final String UPDATE_SQL =
            "UPDATE students SET name = ?, email = ?, department = ?, gpa = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM students WHERE id = ?";
    private static final String SELECT_BY_ID_SQL =
            "SELECT id, name, email, department, gpa FROM students WHERE id = ?";
    private static final String SELECT_BY_NAME_SQL =
            "SELECT id, name, email, department, gpa FROM students WHERE name LIKE ? ORDER BY name";

    private final DatabaseConnection databaseConnection;

    StudentDAOImpl(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public Student add(Student student) throws SQLException {
        try (Connection connection = databaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        INSERT_SQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            setStudentParameters(statement, student);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    student.setId(keys.getInt(1));
                }
            }
            return student;
        }
    }

    @Override
    public List<Student> findAll() throws SQLException {
        List<Student> students = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                students.add(mapStudent(results));
            }
        }
        return students;
    }

    @Override
    public boolean update(Student student) throws SQLException {
        try (Connection connection = databaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            setStudentParameters(statement, student);
            statement.setInt(5, student.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean deleteById(int id) throws SQLException {
        try (Connection connection = databaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public Student findById(int id) throws SQLException {
        try (Connection connection = databaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setInt(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapStudent(results) : null;
            }
        }
    }

    @Override
    public List<Student> findByName(String name) throws SQLException {
        List<Student> students = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_BY_NAME_SQL)) {
            statement.setString(1, "%" + name + "%");
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    students.add(mapStudent(results));
                }
            }
        }
        return students;
    }

    private static void setStudentParameters(PreparedStatement statement, Student student)
            throws SQLException {
        statement.setString(1, student.getName());
        statement.setString(2, student.getEmail());
        statement.setString(3, student.getDepartment());
        statement.setDouble(4, student.getGpa());
    }

    private static Student mapStudent(ResultSet results) throws SQLException {
        return new Student(
                results.getInt("id"),
                results.getString("name"),
                results.getString("email"),
                results.getString("department"),
                results.getDouble("gpa"));
    }
}

/**
 * Application service/controller: validates input and translates persistence
 * failures into user-facing results.
 */
final class StudentController {
    private final StudentDAO studentDAO;

    StudentController(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    public OperationResult add(String name, String email, String department, double gpa) {
        try {
            Student student = studentDAO.add(new Student(name, email, department, gpa));
            return OperationResult.success("Student added with ID " + student.getId() + ".");
        } catch (IllegalArgumentException exception) {
            return OperationResult.failure(exception.getMessage());
        } catch (SQLException exception) {
            return databaseFailure(exception);
        }
    }

    public QueryResult all() {
        try {
            return QueryResult.success(studentDAO.findAll());
        } catch (SQLException exception) {
            return QueryResult.failure(databaseFailure(exception).message());
        }
    }

    public OperationResult update(int id, String name, String email, String department, double gpa) {
        try {
            Student student = new Student(id, name, email, department, gpa);
            return studentDAO.update(student)
                    ? OperationResult.success("Student updated successfully.")
                    : OperationResult.failure("Student ID not found.");
        } catch (IllegalArgumentException exception) {
            return OperationResult.failure(exception.getMessage());
        } catch (SQLException exception) {
            return databaseFailure(exception);
        }
    }

    public OperationResult delete(int id) {
        try {
            return studentDAO.deleteById(id)
                    ? OperationResult.success("Student deleted successfully.")
                    : OperationResult.failure("Student ID not found.");
        } catch (SQLException exception) {
            return databaseFailure(exception);
        }
    }

    public QueryResult searchById(int id) {
        try {
            Student student = studentDAO.findById(id);
            return student == null
                    ? QueryResult.failure("Student ID not found.")
                    : QueryResult.success(List.of(student));
        } catch (SQLException exception) {
            return QueryResult.failure(databaseFailure(exception).message());
        }
    }

    public QueryResult searchByName(String name) {
        try {
            return QueryResult.success(studentDAO.findByName(name));
        } catch (SQLException exception) {
            return QueryResult.failure(databaseFailure(exception).message());
        }
    }

    private static OperationResult databaseFailure(SQLException exception) {
        return OperationResult.failure("Database error: " + exception.getMessage());
    }
}

/**
 * Console view. It owns presentation and input, not business or SQL logic.
 */
final class StudentView {
    private final Scanner scanner;
    private final StudentController controller;

    StudentView(Scanner scanner, StudentController controller) {
        this.scanner = scanner;
        this.controller = controller;
    }

    void start() {
        boolean running = true;
        while (running) {
            printMenu();
            switch (readInt("Choose an option: ")) {
                case 1 -> addStudent();
                case 2 -> display(controller.all());
                case 3 -> updateStudent();
                case 4 -> deleteStudent();
                case 5 -> searchStudent();
                case 6 -> {
                    running = false;
                    System.out.println("Goodbye.");
                }
                default -> System.out.println("Invalid option. Choose 1-6.");
            }
        }
    }

    private void addStudent() {
        OperationResult result = controller.add(
                readText("Name: "),
                readText("Email: "),
                readText("Department: "),
                readGpa("GPA (0.00-4.00): "));
        printResult(result);
    }

    private void updateStudent() {
        int id = readPositiveId();
        OperationResult result = controller.update(
                id,
                readText("New name: "),
                readText("New email: "),
                readText("New department: "),
                readGpa("New GPA (0.00-4.00): "));
        printResult(result);
    }

    private void deleteStudent() {
        printResult(controller.delete(readPositiveId()));
    }

    private void searchStudent() {
        int option = readInt("Search by 1. ID or 2. Name: ");
        if (option == 1) {
            display(controller.searchById(readPositiveId()));
        } else if (option == 2) {
            display(controller.searchByName(readText("Name contains: ")));
        } else {
            System.out.println("Invalid search option.");
        }
    }

    private void display(QueryResult result) {
        if (!result.success()) {
            System.out.println(result.message());
            return;
        }
        if (result.students().isEmpty()) {
            System.out.println("No students found.");
            return;
        }
        System.out.printf("%-5s %-24s %-30s %-20s %-5s%n",
                "ID", "NAME", "EMAIL", "DEPARTMENT", "GPA");
        System.out.println("-".repeat(90));
        for (Student student : result.students()) {
            System.out.printf("%-5d %-24s %-30s %-20s %.2f%n",
                    student.getId(), student.getName(), student.getEmail(),
                    student.getDepartment(), student.getGpa());
        }
    }

    private void printResult(OperationResult result) {
        System.out.println(result.message());
    }

    private int readPositiveId() {
        while (true) {
            int id = readInt("Student ID: ");
            if (id > 0) {
                return id;
            }
            System.out.println("ID must be greater than zero.");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readText(prompt));
            } catch (NumberFormatException exception) {
                System.out.println("Enter a valid whole number.");
            }
        }
    }

    private double readGpa(String prompt) {
        while (true) {
            try {
                double gpa = Double.parseDouble(readText(prompt));
                if (Double.isFinite(gpa) && gpa >= 0.0 && gpa <= 4.0) {
                    return gpa;
                }
            } catch (NumberFormatException exception) {
                // The shared validation message below keeps the prompt concise.
            }
            System.out.println("GPA must be a number from 0.00 to 4.00.");
        }
    }

    private String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This value cannot be empty.");
        }
    }

    private static void printMenu() {
        System.out.println("\n----- Student Management -----");
        System.out.println("1. Add Student");
        System.out.println("2. View All Students");
        System.out.println("3. Update Student");
        System.out.println("4. Delete Student");
        System.out.println("5. Search Student");
        System.out.println("6. Exit");
    }
}

record OperationResult(boolean success, String message) {
    static OperationResult success(String message) {
        return new OperationResult(true, message);
    }

    static OperationResult failure(String message) {
        return new OperationResult(false, message);
    }
}

record QueryResult(boolean success, String message, List<Student> students) {
    static QueryResult success(List<Student> students) {
        return new QueryResult(true, "", students);
    }

    static QueryResult failure(String message) {
        return new QueryResult(false, message, List.of());
    }
}
