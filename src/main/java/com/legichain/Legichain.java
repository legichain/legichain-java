package com.legichain;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.legichain.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Synchronous Java client for the Legichain AML, KYC and Travel Rule API.
 *
 * <p>Example:
 * <pre>{@code
 * Legichain lc = Legichain.builder()
 *     .apiKey(System.getenv("LEGICHAIN_API_KEY"))
 *     .build();
 *
 * ScreeningResponse r = lc.screenPerson(
 *     new PersonQuery("Vladimir Putin", "RU", "1952-10-07", null, null, null)
 * );
 *
 * switch (r.summary().recommendation()) {
 *     case BLOCK  -> denyOnboarding();
 *     case REVIEW -> queueForCompliance();
 *     case CLEAR  -> approve();
 * }
 * }</pre>
 */
public final class Legichain {

    /** Build the bearer header value once at construction. */
    private final String authHeader;
    private final URI    baseUri;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final Duration requestTimeout;
    private final Map<String, String> defaultHeaders;

    private Legichain(Builder b) {
        if (b.apiKey == null || b.apiKey.isEmpty()) {
            throw new IllegalArgumentException("legichain: apiKey is required");
        }
        this.authHeader     = "Bearer " + b.apiKey;
        this.baseUri        = URI.create(stripTrailingSlash(b.baseUrl));
        this.http           = b.httpClient != null
                              ? b.httpClient
                              : HttpClient.newBuilder()
                                          .connectTimeout(Duration.ofSeconds(10))
                                          .build();
        this.mapper         = b.objectMapper != null
                              ? b.objectMapper
                              : defaultMapper();
        this.requestTimeout = b.requestTimeout != null
                              ? b.requestTimeout
                              : Duration.ofSeconds(30);
        this.defaultHeaders = Map.copyOf(b.defaultHeaders);
    }

    public static Builder builder() { return new Builder(); }

    // ── screening ──────────────────────────────────────────────────────

    public ScreeningResponse screenPerson(PersonQuery q) { return screenPerson(q, null); }
    public ScreeningResponse screenPerson(PersonQuery q, String idempotencyKey) {
        return post("/v1/screen/person", q, idempotencyKey, ScreeningResponse.class);
    }

    public ScreeningResponse screenCompany(CompanyQuery q) { return screenCompany(q, null); }
    public ScreeningResponse screenCompany(CompanyQuery q, String idempotencyKey) {
        return post("/v1/screen/company", q, idempotencyKey, ScreeningResponse.class);
    }

    public ScreeningResponse screenCrypto(CryptoQuery q) { return screenCrypto(q, null); }
    public ScreeningResponse screenCrypto(CryptoQuery q, String idempotencyKey) {
        return post("/v1/screen/crypto", q, idempotencyKey, ScreeningResponse.class);
    }

    @SuppressWarnings("unchecked")
    public List<ScreeningResponse> screenBatch(List<?> items) {
        Map<String, Object> body = Map.of("items", items);
        return (List<ScreeningResponse>) post("/v1/screen/batch", body, null, List.class);
    }

    public BatchAsyncResponse screenBatchAsync(List<?> items) {
        Map<String, Object> body = Map.of("items", items);
        return post("/v1/screen/batch/async", body, null, BatchAsyncResponse.class);
    }

    public JobStatus job(String jobId) {
        return get("/v1/screen/jobs/" + java.net.URLEncoder.encode(
                Objects.requireNonNull(jobId, "jobId"),
                java.nio.charset.StandardCharsets.UTF_8), JobStatus.class);
    }

    // ── reports — PDF bytes ────────────────────────────────────────────

    public byte[] reportWallet(CryptoQuery q)  { return pdf("/v1/reports/wallet",  q); }
    public byte[] reportPerson(PersonQuery q)  { return pdf("/v1/reports/person",  q); }
    public byte[] reportCompany(CompanyQuery q) { return pdf("/v1/reports/company", q); }

    // ── KYC SDK ────────────────────────────────────────────────────────
    // Server-side semantics: this SDK never reads NFC chips. Mobile
    // clients (Flutter / React Native / iOS / Android) extract SOD +
    // DG bytes; your backend forwards them here as base64 strings.

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycCreateApplication(Map<String, Object> body) {
        return (Map<String, Object>) post("/v1/kyc/applications", body, null, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycStatus(String applicationId) {
        return kycStatus(applicationId, false);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycStatus(String applicationId, boolean includeExtracted) {
        String q = includeExtracted ? "?include_extracted=true" : "";
        return (Map<String, Object>) get(
            "/v1/kyc/applications/" + enc(applicationId) + "/status" + q, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycUploadDocument(
            String applicationId, String clientToken, Map<String, Object> body) {
        return (Map<String, Object>) post(
            "/v1/kyc/applications/" + enc(applicationId) + "/documents",
            body, null, clientToken, Map.class);
    }

    /** Forward an NFC chip read from a mobile client. Pass base64
     *  strings for sod_b64 / dg*_b64 / active_authentication_b64. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> kycSubmitNfc(
            String applicationId, String clientToken, Map<String, Object> body) {
        return (Map<String, Object>) post(
            "/v1/kyc/applications/" + enc(applicationId) + "/nfc",
            body, null, clientToken, Map.class);
    }

    /** Convenience: mobile reports the chip didn't respond. Server
     *  keeps state at awaiting_nfc — no attempt counter consumed. */
    public Map<String, Object> kycNfcAccessError(
            String applicationId, String clientToken, String protocol, String code) {
        if (protocol == null || protocol.isEmpty()) protocol = "PACE";
        if (code == null || code.isEmpty()) code = "chip_not_responding";
        Map<String, Object> body = new HashMap<>();
        body.put("protocol", protocol);
        body.put("access_error", true);
        body.put("access_error_code", code);
        return kycSubmitNfc(applicationId, clientToken, body);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycUploadSelfie(
            String applicationId, String clientToken, Map<String, Object> body) {
        return (Map<String, Object>) post(
            "/v1/kyc/applications/" + enc(applicationId) + "/selfie",
            body, null, clientToken, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycLivenessChallenge(
            String applicationId, String clientToken, int length, int ttlSeconds) {
        Map<String, Object> body = Map.of("length", length, "ttl_seconds", ttlSeconds);
        return (Map<String, Object>) post(
            "/v1/kyc/applications/" + enc(applicationId) + "/liveness/challenge",
            body, null, clientToken, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycSubmitLiveness(
            String applicationId, String clientToken, Map<String, Object> body) {
        return (Map<String, Object>) post(
            "/v1/kyc/applications/" + enc(applicationId) + "/liveness",
            body, null, clientToken, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycSubmit(String applicationId, String clientToken) {
        return (Map<String, Object>) post(
            "/v1/kyc/applications/" + enc(applicationId) + "/submit",
            Map.of(), null, clientToken, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycRetry(
            String applicationId, String clientToken, String reason) {
        Map<String, Object> body = (reason == null || reason.isEmpty())
            ? Map.of() : Map.of("reason", reason);
        return (Map<String, Object>) post(
            "/v1/kyc/applications/" + enc(applicationId) + "/retry",
            body, null, clientToken, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycExtendTtl(String applicationId, String clientToken) {
        return (Map<String, Object>) post(
            "/v1/kyc/applications/" + enc(applicationId) + "/extend-ttl",
            Map.of(), null, clientToken, Map.class);
    }

    // ── KYC tenant admin ──────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Object> kycAdminList(Map<String, Object> queryParams) {
        StringBuilder qs = new StringBuilder();
        if (queryParams != null) {
            for (var e : queryParams.entrySet()) {
                if (e.getValue() == null) continue;
                qs.append(qs.length() == 0 ? "?" : "&")
                  .append(enc(e.getKey())).append("=").append(enc(String.valueOf(e.getValue())));
            }
        }
        return (Map<String, Object>) get(
            "/v1/admin/kyc/applications" + qs, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycAdminDetail(String applicationId) {
        return (Map<String, Object>) get(
            "/v1/admin/kyc/applications/" + enc(applicationId), Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycAdminApprove(String applicationId, String notes) {
        Map<String, Object> body = new HashMap<>();
        if (notes != null) body.put("notes", notes);
        body.put("reset_risk", false);
        return (Map<String, Object>) post(
            "/v1/admin/kyc/applications/" + enc(applicationId) + "/approve",
            body, null, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycAdminReject(
            String applicationId, String reasonCode, String notes) {
        Map<String, Object> body = new HashMap<>();
        body.put("reason_code", reasonCode);
        if (notes != null) body.put("notes", notes);
        return (Map<String, Object>) post(
            "/v1/admin/kyc/applications/" + enc(applicationId) + "/reject",
            body, null, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> kycAdminRequestRetry(String applicationId, String notes) {
        Map<String, Object> body = (notes == null || notes.isEmpty())
            ? Map.of() : Map.of("notes", notes);
        return (Map<String, Object>) post(
            "/v1/admin/kyc/applications/" + enc(applicationId) + "/request-retry",
            body, null, Map.class);
    }

    // ── Address Verification ──────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Object> avCreate(Map<String, Object> body) {
        return (Map<String, Object>) post(
            "/v1/address-verifications", body, null, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> avUploadProof(
            String verificationId, String clientToken, Map<String, Object> body) {
        return (Map<String, Object>) post(
            "/v1/address-verifications/" + enc(verificationId) + "/proof",
            body, null, clientToken, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> avSubmit(String verificationId, String clientToken) {
        return (Map<String, Object>) post(
            "/v1/address-verifications/" + enc(verificationId) + "/submit",
            Map.of(), null, clientToken, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> avStatus(String verificationId) {
        return (Map<String, Object>) get(
            "/v1/address-verifications/" + enc(verificationId) + "/status", Map.class);
    }

    // ── Personas ──────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Object> personaCreate(Map<String, Object> body) {
        return (Map<String, Object>) post(
            "/v1/personas", body, null, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> personaList(Map<String, Object> queryParams) {
        StringBuilder qs = new StringBuilder("?limit=50");
        if (queryParams != null) {
            qs.setLength(0);
            for (var e : queryParams.entrySet()) {
                if (e.getValue() == null) continue;
                qs.append(qs.length() == 0 ? "?" : "&")
                  .append(enc(e.getKey())).append("=").append(enc(String.valueOf(e.getValue())));
            }
        }
        return (Map<String, Object>) get("/v1/personas" + qs, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> personaGet(String personaId) {
        return (Map<String, Object>) get("/v1/personas/" + enc(personaId), Map.class);
    }

    /** URL-encode a single path / query segment. */
    private static String enc(String s) {
        return java.net.URLEncoder.encode(
            Objects.requireNonNull(s, "path segment"),
            java.nio.charset.StandardCharsets.UTF_8);
    }

    // ── platform ───────────────────────────────────────────────────────

    public StatusPayload status() { return get("/v1/status", StatusPayload.class); }

    // ──────────────────────────────────────────────────────────────────

    private <T> T get(String path, Class<T> type) {
        return send(buildRequest("GET", path, null, null, null, "application/json"), type, false);
    }

    /** GET with X-KYC-Client-Token header. Used by status/extend-ttl
     *  and AV per-verification endpoints. */
    private <T> T get(String path, String clientToken, Class<T> type) {
        return send(buildRequest("GET", path, null, null, clientToken, "application/json"), type, false);
    }

    private <T> T post(String path, Object body, String idem, Class<T> type) {
        HttpRequest req = buildRequest("POST", path, body, idem, null, "application/json");
        return send(req, type, false);
    }

    /** POST with X-KYC-Client-Token header for per-application
     *  artefact endpoints (documents / nfc / selfie / liveness /
     *  submit / retry / extend-ttl) + AV proof / submit. */
    private <T> T post(String path, Object body, String idem, String clientToken, Class<T> type) {
        HttpRequest req = buildRequest("POST", path, body, idem, clientToken, "application/json");
        return send(req, type, false);
    }

    private byte[] pdf(String path, Object body) {
        HttpRequest req = buildRequest("POST", path, body, null, null, "application/pdf");
        return send(req, byte[].class, true);
    }

    private HttpRequest buildRequest(
            String method, String path, Object body, String idem,
            String clientToken, String accept) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(baseUri.resolve(path))
                .timeout(requestTimeout)
                .header("Authorization", authHeader)
                .header("Accept", accept)
                .header("User-Agent", "legichain-java/0.1.0");
        defaultHeaders.forEach(b::header);
        if (idem != null && !idem.isEmpty()) b.header("Idempotency-Key", idem);
        if (clientToken != null && !clientToken.isEmpty())
            b.header("X-KYC-Client-Token", clientToken);

        if (body == null) {
            return b.method(method, HttpRequest.BodyPublishers.noBody()).build();
        }
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new LegichainException("legichain: serialize body", e);
        }
        return b.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T> T send(HttpRequest req, Class<T> type, boolean binary) {
        HttpResponse<byte[]> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new LegichainException("legichain: network error: " + e.getMessage(), e);
        }
        int sc = res.statusCode();
        byte[] body = res.body();

        if (sc < 200 || sc >= 300) {
            ProblemDetails pd = null;
            try {
                pd = mapper.readValue(body, ProblemDetails.class);
            } catch (Exception ignored) { /* tolerate non-JSON errors */ }
            if (pd == null) {
                pd = new ProblemDetails(
                        "https://legichain.com/errors/UNKNOWN",
                        "Error", sc, new String(body),
                        "HTTP_" + sc, null, null);
            }
            throw new LegichainException(pd);
        }

        if (binary) return (T) body;
        if (sc == 204 || body.length == 0) return null;

        try {
            return mapper.readValue(body, type);
        } catch (IOException e) {
            throw new LegichainException("legichain: decode response: " + e.getMessage(), e);
        }
    }

    private static ObjectMapper defaultMapper() {
        return new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    // ── builder ────────────────────────────────────────────────────────

    public static final class Builder {
        private String apiKey;
        private String baseUrl = "https://api.legichain.com";
        private HttpClient httpClient;
        private ObjectMapper objectMapper;
        private Duration requestTimeout;
        private final Map<String, String> defaultHeaders = new HashMap<>();

        public Builder apiKey(String key) { this.apiKey = key; return this; }
        public Builder baseUrl(String url) { this.baseUrl = url; return this; }
        public Builder httpClient(HttpClient c) { this.httpClient = c; return this; }
        public Builder objectMapper(ObjectMapper m) { this.objectMapper = m; return this; }
        public Builder requestTimeout(Duration d) { this.requestTimeout = d; return this; }
        public Builder header(String name, String value) {
            defaultHeaders.put(name, value);
            return this;
        }
        public Legichain build() { return new Legichain(this); }
    }
}
