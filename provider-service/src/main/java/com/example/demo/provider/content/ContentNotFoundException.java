package com.example.demo.provider.content;

public class ContentNotFoundException extends RuntimeException {

    public ContentNotFoundException(String contentId) {
        super("Content not found: " + contentId);
    }
}
