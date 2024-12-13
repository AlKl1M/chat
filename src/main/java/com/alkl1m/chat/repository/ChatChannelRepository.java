package com.alkl1m.chat.repository;

import com.alkl1m.chat.entity.ChatChannel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatChannelRepository extends ReactiveMongoRepository<ChatChannel, String> {
}