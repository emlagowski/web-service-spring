# Spring Boot SOAP XML Signature Demo

Demo contains two Spring Boot applications communicating through Spring Web Services SOAP messages signed with WS-Security XML Signature.

Flow:

```text
user -> REST -> client-service <-> signed SOAP <-> provider-service
                         ^
                         |
                  signed SOAP callback
```

## Modules

- `provider-service`: content provider SOAP application on port `8080`
- `client-service`: REST facade and SOAP callback application on port `8081`
- `soap-contracts`: XSD contracts and generated JAXB model classes

The SOAP security layer uses Spring-WS `Wss4jSecurityInterceptor`. That is the Spring-native integration point, backed by Apache WSS4J transitively through `spring-ws-security`.

## Requirements

- Java 21 or newer
- `keytool` available on `PATH` (bundled with the JDK)
- No local Maven installation is required. `./mvnw` downloads Maven 3.9.11 into ignored `.mvn/apache-maven-*` when needed.

## Security Material

Do not commit demo keys. `*.p12` is ignored by Git.

Generate local PKCS12 stores before building or running the applications:

```bash
scripts/generate-demo-keys.sh
```

The script writes the files into the exact resource directories used by the services:

```text
provider-service/src/main/resources/security/provider-signing.p12
provider-service/src/main/resources/security/provider-truststore.p12
client-service/src/main/resources/security/client-signing.p12
client-service/src/main/resources/security/client-truststore.p12
```

The demo password is fixed to `changeit`. These keys are intentionally non-production.

## Run

```bash
./scripts/generate-demo-keys.sh
./mvnw verify
```

Start the applications in separate terminals:

```bash
./mvnw -pl provider-service spring-boot:run
```

```bash
./mvnw -pl client-service spring-boot:run
```

Then call the public REST API:

```bash
curl http://localhost:8081/api/content/article-1
curl "http://localhost:8081/api/content?category=spring"
curl http://localhost:8081/api/callback-events
```

SOAP WSDLs:

- `http://localhost:8080/ws/content.wsdl`
- `http://localhost:8081/ws/callback.wsdl`

## Public APIs

Client REST API:

- `GET http://localhost:8081/api/content/article-1`
- `GET http://localhost:8081/api/content?category=spring`
- `GET http://localhost:8081/api/callback-events`

Provider SOAP WSDL:

- `GET http://localhost:8080/ws/content.wsdl`
- operations: `GetContent`, `SearchContent`

Client callback SOAP WSDL:

- `GET http://localhost:8081/ws/callback.wsdl`
- operation: `NotifyContentDelivered`

## Request/Response Logging

Both applications include Zalando Logbook with HTTP format logging enabled at `TRACE`.
It logs inbound servlet HTTP traffic, so the public REST API and Spring-WS `/ws` SOAP endpoints show method, URL, headers, request body, response status, response headers, and response body in the application logs.

SOAP clients are built with Spring Boot `WebServiceTemplateBuilder` and use the Logbook `ClientHttpRequestInterceptor` through Spring-WS `ClientHttpRequestMessageSender`, so outbound SOAP calls are logged by Logbook too.
JSON and XML bodies are pretty-printed before Logbook writes them.

Spring-WS `MessageTracing` is intentionally not enabled; Logbook is the single request/response logging mechanism for REST and SOAP HTTP traffic.

## Provider Log Excerpt

[client.log](logs/client.log)

## Client Log Excerpt

[provider.log](logs/provider.log)
