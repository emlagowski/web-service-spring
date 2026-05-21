package com.example.demo.provider.content;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ContentCatalog {

    private final Map<String, ContentItem> items = Map.of(
            "article-1", new ContentItem(
                    "article-1",
                    "Spring Native SOAP",
                    "Demo article returned through SOAP and protected with XML Signature.",
                    "spring"),
            "article-2", new ContentItem(
                    "article-2",
                    "Signed Provider Callback",
                    "Provider can call back the client with a signed SOAP notification.",
                    "spring"),
            "article-3", new ContentItem(
                    "article-3",
                    "REST Facade",
                    "The client exposes ordinary REST while using signed SOAP underneath.",
                    "integration"));

    public ContentItem getById(String contentId) {
        ContentItem item = items.get(contentId);
        if (item == null) {
            throw new ContentNotFoundException(contentId);
        }
        return item;
    }

    public List<ContentItem> searchByCategory(String category) {
        return items.values().stream()
                .filter(item -> item.category().equalsIgnoreCase(category))
                .sorted((left, right) -> left.contentId().compareTo(right.contentId()))
                .toList();
    }
}
