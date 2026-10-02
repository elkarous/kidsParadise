package com.kindererp.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;

/**
 * Loads FXML views with the current translations and Spring-created controllers, and builds
 * scenes/windows that all share the app stylesheet, icon and text direction (RTL in Arabic).
 */
public final class ViewLoader {

    public static final String STYLESHEET = "/styles/app.css";

    private static ApplicationContext context;

    private ViewLoader() {
    }

    public record View<C>(Parent root, C controller) {
    }

    public record Dialog<C>(Stage stage, C controller) {
    }

    public static void setContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    /** Loads {@code /fxml/<name>.fxml}. */
    public static <C> View<C> load(String name) {
        URL url = Objects.requireNonNull(ViewLoader.class.getResource("/fxml/" + name + ".fxml"), "Missing view " + name);
        FXMLLoader loader = new FXMLLoader(url, I18n.bundle());
        loader.setControllerFactory(context::getBean);
        try {
            Parent root = loader.load();
            return new View<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load view " + name, e);
        }
    }

    public static Scene scene(Parent root) {
        Scene scene = new Scene(root);
        style(scene);
        return scene;
    }

    public static void style(Scene scene) {
        String css = Objects.requireNonNull(ViewLoader.class.getResource(STYLESHEET)).toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
        scene.setNodeOrientation(I18n.orientation());
    }

    public static void decorate(Stage stage) {
        if (stage.getIcons().isEmpty()) {
            stage.getIcons().add(AppState.productIcon());
        }
    }

    /** A modal window around a view; call {@code dialog.stage().showAndWait()} after passing data. */
    public static <C> Dialog<C> dialog(String name, String titleKey, Window owner) {
        View<C> view = load(name);
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle(I18n.get(titleKey));
        stage.setScene(scene(view.root()));
        decorate(stage);
        return new Dialog<>(stage, view.controller());
    }
}
