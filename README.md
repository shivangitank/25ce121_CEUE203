# MiniBank (oop-minibank-25ce121)

Console banking application built across the CEUE203 Object-Oriented Programming practicals.

**Student:** Shivangi Tank (25CE121)

## Practical 1 - menu shell

The program prints the bank header, shows a numbered menu and reads the choice with a
`Scanner`. A switch expression maps each `MenuOption` to a placeholder message. The menu
repeats until **Exit** is chosen. Invalid input is rejected and the user is asked again.

### Compile and run

Requires JDK 17 or newer.

```bash
javac -d out src/*.java
java -cp out MiniBank
```

`javac` compiles the `.java` source files into `.class` bytecode files in `out/`;
`java` starts the JVM, which loads `MiniBank.class` and runs its `main` method.
