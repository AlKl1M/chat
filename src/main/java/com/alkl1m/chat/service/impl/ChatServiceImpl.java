package com.alkl1m.chat.service.impl;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.repository.EventRepository;
import com.alkl1m.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final EventRepository eventRepository;
    private final ReactiveGridFsTemplate gridFsTemplate;
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

    public void handleFileMessage(Event event) {
        String base64FileData = event.getFileData();
        byte[] fileBytes = Base64.getDecoder().decode(base64FileData);

        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileBytes);
        Flux<DataBuffer> fileContentFlux = Flux.just(dataBuffer);

        eventRepository.save(event)
                .doOnError(error -> handleError(error, event))
                .subscribe();

        gridFsTemplate.store(
                        fileContentFlux,
                        event.getFilename()
                ).map(ObjectId::toString)
                .doOnSuccess(fileId -> {
                    System.out.println("Stored file with ID: " + fileId);
                })
                .doOnError(error -> {
                    System.err.println("Error storing file: " + error.getMessage());
                })
                .subscribe();

        Sinks.Many<Event> channelSink = channelSinks.get(event.getChannelId());
        if (channelSink != null) {
            channelSink.tryEmitNext(event).orThrow();
        }
    }

    private void handleError(Throwable error, Event event) {
        System.err.println("Error saving event: " + error.getMessage());
    }
}
