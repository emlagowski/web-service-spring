package com.example.demo.client.callback;

import com.example.demo.contract.callback.NotifyContentDeliveredRequest;
import com.example.demo.contract.callback.NotifyContentDeliveredResponse;
import java.time.OffsetDateTime;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class CallbackEndpoint {

    private static final String NAMESPACE = "urn:demo:client:callback:v1";

    private final CallbackEventStore callbackEventStore;

    public CallbackEndpoint(CallbackEventStore callbackEventStore) {
        this.callbackEventStore = callbackEventStore;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "NotifyContentDeliveredRequest")
    @ResponsePayload
    public NotifyContentDeliveredResponse notifyContentDelivered(
            @RequestPayload NotifyContentDeliveredRequest request) {
        OffsetDateTime receivedAt = OffsetDateTime.now();
        callbackEventStore.append(new CallbackEvent(
                request.getEventId(),
                request.getContentId(),
                request.getTitle(),
                request.getDeliveredAt(),
                receivedAt.toString()));

        NotifyContentDeliveredResponse response = new NotifyContentDeliveredResponse();
        response.setAccepted(true);
        response.setReceivedAt(receivedAt.toString());
        return response;
    }
}
