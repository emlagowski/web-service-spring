package com.example.demo.client.rest;

public record ContentSummaryResponse(
        String contentId,
        String title,
        String category) {
}
