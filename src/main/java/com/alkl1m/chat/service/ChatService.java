package com.alkl1m.chat.service;

import com.alkl1m.chat.entity.Event;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * @author AlKl1M
 */
public interface ChatService {

    void processEvent(Event event, String channelId);

    Sinks.Many<Event> getChannelSink(String channelId);

    Flux<Event> getMessagesByChannelId(String channelId);

    void handleFileMessage(Event event);

    Mono<Void> downloadFileById(String fileId, ServerWebExchange exchange);

}
