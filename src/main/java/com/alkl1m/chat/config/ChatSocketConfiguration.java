package com.alkl1m.chat.config;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.service.ChatService;
import com.alkl1m.chat.util.JsonUtils;
import com.alkl1m.chat.websocket.ChatSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ChatService chatService;
    private final JsonUtils jsonUtils;

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
        Map<String, Object> urlMap = Map.of("/ws", new ChatSocketHandler(chatService, jsonUtils));

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