package com.alkl1m.chat.controller;

import com.alkl1m.chat.entity.Event;
import com.alkl1m.chat.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

@Controller
@RequiredArgsConstructor
public class ChatPageController {

    private final EventRepository eventRepository;

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String channelId, Model model) {
        Flux<Event> messages = eventRepository.findByChannelId(channelId);

        model.addAttribute("channelId", channelId);
        model.addAttribute("messages", messages);

        return "chat";
    }
}