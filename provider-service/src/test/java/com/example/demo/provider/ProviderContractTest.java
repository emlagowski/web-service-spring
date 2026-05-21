package com.example.demo.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.contract.content.GetContentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.client.RestClient;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;
import org.zalando.logbook.BodyFilter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProviderContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    @Qualifier("callbackWebServiceTemplate")
    private WebServiceTemplate callbackWebServiceTemplate;

    @Autowired
    @Qualifier("prettyPrintingBodyFilter")
    private BodyFilter prettyPrintingBodyFilter;

    @Test
    void providerWsdlExposesContentOperations() {
        String wsdl = RestClient.create("http://localhost:" + port)
                .get()
                .uri("/ws/content.wsdl")
                .retrieve()
                .body(String.class);

        assertThat(wsdl)
                .contains("GetContent")
                .contains("SearchContent")
                .contains("urn:demo:provider:content:v1");
    }

    @Test
    void providerRejectsUnsignedSoapRequest() throws Exception {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("com.example.demo.contract.content");
        marshaller.afterPropertiesSet();

        WebServiceTemplate unsignedTemplate = new WebServiceTemplate(marshaller);
        unsignedTemplate.setDefaultUri("http://localhost:" + port + "/ws");

        GetContentRequest request = new GetContentRequest();
        request.setContentId("article-1");

        assertThatThrownBy(() -> unsignedTemplate.marshalSendAndReceive(request))
                .isInstanceOf(SoapFaultClientException.class)
                .hasMessageContaining("Security");
    }

    @Test
    void logbookIsConfiguredForFullHttpTrafficLogging() throws Exception {
        assertThat(environment.getProperty("logging.level.org.zalando.logbook.Logbook")).isEqualTo("TRACE");
        assertThat(environment.getProperty("logbook.format.style")).isEqualTo("http");
        assertThat(environment.getProperty("logbook.strategy")).isEqualTo("default");
        assertThat(environment.getProperty("logbook.write.max-body-size")).isEqualTo("-1");
        assertThat(environment.getProperty("logbook.filters.body.default-enabled")).isEqualTo("false");
        assertThat(environment.containsProperty("logging.level.org.springframework.ws.server.MessageTracing.sent"))
                .isFalse();
        assertThat(environment.containsProperty("logging.level.org.springframework.ws.server.MessageTracing.received"))
                .isFalse();
        assertThat(environment.containsProperty("logging.level.org.springframework.ws.client.MessageTracing.sent"))
                .isFalse();
        assertThat(environment.containsProperty("logging.level.org.springframework.ws.client.MessageTracing.received"))
                .isFalse();

        Class<?> logbookType = Class.forName("org.zalando.logbook.Logbook");
        assertThat(applicationContext.getBeanNamesForType(logbookType)).isNotEmpty();

        assertThat(callbackWebServiceTemplate.getMessageSenders()).hasSize(1);
        assertThat(callbackWebServiceTemplate.getMessageSenders()[0])
                .isInstanceOf(ClientHttpRequestMessageSender.class);
        ClientHttpRequestMessageSender messageSender =
                (ClientHttpRequestMessageSender) callbackWebServiceTemplate.getMessageSenders()[0];
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
