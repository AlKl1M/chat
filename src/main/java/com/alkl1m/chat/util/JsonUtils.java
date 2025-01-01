package com.alkl1m.chat.util;

import com.alkl1m.chat.exception.JsonDeserializationException;
import com.alkl1m.chat.exception.JsonSerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author AlKl1M
 */
@Component
public class JsonUtils {

    private final ObjectMapper mapper;

    /**
     * Конструктор для инициализации JsonUtils с заданным ObjectMapper.
     *
     * @param mapper ObjectMapper для сериализации и десериализации объектов.
     */
    public JsonUtils(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Преобразует объект в строку JSON.
     *
     * @param object объект, который необходимо сериализовать в JSON.
     * @return строка в формате JSON, представляющая объект.
     * @throws JsonSerializationException если возникла ошибка при сериализации.
     */
    public String toJSON(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Error serializing object to JSON", e);
        }
    }

    /**
     * Преобразует строку JSON в объект указанного типа.
     *
     * @param json      строка в формате JSON.
     * @param valueType тип объекта, в который необходимо преобразовать JSON.
     * @param <T>       тип объекта, который будет получен из JSON.
     * @return объект типа T, полученный из строки JSON.
     * @throws JsonDeserializationException если строка JSON имеет неверный формат.
     */
    public <T> T toObject(String json, Class<T> valueType) {
        try {
            return mapper.readValue(json, valueType);
        } catch (IOException e) {
            throw new JsonDeserializationException("Invalid JSON format: " + json, e);
        }
    }
}