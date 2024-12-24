package com.alkl1m.chat.controller;

import com.alkl1m.chat.entity.ChatMessage;
import com.alkl1m.chat.service.ChatService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;
    private final ReactiveGridFsTemplate reactiveGridFsTemplate;

    public ChatRestController(ChatService chatService, ReactiveGridFsTemplate reactiveGridFsTemplate) {
        this.chatService = chatService;
        this.reactiveGridFsTemplate = reactiveGridFsTemplate;
    }

    @GetMapping("/channels/{channelId}/messages")
    public Flux<ChatMessage> getMessagesByChannel(@PathVariable String channelId) {
        return chatService.getMessagesByChannel(channelId);
    }

    @GetMapping("/files/{fileId}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(@PathVariable String fileId) {
        return this.reactiveGridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)))
                .flatMap(this.reactiveGridFsTemplate::getResource)
                .map(resource -> {
                    String contentType = getContentType(resource.getFilename());

                    // Stream the file content as Flux<DataBuffer>
                    Flux<DataBuffer> fileStream = resource.getDownloadStream();

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                            .contentType(MediaType.parseMediaType(contentType))
                            .body(fileStream); // Streaming the file as a Flux<DataBuffer>
                });
    }

    // Helper method to determine the content type based on file extension
    private String getContentType(String filename) {
        if (filename.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (filename.endsWith(".jpeg") || filename.endsWith(".jpg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE; // Default for unknown file types
        }
    }

}