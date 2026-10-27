package model;

import java.time.LocalDate;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.DailyLimitExceededException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.annotation.Id;
import model.annotation.MaxLength;
import model.annotation.Positive;

/**
 * Base class for every MiniBank account. The balance is private and changes
 * only through deposit() and withdraw() - there is deliberately no public
 * setBalance(). Each subclass decides its own interest rate and withdrawal
 * rule; withdraw() calls canWithdraw(), so the right rule is picked at run time.
 * Every failure is reported with an exception that explains what went wrong.
 * Methods that read or change the balance are synchronized, so only one
 * thread at a time can run them on the same account (no lost updates).
 */
public abstract class Account implements Transactable, InterestBearing {
    public static final long DAILY_WITHDRAWAL_LIMIT = 1_00_000;
    public static final long PREMIUM_DAILY_WITHDRAWAL_LIMIT = 5_00_000;

    private static int accountCounter = 0;

    @Id
    private final String accountNumber;
    @MaxLength(30)
    private String ownerName;
    @Positive
    private long balance;          // whole rupees
    private boolean active;
    private long withdrawnToday;
    private LocalDate withdrawalDay;
    private String lastActivity;

    protected Account(String ownerName, long openingBalance) {
        this.accountNumber = generateAccountNumber();
        this.ownerName = ownerName;
        this.balance = openingBalance;
        this.active = true;
    }

    protected Account(String ownerName) {
        this(ownerName, 0);
    }

    /** Re-creates an account whose number is already known (for example, one read back from storage). */
    protected Account(String accountNumber, String ownerName, long balance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
        this.active = true;
        reserveNumber(accountNumber);
    }

    /** Yearly interest rate in percent. */
    @Override
    public abstract double interestRate();

    /** True if this account's rules allow taking out the given amount now. */
    public abstract boolean canWithdraw(long amount);

    /** How much could be withdrawn right now under this account's rules (ignoring the daily limit). */
    public long getAvailableBalance() {
        return balance;
    }

    /**
     * Why a withdrawal is blocked when the reason is not simply a low balance
     * (e.g. a locked fixed deposit). Null means "not enough money".
     */
    protected String withdrawalBlockedReason() {
        return null;
    }

    /** Produces numbers such as AC0001, AC0002, ... from one counter shared by every account. */
    private static synchronized String generateAccountNumber() {
        accountCounter++;
        return String.format("AC%04d", accountCounter);
    }

    /** Moves the counter past an existing number so a generated number never repeats it. */
    private static synchronized void reserveNumber(String accountNumber) {
        try {
            int used = Integer.parseInt(accountNumber.substring(2));
            if (used > accountCounter) {
                accountCounter = used;
            }
        } catch (RuntimeException notInAcFormat) {
            // numbers not in the ACnnnn format do not affect the counter
        }
    }

    /** Adds money to the account. */
    @Override
    public synchronized void deposit(long amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        balance += amount;
        lastActivity = "Deposited " + amount;
    }

    /**
     * Takes money out of the account if the subclass rule (canWithdraw) and
     * the daily limit allow it. The balance is unchanged when an exception is thrown.
     * @return true on success
     */
    @Override
    public synchronized boolean withdraw(long amount)
            throws InsufficientFundsException, InvalidAmountException, DailyLimitExceededException {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        if (!canWithdraw(amount)) {
            long shortfall = Math.max(amount - Math.max(getAvailableBalance(), 0), 0);
            String reason = withdrawalBlockedReason();
            if (reason != null) {
                throw new InsufficientFundsException(accountNumber, amount, shortfall, reason);
            }
            throw new InsufficientFundsException(accountNumber, amount, shortfall);
        }
        LocalDate today = LocalDate.now();
        if (!today.equals(withdrawalDay)) {
            withdrawalDay = today;          // a new day starts a new limit
            withdrawnToday = 0;
        }
        if (withdrawnToday + amount > dailyLimit()) {
            throw new DailyLimitExceededException(accountNumber, dailyLimit(), withdrawnToday);
        }
        balance -= amount;
        withdrawnToday += amount;
        lastActivity = "Withdrew " + amount;
        return true;
    }

    /**
     * Moves money to another account. It is all-or-nothing: if the deposit
     * side fails after the withdrawal succeeded, the money is put back and the
     * original exception is re-thrown to the caller.
     * <p>
     * Both accounts are locked for the whole transfer, always in the same order
     * (smaller account number first). Two opposite transfers, A to B and B to A,
     * therefore ask for the locks in the same order and can never deadlock.
     */
    public void transfer(Account to, long amount) throws BankException {
        if (to == null) {
            throw new AccountNotFoundException("(missing target account)");
        }
        if (to.equals(this)) {
            throw new BankException("Cannot transfer from " + accountNumber + " to itself");
        }
        Account first = accountNumber.compareTo(to.accountNumber) < 0 ? this : to;
        Account second = first == this ? to : this;
        synchronized (first) {
            synchronized (second) {
                boolean withdrawn = false;
                boolean completed = false;
                try {
                    withdraw(amount);
                    withdrawn = true;
                    to.deposit(amount);
                    completed = true;
                } catch (BankException e) {
                    if (withdrawn) {
                        refund(amount);
                    }
                    throw e;
                } finally {
                    lastActivity = (completed ? "Transferred " : "Failed transfer of ") + amount + " to " + to.accountNumber;
                }
            }
        }
    }

    /** Puts back money taken by a withdrawal that is being rolled back. */
    private synchronized void refund(long amount) {
        balance += amount;
        withdrawnToday -= amount;
    }

    /** Higher cap for accounts marked with the Premium interface. */
    public long dailyLimit() {
        return this instanceof Premium ? PREMIUM_DAILY_WITHDRAWAL_LIMIT : DAILY_WITHDRAWAL_LIMIT;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public synchronized long getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    /** Freezes (false) or re-opens (true) the account. */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** A short note about the last operation, e.g. "Deposited 500". */
    public synchronized String getLastActivity() {
        return lastActivity;
    }

    /** "SavingsAccount" -> "Savings", "FixedDepositAccount" -> "FixedDeposit". */
    public String getAccountType() {
        return getClass().getSimpleName().replace("Account", "");
    }

    /** One readable line, e.g. "AC0001 | Riya | 4000 | ACTIVE". */
    @Override
    public String toString() {
        return accountNumber + " | " + ownerName + " | " + balance + " | " + (active ? "ACTIVE" : "INACTIVE");
    }

    /** Two accounts are the same account when their account numbers are equal. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Account)) {
            return false;
        }
        Account other = (Account) o;
        return accountNumber.equals(other.accountNumber);
    }

    /** Uses the same field as equals(), so equal accounts always have equal hash codes. */
    @Override
    public int hashCode() {
        return accountNumber.hashCode();
    }
}
