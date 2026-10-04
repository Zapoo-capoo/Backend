package com.capoo.chat.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.capoo.chat.dto.response.ConversationResponse;
import com.capoo.chat.entity.Conversation;

@Mapper(componentModel = "spring")
public interface ConversationMapper {
    ConversationResponse toConversationResponse(Conversation conversation);

    List<ConversationResponse> toConversationResponseList(List<Conversation> conversations);
}
