package com.alkl1m.chat.config;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ChatSocketConfiguration {

    private final EventRepository eventRepository;

    @Bean
    public Sinks.Many<Event> eventPublisher() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Flux<Event> events(Sinks.Many<Event> eventPublisher) {
        return eventPublisher.asFlux()
                .replay(25)
                .autoConnect();
    }

    @Bean
    public HandlerMapping webSocketMapping(Flux<Event> events) {
        Map<String, Object> urlMap = Map.of("/ws", new ChatSocketHandler(eventRepository, null));

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setUrlMap(urlMap);
        handlerMapping.setOrder(10);

        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}