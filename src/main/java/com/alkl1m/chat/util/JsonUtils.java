package com.alkl1m.chat.util;

import com.alkl1m.chat.exception.JsonDeserializationException;
import com.alkl1m.chat.exception.JsonSerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonUtils {

    private final ObjectMapper mapper;

    public JsonUtils(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String toJSON(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Error serializing object to JSON", e);
        }
    }

    public <T> T toObject(String json, Class<T> valueType) {
        try {
            return mapper.readValue(json, valueType);
        } catch (IOException e) {
            throw new JsonDeserializationException("Invalid JSON format: " + json, e);
        }
    }
}