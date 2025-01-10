package com.alkl1m.chat.repository;

import com.alkl1m.chat.entity.Event;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface EventRepository extends ReactiveMongoRepository<Event, String> {

    Flux<Event> findByChannelId(String channelId);

}
