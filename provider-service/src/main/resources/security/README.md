# Demo Security Material

The PKCS12 files in this directory are generated locally by `scripts/generate-demo-keys.sh`.
They are ignored by Git and should not be committed.

Password: `changeit`

- `provider-signing.p12`: provider private key and certificate, alias `provider`
- `provider-truststore.p12`: trusted client certificate, alias `client`

Do not use these keys outside the demo.
