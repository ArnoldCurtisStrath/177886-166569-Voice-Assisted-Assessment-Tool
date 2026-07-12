package com.voiceassess.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    // load .env file from working dir, sets system props so Spring can use ${VAR:default}
    static {
        Path envFile = Paths.get(System.getProperty("user.dir"), ".env");
        if (Files.exists(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile)) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        // only set if not already in the environment
                        if (System.getProperty(key) == null) {
                            System.setProperty(key, val);
                        }
                    }
                }
            } catch (IOException e) {
                // couldn't read .env, the app will use defaults from application.properties
                System.err.println("Warning: couldn't load .env file: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
