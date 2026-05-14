package com.kids;

import com.kids.configuration.GardenSchoolApp;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.kids")
public class KidsParadesApplication {

    public static void main(String[] args) {
        // DO NOT use SpringApplication.run(KidsParadesApplication.class, args);
        // INSTEAD, launch the JavaFX Application class:
        Application.launch(GardenSchoolApp.class, args);
    }
}
