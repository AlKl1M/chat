package com.alkl1m.chat.controller;

import com.alkl1m.chat.dto.ChatMessageDto;
import com.alkl1m.chat.entity.ChatMessage;
import com.alkl1m.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat/channels/{channelId}/messages")
    @SendTo("/topic/messages/{channelId}")
    public Mono<ChatMessage> sendMessageToChannel(
            @Payload ChatMessageDto chatMessageDto,
            @DestinationVariable("channelId") String channelId) {

        return chatService.saveMessage(chatMessageDto);
    }

}