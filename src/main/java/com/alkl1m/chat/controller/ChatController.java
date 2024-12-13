package com.alkl1m.chat.controller;

import com.alkl1m.chat.dto.ChatMessageDto;
import com.alkl1m.chat.entity.ChatMessage;
import com.alkl1m.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Controller
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat/sendMessage/{channelId}")
    @SendTo("/topic/channel/{channelId}")
    public Mono<ChatMessage> sendMessageToChannel(
            @Payload ChatMessageDto chatMessageDto,
            @DestinationVariable("channelId") String channelId) {

        ChatMessage message = new ChatMessage();
        message.setChannelId(channelId);
        message.setSender(chatMessageDto.getSender());
        message.setContent(chatMessageDto.getContent());
        message.setTimestamp(Instant.now());
        message.setDelivered(false);
        message.setReceived(false);

        return chatService.saveMessage(chatMessageDto);
    }
}