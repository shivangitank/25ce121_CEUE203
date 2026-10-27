#!/usr/bin/env bash
# Compiles MiniBank and packages it as a runnable JAR (minibank.jar).
set -euo pipefail
cd "$(dirname "$0")"

rm -rf out
mkdir -p out/classes
find src/main/java -name '*.java' > out/main-sources.txt
javac --release 17 -encoding UTF-8 -d out/classes @out/main-sources.txt

jar cfm minibank.jar manifest.txt -C out/classes .
echo "Built minibank.jar - run it with: java -jar minibank.jar"
