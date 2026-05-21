package com.example.demo.client.rest;

public record ContentResponse(
        String contentId,
        String title,
        String body,
        String category,
        String servedAt) {
}
