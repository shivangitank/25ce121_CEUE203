package model;

/** The kinds of account a customer can open, with the text shown in the menu. */
public enum AccountType {
    SAVINGS("Savings", "4% interest, keep a minimum balance of Rs. 1,000"),
    CURRENT("Current", "no interest, overdraft up to Rs. 10,000, Premium limits"),
    FIXED_DEPOSIT("Fixed deposit", "7% interest, locked for 12 months");

    private final String label;
    private final String description;

    AccountType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
