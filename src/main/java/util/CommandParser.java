package util;

import model.Command;
import model.TransactionType;

/**
 * Turns a typed line such as "DEPOSIT AC0001 500" into a Command.
 * Every part is checked, so a Command that comes out of parse() is always valid.
 */
public final class CommandParser {

    private CommandParser() {
        // only static methods
    }

    /**
     * @throws IllegalArgumentException if the line does not have exactly three
     *         valid parts: TYPE ACCOUNT_NUMBER AMOUNT
     */
    public static Command parse(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("Empty command");
        }
        String[] parts = line.trim().split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expected 3 parts (TYPE ACCOUNT AMOUNT) but found "
                    + parts.length + " in \"" + line.trim() + "\"");
        }

        TransactionType type = parseType(parts[0]);
        String accountNumber = parts[1].toUpperCase();
        if (!Validator.isValidAccountNumber(accountNumber)) {
            throw new IllegalArgumentException("Invalid account number: " + parts[1]);
        }
        if (!Validator.isValidAmount(parts[2])) {
            throw new IllegalArgumentException("Invalid amount: " + parts[2] + " (must be a positive whole number)");
        }
        return new Command(type, accountNumber, Long.parseLong(parts[2]));
    }

    private static TransactionType parseType(String word) {
        for (TransactionType type : TransactionType.values()) {
            if (type.name().equalsIgnoreCase(word)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + word);
    }
}
