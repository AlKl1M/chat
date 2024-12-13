package com.alkl1m.chat.dto;

import lombok.Data;

@Data
public class ChatMessageDto {
    private String channelId;
    private String sender;
    private String content;
}