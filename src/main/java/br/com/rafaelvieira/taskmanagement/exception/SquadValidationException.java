package br.com.rafaelvieira.taskmanagement.exception;

public class SquadValidationException extends RuntimeException {
    public SquadValidationException(String message) {
        super(message);
    }

    public SquadValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
