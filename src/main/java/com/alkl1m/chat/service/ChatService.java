package com.alkl1m.chat.service;

import com.alkl1m.chat.dto.ChatMessageDto;
import com.alkl1m.chat.entity.ChatChannel;
import com.alkl1m.chat.entity.ChatMessage;
import com.alkl1m.chat.repository.ChatChannelRepository;
import com.alkl1m.chat.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class ChatService {
    private final ChatMessageRepository messageRepository;
    private final ChatChannelRepository channelRepository;

    public ChatService(ChatMessageRepository messageRepository, ChatChannelRepository channelRepository) {
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
    }

    public Mono<ChatMessage> saveMessage(ChatMessageDto messageDto) {
        ChatMessage message = new ChatMessage();
        message.setChannelId(messageDto.getChannelId());
        message.setSender(messageDto.getSender());
        message.setContent(messageDto.getContent());
        message.setTimestamp(Instant.now());
        message.setDelivered(false);
        message.setReceived(false);

        return messageRepository.save(message);
    }

    public Mono<ChatChannel> createChannel(String name) {
        ChatChannel channel = new ChatChannel();
        channel.setName(name);
        return channelRepository.save(channel);
    }

    public Flux<ChatMessage> getMessagesByChannel(String channelId) {
        return messageRepository.findByChannelId(channelId);
    }

}