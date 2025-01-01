package com.alkl1m.chat.controller;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

/**
 * Класс для шаблонов thymeleaf.
 *
 * @author AlKl1M
 */
@Controller
@RequiredArgsConstructor
public class ChatPageController {

    private final ChatService chatService;

    /**
     * Обрабатывает запрос на главную страницу и возвращает представление "home".
     *
     * @return имя представления для главной страницы.
     */
    @GetMapping("/")
    public String home() {
        return "home";
    }

    /**
     * Обрабатывает запрос на страницу чата, получая сообщения для указанного канала.
     * Добавляет данные канала и сообщения в модель для отображения на странице.
     *
     * @param channelId идентификатор канала для получения сообщений.
     * @param model     модель, в которую добавляются атрибуты для отображения.
     * @return имя представления для страницы чата.
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String channelId, Model model) {
        Flux<Event> messages = chatService.getMessagesByChannelId(channelId);

        model.addAttribute("channelId", channelId);
        model.addAttribute("messages", messages);

        return "chat";
    }

}
