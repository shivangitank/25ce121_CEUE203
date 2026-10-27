/**
 * Base class for every MiniBank account. The balance is private and changes
 * only through deposit() and withdraw() - there is deliberately no public
 * setBalance(). Each subclass decides its own interest rate and withdrawal
 * rule; withdraw() calls canWithdraw(), so the right rule is picked at run time.
 */
public abstract class Account {
    private static int accountCounter = 0;

    private final String accountNumber;
    private String ownerName;
    private long balance;          // whole rupees
    private boolean active;

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
    public abstract double interestRate();

    /** True if this account's rules allow taking out the given amount now. */
    public abstract boolean canWithdraw(long amount);

    /** Produces numbers such as AC0001, AC0002, ... from one counter shared by every account. */
    private static String generateAccountNumber() {
        accountCounter++;
        return String.format("AC%04d", accountCounter);
    }

    /** Moves the counter past an existing number so a generated number never repeats it. */
    private static void reserveNumber(String accountNumber) {
        try {
            int used = Integer.parseInt(accountNumber.substring(2));
            if (used > accountCounter) {
                accountCounter = used;
            }
        } catch (RuntimeException notInAcFormat) {
            // numbers not in the ACnnnn format do not affect the counter
        }
    }

    /** Adds money to the account. Zero or negative amounts are ignored. */
    public void deposit(long amount) {
        if (amount <= 0) {
            return;
        }
        balance += amount;
    }

    /**
     * Takes money out of the account if the subclass rule (canWithdraw) allows it.
     * @return true on success; false (balance unchanged) otherwise
     */
    public boolean withdraw(long amount) {
        if (amount <= 0 || !canWithdraw(amount)) {
            return false;
        }
        balance -= amount;
        return true;
    }

    /** Moves money to another account using withdraw() and deposit(). */
    public boolean transferTo(Account target, long amount) {
        if (target == null || target == this || !withdraw(amount)) {
            return false;
        }
        target.deposit(amount);
        return true;
    }

    /** Interest earned in one month at the current balance. */
    public double monthlyInterest() {
        return balance * interestRate() / 100 / 12;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    /** "SavingsAccount" -> "Savings", "FixedDepositAccount" -> "FixedDeposit". */
    public String getAccountType() {
        return getClass().getSimpleName().replace("Account", "");
    }

    /** One readable line, e.g. "AC0001 | Riya | 4000 | ACTIVE". */
    public String toString() {
        return accountNumber + " | " + ownerName + " | " + balance + " | " + (active ? "ACTIVE" : "INACTIVE");
    }

    /** Two accounts are the same account when their account numbers are equal. */
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
    public int hashCode() {
        return accountNumber.hashCode();
    }
}
