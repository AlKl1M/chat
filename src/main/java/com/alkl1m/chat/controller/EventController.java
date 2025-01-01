package com.alkl1m.chat.controller;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST API для обработки событий в чате. Содержит методы, которые
 * актуализируют состояние чата или выполняют операции, не требующие соединения в вебсокете.
 *
 * @author AlKl1M
 */
@RestController
@RequiredArgsConstructor
public class EventController {

    private final ChatService chatService;

    /**
     * Получает историю событий для указанной сессии.
     * Возвращает список сообщений, связанных с указанным идентификатором канала (сессии).
     *
     * @param sessionId идентификатор сессии для получения истории событий.
     * @return поток событий (Flux) для указанного sessionId.
     */
    @GetMapping("/api/events/{sessionId}")
    public Flux<Event> getEventHistory(@PathVariable String sessionId) {
        return chatService.getMessagesByChannelId(sessionId);
    }

    /**
     * Обрабатывает запрос на скачивание файла по указанному идентификатору.
     * Возвращает Mono<Void>, что означает завершение операции скачивания.
     *
     * @param fileId   идентификатор файла для скачивания.
     * @param exchange обмен сервером для обработки запроса на скачивание.
     * @return Mono<Void>, завершение операции скачивания.
     */
    @GetMapping("/api/events/download/{fileId}")
    public Mono<Void> downloadFile(@PathVariable String fileId, ServerWebExchange exchange) {
        return chatService.downloadFileById(fileId, exchange);
    }

}
