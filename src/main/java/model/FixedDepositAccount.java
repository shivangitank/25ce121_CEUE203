package model;

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

    @Override
    public double interestRate() {
        return 7.0;
    }

    /** Locked before maturity; after maturity any amount up to the balance may be taken. */
    @Override
    public boolean canWithdraw(long amount) {
        return isMatured() && amount <= getBalance();
    }

    /** Nothing until maturity, then the whole balance. */
    @Override
    public long getAvailableBalance() {
        return isMatured() ? getBalance() : 0;
    }

    @Override
    protected String withdrawalBlockedReason() {
        return isMatured() ? null : "fixed deposit is locked until " + maturityDate;
    }

    public boolean isMatured() {
        return !LocalDate.now().isBefore(maturityDate);
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }
}
