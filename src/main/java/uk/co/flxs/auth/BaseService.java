package uk.co.flxs.auth;

import io.helidon.common.http.Http;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;

/**
 * Common service controller code.
 *
 * @author paul
 */
abstract public class BaseService implements Service {

    protected static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
    protected static final Logger LOGGER = Logger.getLogger(BaseService.class.getName());

    protected <T> T processErrors(Throwable ex, ServerResponse response) {
        LOGGER.log(Level.INFO, ex.getMessage());
        String errorMessageForResponse = ex.getCause().getMessage();
        
        if (ex.getCause() instanceof JsonException) {
            response.status(Http.Status.BAD_REQUEST_400);
            errorMessageForResponse = "Invalid JSON";
            
        } else if (ex.getCause() instanceof BadRequestException) {
            response.status(Http.Status.BAD_REQUEST_400);
            
        } else if (ex.getCause() instanceof SecurityException) {
            errorMessageForResponse = "Access Denied";
            response.status(Http.Status.FORBIDDEN_403);
            
        } else {
            LOGGER.log(Level.WARNING, "Internal error", ex);
            response.status(Http.Status.INTERNAL_SERVER_ERROR_500);
            errorMessageForResponse = "Oops! Something went wrong!";
        }

        JsonObject jsonErrorObject = JSON.createObjectBuilder()
                    .add("error", errorMessageForResponse)
                    .build();
        response.send(jsonErrorObject);
        return null;
    }
    
    protected void checkRequiredParams(JsonObject params, List<String> requiredKeys) {
        List<String> missingParams = new ArrayList<String>();
        
        for (String key : requiredKeys) {
            if (!params.containsKey(key)) {
                missingParams.add(key);
            }
        }
        
        if (missingParams.size() > 0) {
            throw new MissingParameterException(missingParams);
        }
    }
}
