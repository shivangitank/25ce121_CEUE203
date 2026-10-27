package exception;

/** Thrown when no account exists with the given number. */
public class AccountNotFoundException extends BankException {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;

    public AccountNotFoundException(String accountNumber) {
        super("No account found with number " + accountNumber);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
