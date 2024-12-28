package com.alkl1m.chat.controller;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventRepository eventRepository;
    private final ReactiveGridFsTemplate gridFsTemplate;

    @GetMapping("/api/events/{sessionId}")
    public Flux<Event> getEventHistory(@PathVariable String sessionId) {
        return eventRepository.findByChannelId(sessionId);
    }

    @GetMapping("/api/events/download/{fileId}")
    public Mono<Void> downloadFile(@PathVariable String fileId, ServerWebExchange exchange) {
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

}