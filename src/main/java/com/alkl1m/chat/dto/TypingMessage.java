package com.alkl1m.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TypingMessage {
    private String sender;
    private String channelId;
}
