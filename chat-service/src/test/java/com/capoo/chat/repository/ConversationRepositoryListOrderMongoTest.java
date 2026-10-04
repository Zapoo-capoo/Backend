package com.capoo.chat.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;

import com.capoo.chat.entity.Conversation;
import com.capoo.chat.entity.ParticipantInfo;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * Checks the real Spring Data query behind "my conversations, most recently modified first" against a MongoDB (the one
 * from docker-compose). Skipped when none is reachable. Uses a throw-away database that is dropped afterwards.
 */
class ConversationRepositoryListOrderMongoTest {
    private static final String URI = "mongodb://root:root@localhost:27017/?authSource=admin";
    private static final String DB = "chat-service-test-" + UUID.randomUUID();
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "modifiedDate");

    private static MongoClient client;
    private static MongoTemplate template;
    private static ConversationRepository repository;

    @BeforeAll
    static void connect() {
        MongoClient candidate = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(URI))
                .applyToClusterSettings(b -> b.serverSelectionTimeout(2, TimeUnit.SECONDS))
                .build());
        try {
            candidate.getDatabase("admin").runCommand(new Document("ping", 1));
        } catch (Exception e) {
            candidate.close();
            assumeTrue(false, "MongoDB is not reachable, skipping");
        }
        client = candidate;
        template = new MongoTemplate(new SimpleMongoClientDatabaseFactory(client, DB));
        repository = new MongoRepositoryFactory(template)
                .getRepository(
                        ConversationRepository.class,
                        RepositoryFragments.just(new ConversationRepositoryCustomImpl(template)));
    }

    @AfterAll
    static void cleanUp() {
        if (client != null) {
            client.getDatabase(DB).drop();
            client.close();
        }
    }

    private Conversation save(String name, Instant modifiedDate, String... userIds) {
        List<ParticipantInfo> participants = java.util.Arrays.stream(userIds)
                .map(id -> ParticipantInfo.builder().userId(id).build())
                .toList();
        return template.save(Conversation.builder()
                .type("GROUP")
                .name(name)
                .participantsHash("GROUP_" + UUID.randomUUID())
                .participants(participants)
                .modifiedDate(modifiedDate)
                .build());
    }

    private List<String> names(List<Conversation> conversations) {
        return conversations.stream().map(Conversation::getName).toList();
    }

    @Test
    void sortsByModifiedDateNewestFirst_onlyConversationsOfTheUser() {
        String me = "me-" + UUID.randomUUID();
        save("oldest", Instant.parse("2026-01-01T00:00:00Z"), me, "x");
        save("newest", Instant.parse("2026-03-01T00:00:00Z"), me, "x");
        save("middle", Instant.parse("2026-02-01T00:00:00Z"), me, "x");
        save("not-mine", Instant.parse("2026-12-01T00:00:00Z"), "someone-else");

        assertEquals(
                List.of("newest", "middle", "oldest"),
                names(repository.findAllByParticipantIdsContains(me, NEWEST_FIRST)));
    }

    @Test
    void conversationsWithoutModifiedDate_comeLast() {
        String me = "me-" + UUID.randomUUID();
        save("legacy", null, me);
        save("recent", Instant.parse("2026-03-01T00:00:00Z"), me);
        save("older", Instant.parse("2026-01-01T00:00:00Z"), me);

        assertEquals(
                List.of("recent", "older", "legacy"),
                names(repository.findAllByParticipantIdsContains(me, NEWEST_FIRST)));
    }

    @Test
    void unsortedVariantStillReturnsTheSameConversations() {
        String me = "me-" + UUID.randomUUID();
        save("a", Instant.parse("2026-01-01T00:00:00Z"), me);
        save("b", Instant.parse("2026-02-01T00:00:00Z"), me);

        assertEquals(2, repository.findAllByParticipantIdsContains(me).size());
    }
}
