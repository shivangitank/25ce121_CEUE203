package model;

/**
 * One parsed instruction, e.g. the line "DEPOSIT AC0001 500" becomes
 * Command[type=DEPOSIT, accountNumber=AC0001, amount=500].
 */
public record Command(TransactionType type, String accountNumber, long amount) {
}
