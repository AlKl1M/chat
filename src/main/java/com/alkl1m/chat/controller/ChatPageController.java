package com.alkl1m.chat.controller;

import com.alkl1m.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ChatPageController {

    private final ChatService chatService;

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String channelId, Model model) {
        model.addAttribute("channelId", channelId);
        model.addAttribute("messages", chatService.getMessagesByChannel(channelId));
        return "chat";
    }
}