package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import model.annotation.Id;
import model.annotation.MaxLength;

/**
 * A bank customer. The customer id is generated once from a static counter
 * and can never change afterwards.
 */
public class Customer implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private static long customerCounter = 100;

    @Id
    private final String customerId;
    @MaxLength(50)
    private String name;
    private String email;
    @MaxLength(10)
    private String mobile;
    private Address address;

    public Customer(String name, String email, String mobile) {
        this(name, email, mobile, null);
    }

    public Customer(String name, String email, String mobile, Address address) {
        this.customerId = generateCustomerId();
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.address = address;
    }

    /** Produces ids such as CUST101, CUST102, ... shared across all Customer objects. */
    private static synchronized String generateCustomerId() {
        customerCounter++;
        return "CUST" + customerCounter;
    }

    /** Keeps the id counter ahead of ids read back from a file. */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        synchronized (Customer.class) {
            try {
                customerCounter = Math.max(customerCounter, Long.parseLong(customerId.substring(4)));
            } catch (RuntimeException notInCustFormat) {
                // ids not in the CUSTnnn format do not affect the counter
            }
        }
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

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    /**
     * Field-by-field copy made by Object.clone(). Sharing the Address is safe
     * because Address is immutable.
     */
    @Override
    public Customer clone() {
        try {
            return (Customer) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Customer implements Cloneable", e);
        }
    }

    @Override
    public String toString() {
        return customerId + " | " + name + " | " + mobile + " | " + email
                + (address == null ? "" : " | " + address);
    }

    /**
     * Postal address. A static nested class does not need a Customer object to
     * be created: new Customer.Address(...).
     */
    public static class Address implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String line;
        private final String city;
        private final String pincode;

        public Address(String line, String city, String pincode) {
            this.line = line;
            this.city = city;
            this.pincode = pincode;
        }

        public String getLine() {
            return line;
        }

        public String getCity() {
            return city;
        }

        public String getPincode() {
            return pincode;
        }

        @Override
        public String toString() {
            return line + ", " + city + " - " + pincode;
        }
    }
}
