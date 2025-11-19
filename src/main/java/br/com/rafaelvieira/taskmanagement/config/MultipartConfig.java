package br.com.rafaelvieira.taskmanagement.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Multipart configuration to handle file uploads with multiple form fields. Increases Tomcat's
 * default file count limit for multipart requests.
 *
 * <p>This configuration works in conjunction with application.yaml settings: -
 * spring.servlet.multipart.max-file-size - spring.servlet.multipart.max-request-size -
 * server.tomcat.max-file-count
 */
@Configuration
public class MultipartConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultipartConfig.class);

    // Maximum number of multipart parts allowed (default is very low, causing
    // FileCountLimitExceededException)
    private static final String FILE_COUNT_MAX_PROPERTY =
            "org.apache.tomcat.util.http.fileupload.impl.FileCountMax";
    private static final String FILE_COUNT_MAX_VALUE = "1000";

    static {
        System.setProperty(FILE_COUNT_MAX_PROPERTY, FILE_COUNT_MAX_VALUE);
        LOGGER.info(
                "★★★ MultipartConfig: Set "
                        + FILE_COUNT_MAX_PROPERTY
                        + " = "
                        + FILE_COUNT_MAX_VALUE);
    }

    @PostConstruct
    public void logConfiguration() {
        String currentValue = System.getProperty(FILE_COUNT_MAX_PROPERTY);

        LOGGER.info("Multipart Configuration Initialized");
        LOGGER.info("Property: {}", FILE_COUNT_MAX_PROPERTY);
        LOGGER.info("Expected Value: {}", FILE_COUNT_MAX_VALUE);
        LOGGER.info("Current Value: {}", currentValue);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("=".repeat(80));
        }

        if (!FILE_COUNT_MAX_VALUE.equals(currentValue)) {
            LOGGER.warn(
                    "File count max property mismatch! Forcing value to {}", FILE_COUNT_MAX_VALUE);
            System.setProperty(FILE_COUNT_MAX_PROPERTY, FILE_COUNT_MAX_VALUE);
        }
    }
}
