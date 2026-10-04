package com.capoo.chat.service.cache;

import java.time.Duration;

/** Redis keys and settings used for caching chat data. */
public final class ChatCacheKeys {
    /** Number of newest messages kept in the cache for each conversation. */
    public static final int MESSAGE_CACHE_SIZE = 20;

    public static final Duration TTL = Duration.ofMinutes(5);

    private ChatCacheKeys() {}

    /** ZSET: member = messageId, score = createdDate (epoch millis). */
    public static String messageIndex(String conversationId) {
        return "chat:conversation:" + conversationId + ":message";
    }

    /** HASH: messageId -> ChatMessageResponse (JSON). */
    public static String messageData(String conversationId) {
        return "chat:conversation:" + conversationId + ":message:data";
    }

    /** Value: list of participant userIds. */
    public static String participants(String conversationId) {
        return "chat:conversation:" + conversationId + ":participants";
    }
}
