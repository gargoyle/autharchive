package uk.co.flxs.auth.security;

import uk.co.flxs.auth.repo.UserDTO;
import io.helidon.config.Config;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;

/**
 *
 * @author paul
 */
public class JwtFactory {
    private final int tokenTTL = 86400;
    private final int refreshTokenTTL = 86400;
    
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String issuer;

    public JwtFactory(Config config) {
        loadPrivateKey(config.get("private-key").asString().get());
        loadPublicKey(config.get("public-key").asString().get());
        this.issuer = config.get("issuer").asString().get();
    }

    private void loadPrivateKey(String filename) throws RuntimeException {
        try {
            URL fullPath = getClass().getClassLoader().getResource(filename);
            InputStream is = fullPath.openStream();
            byte[] keyBytes = is.readAllBytes();
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = kf.generatePrivate(spec);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key!", e);
        }
    }

    private void loadPublicKey(String filename) throws RuntimeException {
        try {
            URL fullPath = getClass().getClassLoader().getResource(filename);
            InputStream is = fullPath.openStream();
            byte[] keyBytes = is.readAllBytes();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            publicKey = kf.generatePublic(spec);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key!", e);
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String generateJwsForUser(UserDTO user) {
        var now = Calendar.getInstance();
        var expiryTime = (Calendar)now.clone();
        expiryTime.add(Calendar.SECOND, this.tokenTTL);
        
        String jws = Jwts.builder()
                .setIssuer(this.issuer)
                .setIssuedAt(now.getTime())
                .setExpiration(expiryTime.getTime())
                .setSubject(user.getId())
                .claim("nickname", user.getNickname())
                .claim("roles", user.getRoles())
                .signWith(privateKey)
                .compact();

        return jws;
    }

    public Jws<Claims> verifyUserJws(String originalJws) {
        Jws<Claims> parsedJws = Jwts.parser()
                .requireIssuer(this.issuer)
                .setSigningKey(publicKey)
                .parseClaimsJws(originalJws);
        return parsedJws;
    }
}
