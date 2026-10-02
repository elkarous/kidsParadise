package com.kindererp.util;

import javafx.geometry.NodeOrientation;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Current UI language (French or Arabic), its translations ({@code i18n/messages_xx.properties},
 * UTF-8) and formatting locale. The chosen language is remembered per Windows user.
 */
@Slf4j
public final class I18n {

    public enum Language {
        FR("fr", "Français"),
        AR("ar", "العربية");

        private final String code;
        private final String nativeName;

        Language(String code, String nativeName) {
            this.code = code;
            this.nativeName = nativeName;
        }

        public String code() {
            return code;
        }

        @Override
        public String toString() {
            return nativeName;
        }
    }

    private static final String BUNDLE = "i18n.messages";
    private static final String PREF_LANGUAGE = "language";
    private static final Preferences PREFS = Preferences.userRoot().node("com/kindererp");

    private static Language language = savedLanguage();
    private static String region = "TN";
    private static ResourceBundle bundle;

    private I18n() {
    }

    public static Language language() {
        return language;
    }

    public static boolean isRtl() {
        return language == Language.AR;
    }

    public static NodeOrientation orientation() {
        return isRtl() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT;
    }

    /** Locale used to format dates, numbers and month names (language + school country, Latin digits). */
    public static Locale locale() {
        return Locale.forLanguageTag(language.code() + "-" + region + "-u-nu-latn");
    }

    public static synchronized ResourceBundle bundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE, Locale.of(language.code()),
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        }
        return bundle;
    }

    /** Translated text; {0}, {1}... placeholders are filled with {@code args}. */
    public static String get(String key, Object... args) {
        String text;
        try {
            text = bundle().getString(key);
        } catch (MissingResourceException e) {
            log.warn("Missing translation '{}' for {}", key, language);
            return key;
        }
        return args.length == 0 ? text : new MessageFormat(text.replace("'", "''"), locale()).format(bidiSafe(args));
    }

    /**
     * In Arabic, numeric arguments such as "2026-2027" or "12/05" are wrapped in a left-to-right
     * embedding so the bidi algorithm does not swap their parts inside a right-to-left sentence.
     */
    private static Object[] bidiSafe(Object[] args) {
        if (!isRtl()) {
            return args;
        }
        Object[] safe = args.clone();
        for (int i = 0; i < safe.length; i++) {
            if (safe[i] instanceof String s && s.matches("[\\d\\s.,:/\\-]+")) {
                safe[i] = "‪" + s + "‬";
            }
        }
        return safe;
    }

    public static synchronized void setLanguage(Language newLanguage) {
        language = newLanguage;
        bundle = null;
        Locale.setDefault(locale());
        PREFS.put(PREF_LANGUAGE, newLanguage.code());
    }

    /** Country of the school (from its settings), used for regional formats. */
    public static synchronized void setRegion(String countryCode) {
        if (countryCode != null && countryCode.matches("[A-Z]{2}")) {
            region = countryCode;
            Locale.setDefault(locale());
        }
    }

    private static Language savedLanguage() {
        String saved = PREFS.get(PREF_LANGUAGE, null);
        if (saved == null) {
            saved = "ar".equals(Locale.getDefault().getLanguage()) ? "ar" : "fr";
        }
        return "ar".equals(saved) ? Language.AR : Language.FR;
    }
}
