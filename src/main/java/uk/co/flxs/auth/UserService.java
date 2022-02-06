package uk.co.flxs.auth;

import com.google.gson.Gson;
import uk.co.flxs.auth.repo.UserDTO;
import uk.co.flxs.auth.repo.UserRepo;
import uk.co.flxs.auth.security.PasswordFactory;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObject;
import javax.sql.DataSource;

/**
 * Controller responsible for main user CRUD actions.
 */
public class UserService extends BaseService {

    private UserRepo userRepo;

    private List<String> reservedWords;

    public UserService(Config config, DataSource ds) {
        this.userRepo = new UserRepo(ds);
        this.loadReservedWordlist();
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .post("/nicknamecheck", this::nicknameCheckHandler)
                .post("/register", this::registerHandler)
                .post("/pwndpasswordcount", this::checkPasswordHandler)
                .post("/changepassword", this::changePasswordHandler)
                .post("/resetpassword", this::resetPasswordHandler)
                .post("/changenickname", this::changeNicknameHandler)
                .post("/remove", this::removeHandler)
                ;
    }

    private void loadReservedWordlist() {
        reservedWords = new ArrayList<String>();
        InputStream is = null;
        try {
            URL url = getClass().getClassLoader().getResource("reserved-words.txt");
            is = url.openStream();
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);
            String line;
            while ((line = br.readLine()) != null) {
                reservedWords.add(line.toUpperCase());
            }

        } catch (IOException ex) {
            Logger.getLogger(UserService.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                Logger.getLogger(UserService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void nicknameCheckHandler(ServerRequest request, ServerResponse response) {
        request.content().as(JsonObject.class)
                .thenAccept(params -> nicknameCheck(params, response))
                .exceptionally(ex -> processErrors(ex, response));
    }

    private void nicknameCheck(JsonObject params, ServerResponse response) {
        Boolean available = true;
        try {
            checkRequiredParams(params, Arrays.asList("nickname"));
            if (params.getString("nickname").trim().toUpperCase().length() < 3) {
                available = false;
            }
            
            if (reservedWords.contains(params.getString("nickname").trim().toUpperCase())) {
                available = false;
            }

            if (userRepo.isNicknameRegistered(params.getString("nickname").trim())) {
                available = false;
            }

        } catch (Exception e) {
            available = false;
        }

        JsonObject responseData = JSON.createObjectBuilder()
                .add("available", available)
                .build();
        response.send(responseData);
    }

    private void listHandler(ServerRequest request, ServerResponse response) {
        List<UserDTO> userList = this.userRepo.getAllUsers();
        String json = new Gson().toJson(userList);
        response.status(Http.Status.OK_200)
                .headers().contentType(MediaType.APPLICATION_JSON);
        response.send(json);
    }

    private void checkPassword(JsonObject params, ServerResponse response) {
        checkRequiredParams(params, Arrays.asList("password"));
        int pwndCount = PasswordFactory.getPwndCount(params.getString("password"));

        JsonObject responseData = JSON.createObjectBuilder()
                .add("count", pwndCount)
                .build();
        response.send(responseData);
    }

    private void checkPasswordHandler(ServerRequest request, ServerResponse response) {
        request.content().as(JsonObject.class)
                .thenAccept(params -> checkPassword(params, response))
                .exceptionally(ex -> processErrors(ex, response));
    }

    private void registerNewUser(JsonObject params, ServerResponse response) throws RuntimeException {
        try {
            checkRequiredParams(params, Arrays.asList(
                    "nickname",
                    "password"));

            if (reservedWords.contains(params.getString("nickname").trim().toUpperCase())) {
                throw new BadRequestException("Nickname is a reserved word and cannot be used");
            }

            if (params.getString("nickname").trim().toUpperCase().length() < 3) {
                throw new BadRequestException("Nickname is too short, it must be at least 3 characters");
            }
            
            if (params.getString("password").length() < 12) {
                throw new BadRequestException("Password is too short, it must be at least 12 characters");
            }
            
            String passwordHash = PasswordFactory.create(params.getString("password"));
            
            UserDTO user = new UserDTO(
                    UUID.randomUUID().toString(),
                    params.getString("nickname").trim(),
                    passwordHash,
                    "dayone, earlybird");

            userRepo.insertUser(user);

            JsonObject responseData = JSON.createObjectBuilder()
                    .add("id", user.getId())
                    .add("nickname", user.getNickname())
                    .build();
            response.status(Http.Status.CREATED_201).send(responseData);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to register new user", e);
        }
    }

    private void registerHandler(ServerRequest request, ServerResponse response) {
        request.content().as(JsonObject.class)
                .thenAccept(params -> registerNewUser(params, response))
                .exceptionally(ex -> processErrors(ex, response));
    }

    private void changePasswordHandler(ServerRequest request, ServerResponse response) {
        JsonObject responseData = JSON.createObjectBuilder()
                .add("message", "password changed")
                .build();
        response.status(Http.Status.OK_200).send(responseData);
    }

    private void resetPasswordHandler(ServerRequest request, ServerResponse response) {
        JsonObject responseData = JSON.createObjectBuilder()
                .add("message", "password reset")
                .build();
        response.status(Http.Status.OK_200).send(responseData);
    }

    private void changeNicknameHandler(ServerRequest request, ServerResponse response) {
        JsonObject responseData = JSON.createObjectBuilder()
                .add("message", "nicknamme changed")
                .build();
        response.status(Http.Status.OK_200).send(responseData);
    }

    private void removeHandler(ServerRequest request, ServerResponse response) {
        JsonObject responseData = JSON.createObjectBuilder()
                .add("message", "User deleted")
                .build();
        response.status(Http.Status.OK_200).send(responseData);
    }
}
