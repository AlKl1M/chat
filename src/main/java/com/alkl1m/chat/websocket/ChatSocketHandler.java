package com.alkl1m.chat.websocket;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.service.ChatService;
import com.alkl1m.chat.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@RequiredArgsConstructor
public class ChatSocketHandler implements WebSocketHandler {

    private final ChatService chatService;
    private final JsonUtils jsonUtils;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String channelId = extractChannelId(session);

        Sinks.Many<Event> channelSink = chatService.getChannelSink(channelId);

        Flux<Event> inputEvents = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(json -> jsonUtils.toObject(json, Event.class))
                .doOnNext(event -> chatService.processEvent(event, channelId));

        Flux<WebSocketMessage> outputMessages = channelSink.asFlux()
                .filter(event -> event.getChannelId().equals(channelId))
                .map(jsonUtils::toJSON)
                .map(session::textMessage);

        return session.send(outputMessages)
                .and(inputEvents.then());
    }

    private String extractChannelId(WebSocketSession session) {
        return session.getHandshakeInfo().getUri().getQuery()
                .replaceAll(".*channelId=([^&]+).*", "$1");
    }

}