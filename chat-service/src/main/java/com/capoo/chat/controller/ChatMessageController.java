package com.capoo.chat.controller;

import org.springframework.web.bind.annotation.*;

import com.capoo.chat.dto.CursorResponse;
import com.capoo.chat.dto.request.ChatMessageRequest;
import com.capoo.chat.dto.response.ChatMessageResponse;
import com.capoo.chat.service.ChatMessageService;
import com.capoo.dto.ApiResponse;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@RequestMapping("messages")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageController {
    ChatMessageService chatMessageService;

    @PostMapping("/create")
    ApiResponse<ChatMessageResponse> create(@ModelAttribute ChatMessageRequest request) {
        return ApiResponse.<ChatMessageResponse>builder()
                .result(chatMessageService.create(request))
                .build();
    }

    @GetMapping
    ApiResponse<CursorResponse<ChatMessageResponse>> getMessages(
            @RequestParam("conversationId") String conversationId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return ApiResponse.<CursorResponse<ChatMessageResponse>>builder()
                .result(chatMessageService.getMessages(conversationId, cursor, size))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> deleteMessage(@PathVariable("id") String id) {
        chatMessageService.deleteMessage(id);
        return ApiResponse.<Void>builder().build();
    }
}
