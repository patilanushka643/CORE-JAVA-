import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Console application for managing employee records in memory and in a text file.
 */
public class EmployeeManagementApp {
    private static final String DEFAULT_FILE_NAME = "employees.txt";

    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>();
        try (Scanner scanner = new Scanner(System.in)) {
            runMenu(scanner, employees);
        } finally {
            System.out.println("Application closed.");
        }
    }

    private static void runMenu(Scanner scanner, List<Employee> employees) {
        boolean running = true;

        System.out.println("Welcome to the Employee Management System!");
        while (running) {
            printMenu();
            int choice = readInt(scanner, "Enter your choice: ");

            switch (choice) {
                case 1 -> addEmployee(scanner, employees);
                case 2 -> viewAllEmployees(employees);
                case 3 -> searchEmployee(scanner, employees);
                case 4 -> saveEmployees(scanner, employees);
                case 5 -> loadEmployees(scanner, employees);
                case 6 -> {
                    running = false;
                    System.out.println("Thank you for using the Employee Management System.");
                }
                default -> System.out.println("Invalid choice. Select an option from 1 to 6.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n----- Employee Menu -----");
        System.out.println("1. Add Employee");
        System.out.println("2. View All Employees");
        System.out.println("3. Search Employee by ID");
        System.out.println("4. Save Employees to File");
        System.out.println("5. Load Employees from File");
        System.out.println("6. Exit");
    }

    private static void addEmployee(Scanner scanner, List<Employee> employees) {
        int id = readPositiveInt(scanner, "Enter employee ID: ");
        if (findEmployeeById(employees, id) != null) {
            System.out.println("An employee with that ID already exists.");
            return;
        }

        String name = readRequiredText(scanner, "Enter employee name: ");
        String department = readRequiredText(scanner, "Enter department: ");
        double salary = readPositiveDouble(scanner, "Enter salary: ");

        employees.add(new Employee(id, name, department, salary));
        System.out.println("Employee added successfully.");
    }

    private static void viewAllEmployees(List<Employee> employees) {
        if (employees.isEmpty()) {
            System.out.println("No employee records found.");
            return;
        }

        System.out.println("\n----- All Employees -----");
        for (Employee employee : employees) {
            System.out.println(employee);
        }
    }

    private static void searchEmployee(Scanner scanner, List<Employee> employees) {
        int id = readPositiveInt(scanner, "Enter employee ID to search: ");
        Employee employee = findEmployeeById(employees, id);

        if (employee == null) {
            System.out.println("Employee not found.");
        } else {
            System.out.println("Employee found: " + employee);
        }
    }

    private static void saveEmployees(Scanner scanner, List<Employee> employees) {
        String fileName = readFileName(scanner);
        try (FileWriter fileWriter = new FileWriter(fileName);
                BufferedWriter writer = new BufferedWriter(fileWriter)) {
            for (Employee employee : employees) {
                writer.write(employee.toFileString());
                writer.newLine();
            }
            System.out.println("Employees saved successfully to " + fileName + ".");
        } catch (IOException exception) {
            System.out.println("Unable to save employees: " + exception.getMessage());
        }
    }

    private static void loadEmployees(Scanner scanner, List<Employee> employees) {
        String fileName = readFileName(scanner);
        List<Employee> loadedEmployees = new ArrayList<>();

        try (FileReader fileReader = new FileReader(fileName);
                BufferedReader reader = new BufferedReader(fileReader)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    Employee employee = Employee.fromFileString(line);
                    if (findEmployeeById(loadedEmployees, employee.getId()) != null) {
                        System.out.println("Skipping duplicate ID on line " + lineNumber + ".");
                    } else {
                        loadedEmployees.add(employee);
                    }
                } catch (IllegalArgumentException exception) {
                    System.out.println("Skipping invalid record on line " + lineNumber
                            + ": " + exception.getMessage());
                }
            }

            employees.clear();
            employees.addAll(loadedEmployees);
            System.out.println("Loaded " + loadedEmployees.size() + " employee(s) from " + fileName + ".");
        } catch (FileNotFoundException exception) {
            System.out.println("File not found: " + fileName);
        } catch (IOException exception) {
            System.out.println("Unable to load employees: " + exception.getMessage());
        }
    }

    private static Employee findEmployeeById(List<Employee> employees, int id) {
        for (Employee employee : employees) {
            if (employee.getId() == id) {
                return employee;
            }
        }
        return null;
    }

    private static int readPositiveInt(Scanner scanner, String prompt) {
        while (true) {
            int value = readInt(scanner, prompt);
            if (value > 0) {
                return value;
            }
            System.out.println("Value must be greater than zero.");
        }
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static double readPositiveDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (Double.isFinite(value) && value > 0) {
                    return value;
                }
                System.out.println("Salary must be a positive finite number.");
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid numeric salary.");
            }
        }
    }

    private static String readRequiredText(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty() && value.indexOf('\t') < 0) {
                return value;
            }
            System.out.println("Value cannot be empty or contain tab characters.");
        }
    }

    private static String readFileName(Scanner scanner) {
        System.out.print("Enter file name (press Enter for " + DEFAULT_FILE_NAME + "): ");
        String fileName = scanner.nextLine().trim();
        return fileName.isEmpty() ? DEFAULT_FILE_NAME : fileName;
    }

}

/**
 * Immutable identity and validated mutable employee details.
 */
class Employee {
    private int id;
    private String name;
    private String department;
    private double salary;

    public Employee(int id, String name, String department, double salary) {
        validateId(id);
        validateText(name, "Name");
        validateText(department, "Department");
        validateSalary(salary);
        this.id = id;
        this.name = name.trim();
        this.department = department.trim();
        this.salary = salary;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        validateId(id);
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        validateText(name, "Name");
        this.name = name.trim();
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        validateText(department, "Department");
        this.department = department.trim();
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        validateSalary(salary);
        this.salary = salary;
    }

    /**
     * Uses tab-separated values so names and departments can contain spaces.
     */
    public String toFileString() {
        return id + "\t" + name + "\t" + department + "\t" + salary;
    }

    public static Employee fromFileString(String line) {
        String[] fields = line.split("\t", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("Expected four tab-separated fields.");
        }

        try {
            return new Employee(
                    Integer.parseInt(fields[0].trim()),
                    fields[1],
                    fields[2],
                    Double.parseDouble(fields[3].trim()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid numeric field.");
        }
    }

    @Override
    public String toString() {
        return String.format("ID: %d | Name: %s | Department: %s | Salary: %.2f",
                id, name, department, salary);
    }

    private static void validateId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be greater than zero.");
        }
    }

    private static void validateText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty() || value.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(fieldName
                    + " cannot be empty or contain tab characters.");
        }
    }

    private static void validateSalary(double salary) {
        if (!Double.isFinite(salary) || salary < 0) {
            throw new IllegalArgumentException("Salary must be a non-negative finite number.");
        }
    }
}
