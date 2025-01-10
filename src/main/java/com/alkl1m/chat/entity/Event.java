package com.alkl1m.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Основная сущность для каждого события.
 * В случае, если это сообщение или подобное событие, то filename и другие
 * связанные с файлом поля пустые. Иначе - наоборот.
 *
 * @author AlKl1M
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "events")
public class Event {

    @Id
    private String id;

    private String channelId;

    private Type type;

    private String message;

    private String nickname;

    private String filename;

    private String fileData;

}
