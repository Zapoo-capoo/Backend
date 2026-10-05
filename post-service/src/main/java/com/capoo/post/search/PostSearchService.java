package com.capoo.post.search;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.capoo.post.entity.Post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Keeps the search index of the posts in line with Mongo (through Kafka), and searches it. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final EmbeddingClient embeddingClient;
    private final SolrPostSearch solr;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.post-search:post.search.index}")
    private String topic;

    /**
     * Asks for a new post to be indexed. Only sends a message, {@link PostSearchConsumer} does the work, retries it
     * when the embedding service or Solr is down and survives a restart of this service. Never fails the caller: the
     * post is already saved in Mongo.
     */
    public void requestIndex(Post post) {
        if (post.getContent() == null || post.getContent().isBlank()) return;
        send(post.getId(), PostSearchEvent.Action.INDEX);
    }

    public void requestDelete(String postId) {
        send(postId, PostSearchEvent.Action.DELETE);
    }

    private void send(String postId, PostSearchEvent.Action action) {
        try {
            // The post id is the key: the messages of one post stay in order (an INDEX is never handled after a DELETE)
            kafkaTemplate
                    .send(topic, postId, PostSearchEvent.builder().postId(postId).action(action).build())
                    .whenComplete((result, ex) -> {
                        if (ex != null) log.error("Could not send {} of post {} to the indexer", action, postId, ex);
                    });
        } catch (Exception e) {
            log.error("Could not send {} of post {} to the indexer", action, postId, e);
        }
    }

    /** Ids of the posts of {@code userIds} that match {@code query}, best first */
    public List<String> search(String query, Collection<String> userIds, int size) {
        return solr.search(query, embeddingClient.embedQuery(query), userIds, size);
    }
}
