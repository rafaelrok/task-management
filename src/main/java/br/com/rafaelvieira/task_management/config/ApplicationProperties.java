package br.com.rafaelvieira.task_management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class ApplicationProperties {

    private final Database database = new Database();
    private final Cache cache = new Cache();
    private final Api api = new Api();

    @Data
    public static class Database {
        private int maxPoolSize = 20;
        private int minIdle = 5;
        private long connectionTimeout = 30000;
        private long idleTimeout = 300000;
        private long maxLifetime = 1800000;
    }

    @Data
    public static class Cache {
        private boolean enabled = true;
        private long ttl = 3600;
        private int maxSize = 1000;
    }

    @Data
    public static class Api {
        private String version = "1.0.0";
        private int rateLimit = 100;
        private boolean enableSwagger = true;
    }
}
