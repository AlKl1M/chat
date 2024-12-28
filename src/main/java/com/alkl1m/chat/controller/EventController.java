package com.alkl1m.chat.controller;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventRepository eventRepository;

    @GetMapping("/api/events/{sessionId}")
    public Flux<Event> getEventHistory(@PathVariable String sessionId) {
        return eventRepository.findByChannelId(sessionId);
    }

}