package uk.co.flxs.auth;

/**
 *
 * @author paul
 */
public class SecurityException extends RuntimeException {

    public SecurityException() {
        super("Access Denied");
    }

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
