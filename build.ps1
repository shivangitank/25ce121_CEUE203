# Builds MiniBank: compiles the code, runs the JUnit 5 tests and packages minibank.jar.
#   powershell -ExecutionPolicy Bypass -File build.ps1              compile + test + jar
#   powershell -ExecutionPolicy Bypass -File build.ps1 -SkipTests   compile + jar
param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$junit = 'lib/junit-platform-console-standalone-1.11.4.jar'

if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory -Force out/classes, out/test-classes | Out-Null
$mainSources = Get-ChildItem -Recurse src/main/java -Filter *.java | Resolve-Path -Relative
javac --release 17 -encoding UTF-8 -d out/classes $mainSources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }

if (-not $SkipTests) {
    $testSources = Get-ChildItem -Recurse src/test/java -Filter *.java | Resolve-Path -Relative
    javac --release 17 -encoding UTF-8 -cp "out/classes;$junit" -d out/test-classes $testSources
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
    java -jar $junit execute --disable-banner --class-path "out/classes;out/test-classes" --scan-class-path
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
}

jar cfm minibank.jar manifest.txt -C out/classes .
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed' }
Write-Host 'Built minibank.jar - run it with: java -jar minibank.jar'
