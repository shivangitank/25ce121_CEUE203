import java.time.LocalDate;

/**
 * Fixed deposit: earns 7% a year; the money is locked until the maturity date.
 */
public class FixedDepositAccount extends Account {
    public static final int DEFAULT_TERM_MONTHS = 12;

    private final LocalDate maturityDate;

    public FixedDepositAccount(String ownerName, long deposit, LocalDate maturityDate) {
        super(ownerName, deposit);
        this.maturityDate = maturityDate;
    }

    public FixedDepositAccount(String ownerName, long deposit) {
        this(ownerName, deposit, LocalDate.now().plusMonths(DEFAULT_TERM_MONTHS));
    }

    public FixedDepositAccount(String accountNumber, String ownerName, long balance, LocalDate maturityDate) {
        super(accountNumber, ownerName, balance);
        this.maturityDate = maturityDate;
    }

    public double interestRate() {
        return 7.0;
    }

    /** Locked before maturity; after maturity any amount up to the balance may be taken. */
    public boolean canWithdraw(long amount) {
        return isMatured() && amount <= getBalance();
    }

    public boolean isMatured() {
        return !LocalDate.now().isBefore(maturityDate);
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }
}
