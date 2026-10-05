package com.capoo.post.search;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.capoo.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Does the indexing asked for by {@link PostSearchService}. An exception makes Kafka deliver the message again (see
 * {@link SearchKafkaConfig} for the number of retries and where the messages that never succeed end up).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostSearchConsumer {

    private final PostRepository postRepository;
    private final EmbeddingClient embeddingClient;
    private final SolrPostSearch solr;

    @KafkaListener(topics = "${app.kafka.topics.post-search:post.search.index}")
    public void onEvent(@Payload PostSearchEvent event) {
        if (event.getAction() == PostSearchEvent.Action.DELETE) {
            solr.delete(event.getPostId());
            log.info("Removed post {} from the search index", event.getPostId());
            return;
        }
        // The current post is read from Mongo: a post deleted since the message was sent is simply skipped
        postRepository.findById(event.getPostId()).ifPresentOrElse(post -> {
            if (post.getContent() == null || post.getContent().isBlank()) return;
            solr.index(post, embeddingClient.embedPassage(post.getContent()));
            log.info("Indexed post {} for the search", post.getId());
        }, () -> log.info("Post {} no longer exists, nothing to index", event.getPostId()));
    }
}
