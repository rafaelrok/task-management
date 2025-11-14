package br.com.rafaelvieira.taskmanagement.config;

import java.util.Arrays;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching

public class CacheConfig {

    @Bean
    CacheManager cacheManager() {
        var cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(
                Arrays.asList(
                        new ConcurrentMapCache("tasks"),
                        new ConcurrentMapCache("categories"),
                        new ConcurrentMapCache("users"),
                        new ConcurrentMapCache("tasksByStatus"),
                        new ConcurrentMapCache("tasksByPriority")));
        return cacheManager;
    }
}
