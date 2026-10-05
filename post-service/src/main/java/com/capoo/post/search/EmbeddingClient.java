package com.capoo.post.search;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Turns text into a vector with the self-hosted embedding model (HuggingFace Text Embeddings Inference serving
 * intfloat/multilingual-e5-small, 384 dimensions). The e5 models expect every text to start with "query: " (what the
 * user types) or "passage: " (what is searched), they give worse results without it.
 */
@Component
public class EmbeddingClient {

    private final RestClient client;

    public EmbeddingClient(@Value("${app.search.embedding-url:http://localhost:8089}") String embeddingUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(20_000);
        this.client = RestClient.builder()
                .baseUrl(embeddingUrl)
                .requestFactory(factory)
                .build();
    }

    /** Vector of a text that is stored in the index (the content of a post) */
    public float[] embedPassage(String text) {
        return embed("passage: " + text);
    }

    /** Vector of what the user typed in the search box */
    public float[] embedQuery(String text) {
        return embed("query: " + text);
    }

    private float[] embed(String input) {
        // truncate: texts longer than the model accepts (512 tokens) are cut instead of refused
        // normalize: vectors of length 1, so the cosine similarity of Solr is a plain dot product
        float[][] vectors = client.post()
                .uri("/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("inputs", List.of(input), "truncate", true, "normalize", true))
                .retrieve()
                .body(float[][].class);
        if (vectors == null || vectors.length == 0) {
            throw new IllegalStateException("The embedding service returned no vector");
        }
        return vectors[0];
    }
}
