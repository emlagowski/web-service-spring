package com.example.demo.client.callback;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CallbackEventStore {

    private final List<CallbackEvent> events = new ArrayList<>();

    public synchronized void append(CallbackEvent event) {
        events.add(event);
    }

    public synchronized List<CallbackEvent> findAll() {
        return List.copyOf(events);
    }

    public synchronized void clear() {
        events.clear();
    }
}
