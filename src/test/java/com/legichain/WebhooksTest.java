package com.legichain;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WebhooksTest {

    private static String sign(String body, long ts, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((ts + ".").getBytes(StandardCharsets.UTF_8));
        mac.update(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : mac.doFinal()) sb.append(String.format("%02x", b));
        return "t=" + ts + ",v1=" + sb;
    }

    @Test
    void freshSignatureVerifies() throws Exception {
        String body = "{\"event\":\"screening.high_risk\"}";
        long ts = Instant.now().getEpochSecond();
        String sig = sign(body, ts, "whsec_test");
        assertTrue(Webhooks.verifySignature(body.getBytes(), sig, "whsec_test"));
    }

    @Test
    void wrongSecretFails() throws Exception {
        String body = "{\"event\":\"x\"}";
        long ts = Instant.now().getEpochSecond();
        String sig = sign(body, ts, "whsec_other");
        assertFalse(Webhooks.verifySignature(body.getBytes(), sig, "whsec_test"));
    }

    @Test
    void staleMessageFails() throws Exception {
        String body = "{\"event\":\"x\"}";
        long ts = Instant.now().getEpochSecond() - 600;
        String sig = sign(body, ts, "whsec_test");
        assertFalse(Webhooks.verifySignature(body.getBytes(), sig, "whsec_test"));
    }

    @Test
    void malformedHeaderFails() {
        assertFalse(Webhooks.verifySignature(
                "x".getBytes(), "garbage", "whsec_test"));
        assertFalse(Webhooks.verifySignature(
                "x".getBytes(), "t=abc,v1=not-hex", "whsec_test"));
    }

    @Test
    void nullsAreSafe() {
        assertFalse(Webhooks.verifySignature(null, "t=1,v1=00", "x"));
        assertFalse(Webhooks.verifySignature("x".getBytes(), null, "x"));
        assertFalse(Webhooks.verifySignature("x".getBytes(), "t=1,v1=00", null));
    }

    @Test
    void clientBuilderRejectsEmptyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> Legichain.builder().apiKey("").build());
        assertThrows(IllegalArgumentException.class,
                () -> Legichain.builder().build());
    }

    @Test
    void clientBuilderAcceptsValidKey() {
        Legichain lc = Legichain.builder().apiKey("lc_live_x.sk_live_y").build();
        assertNotNull(lc);
    }
}
