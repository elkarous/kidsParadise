# Kindergarten ERP: Cleanup & Delivery Prompts for Claude Code

JavaFX · JPA · SQLite · French + Arabic · Multi-school product

Paste these prompts into Claude Code **one at a time, in order**. After each step, run the app, check the main screens, and commit.

---

## Step 0: Before you start

Run these yourself in the project folder:

```
git init
git add .
git commit -m "Original version before cleanup"
git checkout -b cleanup
```

Tips:
- Press **Shift+Tab** in Claude Code to use plan mode for big steps (3, 4, 5), so you review the plan before files change.
- Never skip testing between steps. If something breaks, `git checkout .` brings you back.

---

## Step 1: Audit (changes nothing)

```
This is a JavaFX ERP for a kindergarten (garden school). It uses JPA with SQLite and supports French and Arabic. Analyze the whole project without changing anything. Give me a report covering: build system (Maven/Gradle/none), Java and JavaFX versions, package structure, database access, hardcoded values (paths, school info, settings), dead or duplicated code, unused files, classes that are too large, missing error handling, how translations are handled, and security issues. Rank the problems by priority. Then create a CLAUDE.md describing the project, its modules, and its conventions.
```

---

## Step 2: Build system

```
Set up a clean Maven build (or fix the existing one) with the JavaFX plugin, a pinned Java LTS version, and all dependencies declared. Set the project encoding to UTF-8. The app must build and run with `mvn clean javafx:run`. Remove any jars committed into the project and add a proper .gitignore (including *.db files).
```

---

## Step 3: Project structure

```
Reorganize the code into a clear MVC structure: model, view (FXML + CSS in resources), controller, repository/dao, service, util. Move business logic out of controllers into services. Fix all imports and FXML paths. Don't change behavior. Show me the plan before moving files.
```

---

## Step 4: Data layer (JPA + SQLite)

### 4a. Decide: keep JPA or switch to JDBI

```
The app uses JPA with SQLite. Audit the data layer and tell me honestly: is JPA causing real problems here (startup time, lazy-loading errors, wrong SQLite dialect, transaction issues, entities leaking into the UI)? Recommend either keeping JPA or migrating to JDBI, with the effort involved. Don't change anything until I choose.
```

### 4b. Option A: keep JPA and clean it up

```
Keep JPA and clean it up:
- Use Hibernate with the official SQLiteDialect from hibernate-community-dialects, and the xerial sqlite-jdbc driver.
- Set hbm2ddl.auto to "validate", never "update" or "create", and manage the schema with Flyway migrations.
- Use a single EntityManagerFactory created at startup, and one EntityManager per operation in the service layer, with explicit transactions.
- Fetch the data each screen needs inside the service (JOIN FETCH or DTOs), so controllers never trigger lazy loading.
- Run database calls off the JavaFX thread with Task, and update the UI on the FX thread.
```

### 4b. Option B: migrate to JDBI

```
Migrate from JPA to JDBI 3 with the SqlObject plugin, one module at a time, starting with the smallest one:
- Replace entities with plain model classes or records, and repositories with JDBI DAO interfaces using explicit SQL.
- Keep the same database schema and data. Manage it with Flyway.
- Keep the service layer's public methods the same so controllers don't change.
- After each module, run the app and the tests before moving on to the next one.
- Remove Hibernate and JPA dependencies only when nothing uses them anymore.
- Run database calls off the JavaFX thread with Task.
```

### 4c. SQLite setup (do this whichever option you chose)

```
Finish the SQLite setup:
- Enable foreign keys on every connection (PRAGMA foreign_keys = ON, it's off by default in SQLite) and use WAL mode.
- Store the .db file in the user's app data folder (e.g. %APPDATA%/AppName on Windows), never inside the install folder, which is read-only once installed. The path must be configurable.
- On first launch, create the database automatically through the Flyway migrations if the file doesn't exist.
- Store dates in ISO format (yyyy-MM-dd) and amounts as INTEGER in the smallest unit (or as TEXT with BigDecimal), never as floating-point numbers.
- Add Backup and Restore buttons in settings that copy the .db file, plus an automatic backup at startup that keeps the last 7 days.
```

---

## Step 5: Make the app generic (multi-school)

```
This app was built for one kindergarten. I want to sell it to many schools. Make it generic:
- Find everything specific to the current school: name, logo, address, phone, director's name, fees, class names, school year, receipt and report headers, window titles, and any hardcoded IDs or values. Give me the full list before changing anything.
- Move all of that into a "school settings" table in the database, editable from a Settings screen in the app (name in French and Arabic, logo upload, address, phone, currency, school year, classes/sections, fee types and amounts).
- Add a first-launch setup wizard that asks for the school info and creates the first admin account. No default password hardcoded in the code. Passwords must be hashed (BCrypt).
- Use the school's name and logo from settings in all receipts, reports, printouts, and the main window.
- Rename packages, the app name, and the installer to a neutral product name: [YOUR PRODUCT NAME], with no reference to the original school.
- Delete any real data: real .db files, sample data with real children's or parents' names, photos, and exports. Replace them with an optional demo dataset of clearly fake data.
- Search the whole project (code, FXML, CSS, images, properties, SQL, docs) for any leftover traces of the original school and list them.
```

---

## Step 6: Code cleanup

```
Remove dead code, unused classes, unused imports, commented-out blocks, and debug System.out.println calls. Merge duplicated code into shared methods. Use English for code names (classes, methods, variables). Don't touch any user-visible French or Arabic text; that's handled in the next step. List everything you deleted.
```

---

## Step 7: Translations (French + Arabic)

```
The app supports Arabic and French. Audit how translations are handled now, then make it clean and consistent:
- Move every user-visible string (FXML, controllers, alerts, validation messages, reports) into ResourceBundles: messages_fr.properties and messages_ar.properties, saved as UTF-8.
- Load FXML with the ResourceBundle and use %key in FXML instead of hardcoded text.
- Arabic must switch the whole UI to right-to-left with NodeOrientation.RIGHT_TO_LEFT on the root of every scene, including dialogs and alerts.
- Use a font that renders Arabic correctly, and check that Arabic text isn't cut off or misaligned in tables, forms, and buttons.
- Add a language switcher (in settings or the login screen) that remembers the choice.
- Format dates, numbers, and currency according to the selected locale.
- Give me a list of keys that exist in one language but are missing in the other.
```

---

## Step 8: Errors, validation, logging

```
Add proper error handling everywhere: user-friendly alert dialogs (translated) instead of crashes or stack traces, input validation on every form (required fields, dates, phone numbers, amounts), and logging to a file in the app data folder with SLF4J + Logback instead of printStackTrace.
```

---

## Step 9: UI consistency

```
Unify the UI: one shared CSS stylesheet, consistent buttons, fonts, spacing and colors across all screens, and consistent window titles and icons. Fix layouts that break when the window is resized. Test every screen in both French and Arabic. In Arabic (RTL), check that icons, margins, and table columns are mirrored correctly.
```

---

## Step 10: Tests

```
Add JUnit 5 tests for the service layer, prioritizing the critical logic: student registration, payments/fees, and attendance. Run the tests against a temporary or in-memory SQLite database, never the real one. Add a test that fails if a key is present in messages_fr but missing in messages_ar, or vice versa. Make sure `mvn test` passes.
```

---

## Step 11: Selling features (license, version, export)

```
Add a simple license system: each installation shows a machine ID, and the app needs a license key tied to that ID to unlock it (with a 30-day trial). Also add a version number on the About screen and an export of all school data to Excel, so schools can get their data out.
```

---

## Step 12: Packaging

```
Package the app as a native installer using jpackage (Windows .exe/.msi as a priority), with a bundled JRE so the client doesn't need Java installed, plus the product icon and name. Bundle the Arabic font. Don't bundle a pre-filled .db file: the app creates an empty one on first run. Make sure uninstalling or updating doesn't delete the database. Add the build commands to the README.
```

---

## Step 13: Documentation

```
Write a README.md covering: what the app does, requirements, how to build, run and package, where the database is stored, and a list of features by module. Also write a short USER_GUIDE in both French and Arabic for school staff, in simple non-technical language, covering first-time setup, daily use, backup/restore, and a short privacy note reminding each school that it is responsible for the children's data it stores.
```

---

## Step 14: Final check before giving the code to anyone

```
Check the entire git history for committed .db files, images, exports, or files containing real personal data. If there are any, remove them from history with git filter-repo and tell me what was removed. Then do a final review: the project builds from scratch, all tests pass, the installer works on a clean machine, and there's no trace of the original school anywhere.
```

Simpler alternative: start a brand-new git repo from the cleaned code, so the old history never leaves your computer.

---

## Before delivering to a school: manual checklist

- [ ] Install on a clean Windows PC with no Java installed
- [ ] Setup wizard works; school name and logo appear on receipts and reports
- [ ] Whole app works in French and in Arabic (RTL), including popups
- [ ] Arabic names save and display correctly
- [ ] Backup and restore work
- [ ] Updating the app keeps the existing data
- [ ] License/trial works
- [ ] No real children's data anywhere in the installer or the code

> Children's data is sensitive. Check the personal data protection law in each country where you sell (e.g. Law 18-07 in Algeria, Law 09-08 in Morocco). This is not legal advice.
