package com.alkl1m.chat.service;

import com.alkl1m.chat.dto.ChatMessageDto;
import com.alkl1m.chat.dto.FileUploadDto;
import com.alkl1m.chat.entity.ChatChannel;
import com.alkl1m.chat.entity.ChatMessage;
import com.alkl1m.chat.repository.ChatChannelRepository;
import com.alkl1m.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository messageRepository;
    private final ChatChannelRepository channelRepository;
    private final ReactiveGridFsTemplate reactiveGridFsTemplate;

    public Mono<ChatMessage> saveMessage(ChatMessageDto messageDto) {
        return channelRepository.findById(messageDto.getChannelId())
                .switchIfEmpty(createChannel(messageDto.getChannelId()))
                .flatMap(channel -> {
                    ChatMessage message = new ChatMessage();
                    message.setChannelId(messageDto.getChannelId());
                    message.setSender(messageDto.getSender());
                    message.setContent(messageDto.getContent());
                    message.setTimestamp(Instant.now());
                    message.setDelivered(false);

                    return messageRepository.save(message);
                });
    }

    public Mono<ChatChannel> createChannel(String channelId) {
        ChatChannel channel = new ChatChannel();
        channel.setId(channelId);
        channel.setName(UUID.randomUUID().toString());
        return channelRepository.save(channel);
    }

    public Flux<ChatMessage> getMessagesByChannel(String channelId) {
        return messageRepository.findByChannelId(channelId);
    }

    public Mono<String> uploadFile(FileUploadDto fileUploadDto, String channelId) {
        if (fileUploadDto.getFileContentBase64() == null || fileUploadDto.getFilename() == null) {
            return Mono.error(new IllegalArgumentException("File content or filename is missing"));
        }

        byte[] decodedBytes = Base64.getDecoder().decode(fileUploadDto.getFileContentBase64());
        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(decodedBytes);
        Flux<DataBuffer> fileContentFlux = Flux.just(dataBuffer);

        return reactiveGridFsTemplate.store(
                fileContentFlux,
                fileUploadDto.getFilename(),
                fileUploadDto.getContentType()
        ).map(ObjectId::toString);
    }

    public Mono<ChatMessage> saveFileMessage(ChatMessage fileMessage) {
        return messageRepository.save(fileMessage);
    }

}