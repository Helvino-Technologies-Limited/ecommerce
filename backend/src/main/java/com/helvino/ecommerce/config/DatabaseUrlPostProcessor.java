package com.helvino.ecommerce.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseUrlPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = environment.getProperty("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) return;

        String jdbcUrl = normalise(rawUrl);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", jdbcUrl);
        props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

        // addFirst so this beats anything in application.yml
        environment.getPropertySources().addFirst(
                new MapPropertySource("normalised-database-url", props));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    static String normalise(String url) {
        // Strip libpq-only parameters the JDBC driver doesn't understand
        url = url.replaceAll("[&?]channel_binding=[^&]*", "")
                 .replaceAll("[&?]connect_timeout=[^&]*", "");

        // Clean up orphaned '?' or '&' left at the end
        url = url.replaceAll("[?&]$", "");

        // Add jdbc:postgresql:// prefix
        if (url.startsWith("postgres://")) {
            url = "jdbc:postgresql://" + url.substring("postgres://".length());
        } else if (url.startsWith("postgresql://")) {
            url = "jdbc:" + url;
        }
        // already jdbc:postgresql:// — leave as-is

        // Ensure SSL for Neon
        if (!url.contains("sslmode=")) {
            url += (url.contains("?") ? "&" : "?") + "sslmode=require";
        }

        return url;
    }
}
