package br.com.rafaelvieira.taskmanagement.config;

import java.util.concurrent.Executor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration that overrides the async executor to run synchronously in tests. This
 * makes @Async listeners execute in the same thread/transaction during tests, avoiding visibility
 * issues with uncommitted data.
 */
@TestConfiguration
@Profile("test")
public class TestAsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        // Run tasks synchronously in the calling thread during tests
        return Runnable::run;
    }
}
