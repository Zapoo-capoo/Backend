package com.capoo.chat.service;

import com.capoo.chat.dto.CursorResponse;
import com.capoo.chat.dto.request.ChatMessageRequest;
import com.capoo.chat.dto.response.ChatMessageResponse;

public interface ChatMessageService {
    /** Newest first. {@code cursor} is the {@code nextCursor} of the previous page, or null for the first page. */
    CursorResponse<ChatMessageResponse> getMessages(String conversationId, String cursor, int size);

    /**
     * Sends a message with some text and/or up to 5 images or videos. The files are uploaded to storage-service by the
     * client beforehand, {@code request.fileIds} refers to them.
     */
    ChatMessageResponse create(ChatMessageRequest request);

    void deleteMessage(String messageId);
}
