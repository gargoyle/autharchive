package uk.co.flxs.auth.repo;

import uk.co.flxs.auth.BadRequestException;

/**
 *
 * @author paul
 */
public class DuplicateUserException extends BadRequestException {

    public DuplicateUserException(String message) {
        super(message);
    }

    public DuplicateUserException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
