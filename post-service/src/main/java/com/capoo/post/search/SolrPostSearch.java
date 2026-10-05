package com.capoo.post.search;

import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.capoo.post.entity.Post;

import lombok.extern.slf4j.Slf4j;

/**
 * The Solr core "posts". Talks to Solr over its HTTP API (no SolrJ: it brings a Jetty that conflicts with the one of
 * Spring Boot). The fields of the core are created on first use, see {@link #ensureSchema()}.
 */
@Slf4j
@Component
public class SolrPostSearch {

    private static final String VECTOR_FIELD = "content_vector";
    private static final int RRF_K = 60;

    private final RestClient client;
    private final int dimensions;
    private final int candidates;
    private volatile boolean schemaReady;

    public SolrPostSearch(
            @Value("${app.search.solr-url:http://localhost:8983/solr/posts}") String solrUrl,
            @Value("${app.search.vector-dimensions:384}") int dimensions,
            @Value("${app.search.candidates:50}") int candidates) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(15_000);
        this.client = RestClient.builder().baseUrl(solrUrl).requestFactory(factory).build();
        this.dimensions = dimensions;
        this.candidates = candidates;
    }

    /** Adds (or replaces, the id is the key) a post in the index */
    public void index(Post post, float[] vector) {
        ensureSchema();
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", post.getId());
        doc.put("userId", post.getUserId());
        doc.put("content", post.getContent());
        doc.put(VECTOR_FIELD, vector);
        if (post.getCreatedDate() != null) {
            // Solr dates are ISO 8601 up to milliseconds
            doc.put("createdDate", post.getCreatedDate().truncatedTo(ChronoUnit.MILLIS).toString());
        }
        // commitWithin: the post is searchable about a second later, without a commit per post
        client.post()
                .uri("/update?commitWithin=1000")
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(doc))
                .retrieve()
                .toBodilessEntity();
    }

    public void delete(String postId) {
        client.post()
                .uri("/update?commitWithin=1000")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("delete", List.of(postId)))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Ids of the best posts of {@code userIds} for the search, best first. Runs two searches, one by meaning (the
     * vector) and one by words, and merges the two rankings with reciprocal rank fusion: a post that is high in both
     * comes first, and an exact word that the model does not understand (a name) is still found.
     */
    public List<String> search(String text, float[] queryVector, Collection<String> userIds, int size) {
        ensureSchema();
        String uids = String.join(",", userIds);

        List<String> byMeaning = queryIds(Map.of(
                "query", "{!knn f=" + VECTOR_FIELD + " topK=" + candidates + "}" + vectorLiteral(queryVector),
                "filter", List.of("{!terms f=userId v=$uids}"),
                "params", Map.of("uids", uids),
                "fields", "id",
                "limit", candidates));
        List<String> byWords = queryIds(Map.of(
                "query", "{!edismax qf=content v=$qq}",
                "filter", List.of("{!terms f=userId v=$uids}"),
                "params", Map.of("uids", uids, "qq", text),
                "fields", "id",
                "limit", candidates));

        Map<String, Double> scores = new HashMap<>();
        addRanks(scores, byMeaning);
        addRanks(scores, byWords);
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(size)
                .map(Map.Entry::getKey)
                .toList();
    }

    private void addRanks(Map<String, Double> scores, List<String> rankedIds) {
        for (int rank = 0; rank < rankedIds.size(); rank++) {
            scores.merge(rankedIds.get(rank), 1.0 / (RRF_K + rank + 1), Double::sum);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> queryIds(Map<String, Object> request) {
        Map<String, Object> body = client.post()
                .uri("/query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (body == null || !(body.get("response") instanceof Map<?, ?> response)) return List.of();
        List<Map<String, Object>> docs = (List<Map<String, Object>>) response.get("docs");
        if (docs == null) return List.of();
        return docs.stream().map(doc -> String.valueOf(doc.get("id"))).toList();
    }

    private String vectorLiteral(float[] vector) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float v : vector) joiner.add(Float.toString(v));
        return joiner.toString();
    }

    /** Creates the field types and the fields of the core the first time, does nothing when they exist */
    private synchronized void ensureSchema() {
        if (schemaReady) return;
        try {
            client.get().uri("/schema/fields/{name}", VECTOR_FIELD).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Creating the schema of the Solr core 'posts'");
            createSchema();
        }
        schemaReady = true;
    }

    private void createSchema() {
        Map<String, Object> textVi = Map.of(
                "name", "text_vi",
                "class", "solr.TextField",
                "positionIncrementGap", "100",
                // No stemming (Vietnamese has none). ASCII folding makes "ha noi" match "Hà Nội"
                "analyzer", Map.of(
                        "tokenizer", Map.of("class", "solr.StandardTokenizerFactory"),
                        "filters", List.of(
                                Map.of("class", "solr.LowerCaseFilterFactory"),
                                Map.of("class", "solr.ASCIIFoldingFilterFactory"))));
        Map<String, Object> vectorType = Map.of(
                "name", "knn_vector",
                "class", "solr.DenseVectorField",
                "vectorDimension", dimensions,
                "similarityFunction", "cosine",
                "knnAlgorithm", "hnsw");
        List<Map<String, Object>> fields = List.of(
                Map.of("name", "userId", "type", "string", "indexed", true, "stored", true),
                Map.of("name", "content", "type", "text_vi", "indexed", true, "stored", true),
                Map.of("name", VECTOR_FIELD, "type", "knn_vector", "indexed", true, "stored", false),
                Map.of("name", "createdDate", "type", "pdate", "indexed", true, "stored", true));

        // Two requests on purpose: Solr checks every command of a request against the schema as it was BEFORE the
        // request, so a field cannot use a type that is added in the same request ("Field type 'text_vi' not found").
        // A type that already exists is left alone, which makes this safe to run again after a half-done attempt.
        addFieldTypeIfMissing("text_vi", textVi);
        addFieldTypeIfMissing("knn_vector", vectorType);

        client.post()
                .uri("/schema")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("add-field", fields))
                .retrieve()
                .toBodilessEntity();
    }

    private void addFieldTypeIfMissing(String name, Map<String, Object> fieldType) {
        try {
            client.get().uri("/schema/fieldtypes/{name}", name).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            client.post()
                    .uri("/schema")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("add-field-type", fieldType))
                    .retrieve()
                    .toBodilessEntity();
        }
    }
}
