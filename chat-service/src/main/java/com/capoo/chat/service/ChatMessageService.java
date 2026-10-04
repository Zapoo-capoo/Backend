package com.capoo.chat.service;

import org.springframework.web.multipart.MultipartFile;

import com.capoo.chat.dto.CursorResponse;
import com.capoo.chat.dto.request.ChatMessageRequest;
import com.capoo.chat.dto.response.ChatMessageResponse;

public interface ChatMessageService {
    /** Newest first. {@code cursor} is the {@code nextCursor} of the previous page, or null for the first page. */
    CursorResponse<ChatMessageResponse> getMessages(String conversationId, String cursor, int size);

    ChatMessageResponse create(ChatMessageRequest request, MultipartFile file);

    void deleteMessage(String messageId);
}
