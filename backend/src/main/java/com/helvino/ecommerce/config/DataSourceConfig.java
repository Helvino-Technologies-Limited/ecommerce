package com.helvino.ecommerce.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:jdbc:postgresql://neondb_owner:npg_Piva61ycWSoA@ep-quiet-shadow-ankrasex-pooler.c-6.us-east-1.aws.neon.tech/neondb?sslmode=require}")
    private String rawDatabaseUrl;

    @Bean
    @Primary
    public DataSource dataSource() {
        String url = normaliseJdbcUrl(rawDatabaseUrl);

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(30_000);
        ds.setIdleTimeout(600_000);
        ds.setMaxLifetime(1_800_000);
        return ds;
    }

    /**
     * Accepts any of the common Neon/Postgres URL formats and returns a valid JDBC URL:
     *   postgresql://user:pass@host/db?...   -> jdbc:postgresql://user:pass@host/db?sslmode=require
     *   postgres://user:pass@host/db?...     -> jdbc:postgresql://user:pass@host/db?sslmode=require
     *   jdbc:postgresql://...               -> left as-is (prefix already correct)
     */
    static String normaliseJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("DATABASE_URL environment variable is not set");
        }

        // Strip unsupported libpq-only parameters
        url = url.replaceAll("[&?]channel_binding=[^&]*", "")
                 .replaceAll("[&?]connect_timeout=[^&]*", "");

        // Ensure jdbc: prefix
        if (url.startsWith("postgres://")) {
            url = "jdbc:postgresql://" + url.substring("postgres://".length());
        } else if (url.startsWith("postgresql://")) {
            url = "jdbc:" + url;
        }
        // If it already starts with jdbc:postgresql:// leave it alone

        // Ensure sslmode=require is present for Neon
        if (!url.contains("sslmode=")) {
            url += (url.contains("?") ? "&" : "?") + "sslmode=require";
        }

        return url;
    }
}
