package com.alkl1m.chat.config;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.repository.EventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;

public class ChatSocketHandler implements WebSocketHandler {

    private final EventRepository eventRepository;
    private final Sinks.Many<Event> eventPublisher = Sinks.many().multicast().onBackpressureBuffer();
    private final ObjectMapper mapper;

    public ChatSocketHandler(EventRepository eventRepository, ObjectMapper mapper) {
        this.eventRepository = eventRepository;
        this.mapper = mapper != null ? mapper : new ObjectMapper();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<Event> inputEvents = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(this::toEvent)
                .doOnNext(this::processEvent);

        Flux<WebSocketMessage> outputMessages = eventPublisher.asFlux()
                .map(this::toJSON)
                .map(session::textMessage);

        return session.send(outputMessages)
                .and(inputEvents.then());
    }

    private void processEvent(Event event) {
        if (event.getType() == Event.Type.CHAT_MESSAGE) {
            eventRepository.save(event)
                    .doOnError(error -> handleError(error, event))
                    .subscribe();
        }

        eventPublisher.tryEmitNext(event).orThrow();
    }

    private void handleError(Throwable error, Event event) {
        System.err.println("Error saving event: " + error.getMessage());
    }

    private String toJSON(Event event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing event to JSON", e);
        }
    }

    private Event toEvent(String json) {
        try {
            return mapper.readValue(json, Event.class);
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON format: " + json, e);
        }
    }
}