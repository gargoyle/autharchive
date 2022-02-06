package uk.co.flxs.auth.security;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Wrapper for creating password hashes.
 *
 * Currently, supports BCrypt hashes.
 *
 * @author paul
 */
public class PasswordFactory {

    private static int rounds = 10;

    private static String pwndApiEndpoint = "https://api.pwnedpasswords.com/range/";

    public static String create(String clearText) throws PwndPasswordException {
        int pwndCount = getPwndCount(clearText);
        if (pwndCount > 0) {
            throw new PwndPasswordException(pwndCount);
        }
        return BCrypt.hashpw(clearText, BCrypt.gensalt(rounds));
    }

    public static boolean verify(String clearText, String hash) {
        return BCrypt.checkpw(clearText, hash);
    }

    public static int getPwndCount(String clearText) {
        String firstFive = firstFive(clearText);
        String theRest = theRest(clearText).toUpperCase();
        int pwndCount = 0;
        
        List<String> pwndRange = getRange(firstFive);
        for (String line : pwndRange) {
            if (line.startsWith(theRest)) {
                pwndCount = Integer.parseInt(line.substring(line.lastIndexOf(":") + 1));
            }
        }

        return pwndCount;
    }

    private static String theRest(String clearText) {
        return DigestUtils.sha1Hex(clearText).substring(5);
    }

    private static String firstFive(String clearText) {
        return DigestUtils.sha1Hex(clearText).substring(0, 5);
    }

    private static List<String> getRange(String firstFive) {
        try {
            List<String> pwndList = new ArrayList<String>();

            URL url = new URL(pwndApiEndpoint + firstFive);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            InputStream is = conn.getInputStream();
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);

            String line;
            while ((line = br.readLine()) != null) {
                pwndList.add(line);
            }
            return pwndList;

        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

}
