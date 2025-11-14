package br.com.rafaelvieira.taskmanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class ApplicationProperties {

    private static final int MAX_POOL_SIZE = 20;
    private static final int MIN_IDLE = 5;
    private static final long CONNECTION_TIMEOUT = 30_000;
    private static final long IDLE_TIMEOUT = 30_0000;
    private static final long MAX_LIFETIME = 180_0000;
    private static final long CACHE_TTL = 3600;
    private static final int CACHE_MAX_SIZE = 1000;
    private static final String API_VERSION = "1.0.0";
    private static final int API_RATE_LIMIT = 100;

    private final Database database = new Database();
    private final Cache cache = new Cache();
    private final Api api = new Api();

    @Data
    public static class Database {
        private int maxPoolSize = MAX_POOL_SIZE;
        private int minIdle = MIN_IDLE;
        private long connectionTimeout = CONNECTION_TIMEOUT;
        private long idleTimeout = IDLE_TIMEOUT;
        private long maxLifetime = MAX_LIFETIME;
    }

    @Data
    public static class Cache {
        private boolean enabled = true;
        private long ttl = CACHE_TTL;
        private int maxSize = CACHE_MAX_SIZE;
    }

    @Data
    public static class Api {
        private String version = API_VERSION;
        private int rateLimit = API_RATE_LIMIT;
        private boolean enableSwagger = true;
    }
}
