# MiniBank (oop-minibank-25ce121)

Console banking application built across the CEUE203 Object-Oriented Programming practicals.

**Student:** Shivangi Tank (25CE121)

## Packages

| Package   | Contents |
|-----------|----------|
| `app`     | `MiniBank` (main menu) and `MenuOption` |
| `model`   | Accounts, `Customer`, records/enums and the interfaces `Transactable`, `InterestBearing`, `WithdrawRule`, `Premium` |
| `util`    | `Validator`, `CommandParser`, `StatementFormatter` |
| `service` | `WithdrawPolicy` (standard withdrawal rules) |
| `demo`    | `LabNNDemo` programs showing each practical's requirements |

## Build and run

Requires JDK 17 or newer.

```bash
./build.sh                                        # Linux / macOS / Codespaces
powershell -ExecutionPolicy Bypass -File build.ps1 # Windows

java -jar minibank.jar                             # start the menu
java -cp minibank.jar demo.Lab06Demo               # run a practical's demo
```

The build compiles everything under `src/main/java` into `out/classes` and packages it as
`minibank.jar`. The manifest (`manifest.txt`) sets `Main-Class: app.MiniBank`, so
`java -jar` knows which class has `main`.
