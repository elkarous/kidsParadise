package com.kindererp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** French and Arabic must stay complete and in sync, and every key the app uses must exist. */
class TranslationKeysTest {

    private static final Path MAIN = Path.of("src/main");
    private static final List<String> ENUM_PREFIXES = List.of("attendanceStatus", "paymentMethod", "salaryType", "staffStatus");

    private static Properties load(String language) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = TranslationKeysTest.class.getResourceAsStream("/i18n/messages_" + language + ".properties")) {
            properties.load(new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8));
        }
        return properties;
    }

    @Test
    void frenchAndArabicHaveExactlyTheSameKeys() throws IOException {
        Set<String> fr = load("fr").stringPropertyNames();
        Set<String> ar = load("ar").stringPropertyNames();

        assertThat(diff(fr, ar)).as("keys in messages_fr but missing in messages_ar").isEmpty();
        assertThat(diff(ar, fr)).as("keys in messages_ar but missing in messages_fr").isEmpty();
    }

    @Test
    void noTranslationIsEmpty() throws IOException {
        for (String language : List.of("fr", "ar")) {
            Properties properties = load(language);
            assertThat(properties.stringPropertyNames()).as("empty values in " + language)
                    .filteredOn(key -> properties.getProperty(key).isBlank()).isEmpty();
        }
    }

    @Test
    void filesAreValidUtf8() throws IOException {
        for (String language : List.of("fr", "ar")) {
            byte[] bytes = Files.readAllBytes(MAIN.resolve("resources/i18n/messages_" + language + ".properties"));
            try {
                StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes));
            } catch (CharacterCodingException e) {
                throw new AssertionError("messages_" + language + " is not valid UTF-8", e);
            }
        }
    }

    @Test
    void everyKeyUsedInViewsAndCodeExists() throws IOException {
        Set<String> defined = load("fr").stringPropertyNames();
        Set<String> used = new TreeSet<>();
        used.addAll(find(MAIN.resolve("resources/fxml"), ".fxml", Pattern.compile("\"%([a-zA-Z0-9._]+)\"")));
        used.addAll(find(MAIN.resolve("java"), ".java", Pattern.compile("\"([a-z][a-zA-Z0-9]*(?:\\.[a-zA-Z0-9]+)+)\""))
                .stream().filter(key -> defined.contains(key) || looksLikeTranslationKey(key)).toList());

        assertThat(diff(used, defined)).as("keys used but not translated").isEmpty();
        for (String prefix : ENUM_PREFIXES) {
            assertThat(defined).as("enum translations for " + prefix).anyMatch(k -> k.startsWith(prefix + "."));
        }
    }

    /** String literals in Java that look like keys of our translation families. */
    private static boolean looksLikeTranslationKey(String literal) {
        String family = literal.substring(0, literal.indexOf('.'));
        return Set.of("btn", "dialog", "error", "validation", "login", "setup", "main", "nav", "school", "fees",
                "students", "student", "class", "classes", "parents", "parent", "tuition", "payment", "payments",
                "month", "attendance", "staffAttendance", "staff", "payroll", "print", "settings", "schoolYears",
                "users", "data", "backup", "table", "file", "msg", "filter", "license", "about", "export").contains(family);
    }

    private static Set<String> find(Path root, String extension, Pattern pattern) throws IOException {
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(f -> f.toString().endsWith(extension)).toList()) {
                Matcher matcher = pattern.matcher(Files.readString(file));
                while (matcher.find()) {
                    keys.add(matcher.group(1));
                }
            }
        }
        return keys;
    }

    private static Set<String> diff(Set<String> a, Set<String> b) {
        return a.stream().filter(k -> !b.contains(k)).collect(Collectors.toCollection(TreeSet::new));
    }
}
