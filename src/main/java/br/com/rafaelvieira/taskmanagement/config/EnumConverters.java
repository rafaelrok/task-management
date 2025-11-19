package br.com.rafaelvieira.taskmanagement.config;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Custom converters to handle "null" string in query parameters for enums
 *
 * @author Rafael Vieira
 * @version 1.0.0
 * @since 1.0.0
 */
public final class EnumConverters {

    private EnumConverters() {}

    @Component
    public static class StringToTaskStatusConverter implements Converter<String, TaskStatus> {
        @Override
        public TaskStatus convert(String source) {
            if (source == null || source.trim().isEmpty() || "null".equalsIgnoreCase(source)) {
                return null;
            }
            try {
                return TaskStatus.valueOf(source.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    @Component
    public static class StringToPriorityConverter implements Converter<String, Priority> {
        @Override
        public Priority convert(String source) {
            if (source == null || source.trim().isEmpty() || "null".equalsIgnoreCase(source)) {
                return null;
            }
            try {
                return Priority.valueOf(source.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
