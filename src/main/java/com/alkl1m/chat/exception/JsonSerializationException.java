package com.alkl1m.chat.exception;

/**
 * Исключение, возникающее при ошибке сериализации JSON.
 *
 * @author AlKl1M
 */
public class JsonSerializationException extends RuntimeException {

    public JsonSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
