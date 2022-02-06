package uk.co.flxs.auth.repo;

/**
 *
 * @author paul
 */
public class UserStorageException extends RuntimeException {

    public UserStorageException(String message) {
        super(message);
    }

    public UserStorageException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
