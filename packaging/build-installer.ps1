<#
.SYNOPSIS
  Builds the KinderERP Windows installer with jpackage (bundled Java runtime, no Java needed on the client).

.EXAMPLE
  .\packaging\build-installer.ps1                 # .exe installer (needs WiX Toolset 3.x on PATH)
  .\packaging\build-installer.ps1 -Type msi       # .msi installer (needs WiX Toolset 3.x on PATH)
  .\packaging\build-installer.ps1 -Type app-image # portable folder, no WiX needed (for testing)

.NOTES
  - Requires a JDK 21 (jpackage is part of the JDK). WiX 3.14: https://github.com/wixtoolset/wix3/releases
  - The installer contains no database: the app creates an empty one in %APPDATA%\KinderERP on first run.
  - Uninstalling or updating never touches %APPDATA%\KinderERP (database, backups, logs).
  - Keep UpgradeUuid unchanged forever: Windows uses it to replace an older version during an update.
#>
param(
    [ValidateSet("exe", "msi", "app-image")]
    [string]$Type = "exe",
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$name = "KinderERP"
$mainClass = "com.kindererp.KinderErpApplication"
$upgradeUuid = "dbeaad62-5cab-448e-af7c-0a6e7a7213a1"

# 1. Build the jar and copy runtime dependencies into target/lib
$mvnArgs = @("-B", "clean", "package")
if ($SkipTests) { $mvnArgs += "-DskipTests" }
# Native tools write warnings to stderr; judge them by their exit code only.
$ErrorActionPreference = "Continue"
& .\mvnw.cmd @mvnArgs
$ErrorActionPreference = "Stop"
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

# 2. Version from the pom (e.g. 1.0.0) and the application jar
[xml]$pom = Get-Content pom.xml
$version = $pom.project.version
$jar = "kindererp-$version.jar"

# 3. jpackage input folder: application jar + its libraries
$input = "target\jpackage-input"
Remove-Item -Recurse -Force $input -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $input | Out-Null
Copy-Item "target\$jar" $input
Copy-Item "target\lib\*.jar" $input

# Java modules of the bundled runtime. jdk.localedata is required for Arabic/French dates and numbers.
$modules = @(
    "java.base", "java.compiler", "java.datatransfer", "java.desktop", "java.instrument", "java.logging",
    "java.management", "java.naming", "java.net.http", "java.prefs", "java.rmi", "java.scripting",
    "java.security.jgss", "java.sql", "java.transaction.xa", "java.xml", "jdk.charsets", "jdk.crypto.ec",
    "jdk.localedata", "jdk.management", "jdk.unsupported", "jdk.zipfs"
) -join ","

$dest = "target\installer"
Remove-Item -Recurse -Force $dest -ErrorAction SilentlyContinue

$jpackageArgs = @(
    "--type", $Type,
    "--name", $name,
    "--app-version", $version,
    "--vendor", $name,
    "--description", "Kindergarten management - gestion de jardin d enfants",
    "--icon", "packaging\kindererp.ico",
    "--input", $input,
    "--main-jar", $jar,
    "--main-class", $mainClass,
    "--add-modules", $modules,
    "--java-options", "-Xmx768m",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--dest", $dest
)
if ($Type -ne "app-image") {
    $jpackageArgs += @(
        "--win-dir-chooser",
        "--win-menu",
        "--win-menu-group", $name,
        "--win-shortcut",
        "--win-shortcut-prompt",
        "--win-upgrade-uuid", $upgradeUuid
    )
}

$ErrorActionPreference = "Continue"
& jpackage @jpackageArgs
$ErrorActionPreference = "Stop"
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

# jpackage marks some outputs read-only, which would make the next "mvn clean" fail
Get-ChildItem -Recurse -File $dest | ForEach-Object { $_.IsReadOnly = $false }
Write-Host "Done: $dest"
Get-ChildItem $dest
