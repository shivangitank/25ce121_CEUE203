package demo;

import java.lang.reflect.Field;
import java.util.Arrays;

import model.Account;
import model.Customer;
import model.SavingsAccount;
import model.annotation.Id;
import model.annotation.MaxLength;
import model.annotation.Positive;
import util.AnnotationValidator;

/**
 * Practical 7 demo: custom annotations (@Id, @Positive, @MaxLength) read at
 * run time with reflection by AnnotationValidator.
 */
public class Lab07Demo {

    public static void main(String[] args) {
        System.out.println("Annotated fields found by reflection on Account:");
        for (Field field : Account.class.getDeclaredFields()) {
            StringBuilder found = new StringBuilder();
            if (field.isAnnotationPresent(Id.class)) {
                found.append(" @Id");
            }
            if (field.isAnnotationPresent(Positive.class)) {
                found.append(" @Positive(message=\"").append(field.getAnnotation(Positive.class).message()).append("\")");
            }
            if (field.isAnnotationPresent(MaxLength.class)) {
                found.append(" @MaxLength(").append(field.getAnnotation(MaxLength.class).value()).append(")");
            }
            if (found.length() > 0) {
                System.out.println("  " + field.getType().getSimpleName() + " " + field.getName() + " ->" + found);
            }
        }

        System.out.println();
        Account negative = new SavingsAccount("Riya", -100, 0);
        show("Account with balance -100", AnnotationValidator.validate(negative));

        Account valid = new SavingsAccount("Riya", 5000);
        show("Valid account (balance 5000)", AnnotationValidator.validate(valid));

        Account longName = new SavingsAccount("Riya Rameshbhai Patel Shah Mehta Desai", 2000);
        show("Owner name longer than 30 characters", AnnotationValidator.validate(longName));

        Customer badMobile = new Customer("Aman", "aman@example.com", "98765432100");
        show("Customer with 11-character mobile (@MaxLength(10))", AnnotationValidator.validate(badMobile));

        Customer goodCustomer = new Customer("Aman", "aman@example.com", "9876543210");
        show("Valid customer", AnnotationValidator.validate(goodCustomer));
    }

    private static void show(String title, String[] errors) {
        System.out.println(title + " -> " + errors.length + " error(s) " + Arrays.toString(errors));
    }
}
