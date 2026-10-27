import java.util.Scanner;

/**
 * MiniBank - console banking application (CEUE203 OOP mini project).
 * Practical 1: the menu shell. Each option prints a placeholder that later
 * labs replace with a real banking feature.
 */
public class MiniBank {

    public static void main(String[] args) {
        BankInfo bank = new BankInfo("MiniBank", "CHARUSAT Campus, Changa");
        printHeader(bank);

        Scanner scanner = new Scanner(System.in);
        MenuOption choice;
        do {
            printMenu();
            choice = readChoice(scanner);
            String message = switch (choice) {
                case OPEN_ACCOUNT -> "Open account - to be implemented in a later lab";
                case DEPOSIT -> "Deposit - to be implemented in a later lab";
                case WITHDRAW -> "Withdraw - to be implemented in a later lab";
                case TRANSFER -> "Transfer - to be implemented in a later lab";
                case WORKING_HOURS -> "Working hours: Mon-Fri 10:00 AM - 4:00 PM, Sat 10:00 AM - 1:00 PM";
                case EXIT -> "Thank you for banking with " + bank.name() + ". Goodbye!";
            };
            System.out.println(message);
        } while (choice != MenuOption.EXIT);
    }

    private static void printHeader(BankInfo bank) {
        System.out.println("==============================================");
        System.out.println("  " + bank.name() + "  |  " + bank.branch());
        System.out.println("==============================================");
    }

    private static void printMenu() {
        System.out.println();
        for (MenuOption option : MenuOption.values()) {
            System.out.println(option.getNumber() + ". " + option.getLabel());
        }
    }

    /** Keeps asking until the user types a number that is on the menu. */
    private static MenuOption readChoice(Scanner scanner) {
        int last = MenuOption.values().length;
        while (true) {
            System.out.print("Enter your choice (1-" + last + "): ");
            if (!scanner.hasNextLine()) {
                return MenuOption.EXIT;          // input closed (Ctrl+Z / end of a piped file)
            }
            String input = scanner.nextLine().trim();
            try {
                MenuOption option = MenuOption.fromNumber(Integer.parseInt(input));
                if (option != null) {
                    return option;
                }
            } catch (NumberFormatException notANumber) {
                // fall through to the error message below
            }
            System.out.println("Invalid choice '" + input + "'. Please enter a number from 1 to " + last + ".");
        }
    }
}
