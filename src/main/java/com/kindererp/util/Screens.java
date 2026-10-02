package com.kindererp.util;

import javafx.scene.Parent;
import javafx.stage.Stage;

/** Switches the main window between the top-level screens (setup, login, application). */
public final class Screens {

    private static Stage primaryStage;

    private Screens() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
        ViewLoader.decorate(stage);
    }

    public static Stage stage() {
        return primaryStage;
    }

    public static void showSetup() {
        show("setup", false);
    }

    public static void showLogin() {
        show("login", false);
    }

    public static void showActivation() {
        show("license", false);
    }

    public static void showMain() {
        show("main", true);
    }

    /** Reloads the current top-level screen, e.g. after a language change. */
    public static void reload(String viewName, boolean maximized) {
        show(viewName, maximized);
    }

    public static void updateTitle() {
        String school = AppState.schoolName();
        primaryStage.setTitle(school.isBlank() ? "KinderERP" : school + " — KinderERP");
    }

    private static void show(String viewName, boolean maximized) {
        Parent root = ViewLoader.load(viewName).root();
        if (primaryStage.getScene() == null) {
            primaryStage.setScene(ViewLoader.scene(root));
        } else {
            primaryStage.getScene().setRoot(root);
            ViewLoader.style(primaryStage.getScene());
        }
        primaryStage.setMinWidth(maximized ? 1000 : 0);
        primaryStage.setMinHeight(maximized ? 650 : 0);
        updateTitle();
        primaryStage.setMaximized(maximized);
        if (!maximized) {
            primaryStage.sizeToScene();
            primaryStage.centerOnScreen();
        }
        primaryStage.show();
    }
}
