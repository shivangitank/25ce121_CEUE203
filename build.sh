#!/usr/bin/env bash
# Builds MiniBank: compiles the code, runs the JUnit 5 tests and packages minibank.jar.
#   ./build.sh               compile + test + jar
#   ./build.sh --skip-tests  compile + jar
set -euo pipefail
cd "$(dirname "$0")"

JUNIT=lib/junit-platform-console-standalone-1.11.4.jar
SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; esac   # Git Bash on Windows

rm -rf out
mkdir -p out/classes out/test-classes
find src/main/java -name '*.java' > out/main-sources.txt
javac --release 17 -encoding UTF-8 -d out/classes @out/main-sources.txt

if [[ "${1:-}" != "--skip-tests" ]]; then
    find src/test/java -name '*.java' > out/test-sources.txt
    javac --release 17 -encoding UTF-8 -cp "out/classes${SEP}${JUNIT}" -d out/test-classes @out/test-sources.txt
    java -jar "$JUNIT" execute --disable-banner --class-path "out/classes${SEP}out/test-classes" --scan-class-path
fi

jar cfm minibank.jar manifest.txt -C out/classes .
echo "Built minibank.jar - run it with: java -jar minibank.jar"
