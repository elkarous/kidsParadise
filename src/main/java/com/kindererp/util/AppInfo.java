package com.kindererp.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Product name and version, filled in from the Maven build (app-info.properties). */
public final class AppInfo {

    private static final Properties INFO = load();

    private AppInfo() {
    }

    public static String name() {
        return INFO.getProperty("app.name", "KinderERP");
    }

    public static String version() {
        return INFO.getProperty("app.version", "dev");
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = AppInfo.class.getResourceAsStream("/app-info.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            // Defaults above are used.
        }
        return properties;
    }
}
