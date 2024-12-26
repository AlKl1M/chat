package com.alkl1m.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "events")
public class Event {

    public enum Type {
        CHAT_MESSAGE, USER_JOINED, USER_STATS, USER_LEFT
    }

    @Id
    private String id;

    private String channelId;

    private Type type;

    private String message;

    private String nickname;
}