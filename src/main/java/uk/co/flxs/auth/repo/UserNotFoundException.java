package uk.co.flxs.auth.repo;

/**
 *
 * @author paul
 */
public class UserNotFoundException extends UserStorageException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
