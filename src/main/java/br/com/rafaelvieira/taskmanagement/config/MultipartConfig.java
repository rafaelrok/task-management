package br.com.rafaelvieira.taskmanagement.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Multipart configuration to handle file uploads with multiple form fields. Increases Tomcat's
 * default file count limit for multipart requests.
 *
 * @author Rafael Vieira
 * @since 1.0.0
 */
@Configuration
public class MultipartConfig {

    private static final String FILE_COUNT_MAX_PROPERTY =
            "org.apache.tomcat.util.http.fileupload.impl.FileCountMax";
    private static final String FILE_COUNT_MAX_VALUE = "500";

    static {
        System.setProperty(FILE_COUNT_MAX_PROPERTY, FILE_COUNT_MAX_VALUE);
    }

    @PostConstruct
    public void refreshProperty() {
        if (!FILE_COUNT_MAX_VALUE.equals(System.getProperty(FILE_COUNT_MAX_PROPERTY))) {
            System.setProperty(FILE_COUNT_MAX_PROPERTY, FILE_COUNT_MAX_VALUE);
        }
    }
}
