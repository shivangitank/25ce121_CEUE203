package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import model.Account;
import model.CurrentAccount;
import model.Customer;
import model.SavingsAccount;

class RepositoryTest {

    @Test
    void storesAndFindsAccountsByTheirIdField() {
        Repository<Account> repository = new Repository<>();
        Account riya = new SavingsAccount("Riya", 5000);
        Account aman = new CurrentAccount("Aman", 2000);
        repository.saveAll(riya, aman);

        assertEquals(2, repository.count());
        assertSame(aman, repository.findById(aman.getAccountNumber()));
        assertEquals(2, repository.findAll().size());
        assertEquals(1, repository.findAll(a -> a.getBalance() > 3000).size());
        assertTrue(repository.delete(riya.getAccountNumber()));
        assertFalse(repository.delete(riya.getAccountNumber()));
        assertNull(repository.findById(riya.getAccountNumber()));
    }

    @Test
    void sameClassWorksForCustomers() {
        Repository<Customer> repository = new Repository<>();
        Customer customer = new Customer("Riya", "riya@example.com", "9876543210");
        repository.save(customer);
        assertSame(customer, repository.findById(customer.getCustomerId()));
    }

    @Test
    void typeWithoutIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Repository<String>().save("no id"));
    }

    @Test
    void averageWorksForAnyNumberType() {
        assertEquals(5.0, Repository.average(new Integer[] {3, 5, 7}));
        assertEquals(2.5, Repository.average(new Double[] {2.0, 3.0}));
        assertEquals(0.0, Repository.average(new Long[0]));
    }
}
