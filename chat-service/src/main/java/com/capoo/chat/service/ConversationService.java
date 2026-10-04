package com.capoo.chat.service;

import java.util.List;

import com.capoo.chat.dto.request.AddParticipantsRequest;
import com.capoo.chat.dto.request.ConversationRequest;
import com.capoo.chat.dto.request.GroupConversationRequest;
import com.capoo.chat.dto.request.UpdateParticipantRequest;
import com.capoo.chat.dto.response.ConversationResponse;

public interface ConversationService {
    List<ConversationResponse> myConversations();

    ConversationResponse create(ConversationRequest request);

    /** Creates a group with the current user as creator plus at least one other member. */
    ConversationResponse createGroup(GroupConversationRequest request);

    /** Adds members to an existing group. Any current member may add people. Members already in the group are skipped. */
    ConversationResponse addParticipants(String conversationId, AddParticipantsRequest request);

    /** Marks the conversation as seen by the current user ({@code hasSeen = true} on their participant entry). */
    void markSeen(String conversationId);

    /**
     * Whether the current user has seen the conversation: false while a message sent by someone else is still
     * unopened. Conversations from before {@code hasSeen} existed count as seen.
     */
    boolean hasSeen(String conversationId);

    void updateParticipant(UpdateParticipantRequest request);
}
