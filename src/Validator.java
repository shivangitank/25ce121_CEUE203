import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks raw user input before MiniBank uses it. Each pattern is compiled once
 * and reused. The ^ and $ anchors make the whole string match, so
 * "98765432100" (11 digits) is rejected even though it starts with a valid number.
 */
public final class Validator {
    private static final Pattern MOBILE = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PAN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern IFSC = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern AMOUNT = Pattern.compile("^[1-9][0-9]{0,14}$");
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("^AC[0-9]{4,}$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{0,49}$");

    private Validator() {
        // only static methods
    }

    /** Indian mobile number: 10 digits starting with 6, 7, 8 or 9. */
    public static boolean isValidMobile(String mobile) {
        return matches(MOBILE, mobile);
    }

    public static boolean isValidEmail(String email) {
        return matches(EMAIL, email);
    }

    /** PAN: 5 capital letters, 4 digits, 1 capital letter, e.g. ABCDE1234F. */
    public static boolean isValidPan(String pan) {
        return matches(PAN, pan);
    }

    /** IFSC: 4 letters (bank), the digit 0, then 6 letters or digits (branch), e.g. SBIN0001234. */
    public static boolean isValidIfsc(String ifsc) {
        return matches(IFSC, ifsc);
    }

    /** A positive whole number of rupees (no sign, no decimals, no leading zero). */
    public static boolean isValidAmount(String amount) {
        return matches(AMOUNT, amount);
    }

    /** MiniBank account numbers look like AC0001. */
    public static boolean isValidAccountNumber(String accountNumber) {
        return matches(ACCOUNT_NUMBER, accountNumber);
    }

    /** A person's name: letters, spaces, dots, apostrophes and hyphens, at most 50 characters. */
    public static boolean isValidName(String name) {
        return matches(NAME, name);
    }

    private static boolean matches(Pattern pattern, String input) {
        if (input == null) {
            return false;
        }
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }
}
