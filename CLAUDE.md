# CLAUDE.md

KinderERP: desktop management for a kindergarten (students, parents, classes, attendance, tuition, staff,
payroll). JavaFX UI, Spring Boot for DI/transactions, Spring Data JPA on SQLite, Flyway migrations.
One installation = one school; everything school-specific lives in the `school_settings` table.
UI languages: French and Arabic (RTL). See README.md for features, data locations and packaging.

## Commands

- Use the wrapper: `./mvnw` (system Maven 3.3 is too old for Spring Boot 4). JDK 21.
- Run: `./mvnw javafx:run` — data goes to `%APPDATA%\KinderERP` (set `KINDERERP_HOME` to use another folder).
- Tests: `./mvnw test` (temporary DB under `target/test-db`, never the real one).
- Installer: `packaging/build-installer.ps1 [-Type exe|msi|app-image]` (exe/msi need WiX 3).
- Screens in FR + AR as PNG (visual check, especially RTL): build classpath with
  `./mvnw dependency:build-classpath -Dmdep.outputFile=target/cp.txt`, then run
  `com.kindererp.tools.ScreenshotTour <outDir>` with `target/classes;target/test-classes;<cp>`.

## Layout (`src/main/java/com/kindererp`)

- `KinderErpApplication` (main, applies pending restore, launches FX) / `FxApplication` (starts Spring, routes to setup / license / login).
- `config/` `AppPaths` (data folder), `DataSourceConfig` (Hikari + SQLite pragmas: foreign_keys, WAL, busy_timeout).
- `model/` entities. `StaffMember`, `StaffAttendanceRecord`, `StaffPaymentRecord` are `@MappedSuperclass`es shared by Teacher/Employee.
  Converters (auto-applied): `LocalDate` → ISO text, `LocalTime` → "HH:mm", `BigDecimal` → INTEGER thousandths (every BigDecimal is money).
- `repository/` Spring Data. `service/` all business rules; throw `BusinessException(messageKey, args)` for user-fixable errors. `service/dto` records for screens.
- `controller/` JavaFX controllers, **all `@Scope("prototype")`** (a new instance per view load). `util/` UI infrastructure.

## Conventions

- DB access from controllers only through services, via `FxAsync.run(busyNode, work, onSuccess)` / `runAction(...)` (single worker thread; failures shown by `Dialogs.showError`).
- Views: `ViewLoader.load(name)` / `ViewLoader.dialog(name, titleKey, owner)` (applies translations, stylesheet, RTL, icon). Never `new FXMLLoader` directly.
- Text: `%key` in FXML, `I18n.get(key, args)` in code. Every key must exist in both `i18n/messages_fr.properties` and `messages_ar.properties` (UTF-8, typographic apostrophe ’ in French). `TranslationKeysTest` enforces it.
- Enums are displayed via keys `<enumName>.<VALUE>` (`ComboBoxes.enumConverter("paymentMethod")`).
- Styling only in `styles/app.css` (no `style=` / `setStyle`). Use logical alignments so RTL mirrors correctly.
- Forms: `FormValidator` (highlights fields, shows all problems); services re-check the same rules (`Checks`).
- Schema changes: new Flyway file `db/migration/V<n>__*.sql`; never edit applied migrations. Hibernate runs `validate`.
  SQLite specifics: id columns must be `integer primary key` (entities declare `columnDefinition = "integer"`, FK columns are `integer`); money columns `bigint`; no `@Lob` (driver lacks Blob support — use plain `byte[]`).
- School-specific values (name, logo, currency, fees, calendar) come from `AppState.settings()`; never hardcode them.

## Licensing

`LicenseService`: machine ID from Windows MachineGuid; key = Ed25519 signature (public key in `LicenseService.PUBLIC_KEY`).
Keys are generated with `tools/LicenseKeyGenerator.java` and the vendor private key, which must stay outside the repo.

## Do not

- Commit databases, backups, exports, logs or `*.key` files (all git-ignored).
- Add real children's or parents' data to fixtures, demo data or docs.
