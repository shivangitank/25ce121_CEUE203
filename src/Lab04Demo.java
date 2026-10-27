/**
 * Practical 4 demo: regex validators, the Command record, CommandParser and
 * the StringBuilder-based statement.
 */
public class Lab04Demo {

    public static void main(String[] args) {
        System.out.println("Validators (one valid, one invalid input each):");
        check("isValidMobile(\"9876543210\")", Validator.isValidMobile("9876543210"));
        check("isValidMobile(\"12345\")", Validator.isValidMobile("12345"));
        check("isValidEmail(\"abc@xyz.com\")", Validator.isValidEmail("abc@xyz.com"));
        check("isValidEmail(\"abc@xyz\")", Validator.isValidEmail("abc@xyz"));
        check("isValidPan(\"ABCDE1234F\")", Validator.isValidPan("ABCDE1234F"));
        check("isValidPan(\"ABCD12345F\")", Validator.isValidPan("ABCD12345F"));
        check("isValidIfsc(\"SBIN0001234\")", Validator.isValidIfsc("SBIN0001234"));
        check("isValidIfsc(\"SBIN1001234\")", Validator.isValidIfsc("SBIN1001234"));
        check("isValidAmount(\"500\")", Validator.isValidAmount("500"));
        check("isValidAmount(\"-50\")", Validator.isValidAmount("-50"));

        System.out.println();
        Command command = CommandParser.parse("DEPOSIT AC0001 500");
        System.out.println("parse(\"DEPOSIT AC0001 500\")");
        System.out.println("  type=" + command.type() + ", accountNumber=" + command.accountNumber()
                + ", amount=" + command.amount());

        String[] badLines = {"DEPOSIT AC0001", "WITHDRAW AC0001 50 extra", "PAY AC0001 50", "DEPOSIT AC0001 -5"};
        for (String bad : badLines) {
            try {
                CommandParser.parse(bad);
            } catch (IllegalArgumentException e) {
                System.out.println("parse(\"" + bad + "\") rejected: " + e.getMessage());
            }
        }

        System.out.println();
        Account account = new SavingsAccount("Riya", 5000);
        account.deposit(2000);
        account.withdraw(3000);
        System.out.println(StatementFormatter.buildStatement(account));
    }

    private static void check(String call, boolean result) {
        System.out.printf("  %-32s -> %b%n", call, result);
    }
}
