package com.capoo.chat.repository;

/** Atomic updates of the {@code hasSeen} flag of conversation participants. */
public interface ConversationRepositoryCustom {
    /**
     * A message was just sent: every participant except the sender now has something unseen ({@code hasSeen = false}),
     * the sender has obviously seen it ({@code hasSeen = true}).
     */
    void markUnread(String conversationId, String senderId);

    /**
     * Marks the conversation as seen by this user.
     *
     * @return false when the conversation does not exist or the user is not one of its participants
     */
    boolean markSeen(String conversationId, String userId);
}
