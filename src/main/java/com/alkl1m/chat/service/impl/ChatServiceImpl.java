package com.alkl1m.chat.service.impl;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.entity.Type;
import com.alkl1m.chat.repository.EventRepository;
import com.alkl1m.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final EventRepository eventRepository;
    private final ReactiveGridFsTemplate gridFsTemplate;
    private final Map<String, Sinks.Many<Event>> channelSinks = new ConcurrentHashMap<>();

    /**
     * Обрабатывает событие и сохраняет его в базе данных, если тип события - сообщение чата,
     * пользователь присоединился или покинул. Затем отправляет событие в соответствующий канал.
     *
     * @param event     событие для обработки.
     * @param channelId идентификатор канала, с которым связано событие.
     */
    @Override
    public void processEvent(Event event, String channelId) {
        event.setChannelId(channelId);

        if (event.getType() == Type.CHAT_MESSAGE
                || event.getType() == Type.USER_JOINED
                || event.getType() == Type.USER_LEFT) {
            eventRepository.save(event)
                    .doOnError(error -> handleError(error, event))
                    .subscribe();
        }

        Sinks.Many<Event> channelSink = channelSinks.get(channelId);
        if (channelSink != null) {
            channelSink.tryEmitNext(event).orThrow();
        }
    }

    /**
     * Возвращает Sink для указанного канала. Если канал не существует, создается новый Sink.
     *
     * @param channelId идентификатор канала.
     * @return Sink для указанного канала.
     */
    @Override
    public Sinks.Many<Event> getChannelSink(String channelId) {
        return channelSinks.computeIfAbsent(channelId, key ->
                Sinks.many().multicast().onBackpressureBuffer()
        );
    }

    /**
     * Получает сообщения для указанного канала.
     *
     * @param channelId идентификатор канала.
     * @return поток сообщений (Flux) для указанного канала.
     */
    @Override
    public Flux<Event> getMessagesByChannelId(String channelId) {
        return eventRepository.findByChannelId(channelId);
    }

    /**
     * Обрабатывает запрос на скачивание файла по идентификатору.
     *
     * @param fileId   идентификатор файла для скачивания.
     * @param exchange объект для обработки запроса на сервере.
     * @return Mono, представляющее завершение операции скачивания.
     */
    @Override
    public Mono<Void> downloadFileById(String fileId, ServerWebExchange exchange) {
        return gridFsTemplate.findOne(query(where("_id").is(fileId)))
                .flatMap(gridFsTemplate::getResource)
                .flatMap(resource -> {
                    exchange.getResponse().getHeaders().setContentDisposition(
                            ContentDisposition.builder("attachment")
                                    .filename(resource.getFilename())
                                    .build()
                    );
                    exchange.getResponse().getHeaders().setContentType(
                            MediaType.APPLICATION_OCTET_STREAM
                    );
                    return exchange.getResponse().writeWith(resource.getDownloadStream());
                });
    }

    /**
     * Обрабатывает сообщение с файлом, сохраняет его в GridFS и обновляет событие с ссылкой на файл.
     *
     * @param event событие, содержащее файл для обработки.
     */
    @Override
    public void handleFileMessage(Event event) {
        String base64FileData = event.getFileData();
        byte[] fileBytes = Base64.getDecoder().decode(base64FileData);

        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileBytes);
        Flux<DataBuffer> fileContentFlux = Flux.just(dataBuffer);

        gridFsTemplate.store(fileContentFlux, event.getFilename())
                .map(ObjectId::toString)
                .doOnSuccess(fileId -> {
                    log.info("Stored file with ID: {}", fileId);
                    event.setFileData(null);
                    event.setMessage("/api/events/download/" + fileId);
                    event.setId(fileId);
                })
                .flatMap(fileId -> eventRepository.save(event)
                        .doOnError(error -> log.error("Error updating event with fileId: {}", error.getMessage())))
                .doOnTerminate(() -> {
                    Sinks.Many<Event> channelSink = channelSinks.get(event.getChannelId());
                    if (channelSink != null) {
                        channelSink.tryEmitNext(event).orThrow();
                    }
                })
                .doOnError(error -> log.error("Error storing file: {}", error.getMessage()))
                .subscribe();
    }

    /**
     * Обрабатывает ошибку при сохранении события и логирует ее.
     *
     * @param error ошибка, произошедшая при сохранении события.
     * @param event событие, с которым произошла ошибка.
     */
    private void handleError(Throwable error, Event event) {
        log.error("Error saving event: {}", error.getMessage());
    }
}