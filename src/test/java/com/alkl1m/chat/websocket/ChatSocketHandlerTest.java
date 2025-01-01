package com.alkl1m.chat.websocket;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.entity.Type;
import com.alkl1m.chat.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.testcontainers.containers.MongoDBContainer;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatSocketHandlerTest {

    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    static {
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "testDB");
    }

    @LocalServerPort
    private String port;

    @Autowired
    private EventRepository eventRepository;

    private static final Duration TIMEOUT = Duration.ofMillis(5000);

    @Test
    void testHandle_withCoupleOfEvents_savesDataToMongo() throws URISyntaxException {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        int count = 5;
        AtomicReference<List<String>> actualRef = new AtomicReference<>();

        Flux<String> input = createInputFlux(count);

        client.execute(getUrl("/ws?channelId=1"), session ->
                        session.send(input.map(session::textMessage))
                                .thenMany(session.receive().take(count).map(WebSocketMessage::getPayloadAsText))
                                .collectList()
                                .doOnNext(actualRef::set)
                                .then())
                .block(TIMEOUT);

        awaitProcessing();

        verifySavedEvents(count);
    }

    private Flux<String> createInputFlux(int count) {
        return Flux.range(1, count)
                .map(index -> {
                    Event event = Event.builder()
                            .id(UUID.randomUUID().toString())
                            .channelId("test")
                            .type(Type.CHAT_MESSAGE)
                            .message("message-" + index)
                            .nickname("user")
                            .build();
                    return serializeEvent(event);
                });
    }

    private String serializeEvent(Event event) {
        try {
            return new ObjectMapper().writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void awaitProcessing() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifySavedEvents(int expectedCount) {
        Flux<Event> savedEvents = eventRepository.findAll();
        Long savedEventsCount = savedEvents.count().block();
        assertEquals(Long.valueOf(expectedCount), savedEventsCount);
    }

    protected URI getUrl(String path) throws URISyntaxException {
        return new URI("ws://localhost:" + this.port + path);
    }
}