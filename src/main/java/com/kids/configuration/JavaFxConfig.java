package com.kids.configuration;


import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Provides ResourceBundle beans for JavaFX controllers.
 *
 * The active locale is resolved from:
 *   1. System property:  -Dapp.locale=ar
 *   2. Application property: app.locale=ar
 *   3. Default: system locale
 *
 * To run in Arabic:  java -Dapp.locale=ar -jar garden-school.jar
 * To run in English: java -Dapp.locale=en -jar garden-school.jar
 */
@Configuration
@RequiredArgsConstructor
public class JavaFxConfig {

    @Bean
    public Locale activeLocale() {
        String lang = System.getProperty("app.locale",
                System.getenv().getOrDefault("APP_LOCALE", "ar"));
        return switch (lang) {
            case "ar" -> new Locale("ar", "TN");
            default   -> Locale.ENGLISH;
        };
    }

    @Bean
    public ResourceBundle resourceBundle(Locale activeLocale) {
        return ResourceBundle.getBundle("messages", activeLocale);
    }
}

