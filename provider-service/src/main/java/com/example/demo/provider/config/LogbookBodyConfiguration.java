package com.example.demo.provider.config;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.core.BodyFilters;
import org.zalando.logbook.json.JsonBodyFilters;
import org.zalando.logbook.json.PrettyPrintingJsonBodyFilter;

@Configuration
class LogbookBodyConfiguration {

    @Bean
    BodyFilter prettyPrintingBodyFilter() {
        BodyFilter obfuscation = BodyFilter.merge(
                BodyFilters.oauthRequest(),
                JsonBodyFilters.accessToken());
        BodyFilter prettyPrinting = BodyFilter.merge(
                new PrettyPrintingJsonBodyFilter(),
                new PrettyPrintingXmlBodyFilter());
        return BodyFilter.merge(obfuscation, prettyPrinting);
    }

    private static final class PrettyPrintingXmlBodyFilter implements BodyFilter {

        @Override
        public String filter(String contentType, String body) {
            if (!isXml(contentType) || body == null || body.isBlank()) {
                return body;
            }
            try {
                DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
                documentFactory.setNamespaceAware(true);
                documentFactory.setXIncludeAware(false);
                documentFactory.setExpandEntityReferences(false);
                documentFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                documentFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                documentFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

                Document document = documentFactory.newDocumentBuilder()
                        .parse(new InputSource(new StringReader(body)));
                removeWhitespaceTextNodes(document);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

                var transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(document), new StreamResult(writer));
                return writer.toString().trim();
            } catch (Exception ex) {
                return body;
            }
        }

        private static boolean isXml(String contentType) {
            if (contentType == null) {
                return false;
            }
            String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            return mediaType.equals("text/xml")
                    || mediaType.equals("application/xml")
                    || mediaType.equals("application/soap+xml")
                    || mediaType.endsWith("+xml");
        }

        private static void removeWhitespaceTextNodes(Node node) {
            for (int index = node.getChildNodes().getLength() - 1; index >= 0; index--) {
                Node child = node.getChildNodes().item(index);
                if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().isBlank()) {
                    node.removeChild(child);
                } else {
                    removeWhitespaceTextNodes(child);
                }
            }
        }
    }
}
