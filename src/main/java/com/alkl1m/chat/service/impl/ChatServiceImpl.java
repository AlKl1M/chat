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
                    .doOnError(error -> log.error("Error saving event: {}", error.getMessage()))
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
        byte[] fileBytes = decodeBase64FileData(event.getFileData());
        Flux<DataBuffer> fileContentFlux = wrapFileBytesToDataBuffer(fileBytes);

        storeFileInGridFs(fileContentFlux, event.getFilename())
                .doOnSuccess(fileId -> {
                    log.info("Stored file with ID: {}", fileId);
                    updateEventWithFileLink(event, fileId);
                })
                .flatMap(fileId -> saveEvent(event))
                .doOnTerminate(() -> sendEventToChannel(event))
                .doOnError(error -> log.error("Error storing file: {}", error.getMessage()))
                .subscribe();
    }

    /**
     * Декодирует строку Base64 в массив байтов.
     *
     * @param base64FileData строка, содержащая данные файла в формате Base64.
     * @return массив байтов, полученный после декодирования.
     */
    private byte[] decodeBase64FileData(String base64FileData) {
        return Base64.getDecoder().decode(base64FileData);
    }

    /**
     * Преобразует массив байтов в DataBuffer для дальнейшей обработки.
     *
     * @param fileBytes массив байтов, представляющий содержимое файла.
     * @return DataBuffer, содержащий данные файла.
     */
    private Flux<DataBuffer> wrapFileBytesToDataBuffer(byte[] fileBytes) {
        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileBytes);
        return Flux.just(dataBuffer);
    }

    /**
     * Сохраняет файл в GridFS и возвращает идентификатор файла.
     *
     * @param fileContentFlux Flux, содержащий данные файла.
     * @param filename        имя файла для хранения.
     * @return Mono, которое завершится идентификатором сохранённого файла.
     */
    private Mono<String> storeFileInGridFs(Flux<DataBuffer> fileContentFlux, String filename) {
        return gridFsTemplate.store(fileContentFlux, filename)
                .map(ObjectId::toString);
    }

    /**
     * Обновляет событие, добавляя ссылку на загруженный файл.
     *
     * @param event  событие, которое нужно обновить.
     * @param fileId идентификатор файла в GridFS.
     */
    private void updateEventWithFileLink(Event event, String fileId) {
        event.setFileData(null);
        event.setMessage("/api/events/download/" + fileId);
        event.setId(fileId);
    }

    /**
     * Сохраняет событие в репозитории.
     *
     * @param event событие, которое нужно сохранить.
     * @return Mono, которое завершится сохранением события.
     */
    private Mono<Event> saveEvent(Event event) {
        return eventRepository.save(event)
                .doOnError(error -> log.error("Error updating event with fileId: {}", error.getMessage()));
    }

    /**
     * Отправляет обновлённое событие в канал.
     *
     * @param event событие, которое нужно отправить.
     */
    private void sendEventToChannel(Event event) {
        Sinks.Many<Event> channelSink = channelSinks.get(event.getChannelId());
        if (channelSink != null) {
            channelSink.tryEmitNext(event).orThrow();
        }
    }

}
