package com.example.demo.client.rest;

import com.example.demo.client.callback.CallbackEvent;
import com.example.demo.client.callback.CallbackEventStore;
import com.example.demo.client.soap.ProviderContentClient;
import com.example.demo.contract.content.GetContentResponse;
import com.example.demo.contract.content.SearchContentResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class ContentRestController {

    private final ProviderContentClient providerContentClient;
    private final CallbackEventStore callbackEventStore;

    public ContentRestController(ProviderContentClient providerContentClient, CallbackEventStore callbackEventStore) {
        this.providerContentClient = providerContentClient;
        this.callbackEventStore = callbackEventStore;
    }

    @GetMapping("/content/{id}")
    public ContentResponse getContent(@PathVariable String id, HttpServletRequest request) {
        GetContentResponse response = providerContentClient.getContent(id, callbackUrl());
        return new ContentResponse(
                response.getContentId(),
                response.getTitle(),
                response.getBody(),
                response.getCategory(),
                response.getServedAt());
    }

    @GetMapping(value = "/content", params = "category")
    public List<ContentSummaryResponse> searchContent(@RequestParam String category) {
        SearchContentResponse response = providerContentClient.searchContent(category);
        return response.getContent().stream()
                .map(content -> new ContentSummaryResponse(
                        content.getContentId(),
                        content.getTitle(),
                        content.getCategory()))
                .toList();
    }

    @GetMapping("/callback-events")
    public List<CallbackEvent> callbackEvents() {
        return callbackEventStore.findAll();
    }

    private static String callbackUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .replacePath("/ws")
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}
