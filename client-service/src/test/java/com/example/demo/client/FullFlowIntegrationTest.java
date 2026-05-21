package com.example.demo.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.contract.content.GetContentRequest;
import com.example.demo.provider.ProviderServiceApplication;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.security.wss4j2.support.CryptoFactoryBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FullFlowIntegrationTest {

    private static ConfigurableApplicationContext providerContext;

    @LocalServerPort
    private int clientPort;

    @DynamicPropertySource
    static void registerProviderUrl(DynamicPropertyRegistry registry) {
        providerContext = new SpringApplicationBuilder(ProviderServiceApplication.class)
                .run("--server.port=0", "--spring.application.name=provider-service");
        int providerPort = providerContext.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
        registry.add("provider.soap.uri", () -> "http://localhost:" + providerPort + "/ws");
    }

    @AfterAll
    static void stopProvider() {
        if (providerContext != null) {
            SpringApplication.exit(providerContext);
        }
    }

    @Test
    void restFacadeCallsSignedProviderSoapAndStoresSignedCallback() {
        RestClient restClient = RestClient.create("http://localhost:" + clientPort);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.get()
                .uri("/api/content/article-1")
                .retrieve()
                .body(Map.class);

        assertThat(response)
                .containsEntry("contentId", "article-1")
                .containsEntry("title", "Spring Native SOAP");
        assertThat((String) response.get("body")).contains("XML Signature");

        @SuppressWarnings("unchecked")
        Map<String, Object>[] events = restClient.get()
                .uri("/api/callback-events")
                .retrieve()
                .body(Map[].class);

        assertThat(events)
                .hasSize(1);
        assertThat(events[0].get("contentId")).isEqualTo("article-1");
    }

    @Test
    void clientRejectsProviderResponseSignedByUntrustedCertificate() throws Exception {
        int providerPort = providerContext.getEnvironment().getRequiredProperty("local.server.port", Integer.class);

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("com.example.demo.contract.content");
        marshaller.afterPropertiesSet();

        Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();
        interceptor.setSecurementActions("Timestamp Signature");
        interceptor.setSecurementUsername("client");
        interceptor.setSecurementPassword("changeit");
        interceptor.setSecurementSignatureKeyIdentifier("DirectReference");
        interceptor.setSecurementSignatureCrypto(crypto("classpath:security/client-signing.p12", "client").getObject());
        interceptor.setSecurementSignatureParts("{}{http://schemas.xmlsoap.org/soap/envelope/}Body;"
                + "{}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp");
        interceptor.setValidationActions("Timestamp Signature");
        interceptor.setValidationSignatureCrypto(crypto("classpath:security/client-signing.p12", "client").getObject());
        interceptor.afterPropertiesSet();

        WebServiceTemplate template = new WebServiceTemplate(marshaller);
        template.setDefaultUri("http://localhost:" + providerPort + "/ws");
        template.setInterceptors(new org.springframework.ws.client.support.interceptor.ClientInterceptor[] {interceptor});

        GetContentRequest request = new GetContentRequest();
        request.setContentId("article-1");

        assertThatThrownBy(() -> template.marshalSendAndReceive(request))
                .isInstanceOf(UnmarshallingFailureException.class)
                .hasMessageContaining("JAXB unmarshalling exception");
    }

    private static CryptoFactoryBean crypto(String location, String alias) throws Exception {
        CryptoFactoryBean crypto = new CryptoFactoryBean();
        crypto.setKeyStoreLocation(new org.springframework.core.io.DefaultResourceLoader().getResource(location));
        crypto.setKeyStorePassword("changeit");
        crypto.setKeyStoreType("pkcs12");
        crypto.setDefaultX509Alias(alias);
        crypto.afterPropertiesSet();
        return crypto;
    }
}
