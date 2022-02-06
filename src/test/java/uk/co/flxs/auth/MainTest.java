package uk.co.flxs.auth;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.net.URL;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonReaderFactory;

import io.helidon.webserver.WebServer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MainTest {

    private static WebServer webServer;
    private static final JsonReaderFactory JSON = Json.createReaderFactory(Collections.emptyMap());

    @BeforeAll
    public static void startTheServer() throws Exception {
        webServer = Main.startServer();

        long timeout = 2000; // 2 seconds should be enough to start the server
        long now = System.currentTimeMillis();

        while (!webServer.isRunning()) {
            Thread.sleep(100);
            if ((System.currentTimeMillis() - now) > timeout) {
                Assertions.fail("Failed to start webserver");
            }
        }
    }

    @AfterAll
    public static void stopServer() throws Exception {
        if (webServer != null) {
            webServer.shutdown()
                     .toCompletableFuture()
                     .get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testEndpoints() throws Exception {
        HttpURLConnection conn;

        conn = getURLConnection("POST","/u1/nicknamecheck");
        Assertions.assertEquals(400, conn.getResponseCode(), "Register");
        conn = getURLConnection("POST","/u1/register");
        Assertions.assertEquals(400, conn.getResponseCode(), "Register");
        conn = getURLConnection("POST","/u1/pwndpasswordcount");
        Assertions.assertEquals(400, conn.getResponseCode(), "Register");
        
        conn = getURLConnection("POST", "/u1/changepassword");
        Assertions.assertEquals(200, conn.getResponseCode(), "Change Password");
        conn = getURLConnection("POST", "/u1/resetpassword");
        Assertions.assertEquals(200, conn.getResponseCode(), "Reset Password");
        conn = getURLConnection("POST", "/u1/changenickname");
        Assertions.assertEquals(200, conn.getResponseCode(), "Change Nickname");
        conn = getURLConnection("POST", "/u1/remove");
        Assertions.assertEquals(200, conn.getResponseCode(), "Remove");
        
        conn = getURLConnection("GET", "/a1/publickey");
        Assertions.assertEquals(200, conn.getResponseCode(), "Public key");
        conn = getURLConnection("POST", "/a1/authenticate");
        Assertions.assertEquals(400, conn.getResponseCode(), "Authenticate");
        conn = getURLConnection("POST", "/a1/refresh");
        Assertions.assertEquals(200, conn.getResponseCode(), "Refresh Token");
        conn = getURLConnection("POST", "/a1/verifyjws");
        Assertions.assertEquals(400, conn.getResponseCode(), "Verify JWS Token");

        
        conn = getURLConnection("GET", "/health");
        Assertions.assertEquals(200, conn.getResponseCode(), "Healthcheck");

        conn = getURLConnection("GET", "/metrics");
        Assertions.assertEquals(200, conn.getResponseCode(), "Metrics");
    }

    private HttpURLConnection getURLConnection(String method, String path) throws Exception {
        URL url = new URL("http://localhost:" + webServer.port() + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");
        System.out.println("Connecting: " + method + " " + url);
        return conn;
    }
}
