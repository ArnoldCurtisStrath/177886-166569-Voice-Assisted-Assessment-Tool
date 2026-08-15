package com.voiceassess.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the .env file from the working directory before Spring resolves
 * any ${VAR} placeholders. Runs on every context startup — main() and tests alike.
 * Real environment variables always win over the file.
 * Values go into the Spring environment directly (system properties are not
 * reliably visible to placeholder resolution at bean-creation time).
 */
public class DotenvLoader implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = Paths.get(System.getProperty("user.dir"), ".env");
        if (!Files.exists(envFile)) return;

        Map<String, Object> vars = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                // real env vars take precedence over the file
                if (System.getenv(key) == null && !environment.containsProperty(key)) {
                    vars.put(key, val);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: couldn't load .env file: " + e.getMessage());
            return;
        }

        if (!vars.isEmpty()) {
            // system properties cover direct reads (e.g. JwtUtil's fallback),
            // the environment source covers ${VAR} placeholder resolution
            vars.forEach((k, v) -> System.setProperty(k, String.valueOf(v)));
            environment.getPropertySources().addFirst(new MapPropertySource("dotenv", vars));
        }
    }
}
