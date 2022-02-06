package uk.co.flxs.auth.security;

/**
 *
 * @author paul
 */
public class AuthenticationException extends uk.co.flxs.auth.SecurityException {

    public AuthenticationException() {
        super("Authentication Failed");
    }

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
