package com.alkl1m.chat.service.impl;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.repository.EventRepository;
import com.alkl1m.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final EventRepository eventRepository;
    private final Map<String, Sinks.Many<Event>> channelSinks = new ConcurrentHashMap<>();

    @Override
    public void processEvent(Event event, String channelId) {
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

    @Override
    public Sinks.Many<Event> getChannelSink(String channelId) {
        return channelSinks.computeIfAbsent(channelId, key ->
                Sinks.many().multicast().onBackpressureBuffer()
        );
    }

    public Flux<Event> getMessagesByChannelId(String channelId) {
        return eventRepository.findByChannelId(channelId);
    }

    private void handleError(Throwable error, Event event) {
        System.err.println("Error saving event: " + error.getMessage());
    }
}
