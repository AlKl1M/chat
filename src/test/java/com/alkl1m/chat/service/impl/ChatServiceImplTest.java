package com.alkl1m.chat.service.impl;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.entity.Type;
import com.alkl1m.chat.repository.EventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Тестовые сценарии работы ChatServiceImpl")
class ChatServiceImplTest {

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
    private ChatServiceImpl chatService;

    @Autowired
    private EventRepository eventRepository;

    @AfterEach
    void cleanUp() {
        eventRepository.deleteAll().subscribe();
    }

    @Test
    @DisplayName("Обработка события: сохранение сообщения чата в базу данных")
    void testProcessEvent_chatMessage_eventSavedToDatabase() {
        Event event = new Event();
        event.setType(Type.CHAT_MESSAGE);
        event.setMessage("Test message");
        String channelId = "channel1";

        chatService.processEvent(event, channelId);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Mono<Event> savedEvent = eventRepository.findById(event.getId());
                    assertNotNull(savedEvent.block());
                });
    }

    @Test
    @DisplayName("Получение Sink: проверка возврата корректного Sink для канала")
    void testGetChannelSink_correctSinkReturned() {
        String channelId = "channel1";
        Sinks.Many<Event> channelSink = chatService.getChannelSink(channelId);

        assertNotNull(channelSink);
    }

    @Test
    @DisplayName("Обработка события: сохранение события 'пользователь присоединился' в базу данных")
    void testProcessEvent_userJoined_eventSavedToDatabase() {
        Event event = new Event();
        event.setType(Type.USER_JOINED);
        event.setMessage("User joined");
        String channelId = "channel1";

        chatService.processEvent(event, channelId);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Mono<Event> savedEvent = eventRepository.findById(event.getId());
                    assertNotNull(savedEvent.block());
                    assertEquals(Type.USER_JOINED, savedEvent.block().getType());
                });
    }

    @Test
    @DisplayName("Обработка события: сохранение события 'пользователь покинул' в базу данных")
    void testProcessEvent_userLeft_eventSavedToDatabase() {
        Event event = new Event();
        event.setType(Type.USER_LEFT);
        event.setMessage("User left");
        String channelId = "channel1";

        chatService.processEvent(event, channelId);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Mono<Event> savedEvent = eventRepository.findById(event.getId());
                    assertNotNull(savedEvent.block());
                    assertEquals(Type.USER_LEFT, savedEvent.block().getType());
                });
    }

    @Test
    @DisplayName("Получение сообщений по ID канала: проверка порядка сообщений")
    void testGetMessagesByChannelId_returnMessagesInOrder() {
        Event event1 = new Event();
        event1.setType(Type.CHAT_MESSAGE);
        event1.setMessage("Message 1");
        event1.setChannelId("channel1");

        Event event2 = new Event();
        event2.setType(Type.CHAT_MESSAGE);
        event2.setMessage("Message 2");
        event2.setChannelId("channel1");

        chatService.processEvent(event1, "channel1");
        chatService.processEvent(event2, "channel1");

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    StepVerifier.create(eventRepository.findByChannelId("channel1"))
                            .expectNextMatches(e -> "Message 1".equals(e.getMessage()))
                            .expectNextMatches(e -> "Message 2".equals(e.getMessage()))
                            .verifyComplete();
                });
    }

    @Test
    @DisplayName("Обработка события: проверка, что событие отправлено в Sink")
    void testProcessEvent_channelSink_emitsEvent() {
        Event event = new Event();
        event.setType(Type.CHAT_MESSAGE);
        event.setMessage("Message emitted to sink");
        String channelId = "channel1";

        Sinks.Many<Event> channelSink = chatService.getChannelSink(channelId);

        channelSink.asFlux()
                .doOnNext(e -> assertEquals("Message emitted to sink", e.getMessage()))
                .subscribe();

        chatService.processEvent(event, channelId);
    }

    @Test
    @DisplayName("Получение сообщений по ID канала: проверка возврата пустого результата для пустого канала")
    void testGetMessagesByChannelId_emptyChannel() {
        String channelId = "emptyChannel";

        StepVerifier.create(chatService.getMessagesByChannelId(channelId))
                .verifyComplete();
    }

}
