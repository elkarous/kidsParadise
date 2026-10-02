# KinderERP

Desktop management software for kindergartens: children and families, classes, daily attendance,
monthly tuition, teachers and staff, payroll. French and Arabic (right-to-left) interface.
Each installation serves one school, whose identity (name, logo, currency, fees) is set on first launch.

- **Platform:** Windows 10/11, installed with a native installer that bundles its own Java runtime.
- **Stack:** Java 21, JavaFX 21, Spring Boot 4 (dependency injection, transactions), Spring Data JPA /
  Hibernate 7, SQLite, Flyway, Apache POI.

## Features by module

| Module | What it does |
|---|---|
| First-launch setup | School name (FR/AR), logo, address, country, currency, fees, first school year, first administrator account (no default password). Optional fictitious demo data. |
| Login | Username/password (BCrypt), school year selection, language switch (remembered per Windows user). |
| Students | Registration and edit (name, birth date, class, parent), search, filter by level/class. |
| Parents | Contact details, number of enrolled children, monthly fee after sibling discount. |
| Tuition | Per month: who paid, amounts, totals expected / collected / pending; register a payment and print the receipt with the school header. Closed months refuse payments. |
| Student attendance | Daily register per class (present, late, absent, excused, notes). |
| Staff | Teachers and employees, fixed monthly salary or paid per session, absence penalty, status. |
| Staff attendance | Daily status, arrival/departure time, sessions worked. |
| Payroll | Salary due per month from attendance, advances and final payment, printable pay slip. |
| Settings | School identity and fees, school years and months (open/close), levels and classes, users, backup / restore, Excel export of all data, version and license. |

## Requirements (development)

- JDK 21 (includes `jpackage`). Maven is provided by the wrapper (`mvnw` / `mvnw.cmd`); the
  system Maven may be too old for Spring Boot 4.
- To build `.exe` / `.msi` installers: [WiX Toolset 3.14](https://github.com/wixtoolset/wix3/releases) on the `PATH`.

## Build, run, test

```powershell
.\mvnw.cmd clean javafx:run        # run the application
.\mvnw.cmd test                    # unit + integration tests (temporary SQLite files, never the real database)
.\mvnw.cmd clean package           # jar in target/, runtime libraries in target/lib/
```

Main class: `com.kindererp.KinderErpApplication`.

## Package (Windows installer)

```powershell
.\packaging\build-installer.ps1                 # target\installer\KinderERP-<version>.exe  (needs WiX)
.\packaging\build-installer.ps1 -Type msi       # .msi (needs WiX)
.\packaging\build-installer.ps1 -Type app-image # portable folder target\installer\KinderERP\ (no WiX), for testing
```

The installer bundles a Java runtime (the school does not need Java), the product icon and the
Arabic font. It contains **no database**. Updating installs over the previous version (fixed upgrade
UUID in the script — never change it). Uninstalling or updating does not touch the data folder.

To publish a new version: change `<version>` in `pom.xml`, rebuild the installer.

## Where the data is stored

| What | Location |
|---|---|
| Database | `%APPDATA%\KinderERP\kindererp.db` (SQLite, created on first launch by the Flyway migrations) |
| Automatic backups | `%APPDATA%\KinderERP\backups\auto-YYYY-MM-DD.db` (one per day at startup, last 7 days kept) |
| Logs | `%APPDATA%\KinderERP\logs\kindererp.log` (rotated, 30 files kept) |

Override with Java options in the installed app's `app\KinderERP.cfg` or on the command line:
`-Dkindererp.home=D:\KinderERP` (whole folder) or `-Dkindererp.db=D:\data\school.db` (database only).
The environment variable `KINDERERP_HOME` works too.

Database rules: foreign keys are enforced and WAL mode is on for every connection; dates are stored as
ISO text (`yyyy-MM-dd`); amounts as INTEGER thousandths (12.500 → `12500`), never floating point.
Schema changes go in a new migration file `src/main/resources/db/migration/V<n>__description.sql`
(never edit an applied migration); Hibernate only validates the schema.

## Licensing (vendor side)

Each installation shows a **machine ID** (Settings › About & license). A license key is an Ed25519
signature of that ID; the application only contains the public key. There is a 30-day trial.

```powershell
# once: create the vendor key pair (prints the public key to put in LicenseService.PUBLIC_KEY)
java tools\LicenseKeyGenerator.java init $env:USERPROFILE\.kindererp-vendor\license-private.key
# per customer: create the key for their machine ID
java tools\LicenseKeyGenerator.java sign $env:USERPROFILE\.kindererp-vendor\license-private.key 7D01-857F-F4B1-6D3C
```

Keep the private key secret and backed up, **outside the repository** (`*.key` is git-ignored).
Losing it means existing customers cannot get new keys from you.

## Project structure

```
src/main/java/com/kindererp/
  KinderErpApplication, FxApplication   entry point, JavaFX startup (setup / license / login routing)
  config/       data folder resolution (AppPaths), SQLite DataSource
  model/        JPA entities and enums; converters for dates, times and money
  repository/   Spring Data repositories
  service/      business rules (one service per area), BusinessException, DTOs
  controller/   JavaFX controllers (prototype scope, one per view)
  util/         view loading + RTL, dialogs, background tasks, formats, validation, printing
src/main/resources/
  fxml/         views (settings/ holds the Settings tabs)
  styles/app.css the single stylesheet
  i18n/         messages_fr.properties, messages_ar.properties (UTF-8, same keys)
  db/migration/ Flyway SQL migrations
  fonts/        Tajawal (SIL Open Font License, see Tajawal-OFL.txt)
packaging/      installer script and icon
tools/          LicenseKeyGenerator (vendor only, not shipped)
src/test/java/  JUnit 5 tests; tools/ScreenshotTour renders every screen in FR and AR to PNG
```

## Translations

All visible text lives in `src/main/resources/i18n/messages_fr.properties` and
`messages_ar.properties` (UTF-8). Use `%key` in FXML and `I18n.get("key")` in code. A test fails if a key
exists in one language only, or if a key used in a view or in the code is missing.

## User documentation

- [Guide d'utilisation (français)](docs/USER_GUIDE_fr.md)
- [دليل الاستعمال (العربية)](docs/USER_GUIDE_ar.md)
