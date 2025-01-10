package com.alkl1m.chat.controller;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.entity.Type;
import com.alkl1m.chat.repository.EventRepository;
import com.alkl1m.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@AutoConfigureWebTestClient
@DisplayName("Тестовые сценарии работы EventController")
class EventControllerTest {

    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    static {
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "testDB");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ReactiveGridFsTemplate gridFsTemplate;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll().subscribe();
        gridFsTemplate.delete(Query.query(Criteria.where("_id").exists(true))).subscribe();
    }

    @Test
    @DisplayName("Получение истории событий: проверка возвращаемых данных для сохраненного события")
    void getEventHistory_withValidSavedData_ReturnsCorrectData() {
        Event event1 = createEvent("channel1", Type.CHAT_MESSAGE, "Message 1", "user1", "file1.txt", "filedata1");
        eventRepository.save(event1).block();

        webTestClient.get().uri("/api/events/channel1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Event.class)
                .value(events -> {
                    assertThat(events).hasSize(1);
                    assertThat(events.get(0).getMessage()).isEqualTo("Message 1");
                });
    }

    @Test
    @DisplayName("Получение истории событий: проверка возврата всех событий для одного канала")
    void getEventHistory_WithMultipleEvents_ReturnsAllEvents() {
        Event event1 = createEvent("channel1", Type.CHAT_MESSAGE, "Message 1", "user1", "file1.txt", "filedata1");
        Event event2 = createEvent("channel1", Type.FILE_MESSAGE, "File Message 1", "user2", "file2.txt", "filedata2");

        eventRepository.saveAll(List.of(event1, event2)).blockLast();

        webTestClient.get().uri("/api/events/channel1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Event.class)
                .value(events -> {
                    assertThat(events).hasSize(2);
                    assertThat(events.get(1).getMessage()).isEqualTo("File Message 1");
                });
    }

    @Test
    @DisplayName("Получение истории событий: проверка возврата пустого списка для несуществующего канала")
    void getEventHistory_WithNonExistentChannel_ReturnsEmptyList() {
        webTestClient.get().uri("/api/events/nonExistentChannel")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Event.class)
                .value(events -> assertThat(events).isEmpty());
    }

    @Test
    @DisplayName("Получение истории событий: проверка типов событий при добавлении и выходе пользователя")
    void getEventHistory_WithUserJoinedAndLeftEvents_ReturnsCorrectEventTypes() {
        Event userJoinedEvent = createEvent("channel1", Type.USER_JOINED, "User joined", "user1", null, null);
        Event userLeftEvent = createEvent("channel1", Type.USER_LEFT, "User left", "user1", null, null);

        eventRepository.saveAll(List.of(userJoinedEvent, userLeftEvent)).blockLast();

        webTestClient.get().uri("/api/events/channel1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Event.class)
                .value(events -> {
                    assertThat(events).hasSize(2);
                    assertThat(events.get(0).getType()).isEqualTo(Type.USER_JOINED);
                    assertThat(events.get(1).getType()).isEqualTo(Type.USER_LEFT);
                });
    }

    @Test
    @DisplayName("Получение истории событий: проверка данных файла для события с файлом")
    void getEventHistory_WithFileMessage_ReturnsCorrectFileData() {
        Event fileMessageEvent = createEvent("channel1", Type.FILE_MESSAGE, "File Message 1", "user1", "file1.pdf", "filedata1");
        eventRepository.save(fileMessageEvent).block();

        webTestClient.get().uri("/api/events/channel1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Event.class)
                .value(events -> {
                    assertThat(events).hasSize(1);
                    assertThat(events.get(0).getFilename()).isEqualTo("file1.pdf");
                    assertThat(events.get(0).getFileData()).isEqualTo("filedata1");
                });
    }

    @Test
    @DisplayName("Загрузка файла: проверка успешного ответа при пустых данных файла")
    void testDownloadFile_withEmptyData_ReturnsOkForValidFile() {
        Event fileMessageEvent = createEvent("channel1", Type.FILE_MESSAGE, "File Message 1", "user1", "file1.pdf", "filedata1");
        eventRepository.save(fileMessageEvent).block();

        webTestClient.get().uri("/api/events/download/file1.pdf")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .isEmpty();
    }

    @Test
    @DisplayName("Загрузка файла: проверка успешной загрузки файла с base64-кодировкой")
    void testDownloadFile_AfterSaveNewBase64EncodedFile_returnsOkStatus() {
        String base64File = Base64.getEncoder().encodeToString("Test file content".getBytes());

        Event event = createEvent("testChannel", Type.FILE_MESSAGE, "This is a file message.", "user1", "testFile.txt", base64File);

        chatService.handleFileMessage(event);

        String fileId = event.getId();

        webTestClient.get()
                .uri("/api/events/download/" + fileId)
                .exchange()
                .expectStatus().isOk();
    }

    private Event createEvent(String channelId, Type type, String message, String nickname, String filename, String fileData) {
        return Event.builder()
                .channelId(channelId)
                .type(type)
                .message(message)
                .nickname(nickname)
                .filename(filename)
                .fileData(fileData)
                .build();
    }

}
