package com.example.demo.client.soap;

import com.example.demo.contract.content.GetContentRequest;
import com.example.demo.contract.content.GetContentResponse;
import com.example.demo.contract.content.SearchContentRequest;
import com.example.demo.contract.content.SearchContentResponse;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

@Component
public class ProviderContentClient {

    private final WebServiceTemplate providerWebServiceTemplate;

    public ProviderContentClient(WebServiceTemplate providerWebServiceTemplate) {
        this.providerWebServiceTemplate = providerWebServiceTemplate;
    }

    public GetContentResponse getContent(String contentId, String callbackUrl) {
        GetContentRequest request = new GetContentRequest();
        request.setContentId(contentId);
        request.setCallbackUrl(callbackUrl);
        return (GetContentResponse) providerWebServiceTemplate.marshalSendAndReceive(request);
    }

    public SearchContentResponse searchContent(String category) {
        SearchContentRequest request = new SearchContentRequest();
        request.setCategory(category);
        return (SearchContentResponse) providerWebServiceTemplate.marshalSendAndReceive(request);
    }
}
