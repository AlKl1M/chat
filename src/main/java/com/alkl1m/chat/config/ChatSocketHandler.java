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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatSocketHandler implements WebSocketHandler {

    private final EventRepository eventRepository;
    private final Map<String, Sinks.Many<Event>> channelSinks = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public ChatSocketHandler(EventRepository eventRepository, ObjectMapper mapper) {
        this.eventRepository = eventRepository;
        this.mapper = mapper != null ? mapper : new ObjectMapper();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String channelId = extractChannelId(session);

        Sinks.Many<Event> channelSink = channelSinks.computeIfAbsent(channelId, key ->
                Sinks.many().multicast().onBackpressureBuffer()
        );

        Flux<Event> inputEvents = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(this::toEvent)
                .doOnNext(event -> processEvent(event, channelId));

        Flux<WebSocketMessage> outputMessages = channelSink.asFlux()
                .filter(event -> event.getChannelId().equals(channelId))
                .map(this::toJSON)
                .map(session::textMessage);

        return session.send(outputMessages)
                .and(inputEvents.then());
    }

    private void processEvent(Event event, String channelId) {
        event.setChannelId(channelId);

        if (event.getType() == Event.Type.CHAT_MESSAGE
                || event.getType() == Event.Type.USER_JOINED
                || event.getType() == Event.Type.USER_LEFT) {
            eventRepository.save(event)
                    .doOnError(error -> handleError(error, event))
                    .subscribe();
        }

        Sinks.Many<Event> channelSink = channelSinks.get(channelId);
        if (channelSink != null) {
            channelSink.tryEmitNext(event).orThrow();
        }
    }

    private String extractChannelId(WebSocketSession session) {
        return session.getHandshakeInfo().getUri().getQuery()
                .replaceAll(".*channelId=([^&]+).*", "$1");
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