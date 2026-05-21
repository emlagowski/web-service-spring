# Demo Security Material

The PKCS12 files in this directory are generated locally by `scripts/generate-demo-keys.sh`.
They are ignored by Git and should not be committed.

Password: `changeit`

- `client-signing.p12`: client private key and certificate, alias `client`
- `client-truststore.p12`: trusted provider certificate, alias `provider`

Do not use these keys outside the demo.
