/**
 * The fixed set of actions MiniBank offers. The menu number shown to the
 * user is the constant's position in this list (ordinal + 1).
 */
public enum MenuOption {
    OPEN_ACCOUNT("Open a new account"),
    DEPOSIT("Deposit money"),
    WITHDRAW("Withdraw money"),
    TRANSFER("Transfer between accounts"),
    WORKING_HOURS("Bank working hours"),
    EXIT("Exit");

    private final String label;

    MenuOption(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public int getNumber() {
        return ordinal() + 1;
    }

    /** Returns the option for a menu number, or null if the number is not on the menu. */
    public static MenuOption fromNumber(int number) {
        for (MenuOption option : values()) {
            if (option.getNumber() == number) {
                return option;
            }
        }
        return null;
    }
}
