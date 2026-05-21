package com.example.demo.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;
import org.zalando.logbook.BodyFilter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "provider.soap.uri=http://localhost:65535/ws")
class ClientContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    @Qualifier("providerWebServiceTemplate")
    private WebServiceTemplate providerWebServiceTemplate;

    @Autowired
    @Qualifier("prettyPrintingBodyFilter")
    private BodyFilter prettyPrintingBodyFilter;

    @Test
    void callbackWsdlExposesDeliveryNotificationOperation() {
        String wsdl = RestClient.create("http://localhost:" + port)
                .get()
                .uri("/ws/callback.wsdl")
                .retrieve()
                .body(String.class);

        assertThat(wsdl)
                .contains("NotifyContentDelivered")
                .contains("urn:demo:client:callback:v1");
    }

    @Test
    void logbookIsConfiguredForFullHttpTrafficLogging() throws Exception {
        assertThat(environment.getProperty("logging.level.org.zalando.logbook.Logbook")).isEqualTo("TRACE");
        assertThat(environment.getProperty("logbook.format.style")).isEqualTo("http");
        assertThat(environment.getProperty("logbook.strategy")).isEqualTo("default");
        assertThat(environment.getProperty("logbook.write.max-body-size")).isEqualTo("-1");
        assertThat(environment.getProperty("logbook.filters.body.default-enabled")).isEqualTo("false");
        assertThat(environment.containsProperty("logging.level.org.springframework.ws.client.MessageTracing.sent"))
                .isFalse();
        assertThat(environment.containsProperty("logging.level.org.springframework.ws.client.MessageTracing.received"))
                .isFalse();
        assertThat(environment.containsProperty("logging.level.org.springframework.ws.server.MessageTracing.sent"))
                .isFalse();
        assertThat(environment.containsProperty("logging.level.org.springframework.ws.server.MessageTracing.received"))
                .isFalse();

        Class<?> logbookType = Class.forName("org.zalando.logbook.Logbook");
        assertThat(applicationContext.getBeanNamesForType(logbookType)).isNotEmpty();

        assertThat(providerWebServiceTemplate.getMessageSenders()).hasSize(1);
        assertThat(providerWebServiceTemplate.getMessageSenders()[0])
                .isInstanceOf(ClientHttpRequestMessageSender.class);
        ClientHttpRequestMessageSender messageSender =
                (ClientHttpRequestMessageSender) providerWebServiceTemplate.getMessageSenders()[0];
        assertThat(messageSender.getRequestFactory())
                .isInstanceOf(InterceptingClientHttpRequestFactory.class);
    }

    @Test
    void logbookPrettyPrintsJsonAndXmlBodies() {
        String json = prettyPrintingBodyFilter.filter("application/json", "{\"contentId\":\"article-1\",\"items\":[1,2]}");
        String xml = prettyPrintingBodyFilter.filter("text/xml;charset=utf-8", "<root><item id=\"1\">value</item></root>");

        assertThat(json)
                .contains(System.lineSeparator())
                .contains("  \"contentId\"");
        assertThat(xml)
                .contains(System.lineSeparator())
                .contains("  <item id=\"1\">value</item>");
    }
}
