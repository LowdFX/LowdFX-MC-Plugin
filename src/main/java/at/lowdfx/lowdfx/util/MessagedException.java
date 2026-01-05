package at.lowdfx.lowdfx.util;

/**
 * An exception that carries a user-friendly message.
 */
public class MessagedException extends Exception {
    public MessagedException(String message) {
        super(message);
    }

    public MessagedException(String message, Throwable cause) {
        super(message, cause);
    }

    public String message() {
        return getMessage();
    }
}
