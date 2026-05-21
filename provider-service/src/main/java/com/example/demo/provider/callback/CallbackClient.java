package com.example.demo.provider.callback;

import com.example.demo.contract.callback.NotifyContentDeliveredRequest;
import com.example.demo.contract.callback.NotifyContentDeliveredResponse;
import com.example.demo.provider.content.ContentItem;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.ws.client.core.WebServiceTemplate;

@Component
public class CallbackClient {

    private final WebServiceTemplate callbackWebServiceTemplate;

    public CallbackClient(WebServiceTemplate callbackWebServiceTemplate) {
        this.callbackWebServiceTemplate = callbackWebServiceTemplate;
    }

    public void notifyDelivered(String callbackUrl, ContentItem contentItem, OffsetDateTime deliveredAt) {
        if (!StringUtils.hasText(callbackUrl)) {
            return;
        }

        NotifyContentDeliveredRequest request = new NotifyContentDeliveredRequest();
        request.setEventId(UUID.randomUUID().toString());
        request.setContentId(contentItem.contentId());
        request.setTitle(contentItem.title());
        request.setDeliveredAt(deliveredAt.toString());

        Object response = callbackWebServiceTemplate.marshalSendAndReceive(callbackUrl, request);
        if (!(response instanceof NotifyContentDeliveredResponse callbackResponse) || !callbackResponse.isAccepted()) {
            throw new IllegalStateException("Callback was not accepted by client: " + callbackUrl);
        }
    }
}
