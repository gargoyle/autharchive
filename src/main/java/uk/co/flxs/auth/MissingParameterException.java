package uk.co.flxs.auth;

import java.util.List;

/**
 *
 * @author paul
 */
public class MissingParameterException extends BadRequestException {

    public MissingParameterException(List<String> missingParams) {
        super("Required parameter(s): " + String.join(",", missingParams) + " are missing from the request");
    }
    
    
}
