package com.kindererp.util;

import com.kindererp.model.AppUser;
import com.kindererp.model.SchoolSettings;
import com.kindererp.model.SchoolYear;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.util.Objects;

/** Session state shared by the screens: logged-in user, selected school year and school settings. */
public final class AppState {

    private static final String PRODUCT_ICON = "/images/kindererp-icon.png";

    private static AppUser currentUser;
    private static SchoolYear currentSchoolYear;
    private static final ObjectProperty<SchoolSettings> SETTINGS = new SimpleObjectProperty<>(new SchoolSettings());

    private AppState() {
    }

    public static AppUser currentUser() {
        return currentUser;
    }

    public static void setCurrentUser(AppUser user) {
        currentUser = user;
    }

    public static SchoolYear currentSchoolYear() {
        return currentSchoolYear;
    }

    public static void setCurrentSchoolYear(SchoolYear year) {
        currentSchoolYear = year;
    }

    public static SchoolSettings settings() {
        return SETTINGS.get();
    }

    /** Observable so open screens can refresh the school name and logo after a change. */
    public static ObjectProperty<SchoolSettings> settingsProperty() {
        return SETTINGS;
    }

    public static void setSettings(SchoolSettings newSettings) {
        I18n.setRegion(newSettings.getRegion());
        SETTINGS.set(Objects.requireNonNull(newSettings));
    }

    public static String schoolName() {
        return settings().displayName(I18n.isRtl());
    }

    /** The school logo, or the product icon when the school has none. */
    public static Image logo() {
        byte[] logo = settings().getLogo();
        if (logo != null && logo.length > 0) {
            Image image = new Image(new ByteArrayInputStream(logo));
            if (!image.isError()) {
                return image;
            }
        }
        return productIcon();
    }

    public static Image productIcon() {
        return new Image(Objects.requireNonNull(AppState.class.getResourceAsStream(PRODUCT_ICON)));
    }

    public static void logout() {
        currentUser = null;
        currentSchoolYear = null;
    }
}
