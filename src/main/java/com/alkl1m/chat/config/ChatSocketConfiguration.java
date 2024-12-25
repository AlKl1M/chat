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
import reactor.core.publisher.UnicastProcessor;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ChatSocketConfiguration {

    private final EventRepository eventRepository;

    @Bean
    public UnicastProcessor<Event> eventPublisher() {
        return UnicastProcessor.create();
    }

    public Flux<Event> events(UnicastProcessor<Event> eventPublisher) {
        return eventPublisher
                .replay(25)
                .autoConnect();
    }

    @Bean
    public HandlerMapping webSocketMapping(Flux<Event> events) {
        Map<String, Object> map = new HashMap<>();
        map.put("/ws", new ChatSocketHandler(eventRepository, events));
        SimpleUrlHandlerMapping simpleUrlHandlerMapping = new SimpleUrlHandlerMapping();
        simpleUrlHandlerMapping.setUrlMap(map);

        simpleUrlHandlerMapping.setOrder(10);
        return simpleUrlHandlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

}
