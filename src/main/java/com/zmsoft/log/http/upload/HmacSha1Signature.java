package com.zmsoft.log.http.upload;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Formatter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacSha1Signature {

    private HmacSha1Signature() {
    }

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static byte[] hmacSHA1WithRFC2104(String data, String key) throws java.security.SignatureException {
        try {
            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);

            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            return mac.doFinal(data.getBytes());

        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
    }


    public static String hmacAndHex(String data, String key) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return toHexString(hmacSHA1WithRFC2104(data, key));
    }


    /**
     * Computes RFC 2104-compliant HMAC signature. * @param data The data to be
     * signed.
     *
     * @param key The signing key.
     * @return The Base64-encoded RFC 2104-compliant HMAC signature.
     * @throws java.security.SignatureException when signature generation fails
     */
    public static String hmacAndBase64(String data, String key)
            throws java.security.SignatureException {
        String result;
        try {
            // base64-encode the hmac
            result = Base64.getEncoder().encodeToString(hmacSHA1WithRFC2104(data, key));
//			result = Encoding.EncodeBase64(rawHmac);
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
        return result;
    }

}