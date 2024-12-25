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
    private final Flux<String> outputEvents;
    private final ObjectMapper mapper;

    public ChatSocketHandler(EventRepository eventRepository, Flux<Event> outputEvents) {
        this.eventRepository = eventRepository;
        this.mapper = new ObjectMapper();
        this.outputEvents = Flux.from(outputEvents).map(this::toJSON);
    }

    private String toJSON(Event event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Event toEvent(String json) {
        try {
            return mapper.readValue(json, Event.class);
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON:" + json, e);
        }
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        WebSocketMessageSubscriber subscriber = new WebSocketMessageSubscriber(eventPublisher, eventRepository);

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(this::toEvent)
                .doOnNext(event -> {
                    String channelId = (String) event.getPayload().get("channelId");
                    event.setChannelId(channelId);

                    if (event.getType() == Event.Type.CHAT_MESSAGE) {
                        eventRepository.save(event)
                                .doOnError(error -> System.err.println("Error saving event: " + error.getMessage()))
                                .subscribe();
                    }
                })
                .doOnNext(subscriber::onNext)
                .doOnError(subscriber::onError)
                .doOnComplete(subscriber::onComplete)
                .zipWith(session.send(outputEvents.map(session::textMessage)))
                .then();
    }

    private static class WebSocketMessageSubscriber {
        private final Sinks.Many<Event> eventPublisher;
        private final EventRepository eventRepository;

        public WebSocketMessageSubscriber(Sinks.Many<Event> eventPublisher, EventRepository eventRepository) {
            this.eventPublisher = eventPublisher;
            this.eventRepository = eventRepository;
        }

        public void onNext(Event event) {
            eventPublisher.tryEmitNext(event).orThrow();
        }

        public void onError(Throwable error) {
            error.printStackTrace();
        }

        public void onComplete() {
            eventPublisher.tryEmitComplete();
        }
    }
}
