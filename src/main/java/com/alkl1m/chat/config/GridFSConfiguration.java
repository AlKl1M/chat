package com.alkl1m.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;

/**
 * Класс конфигурации реактивного GridFSTemplate для сохранения файлов в монго.
 *
 * @author AlKl1M
 */
@Configuration
public class GridFSConfiguration {

    /**
     * Создает и возвращает экземпляр ReactiveGridFsTemplate, который используется для работы с GridFS в MongoDB.
     * Он позволяет работать с файлами в реактивном стиле, используя MongoDB как хранилище для больших файлов.
     *
     * @param reactiveMongoDatabaseFactory фабрика для создания подключения к базе данных MongoDB.
     * @param mappingMongoConverter        преобразователь, используемый для конвертации объектов в формат MongoDB.
     * @return экземпляр ReactiveGridFsTemplate для работы с файлами в GridFS.
     */
    @Bean
    public ReactiveGridFsTemplate reactiveGridFsTemplate(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory, MappingMongoConverter mappingMongoConverter) {
        return new ReactiveGridFsTemplate(reactiveMongoDatabaseFactory, mappingMongoConverter);
    }

}