# Changelog

All notable changes to the Legichain Java SDK.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
the artifact follows [Semantic Versioning](https://semver.org/).

## [0.1.0] — 2026-05-20

First public release. Until the artifact lands on Maven Central, pull
it from [JitPack](https://jitpack.io) using the `v0.1.0` git tag:

```xml
<repositories>
  <repository><id>jitpack.io</id><url>https://jitpack.io</url></repository>
</repositories>
<dependency>
  <groupId>com.github.legichain</groupId>
  <artifactId>legichain-java</artifactId>
  <version>v0.1.0</version>
</dependency>
```

### Added
- `Legichain.builder().apiKey(...).build()` — fluent builder, immutable client.
- `screenPerson / screenCompany / screenCrypto / screenBatch /
  screenBatchAsync / job` with optional `Idempotency-Key`.
- `reportWallet / reportPerson / reportCompany` — return PDF as
  `byte[]`.
- `status()` — public platform status, no auth.
- Strong typing via Java records: `ScreeningResponse`, `Hit`,
  `HitFlags`, `ScreeningSummary`, `JobStatus`, `BatchAsyncResponse`,
  `StatusPayload`, `StatusIncident`, `ProblemDetails`.
- `RiskLevel` and `Recommendation` enums (`@JsonValue` round-trip).
- `LegichainException` carries the RFC 7807 problem body — branch on
  `e.code()` / `e.status()` / `e.detail()`.
- `Webhooks.verifySignature(body, header, secret)` — HMAC-SHA256,
  constant-time `MessageDigest.isEqual`, default 5-minute replay
  tolerance.
- Native `java.net.http.HttpClient`; only runtime dependency is
  `com.fasterxml.jackson:jackson-databind`.

### Requirements
- Java 17 (LTS) or newer.
- Jackson-databind 2.18 (transitively pulled).
