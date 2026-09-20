# Maven SDK release signatures

Maven Central v2.0.0 artifacts are signed with the Legichain SDK release key.

- Identity: Legichain SDK Releases <contact@legichain.com>
- Algorithm: RSA 4096, SHA-512 artifact signatures
- Fingerprint: `F98DA54400147C462DC6D6286909BAEB276FA181`
- Public key: [keys/sdk-releases.asc](keys/sdk-releases.asc)
- Public keyserver: https://keyserver.ubuntu.com

Verify a downloaded artifact and its detached signature:

```sh
gpg --import keys/sdk-releases.asc
gpg --verify legichain-2.0.0.jar.asc legichain-2.0.0.jar
```

Private signing material is not stored in this repository. Central publishing
credentials authenticate upload; the separate GPG key signs the artifacts.
