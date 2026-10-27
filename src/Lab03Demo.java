/**
 * Practical 3 demo: toString(), equals()/hashCode(), the nested Address class,
 * clone() and the instanceof operator.
 */
public class Lab03Demo {

    public static void main(String[] args) {
        Account a1 = new Account("AC0001", "Riya", 4000);
        Account a2 = new Account("AC0001", "Riya", 4000);
        Account a3 = new Account("Aman", 2500);       // gets the next free number

        System.out.println("a1 = " + a1);
        System.out.println("a2 = " + a2);
        System.out.println("a3 = " + a3);
        System.out.println("a1 == a2           : " + (a1 == a2) + "   (two different objects)");
        System.out.println("a1.equals(a2)      : " + a1.equals(a2) + "    (same account number)");
        System.out.println("a1.equals(a3)      : " + a1.equals(a3));
        System.out.println("hash codes equal   : " + (a1.hashCode() == a2.hashCode()));

        Object something = a1;
        System.out.println("a1 instanceof Account  : " + (something instanceof Account));
        Object text = "AC0001";
        System.out.println("\"AC0001\" instanceof Account : " + (text instanceof Account));

        Customer.Address address = new Customer.Address("12, Station Road", "Anand", "388001");
        Customer riya = new Customer("Riya Patel", "riya.patel@example.com", "9876543210", address);
        Customer copy = riya.clone();

        System.out.println();
        System.out.println("customer : " + riya);
        System.out.println("clone    : " + copy);
        System.out.println("same object?         " + (riya == copy));
        System.out.println("same customer id?    " + riya.getCustomerId().equals(copy.getCustomerId()));
        System.out.println("city from address    " + riya.getAddress().getCity());
    }
}
