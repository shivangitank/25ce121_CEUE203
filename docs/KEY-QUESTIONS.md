# Key questions and supplementary problems

Short answers to the "Key Questions / Analysis" of each practical, using MiniBank's own code as the
example. The **Supplementary** line of each practical says where its supplementary problems are implemented.

---

## Practical 1 — Menu shell

**1. What is the role of the JVM, and what file does the javac compiler produce?**
`javac` translates `MiniBank.java` into platform-independent bytecode, `MiniBank.class`. The JVM
loads the `.class` files, verifies the bytecode, and then runs it, interpreting it and compiling the
busy parts to machine code. Because the bytecode is the same everywhere, the same `minibank.jar` runs
on Windows, Linux and Codespaces.

**2. How does a switch expression differ from a traditional switch statement?**
A switch expression *produces a value* (`String message = switch (choice) { case DEPOSIT -> "..."; ... };`).
It uses `->` arms with no fall-through, so no `break` is needed. For an enum, the compiler checks
that every constant is handled. A traditional switch is only a statement. It falls through to
the next case unless you write `break`, and the compiler does not check that all cases are covered.

**3. Why is an enum a good choice for a fixed set of menu options, and a record for read-only data?**
An enum lists every legal value, so an invalid option cannot exist. It works with `switch`, and it
can carry data (`MenuOption` holds its label and number). A record (`BankInfo`) is a short, immutable
data carrier. The compiler writes the constructor, accessors, `equals`, `hashCode` and `toString`,
and its fields are final.

**Supplementary:** invalid numbers re-prompt (`MiniBank.readChoice`); working hours are menu option 10.

## Practical 2 — Customer and Account

**1. What is encapsulation, and how do private fields with public getters enforce it?**
Encapsulation keeps an object's data and the rules that change it together. `balance` is `private`,
so other classes can only read it with `getBalance()`. They can change it only through `deposit()`
and `withdraw()`, which enforce the rules. There is no `setBalance()`, so no other code can set an
invalid balance.

**2. Why is accountNumber declared final?**
An account's number identifies it for life: it is the key in maps and repositories and the basis of
`equals`/`hashCode`. `final` means it is set once in the constructor and can never be changed.

**3. What is the difference between a static field and an instance field when generating ids?**
An instance field exists once per object. A static field (`accountCounter`, `customerCounter`) exists
once for the whole class and is shared by all objects. Each constructor increments the shared counter,
so every object gets the next unique number (AC0001, AC0002, …). A counter stored as an instance
field would start again for every object.

**Supplementary:** transfer helper (`Account.transfer`, first as `transferTo` in lab-02); negative amounts are
rejected (since Practical 8 with `InvalidAmountException`).

## Practical 3 — Object methods

**1. Why must equals() and hashCode() be overridden together?**
Hash-based collections (`HashMap`, `HashSet`) first use `hashCode()` to choose a bucket and then
`equals()` inside it. The contract says equal objects must have equal hash codes. If only
`equals()` is overridden, two equal accounts could land in different buckets and never be found.

**2. What does the default Object.toString() return, and why override it?**
It returns `ClassName@hexHashCode`, e.g. `SavingsAccount@1b6d3586`, which tells the reader nothing.
Overriding it gives a readable line, `AC0001 | Riya | 4000 | ACTIVE`, for printing and logging.

**3. What is the difference between a static nested class and an inner class?**
A static nested class (`Customer.Address`) does not belong to any outer object. It is created with
`new Customer.Address(...)` and cannot use the outer object's instance fields. An inner
(non-static) class is tied to an instance of the outer class and keeps a hidden reference to it.

**Supplementary:** `toString()` shows ACTIVE/INACTIVE; the multi-line statement is `StatementFormatter.buildStatement`.

## Practical 4 — Validation and parsing

**1. What is the difference between String, StringBuilder and StringBuffer, and when is each preferred?**
`String` is immutable, so every change creates a new object. It is best for values that do not
change. `StringBuilder` is a mutable buffer and the fastest way to build text in a loop
(`StatementFormatter`). `StringBuffer` works the same way but every method is synchronized. It is
only needed when several threads share one buffer.

**2. What does the regular expression [6-9][0-9]{9} match, and why use anchors?**
It matches one digit from 6 to 9 followed by exactly nine more digits: a 10-digit Indian mobile number.
Without `^…$` (or `matches()`), a search would also accept text that merely *contains* such a
number, like `"98765432100"` or `"abc9876543210"`. The anchors force the whole input to match.

**3. Why should input be validated at the boundary before it is used?**
Bad data that gets inside spreads and causes errors far from where it entered. Checking at the
boundary (menu input, parsed commands) means the rest of the program can trust its data. It also
lets the user see a clear message straight away.

**Supplementary:** `Validator.isValidAmount`; `CommandParser.parse` reports the wrong number of parts.

## Practical 5 — Inheritance and polymorphism

**1. What is the difference between an abstract class and a concrete class?**
An abstract class (`Account`) cannot be instantiated. It can declare abstract methods
(`interestRate()`, `canWithdraw()`) that subclasses must implement, and it shares common fields and
code. A concrete class (`SavingsAccount`) implements everything and can be created with `new`.

**2. What is dynamic method dispatch (run-time polymorphism)?**
The method called is chosen by the *actual object's* class at run time, not by the variable's
type. Looping over an `Account[]` and calling `interestRate()` runs the Savings, Current or
FixedDeposit version for each element.

**3. What does the super keyword do in a subclass constructor?**
`super(ownerName, openingBalance)` calls the parent constructor first so the inherited private
fields (number, owner, balance) are initialised. It must be the first statement.

**Supplementary:** `monthlyInterest()` (moved into `InterestBearing` in lab-06); `FixedDepositAccount` allows
withdrawal after its maturity date.

## Practical 6 — Interfaces, packages, JAR

**1. What is the difference between an interface and an abstract class?**
An interface is a contract of capabilities. A class can implement many interfaces (`Account
implements Transactable, InterestBearing, Serializable, Comparable<Account>`). Interfaces have no
instance state, only constants and abstract/default/static/private methods. An abstract class can
hold fields and constructors, but a class can extend only one.

**2. What is a functional interface, and how does a lambda use it?**
An interface with exactly one abstract method (`WithdrawRule.allow`). A lambda such as
`(account, amount) -> account.isActive()` supplies the body of that single method, so it can be
used wherever a `WithdrawRule` is expected. It is a shorter form of the anonymous class.

**3. Why is a default method useful in an interface?**
It adds behaviour to every implementing class without changing those classes.
`InterestBearing.yearlyInterest()` works for all accounts because each one already has
`interestRate()` and `getBalance()`. New methods can be added later without breaking existing implementers.

**Supplementary:** second default method `monthlyInterest()`; `import static util.Validator.*` in `Lab06Demo`
and `MiniBank`.

## Practical 7 — Annotations and reflection

**1. What are the meta-annotations @Retention and @Target used for?**
`@Target(FIELD)` limits where the annotation may be placed. `@Retention(RUNTIME)` keeps it in the
class file so it can be read by reflection while the program runs. With the default `CLASS`
retention, `isAnnotationPresent` would always return false.

**2. What is reflection, and what does getDeclaredFields() return?**
Reflection lets a program inspect and use classes, fields and methods at run time. On a
`Class` object, `getDeclaredFields()` returns *all* fields declared in that exact class (private
ones included) but *not* inherited ones. That is why `AnnotationValidator` walks up through
`getSuperclass()`.

**3. How do annotations plus reflection let a framework validate any object?**
The rules are written as metadata on the fields (`@Positive`, `@MaxLength(30)`). One generic method
loops over any object's fields, reads each annotation, and checks the value with
`setAccessible(true)` and `field.get(obj)`. No validation code is needed in each class.

**Supplementary:** `@MaxLength(10)` on `Customer.mobile` (tested); messages start with the field name.

## Practical 8 — Exceptions (Milestone 1)

**1. What is the difference between a checked and an unchecked exception?**
Checked exceptions (subclasses of `Exception`, like `BankException`) must be caught or declared
with `throws`, and the compiler enforces this. Use them for expected failures such as low funds.
Unchecked exceptions (`RuntimeException`, e.g. `IllegalArgumentException`) usually signal
programming errors and need not be declared.

**2. What is the purpose of the finally block, and when does it run?**
It holds clean-up code. It runs after the `try` block whether that block finished normally, threw
an exception that was caught, or threw one that was not. `Account.transfer` uses it to record the
attempt in every case.

**3. How does try-with-resources guarantee a resource is closed?**
Any `AutoCloseable` declared in `try (...)` has `close()` called automatically when the block
ends, even if an exception was thrown (`TellerSession` shows this). Exceptions thrown by `close()`
are attached to the original one as suppressed exceptions, so the first error is not lost.

**Supplementary:** `DailyLimitExceededException`; the demo prints `Withdrawal failed: short by 4000`.

## Practical 9 — Threads

**1. What is a race condition, and why does it occur here?**
The result depends on the timing of threads. `balance += amount` is three steps: read, add, write.
Two threads can read the same old balance, both add 1, and both write, so one update is lost. In
the lab-09 "before" commit, 10 × 1000 deposits gave 9,397, 7,041 and 7,620 in three runs instead of
10,000 (`docs/sample-output/lab09-before-fix.txt`).

**2. What does the synchronized keyword do?**
A thread must hold the object's lock (monitor) to run a synchronized method or block, so only one
thread at a time runs `deposit()` on an account. It also makes the changes visible to the next
thread that takes the lock. After the fix, the balance is exactly 10,000 every run.

**3. What are the states in a thread's life cycle?**
NEW (created), RUNNABLE (started, running or ready), BLOCKED (waiting for a monitor lock),
WAITING / TIMED_WAITING (in `wait()`, `join()`, `sleep()`), and TERMINATED (finished). `Lab09Demo` prints
NEW → RUNNABLE → TERMINATED, and `Lab10Demo` shows two deadlocked threads in BLOCKED.

**Supplementary:** `AtomicLong` version and mixed depositing/withdrawing threads in `Lab09Demo`.

## Practical 10 — Thread pools, wait/notify, deadlock

**1. Why is a thread pool better than creating a new thread per task?**
Creating threads is costly. A pool creates a few threads once (4 in `TransactionProcessor`) and
reuses them for many tasks. It also limits how many tasks run at the same time and gives
`shutdown()`/`awaitTermination()` for a clean stop.

**2. How do wait() and notify() coordinate two threads?**
Inside a `synchronized` block, `wait()` releases the lock and sleeps until another thread calls
`notify()`/`notifyAll()` on the same object. In `TransactionBuffer`, the producer waits while the
buffer is full and the consumer waits while it is empty. Each wakes the other after changing it,
and each re-checks its condition in a `while` loop.

**3. What causes a deadlock, and how does consistent lock ordering prevent it?**
Thread 1 holds A and waits for B while thread 2 holds B and waits for A, so neither can continue.
If every thread takes locks in the same global order (the smaller account number first in
`Account.transfer`), a cycle of waiting threads cannot form.

**Supplementary:** tasks per pool thread (`getTasksPerThread`); warning for tasks over 500 ms.

## Practical 11 — Files and serialization

**1. What does it mean for a class to be Serializable, and why add serialVersionUID?**
`Serializable` marks a class whose objects `ObjectOutputStream` may turn into bytes and later
rebuild. `serialVersionUID` is the class's version number. If it is not declared, Java computes one
from the class structure, so a small code change would make old `accounts.dat` files unreadable.

**2. What is the difference between a byte stream and a character stream?**
Byte streams (`InputStream`/`OutputStream`) move raw bytes, used for `accounts.dat`. Character
streams (`Reader`/`Writer`, e.g. `BufferedReader`) move text and convert bytes using a charset
(UTF-8), used for the log files.

**3. How does NIO's Path/Files API differ from the old File class, and how is a directory stream iterated?**
`Path` + `Files` throw meaningful exceptions instead of returning `false`. They also support
attributes (`readAttributes`), atomic moves, open options (`CREATE`, `APPEND`), and lazy directory
streams. `Files.newDirectoryStream(dir)` is used in try-with-resources and iterated with a for-each
loop, as in `ReportGenerator`.

**Supplementary:** `lastActivity` is `transient` (null after reload); empty and non-`.log` files are skipped.

## Practical 12 — Collections (Milestone 2)

**1. What is the difference between a List, a Set and a Map?**
A `List` is ordered and allows duplicates (the transaction history). A `Set` holds each element
only once (unique owner names). A `Map` stores key → value pairs with unique keys
(account number → account).

**2. When do you use a HashMap versus a TreeMap?**
`HashMap` gives O(1) average lookup with no order. Use it for fast access by key (`AccountStore`).
`TreeMap` keeps the keys sorted with O(log n) operations. Use it when ordered iteration or range
queries are needed (`asSortedMap()`).

**3. What is the difference between Comparable and Comparator?**
`Comparable` is the class's single *natural* order, written inside the class
(`Account.compareTo` by account number). A `Comparator` is a separate ordering object, and you can
have as many as you need (`AccountStore.BY_BALANCE_DESC`).

**Supplementary:** `balanceAbove(amount)`; `asSortedMap()` returns a `TreeMap`.

## Practical 13 — Generics

**1. What problem do generics solve compared with using Object?**
Type safety at compile time and no casts. `Repository<Account>.findById()` returns an `Account`,
and saving a `Customer` into it does not compile. With `Object`, mistakes appear only at run time
as a `ClassCastException`.

**2. What is a bounded type parameter, and why is <N extends Number> useful?**
It limits the allowed types to `Number` and its subclasses. That lets the method call
`doubleValue()` on each element, so `average()` works for `Integer[]`, `Long[]` and `Double[]`,
while a `String[]` is rejected by the compiler.

**3. What is type erasure?**
Generic types are checked by the compiler and then erased. At run time, `Repository<Account>` and
`Repository<Customer>` are the same class, with `T` replaced by `Object` plus inserted casts. That
is why the runtime cannot do `new T()` or check `instanceof T`, and why `@SafeVarargs` is needed
for `T...`.

**Supplementary:** `Repository<Customer>` reuses the same class; `count()` returns the number of items.

## Practical 14 — Capstone (Milestone 3)

**1. What is a layered architecture (model, service, user interface), and why is it useful?**
Each layer has one job. The UI (`app.MiniBank`) only reads input and prints output. The service
(`BankService`) holds the business rules. The model and repository hold the data. Layers talk
through interfaces (`BankOperations`), so each can be changed or tested on its own: the JUnit tests
call `BankService` directly with no menu.

**2. How do you assert in JUnit that a method throws an expected exception?**
`assertThrows(InsufficientFundsException.class, () -> account.withdraw(5000))`. The call passes only
if that exception (or a subclass) is thrown, and it returns the exception so its details can be
checked, e.g. `assertEquals(4000, e.getShortfall())`.

**3. What does packaging the application as a runnable JAR provide?**
One file contains all compiled classes plus a manifest naming the entry point
(`Main-Class: app.MiniBank`). Anyone with a JRE can run it with `java -jar minibank.jar`, without the
source code, the classpath, or knowing the main class.

**Supplementary:** search by owner name (menu option 7); `BankServiceTest.stateSurvivesRestartUnchanged`.
**Optional:** `@Audit` with the reflective `AuditProxy` (dynamic proxy) writes `data/audit/audit-log.txt`.
