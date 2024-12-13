package com.alkl1m.chat.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document
public class ChatMessage {
    @Id
    private String id;
    private String channelId;
    private String sender;
    private String content;
    private Instant timestamp;
    private boolean delivered;
    private boolean received;
}
