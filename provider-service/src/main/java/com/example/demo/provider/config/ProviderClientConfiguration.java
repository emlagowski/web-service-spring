package com.example.demo.provider.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

@Configuration
public class ProviderClientConfiguration {

    @Bean
    WebServiceTemplate callbackWebServiceTemplate(
            WebServiceTemplateBuilder webServiceTemplateBuilder,
            Jaxb2Marshaller providerMarshaller,
            @Qualifier("providerSecurityInterceptor") Wss4jSecurityInterceptor securityInterceptor,
            ClientHttpRequestMessageSender logbookSoapMessageSender) {
        return webServiceTemplateBuilder
                .setMarshaller(providerMarshaller)
                .setUnmarshaller(providerMarshaller)
                .messageSenders(logbookSoapMessageSender)
                .interceptors(new ClientInterceptor[] {securityInterceptor})
                .build();
    }

    @Bean
    ClientHttpRequestMessageSender logbookSoapMessageSender(
            LogbookClientHttpRequestInterceptor logbookClientHttpRequestInterceptor) {
        InterceptingClientHttpRequestFactory requestFactory = new InterceptingClientHttpRequestFactory(
                new SimpleClientHttpRequestFactory(),
                List.of(logbookClientHttpRequestInterceptor));
        return new ClientHttpRequestMessageSender(requestFactory);
    }
}
