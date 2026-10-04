package com.capoo.chat.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.capoo.chat.entity.Attachment;
import com.capoo.chat.entity.ChatMessage;

class ChatMessageMapperAttachmentTest {
    private final ChatMessageMapper mapper = new ChatMessageMapperImpl();

    private ChatMessage.ChatMessageBuilder message() {
        return ChatMessage.builder().id("m1").conversationId("c1").createdDate(Instant.parse("2026-10-04T05:00:00Z"));
    }

    @Test
    void oldMessageWithOnlyAnImgUrl_isShownAsOneImageAttachment() {
        var response = mapper.toChatMessageResponse(
                message().imgUrl("http://old/x.png").build());

        assertEquals(1, response.getAttachments().size());
        assertEquals(Attachment.IMAGE, response.getAttachments().get(0).getType());
        assertEquals("http://old/x.png", response.getAttachments().get(0).getUrl());
        assertEquals("http://old/x.png", response.getImgUrl());
    }

    @Test
    void newMessage_keepsItsOwnAttachments_andDoesNotInventAnotherFromImgUrl() {
        var attachment = Attachment.builder()
                .fileId("f1")
                .type(Attachment.IMAGE)
                .url("http://new/f1.webp")
                .build();

        var response = mapper.toChatMessageResponse(message()
                .attachments(List.of(attachment))
                .imgUrl("http://new/f1.webp")
                .build());

        assertEquals(1, response.getAttachments().size());
        assertEquals("f1", response.getAttachments().get(0).getFileId());
    }

    @Test
    void messageWithoutAnyMedia_hasAnEmptyListNotNull() {
        var response = mapper.toChatMessageResponse(message().message("hello").build());

        assertNotNull(response.getAttachments());
        assertTrue(response.getAttachments().isEmpty());
    }

    @Test
    void listMapping_alsoFillsAttachments() {
        var responses = mapper.toChatMessageResponses(
                List.of(message().imgUrl("http://old/x.png").build()));

        assertEquals(1, responses.get(0).getAttachments().size());
    }
}
