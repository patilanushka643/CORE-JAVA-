import java.util.Scanner;

/**
 * A basic console application for managing a bank account.
 */
public class BankManagementApp {

    public static void main(String[] args) {
        double balance = 1000.00;
        boolean isRunning = true;

        // Scanner reads text entered by the user from the keyboard.
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to the Bank Management Console App!");

        // The while loop keeps showing the menu until the user chooses Exit.
        while (isRunning) {
            System.out.println("\n----- Bank Menu -----");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");

            String choiceInput = scanner.nextLine();
            int choice;

            // Validate the menu choice before using it in the switch statement.
            try {
                choice = Integer.parseInt(choiceInput);
            } catch (NumberFormatException exception) {
                System.out.println("Invalid choice. Please enter a number from 1 to 4.");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.printf("Current balance: $%.2f%n", balance);
                    break;

                case 2:
                    System.out.print("Enter the amount to deposit: $");
                    Double depositAmount = readAmount(scanner);

                    if (depositAmount == null) {
                        continue;
                    }

                    // A deposit must be greater than zero.
                    if (depositAmount > 0) {
                        balance += depositAmount;
                        System.out.printf("Deposit successful. New balance: $%.2f%n", balance);
                    } else {
                        System.out.println("Deposit amount must be positive.");
                    }
                    break;

                case 3:
                    System.out.print("Enter the amount to withdraw: $");
                    Double withdrawalAmount = readAmount(scanner);

                    if (withdrawalAmount == null) {
                        continue;
                    }

                    // Check both that the withdrawal is positive and that it will not overdraw the account.
                    if (withdrawalAmount <= 0) {
                        System.out.println("Withdrawal amount must be positive.");
                    } else if (withdrawalAmount > balance) {
                        System.out.println("Insufficient balance. Withdrawal cancelled.");
                    } else {
                        balance -= withdrawalAmount;
                        System.out.printf("Withdrawal successful. New balance: $%.2f%n", balance);
                    }
                    break;

                case 4:
                    isRunning = false;
                    System.out.println("Thank you for using the Bank Management Console App. Goodbye!");
                    break;

                default:
                    System.out.println("Invalid choice. Please select an option from 1 to 4.");
            }
        }

        // Close Scanner after the loop so the input resource is released cleanly.
        scanner.close();
    }

    /**
     * Reads and validates a monetary amount entered by the user.
     *
     * @param scanner Scanner used to read console input
     * @return the entered amount, or null when the input is not a valid number
     */
    private static Double readAmount(Scanner scanner) {
        String amountInput = scanner.nextLine();

        try {
            return Double.parseDouble(amountInput);
        } catch (NumberFormatException exception) {
            System.out.println("Invalid amount. Please enter a numeric value.");
            return null;
        }
    }
}
