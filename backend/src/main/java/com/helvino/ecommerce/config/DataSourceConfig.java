package com.helvino.ecommerce.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:}")
    private String rawDatabaseUrl;

    @Bean
    @Primary
    public DataSource dataSource() throws Exception {
        if (rawDatabaseUrl == null || rawDatabaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL environment variable is not set");
        }

        // Strip libpq-only parameters the JDBC driver doesn't understand
        String url = rawDatabaseUrl.trim()
                .replaceAll("[&?]channel_binding=[^&]*", "")
                .replaceAll("[&?]connect_timeout=[^&]*", "")
                .replaceAll("[?&]$", "");

        // Strip jdbc: prefix so java.net.URI can parse it
        if (url.startsWith("jdbc:")) url = url.substring(5);
        // Normalise scheme
        if (url.startsWith("postgres://"))   url = "postgresql://" + url.substring("postgres://".length());

        URI uri = URI.create(url);
        String host     = uri.getHost();
        int    port     = uri.getPort() > 0 ? uri.getPort() : 5432;
        String database = uri.getPath().replaceFirst("^/", "");
        String userInfo = uri.getUserInfo();          // user:password
        String user     = userInfo.split(":", 2)[0];
        String password = userInfo.split(":", 2)[1];

        // Use PGSimpleDataSource directly — avoids acceptsURL() classloader trap
        PGSimpleDataSource pg = new PGSimpleDataSource();
        pg.setServerNames(new String[]{host});
        pg.setPortNumbers(new int[]{port});
        pg.setDatabaseName(database);
        pg.setUser(user);
        pg.setPassword(password);
        pg.setSslMode("require");

        // Wrap in HikariCP for connection pooling
        HikariConfig hk = new HikariConfig();
        hk.setDataSource(pg);                // <-- DataSource mode, no acceptsURL call
        hk.setMaximumPoolSize(10);
        hk.setMinimumIdle(2);
        hk.setConnectionTimeout(30_000);
        hk.setIdleTimeout(600_000);
        hk.setMaxLifetime(1_800_000);
        hk.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(hk);
    }
}
