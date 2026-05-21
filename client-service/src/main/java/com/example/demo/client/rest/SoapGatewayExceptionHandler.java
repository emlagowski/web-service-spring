package com.example.demo.client.rest;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.oxm.XmlMappingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.ws.client.WebServiceClientException;

@RestControllerAdvice
public class SoapGatewayExceptionHandler {

    @ExceptionHandler({WebServiceClientException.class, XmlMappingException.class})
    ResponseEntity<Map<String, String>> handleSoapFailure(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "SOAP_GATEWAY_FAILURE",
                        "message", exception.getMessage()));
    }
}
