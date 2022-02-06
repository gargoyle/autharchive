package uk.co.flxs.auth;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.OverrideSources;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.cors.CorsSupport;
import io.helidon.webserver.cors.CrossOriginConfig;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class Main {


    private Main() { }

    /**
     * Application main entry point.
     * 
     * @param args command line arguments.
     * @throws IOException if there are problems reading logging properties
     */
    public static void main(final String[] args) throws IOException {
        startServer();
    }

    static WebServer startServer() throws IOException {
        setupLogging();

        // By default this will pick up application.yaml from the classpath
        Config config = Config.builder()
                .sources(
                        ConfigSources.file("conf/overrides.properties").optional(true),
                        ConfigSources.classpath("application.yaml"))
                .disableEnvironmentVariablesSource()
                .build();
        
        WebServer server = WebServer.builder(createRouting(config))
                .config(config.get("server"))
                .addMediaSupport(JsonpSupport.create())
                .build();
        
        server.start()
            .thenAccept(ws -> {
                System.out.println("Service is up! http://localhost:" + ws.port() + "/");
                ws.whenShutdown().thenRun(()
                    -> System.out.println("WEB server is DOWN. Good bye!"));
                })
            .exceptionally(t -> {
                System.err.println("Startup failed: " + t.getMessage());
                t.printStackTrace(System.err);
                return null;
            });
        
        return server;
    }

    private static Routing createRouting(Config config) {
        MetricsSupport metrics = MetricsSupport.create();
        HealthSupport health = HealthSupport.builder()
                .addLiveness(HealthChecks.healthChecks())   // Adds a convenient set of checks
                .build();

        var dbConfig = config.get("database");
        DataSource ds = createDataSource(dbConfig);
        
        CorsSupport corsSupport = CorsSupport.builder()
                .addCrossOrigin(CrossOriginConfig.create())
                .build();
        
        UserService userService = new UserService(config, ds);
        AuthenticationService authService = new AuthenticationService(config, ds);
        
        return Routing.builder()
                .register(health)                   // Health at "/health"
                .register(metrics)                  // Metrics at "/metrics"
                .register(corsSupport)
                .register("/u1", userService)
                .register("/a1", authService)
                .build();
    }

    private static void setupLogging() throws IOException {
        try (InputStream is = Main.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        }
    }

    private static DataSource createDataSource(Config config)
    {   
        HikariConfig dbConfig = new HikariConfig();
        dbConfig.setJdbcUrl(config.get("jdbcurl").asString().get());
        dbConfig.setUsername(config.get("username").asString().get());
        dbConfig.setPassword(config.get("password").asString().get());
        dbConfig.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        
        return new HikariDataSource(dbConfig);
    }
}
