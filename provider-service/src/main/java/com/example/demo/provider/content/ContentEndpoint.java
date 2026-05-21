package com.example.demo.provider.content;

import com.example.demo.contract.content.ContentSummary;
import com.example.demo.contract.content.GetContentRequest;
import com.example.demo.contract.content.GetContentResponse;
import com.example.demo.contract.content.SearchContentRequest;
import com.example.demo.contract.content.SearchContentResponse;
import com.example.demo.provider.callback.CallbackClient;
import java.time.OffsetDateTime;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class ContentEndpoint {

    private static final String NAMESPACE = "urn:demo:provider:content:v1";

    private final ContentCatalog contentCatalog;
    private final CallbackClient callbackClient;

    public ContentEndpoint(ContentCatalog contentCatalog, CallbackClient callbackClient) {
        this.contentCatalog = contentCatalog;
        this.callbackClient = callbackClient;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "GetContentRequest")
    @ResponsePayload
    public GetContentResponse getContent(@RequestPayload GetContentRequest request) {
        OffsetDateTime servedAt = OffsetDateTime.now();
        ContentItem item = contentCatalog.getById(request.getContentId());
        callbackClient.notifyDelivered(request.getCallbackUrl(), item, servedAt);

        GetContentResponse response = new GetContentResponse();
        response.setContentId(item.contentId());
        response.setTitle(item.title());
        response.setBody(item.body());
        response.setCategory(item.category());
        response.setServedAt(servedAt.toString());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "SearchContentRequest")
    @ResponsePayload
    public SearchContentResponse searchContent(@RequestPayload SearchContentRequest request) {
        SearchContentResponse response = new SearchContentResponse();
        for (ContentItem item : contentCatalog.searchByCategory(request.getCategory())) {
            ContentSummary summary = new ContentSummary();
            summary.setContentId(item.contentId());
            summary.setTitle(item.title());
            summary.setCategory(item.category());
            response.getContent().add(summary);
        }
        return response;
    }
}
