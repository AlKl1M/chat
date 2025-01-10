package com.alkl1m.chat.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Класс конфигурации mongodb.
 *
 * @author AlKl1M
 */
@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.alkl1m.chat.repository")
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String mongoDatabase;

    /**
     * Возвращает имя базы данных, которое будет использовано для подключения к MongoDB.
     *
     * @return имя базы данных.
     */
    @Override
    protected String getDatabaseName() {
        return mongoDatabase;
    }

    /**
     * Создает и возвращает MongoClient для подключения к MongoDB, используя URI из конфигурации.
     *
     * @return экземпляр MongoClient для подключения к базе данных MongoDB.
     */
    @Override
    public MongoClient reactiveMongoClient() {
        return MongoClients.create(mongoUri);
    }

}
