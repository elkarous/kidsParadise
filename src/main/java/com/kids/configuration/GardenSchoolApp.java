package com.kids.configuration;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
        var fxmlLocation = getClass().getResource("/fxml/student_management.fxml");
        //var cssLocation = getClass().getResource("/css/garden-school.css");

        if (fxmlLocation == null) {
            throw new RuntimeException("ERROR: /fxml/main.fxml not found in resources!");
        }


        ResourceBundle bundle = springContext.getBean(ResourceBundle.class);
        FXMLLoader loader = new FXMLLoader(fxmlLocation, bundle);
        loader.setControllerFactory(springContext::getBean);

        Scene scene = new Scene(loader.load(), 1200, 750);


        primaryStage.setScene(scene);
        primaryStage.setTitle("روضة جنة الصغار — Garden School");
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }
}