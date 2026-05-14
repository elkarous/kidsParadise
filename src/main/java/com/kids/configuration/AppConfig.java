package com.kids.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Application configuration.
 *
 * UTF-8 note:
 * Spring Boot 2.7+ reads .properties files as UTF-8 by default (spring.messages.encoding).
 * For older versions, add to application.properties:
 *   spring.messages.encoding=UTF-8
 *   spring.messages.basename=i18n/messages
 */
@Configuration
@EnableJpaAuditing
public class AppConfig {

    /**
     * Message source for Spring's @Autowired MessageSource (REST controllers, services).
     * JavaFX controllers use ResourceBundle directly (see JavaFxConfig below).
     */
    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource src = new ResourceBundleMessageSource();
        src.setBasename("i18n/messages");
        src.setDefaultEncoding("UTF-8");
        src.setFallbackToSystemLocale(false);
        return src;
    }
}
