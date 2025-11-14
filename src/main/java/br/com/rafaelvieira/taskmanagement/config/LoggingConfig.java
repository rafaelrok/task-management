package br.com.rafaelvieira.taskmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class LoggingConfig {

    private static final int MAX_PAYLOAD_LENGTH = 10000;

    @Bean


    public CommonsRequestLoggingFilter requestLoggingFilter() {
        var loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(MAX_PAYLOAD_LENGTH);
        loggingFilter.setIncludeHeaders(false);
        loggingFilter.setAfterMessagePrefix("REQUEST DATA : ");
        return loggingFilter;
    }
}
