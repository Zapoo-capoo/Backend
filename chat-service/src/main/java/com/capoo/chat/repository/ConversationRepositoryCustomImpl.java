package com.capoo.chat.repository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.capoo.chat.entity.Conversation;

import lombok.RequiredArgsConstructor;

/**
 * Both operations are single atomic updates of the matching array elements, so they cannot overwrite a concurrent
 * change of the same conversation (like a member being added) the way load-modify-save would.
 */
@RequiredArgsConstructor
public class ConversationRepositoryCustomImpl implements ConversationRepositoryCustom {
    private final MongoTemplate mongoTemplate;

    @Override
    public void markUnread(String conversationId, String senderId) {
        Update update = new Update()
                .set("participants.$[other].hasSeen", false)
                .set("participants.$[sender].hasSeen", true)
                .filterArray(Criteria.where("other.userId").ne(senderId))
                .filterArray(Criteria.where("sender.userId").is(senderId));
        mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(conversationId)), update, Conversation.class);
    }

    @Override
    public boolean markSeen(String conversationId, String userId) {
        Query query = Query.query(Criteria.where("id")
                .is(conversationId)
                .and("participants.userId")
                .is(userId));
        Update update = new Update()
                .set("participants.$[me].hasSeen", true)
                .filterArray(Criteria.where("me.userId").is(userId));
        return mongoTemplate.updateFirst(query, update, Conversation.class).getMatchedCount() > 0;
    }
}
