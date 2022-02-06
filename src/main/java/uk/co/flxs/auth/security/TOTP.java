package uk.co.flxs.auth.security;

import java.lang.reflect.UndeclaredThrowableException;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Random;
import org.apache.commons.codec.binary.Base32;

/**
 * Based on the example implementation of the OATH TOTP algorithm at
 * www.openauthentication.org.
 */
public class TOTP {

    private final int keyLength = 20;
    private final int stepSeconds = 30;
    private final String crypto = "HmacSHA1"; // or HmacSHA512 or HmacSHA256
    private final int codeDigits = 6;

    private static final int[] DIGITS_POWER
            // 0  1   2    3     4      5       6        7         8
            = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    private final byte[] key;

    public TOTP() {
        key = generateKey();
    }

    public TOTP(String secret) {
        Base32 b32 = new Base32();
        key = b32.decode(secret);
    }

    public String getSecret() {
        Base32 b32 = new Base32();
        return b32.encodeAsString(key);
    }

    public String getCurrent() {
        long timestamp = Instant.now().getEpochSecond();
        return generateTOTP(timestamp);
    }

    public String getPrevious() {
        long timestamp = Instant.now().getEpochSecond() - stepSeconds;
        return generateTOTP(timestamp);
    }

    public String getNext() {
        long timestamp = Instant.now().getEpochSecond() + stepSeconds;
        return generateTOTP(timestamp);
    }
    
    public void verify(String code) throws Exception {
        if (
                (!getCurrent().equals(code)) && 
                (!getPrevious().equals(code)) &&
                (!getNext().equals(code))) {
            throw new AuthenticationException("TOTP Code could not be verified!");
        }
    }

    private byte[] generateKey() {
        byte[] b = new byte[keyLength];
        Random rand = new Random();
        rand.nextBytes(b);
        return b;
    }

    /**
     * This method uses the JCE to provide the crypto algorithm. HMAC computes a
     * Hashed Message Authentication Code with the crypto hash algorithm as a
     * parameter.
     *
     * @param crypto the crypto algorithm (HmacSHA1, HmacSHA256, HmacSHA512)
     * @param keyBytes the bytes to use for the HMAC key
     * @param text the message or text to be authenticated.
     */
    private byte[] hmac_sha1(byte[] keyBytes, byte[] text) {
        try {
            Mac hmac;
            hmac = Mac.getInstance(crypto);
            SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
            hmac.init(macKey);
            return hmac.doFinal(text);
        } catch (GeneralSecurityException gse) {
            throw new UndeclaredThrowableException(gse);
        }
    }

    private byte[] timestampToMessage(long timestamp) {
        long T = timestamp / stepSeconds;
        final byte[] msg = new byte[8];
        for (int i = 0; i < 8; i++) {
            msg[7 - i] = (byte) (T >>> i * 8);
        }
        return msg;
    }

    /**
     * This method generates an TOTP value for the given set of parameters.
     *
     * @param timestamp seconds since unix epoch
     *
     * @return A numeric String in base 10 that includes
     * {@link truncationDigits} digits
     */
    private String generateTOTP(long timestamp) {
        String result = null;
        byte[] hash;

        hash = hmac_sha1(key, timestampToMessage(timestamp));
        int offset = hash[hash.length - 1] & 0xf;
        int binary
                = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);

        int otp = binary % DIGITS_POWER[codeDigits];

        result = Integer.toString(otp);
        while (result.length() < codeDigits) {
            result = "0" + result;
        }
        return result;
    }
}
