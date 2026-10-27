# MiniBank — oop-minibank-25ce121

A menu-driven console banking application in Java, built step by step across the 14 practicals
of **CEUE203 Object-Oriented Programming** (CHARUSAT, Semester 3, 2026-27 ODD).

**Student:** Shivangi Tank, 25CE121

MiniBank opens Savings, Current and Fixed-Deposit accounts, then handles deposits, withdrawals,
transfers and statements. It also searches accounts by owner, runs a batch of transactions on a
thread pool, and writes an end-of-day report. Every input is validated, every failure is a clear
exception, and every operation is logged. All data is saved to disk, so it survives restarts.

---

## Quick start

Requires **JDK 17 or newer** (tested on JDK 25). No other installation is needed. The JUnit 5
console launcher is included in `lib/`.

```bash
# Linux / macOS / GitHub Codespaces / Git Bash
./build.sh                  # compile, run the 35 JUnit tests, build minibank.jar
./build.sh --skip-tests     # compile and build the JAR only

# Windows PowerShell
powershell -ExecutionPolicy Bypass -File build.ps1
powershell -ExecutionPolicy Bypass -File build.ps1 -SkipTests

# Run
java -jar minibank.jar              # data is kept in ./data
java -jar minibank.jar D:\bankdata  # or in a folder of your choice
```

### Compiling by hand (what the scripts do)

```bash
javac --release 17 -d out/classes $(find src/main/java -name "*.java")
jar cfm minibank.jar manifest.txt -C out/classes .        # manifest: Main-Class: app.MiniBank
java -jar minibank.jar
```

### Running each practical's demo

Every practical has a small demo program in the `demo` package that shows that lab's requirements:

```bash
java -cp minibank.jar demo.Lab05Demo     # Lab02Demo ... Lab13Demo
```

Practical 1 is the menu itself and Practical 14 is the finished application (`java -jar minibank.jar`).
Saved runs of every demo are in [`docs/sample-output/`](docs/sample-output).

---

## The menu

```
 1. Open a new account                       7. Search accounts by owner name
 2. Deposit money                            8. Process a batch of transactions (thread pool)
 3. Withdraw money                           9. Generate end-of-day report
 4. Transfer between accounts               10. Bank working hours
 5. View account statement                  11. Save and exit
 6. List all accounts
```

* Invalid menu numbers and invalid input (mobile, email, amount, account number) are rejected and asked again.
* Pressing **Enter on an empty line** cancels the current action and returns to the menu.
* Batch mode accepts lines such as `DEPOSIT AC0001 500` / `WITHDRAW AC0002 200`, or `SAMPLE`
  for a ready-made batch of 40 commands.

### Account rules

| Type          | Interest | Withdrawal rule                                    | Extra |
|---------------|----------|----------------------------------------------------|-------|
| Savings       | 4.0 %    | must keep a minimum balance of Rs. 1,000           | opening deposit at least Rs. 1,000 |
| Current       | 0.0 %    | may go overdrawn down to −Rs. 10,000               | `Premium`: higher limits |
| Fixed deposit | 7.0 %    | locked until maturity (12 months), then any amount |  |

Limits for all accounts: at most Rs. 50,000 per withdrawal (Rs. 2,00,000 for Premium accounts).
The daily total is capped at Rs. 1,00,000 (Rs. 5,00,000 for Premium accounts).

---

## Project structure

```
oop-minibank-25ce121/
├── build.sh / build.ps1          build + test + package scripts
├── manifest.txt                  Main-Class: app.MiniBank
├── lib/                          junit-platform-console-standalone-1.11.4.jar
├── src/main/java/
│   ├── app/                      MiniBank (menu UI), MenuOption
│   ├── model/                    Account (abstract), SavingsAccount, CurrentAccount, FixedDepositAccount,
│   │                             Customer (+ nested Address), BankInfo, Command, TransactionType, AccountType,
│   │                             Transactable, InterestBearing, WithdrawRule, Premium
│   ├── model/annotation/         @Id, @Positive, @MaxLength, @Audit
│   ├── exception/                BankException and its subclasses
│   ├── util/                     Validator, CommandParser, StatementFormatter, AnnotationValidator,
│   │                             StatePersister, TransactionLog, ReportGenerator
│   ├── service/                  BankService, BankOperations, Repository<T>, AccountStore,
│   │                             TransactionProcessor, TransactionBuffer, AccountWorker,
│   │                             WithdrawPolicy, TellerSession, AuditProxy, BatchResult
│   └── demo/                     Lab02Demo ... Lab13Demo
├── src/test/java/                JUnit 5 tests (35)
└── docs/
    ├── KEY-QUESTIONS.md          answers to every practical's key questions
    └── sample-output/            captured output of every demo, the menu and the tests
```

### Layered design (Practical 14)

```
 app.MiniBank (UI: reads & validates input, prints results)
        │  calls the BankOperations interface (through the @Audit proxy)
        ▼
 service.BankService ── Validator, AnnotationValidator, WithdrawRule, exceptions
        │           ── TransactionProcessor (thread pool over a ConcurrentHashMap)
        ▼
 Repository<Account>, Repository<Customer>   (generic, keyed by the @Id field)
        │
        ▼
 StatePersister (accounts.dat, customers.dat) · TransactionLog (logs/*.log) · ReportGenerator (reports/)
```

### Files created at run time (in `data/`, not committed)

| File | Written by | Purpose |
|------|------------|---------|
| `accounts.dat`, `customers.dat` | `StatePersister` | serialized state, loaded at start-up, saved after every change and on exit |
| `logs/transactions-YYYY-MM-DD.log` | `TransactionLog` | append-only lines such as `DEPOSIT AC0001 500 2026-10-27T10:15:30` |
| `reports/eod-report-YYYY-MM-DD.txt` | `ReportGenerator` | totals of deposits, withdrawals, transfers and the net change |
| `audit/audit-log.txt` | `AuditProxy` | every call to an `@Audit` method with its result and time |

---

## Practical-by-practical

Each practical is a git commit with a tag (`git tag` lists `lab-01` … `lab-14`;
`git checkout lab-05` shows the project as it was after Practical 5).

| Tag | Practical — Part B | What was added |
|-----|--------------------|----------------|
| `lab-01` | Menu shell | `MiniBank`, `BankInfo` record, `MenuOption` enum, switch-expression menu loop, re-prompt on invalid input, working-hours option |
| `lab-02` | Classes & encapsulation | `Customer` (CUST101…), `Account` (AC0001…) with private balance, `this(...)` constructor chaining, no balance setter, negative amounts rejected, transfer helper |
| `lab-03` | Object methods | `toString`, `equals`/`hashCode` by account number, static nested `Customer.Address`, `clone()`, `instanceof` |
| `lab-04` | Strings & regex | `Validator` (mobile, email, PAN, IFSC, amount), `TransactionType`, `Command` record, `CommandParser`, `StatementFormatter` (StringBuilder) |
| `lab-05` | Inheritance & polymorphism | abstract `Account` with `interestRate()`/`canWithdraw()`, Savings/Current/FixedDeposit subclasses, `super(...)`, pattern `instanceof`, `monthlyInterest()`, FD maturity |
| `lab-06` | Interfaces & packages | `Transactable`, `InterestBearing` (default/static/private methods), `@FunctionalInterface WithdrawRule` (anonymous class + lambda), `Premium` marker, packages, static import, runnable JAR |
| `lab-07` | Annotations & reflection | `@Override` everywhere, `@Id`, `@Positive`, `@MaxLength`, `AnnotationValidator` (reflection, inherited fields, field names in messages) |
| `lab-08` | Exceptions (Milestone 1) | checked `BankException` hierarchy, `InsufficientFundsException` with shortfall, `DailyLimitExceededException`, all-or-nothing `transfer()` with try/catch/finally re-throw, try-with-resources `TellerSession` |
| `lab-09` | Threads | `AccountWorker` (Runnable), race condition shown (balance below 10,000 and different every run), fixed with `synchronized`, AtomicLong comparison, mixed deposit/withdraw threads, thread states |
| `lab-10` | Thread pools | `TransactionProcessor` (fixed pool of 4, tasks per thread, slow-task warning), producer–consumer `TransactionBuffer` with `wait`/`notifyAll`, deadlock shown and fixed by locking the lower account number first |
| `lab-11` | Files & serialization | `Serializable` model with `serialVersionUID`, `transient` field, `StatePersister`, `TransactionLog` (CREATE + APPEND), NIO `ReportGenerator` (DirectoryStream, attributes, skips empty / non-.log files) |
| `lab-12` | Collections (Milestone 2) | `AccountStore` (HashMap + ArrayList history), Comparator / Comparable sorting, `balanceAbove()`, TreeMap view, ConcurrentHashMap in `TransactionProcessor`, per-account history in statements |
| `lab-13` | Generics | `Repository<T>` keyed by `@Id` via reflection, `@SafeVarargs saveAll`, wildcards, bounded `<N extends Number> average()`, reused for `Customer` |
| `lab-14` | Capstone (Milestone 3) | `BankService` + `BankOperations`, full menu wired to real operations, load on start / save on exit (and after every change), batch on the thread pool, report menu option, search by owner, optional `@Audit` reflective proxy, 35 JUnit tests, final JAR |

All supplementary problems of every practical are implemented. [`docs/KEY-QUESTIONS.md`](docs/KEY-QUESTIONS.md)
lists where each one is and answers every practical's key questions.

---

## Tests

`./build.sh` runs 35 JUnit 5 tests (`src/test/java`). The tests cover:

* **AccountTest**: deposit increases the balance; a withdrawal over the limit throws (with
  shortfall); the rules of each account type; the daily limit; all-or-nothing transfer;
  equals/hashCode/compareTo; interest.
* **BankServiceTest**: open/deposit/withdraw/transfer through the service; validation errors;
  unknown accounts; search; **state saved and reloaded unchanged**; batch on the thread pool;
  report totals; concurrent deposits through the service.
* **ConcurrencyTest**: **10 threads × 1000 deposits = exactly 10000**; thread-pool processing;
  opposite transfers finish without deadlock.
* **RepositoryTest** and **ValidationTest**: the generic repository, regexes, the command
  parser and the annotation validator.

The output of the latest run is in
[`docs/sample-output/lab14-junit-results.txt`](docs/sample-output/lab14-junit-results.txt).
