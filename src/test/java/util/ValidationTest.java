package util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import model.Command;
import model.Customer;
import model.SavingsAccount;
import model.TransactionType;

/** Tests for the validation layer: Validator, CommandParser and AnnotationValidator. */
class ValidationTest {

    @Test
    void validatorAcceptsGoodAndRejectsBadInput() {
        assertTrue(Validator.isValidMobile("9876543210"));
        assertFalse(Validator.isValidMobile("12345"));
        assertFalse(Validator.isValidMobile("98765432100"));
        assertTrue(Validator.isValidEmail("abc@xyz.com"));
        assertFalse(Validator.isValidEmail("abc@xyz"));
        assertTrue(Validator.isValidPan("ABCDE1234F"));
        assertFalse(Validator.isValidPan("abcde1234f"));
        assertTrue(Validator.isValidIfsc("SBIN0001234"));
        assertFalse(Validator.isValidIfsc("SBIN1001234"));
        assertTrue(Validator.isValidAmount("500"));
        assertFalse(Validator.isValidAmount("0"));
        assertFalse(Validator.isValidAmount("-5"));
        assertFalse(Validator.isValidAmount("12.5"));
        assertTrue(Validator.isValidAccountNumber("AC0001"));
        assertFalse(Validator.isValidAccountNumber("AB0001"));
        assertFalse(Validator.isValidMobile(null));
    }

    @Test
    void parserBuildsCommand() {
        Command command = CommandParser.parse("DEPOSIT AC0001 500");
        assertEquals(new Command(TransactionType.DEPOSIT, "AC0001", 500), command);
        assertEquals(TransactionType.WITHDRAW, CommandParser.parse("  withdraw   ac0002  75 ").type());
    }

    @Test
    void parserRejectsWrongInput() {
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("DEPOSIT AC0001"));
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("DEPOSIT AC0001 5 6"));
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("PAY AC0001 5"));
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("DEPOSIT X1 5"));
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("DEPOSIT AC0001 -5"));
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse(""));
    }

    @Test
    void annotationValidatorReadsFieldRules() {
        assertArrayEquals(new String[] {"balance: must be > 0"},
                AnnotationValidator.validate(new SavingsAccount("Riya", -100, 0)));
        assertEquals(0, AnnotationValidator.validate(new SavingsAccount("Riya", 5000)).length);
        assertEquals(1, AnnotationValidator.validate(new SavingsAccount("x".repeat(31), 5000)).length);
        assertEquals(1, AnnotationValidator.validate(new Customer("Aman", "a@b.com", "98765432100")).length);
    }
}
