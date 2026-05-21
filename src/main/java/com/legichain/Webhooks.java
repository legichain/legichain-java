package com.legichain;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/** HMAC-SHA256 verifier for inbound Legichain webhook deliveries.
 *
 *  <p>The server signs every delivery with
 *  {@code HMAC-SHA256(secret, "{t}.{body}")} and emits the
 *  {@code Legichain-Signature: t=<unix>,v1=<hex>} header.
 *  Verification:
 *  <ol>
 *    <li>parses the header,</li>
 *    <li>rejects messages older than the tolerance (default 5 min),</li>
 *    <li>recomputes the HMAC and compares it in constant time.</li>
 *  </ol>
 */
public final class Webhooks {

    /** Default replay window — matches the server tolerance. */
    public static final long DEFAULT_TOLERANCE_SECONDS = 5 * 60L;

    private Webhooks() { }

    public static boolean verifySignature(
            byte[] body, String signatureHeader, String secret) {
        return verifySignature(body, signatureHeader, secret,
                DEFAULT_TOLERANCE_SECONDS, Instant.now().getEpochSecond());
    }

    public static boolean verifySignature(
            byte[] body, String signatureHeader, String secret,
            long toleranceSeconds, long nowEpochSeconds) {

        if (body == null || signatureHeader == null || secret == null) return false;
        if (toleranceSeconds <= 0) toleranceSeconds = DEFAULT_TOLERANCE_SECONDS;

        String tStr = null, v1 = null;
        for (String raw : signatureHeader.split(",")) {
            String p = raw.trim();
            if (p.startsWith("t="))  tStr = p.substring(2);
            else if (p.startsWith("v1=")) v1 = p.substring(3);
        }
        if (tStr == null || v1 == null) return false;

        long ts;
        try {
            ts = Long.parseLong(tStr);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(nowEpochSeconds - ts) > toleranceSeconds) return false;

        byte[] expected;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((tStr + ".").getBytes(StandardCharsets.UTF_8));
            mac.update(body);
            expected = mac.doFinal();
        } catch (Exception e) {
            return false;
        }
        byte[] received = hexDecode(v1);
        if (received == null) return false;

        // Constant-time comparison.
        return MessageDigest.isEqual(expected, received);
    }

    private static byte[] hexDecode(String s) {
        int len = s.length();
        if ((len & 1) != 0) return null;
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i),     16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) return null;
            out[i / 2] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
