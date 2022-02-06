package uk.co.flxs.auth.security;

import uk.co.flxs.auth.BadRequestException;

/**
 *
 * @author paul
 */
public class PwndPasswordException extends BadRequestException {

    private final int pwndCount;

    public PwndPasswordException(int pwndCount) {
        super(String.format("Chosen password/passphrase appears %d times in data breaches and cannot be used.", pwndCount));
        this.pwndCount = pwndCount;
    }

    public int getPwndCount() {
        return pwndCount;
    }
    
    
}
