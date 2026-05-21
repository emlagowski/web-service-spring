package com.example.demo.provider.config;

import java.util.List;
import org.apache.wss4j.common.crypto.Crypto;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.security.wss4j2.support.CryptoFactoryBean;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class ProviderSoapConfiguration implements WsConfigurer {

    static final String SOAP_BODY_AND_TIMESTAMP = "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;"
            + "{}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp";

    @Bean
    ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "content")
    DefaultWsdl11Definition contentWsdl(XsdSchema contentSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("ContentPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("urn:demo:provider:content:v1");
        wsdl.setSchema(contentSchema);
        return wsdl;
    }

    @Bean
    XsdSchema contentSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/content.xsd"));
    }

    @Bean
    Jaxb2Marshaller providerMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan(
                "com.example.demo.contract.content",
                "com.example.demo.contract.callback");
        return marshaller;
    }

    @Bean
    Wss4jSecurityInterceptor providerSecurityInterceptor() throws Exception {
        Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();
        interceptor.setValidationActions("Timestamp Signature");
        interceptor.setValidationSignatureCrypto(trustedClientCrypto());
        interceptor.setValidationTimeToLive(300);
        interceptor.setTimestampStrict(true);
        interceptor.setSecurementActions("Timestamp Signature");
        interceptor.setSecurementUsername("provider");
        interceptor.setSecurementPassword("changeit");
        interceptor.setSecurementSignatureCrypto(providerSigningCrypto());
        interceptor.setSecurementSignatureKeyIdentifier("DirectReference");
        interceptor.setSecurementSignatureParts(SOAP_BODY_AND_TIMESTAMP);
        interceptor.setSecurementTimeToLive(300);
        interceptor.setSecurementMustUnderstand(true);
        interceptor.afterPropertiesSet();
        return interceptor;
    }

    @Bean
    Crypto providerSigningCrypto() throws Exception {
        return crypto("security/provider-signing.p12", "provider").getObject();
    }

    @Bean
    Crypto trustedClientCrypto() throws Exception {
        return crypto("security/provider-truststore.p12", "client").getObject();
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        try {
            interceptors.add(providerSecurityInterceptor());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot configure provider SOAP security", ex);
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
