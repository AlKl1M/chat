package com.alkl1m.chat.exception;

/**
 * Исключение, возникающее при ошибке десериализации JSON.
 *
 * @author AlKl1M
 */
public class JsonDeserializationException extends RuntimeException {

    public JsonDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
