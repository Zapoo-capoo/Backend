package com.capoo.chat.mapper;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.capoo.chat.dto.request.ChatMessageRequest;
import com.capoo.chat.dto.response.ChatMessageResponse;
import com.capoo.chat.entity.Attachment;
import com.capoo.chat.entity.ChatMessage;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {
    ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage);

    ChatMessage toChatMessage(ChatMessageRequest request);

    List<ChatMessageResponse> toChatMessageResponses(List<ChatMessage> chatMessages);

    /**
     * Responses always carry a list of attachments. A message from before attachments existed only has an
     * {@code imgUrl}, which is presented as a single image attachment.
     */
    @AfterMapping
    default void fillAttachments(
            ChatMessage source, @MappingTarget ChatMessageResponse.ChatMessageResponseBuilder target) {
        // The response is built with Lombok's builder, so MapStruct calls this with the builder (before build())
        List<Attachment> attachments =
                source.getAttachments() == null ? new ArrayList<>() : new ArrayList<>(source.getAttachments());
        if (attachments.isEmpty() && source.getImgUrl() != null) {
            attachments.add(Attachment.builder()
                    .type(Attachment.IMAGE)
                    .url(source.getImgUrl())
                    .build());
        }
        target.attachments(attachments);
    }
}
