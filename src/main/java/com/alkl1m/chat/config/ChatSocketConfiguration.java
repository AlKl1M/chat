package com.alkl1m.chat.config;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.service.ChatService;
import com.alkl1m.chat.util.JsonUtils;
import com.alkl1m.chat.websocket.ChatSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;

/**
 * Класс конфигурации сокетов для чата.
 *
 * @author AlKl1M
 */
@Configuration
@RequiredArgsConstructor
public class ChatSocketConfiguration {

    private final ChatService chatService;
    private final JsonUtils jsonUtils;

    /**
     * Создает и возвращает экземпляр Sinks.Many для публикации событий.
     * Используется для многократной рассылки событий с поддержкой буферизации при избыточной нагрузке.
     *
     * @return экземпляр Sinks.Many, предназначенный для публикации событий.
     */
    @Bean
    public Sinks.Many<Event> eventPublisher() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    /**
     * Создает и возвращает поток событий (Flux), который будет воспроизводить последние 25 событий.
     * Этот поток автоматически подключается и поддерживает многократное подключение.
     *
     * @param eventPublisher экземпляр Sinks.Many для публикации событий.
     * @return поток событий, которые можно подписать.
     */
    @Bean
    public Flux<Event> events(Sinks.Many<Event> eventPublisher) {
        return eventPublisher.asFlux()
                .replay(25)
                .autoConnect();
    }

    /**
     * Настроит отображение URL для WebSocket с использованием обработчика событий чата.
     * URL "/ws" будет привязан к обработчику WebSocket.
     *
     * @param events поток событий для обработки через WebSocket.
     * @return объект SimpleUrlHandlerMapping с привязкой URL.
     */
    @Bean
    public HandlerMapping webSocketMapping(Flux<Event> events) {
        Map<String, Object> urlMap = Map.of("/ws", new ChatSocketHandler(chatService, jsonUtils));

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setUrlMap(urlMap);
        handlerMapping.setOrder(10);

        return handlerMapping;
    }

    /**
     * Создает и возвращает адаптер для WebSocket обработчика.
     * Этот адаптер необходим для корректной работы с WebSocket в Spring.
     *
     * @return объект WebSocketHandlerAdapter.
     */
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

}
