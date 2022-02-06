package uk.co.flxs.auth;

import uk.co.flxs.auth.repo.UserDTO;
import uk.co.flxs.auth.repo.UserRepo;
import uk.co.flxs.auth.repo.UserStorageException;
import uk.co.flxs.auth.security.AuthenticationException;
import uk.co.flxs.auth.security.JwtFactory;
import uk.co.flxs.auth.security.PasswordFactory;
import uk.co.flxs.auth.security.TOTP;
import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import javax.json.JsonObject;
import javax.sql.DataSource;

/**
 * Controller responsible for authentication related actions.
 */
public class AuthenticationService extends BaseService {

    private final UserRepo userRepo;
    private final JwtFactory jwtFactory;

    public AuthenticationService(Config config, DataSource ds) {
        this.userRepo = new UserRepo(ds);
        this.jwtFactory = new JwtFactory(config.get("jwt"));
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/publickey", this::publicKeyHandler)
                .post("/authenticate", this::authenticationHandler)
                .post("/refresh", this::refreshHandler)
                .post("/verifyjws", this::verifyJwsHandler);
    }

    private void verifyJws(JsonObject params, ServerResponse response) {
        try {
            checkRequiredParams(params, Arrays.asList("jws"));
            Jws<Claims> claims = jwtFactory.verifyUserJws(params.getString("jws"));

            String id = claims.getBody().getSubject();
            String roles = claims.getBody().get("roles", String.class);
            String nickname = claims.getBody().get("nickname", String.class);

            JsonObject responseData = JSON.createObjectBuilder()
                    .add("verified", true)
                    .add("id", id)
                    .add("roles", roles)
                    .add("nickname", nickname)
                    .build();
            response.status(Http.Status.OK_200).send(responseData);
        } catch (Exception e) {
            JsonObject responseData = JSON.createObjectBuilder()
                    .add("verified", false)
                    .build();
            response.status(Http.Status.BAD_REQUEST_400).send(responseData);
        }
    }

private void verifyJwsHandler(ServerRequest request, ServerResponse response) {
    request.content().as(JsonObject.class)
            .thenAccept(params -> verifyJws(params, response))
            .exceptionally(ex -> processErrors(ex, response));
}

private void publicKeyHandler(ServerRequest request, ServerResponse response) {
    CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
        JsonObject responseData = JSON.createObjectBuilder()
            .add("der", new String(Base64.getUrlEncoder().encode(jwtFactory.getPublicKey().getEncoded())))
            .build();
        return responseData;
    });
    future.thenAccept(jo -> {response.status(Http.Status.OK_200).send(jo);});
}

//    private void authenticateUserViaTotp(JsonObject params, ServerResponse response) {
//        try {
//            checkRequiredParams(params, Arrays.asList(
//                    "nickname",
//                    "totp"));
//
//            UserDTO user = userRepo.getUserByNickname(params.getString("nickname"));
//            TOTP totp = new TOTP(user.getTotpSecret());
//            totp.verify(params.getString("totp"));
//
//            String jws = jwtFactory.generateJwsForUser(user);
//            JsonObject responseData = JSON.createObjectBuilder()
//                    .add("token", jws)
//                    .build();
//            response.status(Http.Status.OK_200).send(responseData);
//
//        } catch (UserStorageException e) {
//            throw new AuthenticationException("No such user");
//        } catch (RuntimeException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new RuntimeException("Unable to complete authentication", e);
//        }
//    }

    private void authenticateUser(JsonObject params, ServerResponse response) {
        try {
            checkRequiredParams(params, Arrays.asList(
                    "nickname",
                    "password"));

            UserDTO user = userRepo.getUserByNickname(params.getString("nickname"));
            if (!PasswordFactory.verify(params.getString("password"), user.getPasswordHash())) {
                throw new AuthenticationException("Password missmatch");
            }

            String jws = jwtFactory.generateJwsForUser(user);
            JsonObject responseData = JSON.createObjectBuilder()
                    .add("token", jws)
                    .build();
            response.status(Http.Status.OK_200).send(responseData);

        } catch (UserStorageException e) {
            throw new AuthenticationException("No such user");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to complete authentication", e);
        }
    }

    private void authenticationHandler(ServerRequest request, ServerResponse response) {
        request.content().as(JsonObject.class)
                .thenAccept(params -> authenticateUser(params, response))
                .exceptionally(ex -> processErrors(ex, response));
    }

//    private void authenticationTotpHandler(ServerRequest request, ServerResponse response) {
//        request.content().as(JsonObject.class)
//                .thenAccept(params -> authenticateUserViaTotp(params, response))
//                .exceptionally(ex -> processErrors(ex, response));
//    }

    private void refreshHandler(ServerRequest request, ServerResponse response) {
        JsonObject responseData = JSON.createObjectBuilder()
                .add("message", "token refreshed!")
                .build();
        response.status(Http.Status.OK_200).send(responseData);
    }

}
