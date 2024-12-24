package com.alkl1m.chat.controller;

import com.alkl1m.chat.dto.ChatMessageDto;
import com.alkl1m.chat.dto.FileUploadDto;
import com.alkl1m.chat.dto.TypingMessage;
import com.alkl1m.chat.entity.ChatMessage;
import com.alkl1m.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.time.Instant;

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

    @MessageMapping("/chat/channels/{channelId}/typing")
    @SendTo("/topic/typing/{channelId}")
    public Mono<TypingMessage> typing(
            @Payload TypingMessage typingMessage,
            @DestinationVariable("channelId") String channelId) {

        return Mono.just(typingMessage);
    }

    @MessageMapping("/chat/channels/{channelId}/files")
    @SendTo("/topic/messages/{channelId}")
    public Mono<ChatMessage> uploadFile(
            @Payload FileUploadDto fileUploadDto,
            @DestinationVariable("channelId") String channelId) {

        return chatService.uploadFile(fileUploadDto, channelId)
                .flatMap(fileId -> {
                    String downloadUrl = "/api/chat/files/" + fileId;
                    ChatMessage fileMessage = new ChatMessage();
                    fileMessage.setChannelId(channelId);
                    fileMessage.setContent("File uploaded");
                    fileMessage.setFileUrl(downloadUrl);
                    fileMessage.setTimestamp(Instant.now());
                    return chatService.saveFileMessage(fileMessage);
                });
    }

}