package com.example.demo.client.config;

import java.util.List;
import org.apache.wss4j.common.crypto.Crypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.security.wss4j2.support.CryptoFactoryBean;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

@EnableWs
@Configuration
public class ClientSoapConfiguration implements WsConfigurer {

    static final String SOAP_BODY_AND_TIMESTAMP = "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;"
            + "{}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp";

    @Bean
    ServletRegistrationBean<MessageDispatcherServlet> callbackMessageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "callback")
    DefaultWsdl11Definition callbackWsdl(XsdSchema callbackSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("CallbackPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("urn:demo:client:callback:v1");
        wsdl.setSchema(callbackSchema);
        return wsdl;
    }

    @Bean
    XsdSchema callbackSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/callback.xsd"));
    }

    @Bean
    Jaxb2Marshaller clientMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan(
                "com.example.demo.contract.content",
                "com.example.demo.contract.callback");
        return marshaller;
    }

    @Bean
    WebServiceTemplate providerWebServiceTemplate(
            WebServiceTemplateBuilder webServiceTemplateBuilder,
            Jaxb2Marshaller clientMarshaller,
            Wss4jSecurityInterceptor clientSecurityInterceptor,
            ClientHttpRequestMessageSender logbookSoapMessageSender,
            @Value("${provider.soap.uri}") String providerSoapUri) {
        return webServiceTemplateBuilder
                .setMarshaller(clientMarshaller)
                .setUnmarshaller(clientMarshaller)
                .setDefaultUri(providerSoapUri)
                .messageSenders(logbookSoapMessageSender)
                .interceptors(new ClientInterceptor[] {clientSecurityInterceptor})
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

    @Bean
    Wss4jSecurityInterceptor clientSecurityInterceptor() throws Exception {
        Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();
        interceptor.setValidationActions("Timestamp Signature");
        interceptor.setValidationSignatureCrypto(trustedProviderCrypto());
        interceptor.setValidationTimeToLive(300);
        interceptor.setTimestampStrict(true);
        interceptor.setSecurementActions("Timestamp Signature");
        interceptor.setSecurementUsername("client");
        interceptor.setSecurementPassword("changeit");
        interceptor.setSecurementSignatureCrypto(clientSigningCrypto());
        interceptor.setSecurementSignatureKeyIdentifier("DirectReference");
        interceptor.setSecurementSignatureParts(SOAP_BODY_AND_TIMESTAMP);
        interceptor.setSecurementTimeToLive(300);
        interceptor.setSecurementMustUnderstand(true);
        interceptor.afterPropertiesSet();
        return interceptor;
    }

    @Bean
    Crypto clientSigningCrypto() throws Exception {
        return crypto("security/client-signing.p12", "client").getObject();
    }

    @Bean
    Crypto trustedProviderCrypto() throws Exception {
        return crypto("security/client-truststore.p12", "provider").getObject();
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        try {
            interceptors.add(clientSecurityInterceptor());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot configure client callback SOAP security", ex);
        }
    }

    private static CryptoFactoryBean crypto(String classpathLocation, String alias) throws Exception {
        CryptoFactoryBean crypto = new CryptoFactoryBean();
        crypto.setKeyStoreLocation(new ClassPathResource(classpathLocation));
        crypto.setKeyStorePassword("changeit");
        crypto.setKeyStoreType("pkcs12");
        crypto.setDefaultX509Alias(alias);
        crypto.afterPropertiesSet();
        return crypto;
    }
}
