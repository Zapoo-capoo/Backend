package com.capoo.chat.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.capoo.chat.entity.Conversation;
import com.capoo.chat.entity.ParticipantInfo;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * Runs the real update operators against a MongoDB (the one from docker-compose). Skipped when none is reachable.
 * Uses a throw-away database that is dropped afterwards.
 */
class ConversationRepositoryCustomImplMongoTest {
    private static final String URI = "mongodb://root:root@localhost:27017/?authSource=admin";
    private static final String DB = "chat-service-test-" + UUID.randomUUID();

    private static MongoClient client;
    private static MongoTemplate template;
    private static ConversationRepositoryCustomImpl repository;

    @BeforeAll
    static void connect() {
        MongoClient candidate = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(URI))
                .applyToClusterSettings(b -> b.serverSelectionTimeout(2, java.util.concurrent.TimeUnit.SECONDS))
                .build());
        try {
            candidate.getDatabase("admin").runCommand(new Document("ping", 1));
        } catch (Exception e) {
            candidate.close();
            assumeTrue(false, "MongoDB is not reachable, skipping");
        }
        client = candidate;
        template = new MongoTemplate(new SimpleMongoClientDatabaseFactory(client, DB));
        repository = new ConversationRepositoryCustomImpl(template);
    }

    @AfterAll
    static void cleanUp() {
        if (client != null) {
            client.getDatabase(DB).drop();
            client.close();
        }
    }

    private Conversation save(Boolean hasSeen, String... userIds) {
        List<ParticipantInfo> participants = new ArrayList<>();
        for (String userId : userIds) {
            participants.add(ParticipantInfo.builder()
                    .userId(userId)
                    .username(userId)
                    .hasSeen(hasSeen)
                    .build());
        }
        return template.save(Conversation.builder()
                .type("GROUP")
                .participantsHash("GROUP_" + UUID.randomUUID())
                .participants(participants)
                .build());
    }

    private Boolean seenOf(String conversationId, String userId) {
        return template.findById(conversationId, Conversation.class).getParticipants().stream()
                .filter(p -> userId.equals(p.getUserId()))
                .findFirst()
                .orElseThrow()
                .getHasSeen();
    }

    @Test
    void markUnread_everyoneButSenderBecomesUnseen_senderSeen() {
        Conversation conversation = save(true, "a", "b", "c");

        repository.markUnread(conversation.getId(), "a");

        assertEquals(true, seenOf(conversation.getId(), "a"));
        assertEquals(false, seenOf(conversation.getId(), "b"));
        assertEquals(false, seenOf(conversation.getId(), "c"));
    }

    @Test
    void markUnread_senderWhoHadUnseenMessagesIsSeenAgain() {
        Conversation conversation = save(false, "a", "b");

        repository.markUnread(conversation.getId(), "a");

        assertEquals(true, seenOf(conversation.getId(), "a"));
        assertEquals(false, seenOf(conversation.getId(), "b"));
    }

    @Test
    void markUnread_conversationWithoutTheFieldYet_getsItAdded() {
        Conversation conversation = save(null, "a", "b"); // like conversations created before hasSeen existed
        assertNull(seenOf(conversation.getId(), "b"));

        repository.markUnread(conversation.getId(), "a");

        assertEquals(true, seenOf(conversation.getId(), "a"));
        assertEquals(false, seenOf(conversation.getId(), "b"));
    }

    @Test
    void markSeen_onlyTouchesTheCaller() {
        Conversation conversation = save(false, "a", "b", "c");

        assertTrue(repository.markSeen(conversation.getId(), "b"));

        assertEquals(false, seenOf(conversation.getId(), "a"));
        assertEquals(true, seenOf(conversation.getId(), "b"));
        assertEquals(false, seenOf(conversation.getId(), "c"));
    }

    @Test
    void markSeen_isIdempotent() {
        Conversation conversation = save(false, "a", "b");

        assertTrue(repository.markSeen(conversation.getId(), "a"));
        assertTrue(repository.markSeen(conversation.getId(), "a"));

        assertEquals(true, seenOf(conversation.getId(), "a"));
    }

    @Test
    void markSeen_nonParticipantOrUnknownConversation_returnsFalse() {
        Conversation conversation = save(false, "a", "b");

        assertFalse(repository.markSeen(conversation.getId(), "stranger"));
        assertFalse(repository.markSeen("does-not-exist", "a"));
        assertEquals(false, seenOf(conversation.getId(), "a"));
    }

    @Test
    void markUnread_doesNotTouchOtherConversations() {
        Conversation target = save(true, "a", "b");
        Conversation other = save(true, "a", "b");

        repository.markUnread(target.getId(), "a");

        assertEquals(false, seenOf(target.getId(), "b"));
        assertEquals(true, seenOf(other.getId(), "b"));
    }
}
