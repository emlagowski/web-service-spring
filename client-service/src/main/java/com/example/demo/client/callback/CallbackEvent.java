package com.example.demo.client.callback;

public record CallbackEvent(String eventId, String contentId, String title, String deliveredAt, String receivedAt) {
}
