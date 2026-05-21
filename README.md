# Legichain Java SDK

Official Java client for the **[Legichain](https://legichain.com)** AML, KYC
and Travel Rule API.

```xml
<!-- Maven -->
<dependency>
  <groupId>com.legichain</groupId>
  <artifactId>legichain</artifactId>
  <version>0.1.0</version>
</dependency>
```

```kotlin
// Gradle (Kotlin DSL)
implementation("com.legichain:legichain:0.1.0")
```

[![GitHub release](https://img.shields.io/github/v/tag/legichain/legichain-java.svg)](https://github.com/legichain/legichain-java/releases)
[![License](https://img.shields.io/github/license/legichain/legichain-java.svg)](https://github.com/legichain/legichain-java/blob/main/LICENSE)

- Java 17+ (LTS), zero reflection
- Native `java.net.http.HttpClient` — no extra HTTP dependencies
- Jackson-databind for JSON
- Typed `LegichainException` carries the full RFC 7807 problem body
- HMAC-SHA256 webhook verifier (constant-time compare)

## Get an API key

Sign up at **<https://legichain.com>** — the Free plan ships with 1 RPS
and 300 monthly credits, no card required. Once signed in:

> **panel.legichain.com → Settings → API Keys → New key**

Keys look like `lc_live_<22>.sk_live_<44>` (production) or
`lc_test_<22>.sk_test_<44>` (test mode — never spends credits, safe in CI).
Store the secret half in your secret manager — the Argon2 hash means a
lost key can't be recovered. See the
[full API guide](https://legichain.com/developers) for plans, rate limits
and reference docs.

## Quick start

```java
import com.legichain.Legichain;
import com.legichain.LegichainException;
import com.legichain.model.PersonQuery;
import com.legichain.model.ScreeningResponse;

Legichain lc = Legichain.builder()
    .apiKey(System.getenv("LEGICHAIN_API_KEY"))
    .build();

try {
    ScreeningResponse r = lc.screenPerson(
        new PersonQuery("Vladimir Putin", "RU", "1952-10-07", null, null, null)
    );

    switch (r.summary().recommendation()) {
        case BLOCK  -> denyOnboarding(customerId);
        case REVIEW -> queueForCompliance(customerId, r.screeningId());
        case CLEAR  -> approveOnboarding(customerId);
    }

    System.out.printf("%d credits spent; %d remaining%n",
                      r.costCredits(), r.creditsRemaining());

} catch (LegichainException e) {
    // RFC 7807 — branch on stable codes
    switch (e.code()) {
        case "BIL_001_INSUFFICIENT_CREDITS" -> topUp();
        case "RL_001_RATE_LIMITED"          -> backoff();
        case "AUTH_002_INVALID_TOKEN"       -> rotateKey();
        default                              -> log(e.status(), e.detail());
    }
}
```

### Company + crypto wallet

```java
ScreeningResponse company = lc.screenCompany(
    new CompanyQuery("Rosneft Oil Company", "RU", null, null)
);

ScreeningResponse wallet = lc.screenCrypto(
    new CryptoQuery("0x098B716B8Aaf21512996dC57EB0615e2383E2f96")
);
```

### Batch (sync + async)

```java
List<Object> items = List.of(
    new PersonQuery("Acme Trading GmbH"),
    new CryptoQuery("TVj7RNVH...", "tron"),
    new PersonQuery("Maria Lopez", "ES", null, null, null, null)
);

// up to 200 items synchronously
List<ScreeningResponse> results = lc.screenBatch(items);

// async — result delivered to your webhook
BatchAsyncResponse job = lc.screenBatchAsync(items);
JobStatus done = lc.job(job.jobId());
```

### PDF reports

```java
byte[] pdf = lc.reportWallet(new CryptoQuery(
    "0x6c0bD2BB04Fda9CBfeBb8DC1208Db32a0F8a4Edd", "eth"
));
java.nio.file.Files.write(java.nio.file.Path.of("wallet.pdf"), pdf);
```

### Idempotency

```java
lc.screenPerson(new PersonQuery("Maria Lopez"), "onboarding-2026-05-20-7f3c");
// Re-running the same key within 24h returns the cached response.
```

### Webhook verification

```java
import com.legichain.Webhooks;
import jakarta.servlet.http.*;

@WebServlet("/webhooks/legichain")
public class LegichainWebhook extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        byte[] body = req.getInputStream().readAllBytes();
        boolean ok = Webhooks.verifySignature(
            body,
            req.getHeader("Legichain-Signature"),
            System.getenv("LEGICHAIN_WEBHOOK_SECRET")
        );
        if (!ok) { res.setStatus(401); return; }
        // ... handle the event
    }
}
```

## Configuration

```java
Legichain lc = Legichain.builder()
    .apiKey(key)
    .baseUrl("https://staging.api.legichain.com")
    .requestTimeout(java.time.Duration.ofSeconds(60))
    .header("X-My-App", "billing-svc")
    .build();
```

You can inject your own `HttpClient` (e.g. one that goes through a
corporate proxy) and your own `ObjectMapper`:

```java
HttpClient http = HttpClient.newBuilder()
    .proxy(ProxySelector.of(new InetSocketAddress("proxy.bank.tr", 8080)))
    .build();

Legichain lc = Legichain.builder().apiKey(key).httpClient(http).build();
```

## Reference

| Method | Endpoint |
| --- | --- |
| `lc.screenPerson(q)`     | `POST /v1/screen/person` |
| `lc.screenCompany(q)`    | `POST /v1/screen/company` |
| `lc.screenCrypto(q)`     | `POST /v1/screen/crypto` |
| `lc.screenBatch(items)`  | `POST /v1/screen/batch` |
| `lc.screenBatchAsync(items)` | `POST /v1/screen/batch/async` |
| `lc.job(jobId)`          | `GET  /v1/screen/jobs/{id}` |
| `lc.reportWallet(q)`     | `POST /v1/reports/wallet` → `byte[]` (PDF) |
| `lc.reportPerson(q)`     | `POST /v1/reports/person` → `byte[]` (PDF) |
| `lc.reportCompany(q)`    | `POST /v1/reports/company` → `byte[]` (PDF) |
| `lc.status()`            | `GET  /v1/status` |

---

## Versioning & support

- Tracks API `v1`. Breaking changes ship on `/v2/` with ≥ 6 months overlap.
- Status: <https://legichain.com/status>
- Email: `contact@legichain.com`
- GitHub: <https://github.com/legichain/legichain-java>
