package com.kids.configuration;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ResourceBundle;

/**
 * Main entry point.
 *
 * Spring Boot manages all services and repositories.
 * JavaFX controllers are Spring @Component beans — they receive
 * dependencies via constructor injection, fully decoupled from the UI.
 */
@SpringBootApplication(scanBasePackages = "com.kids")
public class GardenSchoolApp extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = SpringApplication.run(GardenSchoolApp.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        var fxmlLocation = getClass().getResource("/fxml/main.fxml");
        //var cssLocation = getClass().getResource("/css/garden-school.css");

        if (fxmlLocation == null) {
            throw new RuntimeException("ERROR: /fxml/main.fxml not found in resources!");
        }

        // 1. جلب الـ ResourceBundle من سبرينغ
        ResourceBundle bundle = springContext.getBean(ResourceBundle.class);

        // 2. إعداد الـ FXMLLoader وتمرير الـ الـ ControllerFactory الخاص بـ Spring
        FXMLLoader loader = new FXMLLoader(fxmlLocation, bundle);
        loader.setControllerFactory(springContext::getBean);

        javafx.scene.Parent page = loader.load();

        if (bundle.getLocale().getLanguage().equals("ar")) {
            page.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        } else {
            page.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }

        // 5. تمرير الـ page الجاهزة والمعدلة مباشرة إلى الـ Scene
        Scene scene = new Scene(page, 1200, 750);

        primaryStage.setScene(scene);
        primaryStage.setTitle("روضة جنة الصغار — Garden School");
        // ================================================================
        // إضافة أيقونة التطبيق (لتظهر في شريط العنوان وشريط المهام)
        // ================================================================
        var iconStream = getClass().getResourceAsStream("/images/app_icon.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
        }
        // ================================================================
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }
}