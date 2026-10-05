package com.capoo.post.search;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class SearchKafkaConfig {

    /**
     * A message that fails (the embedding service or Solr is down) is tried again every 5 seconds, 5 more times. After
     * that it is moved to the topic "<topic>.DLT" so it does not block the others (the default is 9 immediate retries,
     * which flood the log and fail all of them within a second). A Solr or model that is back later gets its posts with
     * a re-index from Mongo.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(5_000L, 5));
    }
}
