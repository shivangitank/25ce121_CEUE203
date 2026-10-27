/**
 * A bank customer. The customer id is generated once from a static counter
 * and can never change afterwards.
 */
public class Customer {
    private static long customerCounter = 100;

    private final String customerId;
    private String name;
    private String email;
    private String mobile;

    public Customer(String name, String email, String mobile) {
        this.customerId = generateCustomerId();
        this.name = name;
        this.email = email;
        this.mobile = mobile;
    }

    /** Produces ids such as CUST101, CUST102, ... shared across all Customer objects. */
    private static String generateCustomerId() {
        customerCounter++;
        return "CUST" + customerCounter;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMobile() {
        return mobile;
    }
}
