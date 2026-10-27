# Compiles MiniBank and packages it as a runnable JAR (minibank.jar).
# Run with:  powershell -ExecutionPolicy Bypass -File build.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory -Force out/classes | Out-Null
$mainSources = Get-ChildItem -Recurse src/main/java -Filter *.java | Resolve-Path -Relative
javac --release 17 -encoding UTF-8 -d out/classes $mainSources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }

jar cfm minibank.jar manifest.txt -C out/classes .
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed' }
Write-Host 'Built minibank.jar - run it with: java -jar minibank.jar'
