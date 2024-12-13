package com.alkl1m.chat.controller;

import com.alkl1m.chat.entity.ChatChannel;
import com.alkl1m.chat.entity.ChatMessage;
import com.alkl1m.chat.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/channels")
    public Mono<ChatChannel> createChannel(@RequestBody String name) {
        return chatService.createChannel(name);
    }

    @GetMapping("/channels/{channelId}/messages")
    public Flux<ChatMessage> getMessagesByChannel(@PathVariable String channelId) {
        return chatService.getMessagesByChannel(channelId);
    }
}