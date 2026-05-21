#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
PASSWORD="changeit"
PROVIDER_SECURITY="$ROOT_DIR/provider-service/src/main/resources/security"
CLIENT_SECURITY="$ROOT_DIR/client-service/src/main/resources/security"
TMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

mkdir -p "$PROVIDER_SECURITY" "$CLIENT_SECURITY"
rm -f \
  "$PROVIDER_SECURITY/provider-signing.p12" \
  "$PROVIDER_SECURITY/provider-truststore.p12" \
  "$CLIENT_SECURITY/client-signing.p12" \
  "$CLIENT_SECURITY/client-truststore.p12"

keytool -genkeypair \
  -alias provider \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -dname "CN=Demo Provider, OU=Spring SOAP Demo, O=Example, L=Warsaw, ST=Mazowieckie, C=PL" \
  -storetype PKCS12 \
  -keystore "$PROVIDER_SECURITY/provider-signing.p12" \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD" \
  -noprompt

keytool -genkeypair \
  -alias client \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -dname "CN=Demo Client, OU=Spring SOAP Demo, O=Example, L=Warsaw, ST=Mazowieckie, C=PL" \
  -storetype PKCS12 \
  -keystore "$CLIENT_SECURITY/client-signing.p12" \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD" \
  -noprompt

keytool -exportcert \
  -alias provider \
  -keystore "$PROVIDER_SECURITY/provider-signing.p12" \
  -storetype PKCS12 \
  -storepass "$PASSWORD" \
  -rfc \
  -file "$TMP_DIR/provider.cer"

keytool -exportcert \
  -alias client \
  -keystore "$CLIENT_SECURITY/client-signing.p12" \
  -storetype PKCS12 \
  -storepass "$PASSWORD" \
  -rfc \
  -file "$TMP_DIR/client.cer"

keytool -importcert \
  -alias client \
  -file "$TMP_DIR/client.cer" \
  -storetype PKCS12 \
  -keystore "$PROVIDER_SECURITY/provider-truststore.p12" \
  -storepass "$PASSWORD" \
  -noprompt

keytool -importcert \
  -alias provider \
  -file "$TMP_DIR/provider.cer" \
  -storetype PKCS12 \
  -keystore "$CLIENT_SECURITY/client-truststore.p12" \
  -storepass "$PASSWORD" \
  -noprompt

echo "Generated demo-only PKCS12 stores with password '$PASSWORD'."
