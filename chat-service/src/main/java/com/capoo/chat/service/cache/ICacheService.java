package com.capoo.chat.service.cache;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ICacheService {
    <T> Optional<T> get(String key, Class<T> type);

    void set(String key, Object value);

    void set(String key, Object value, Duration ttl);

    boolean exists(String key);

    boolean evict(String key);

    long evictByPattern(String pattern);

    boolean expire(String key, Duration ttl);

    <T> List<T> mGet(List<String> keys, Class<T> type);

    /* ZSET */
    boolean zAdd(String key, Object value, double score);

    long zAddAll(String key, Map<Object, Double> values);

    long zRemove(String key, Object... values);

    long zCard(String key);

    Long zRank(String key, Object value);

    Double zScore(String key, Object value);

    Double zIncrementScore(String key, Object value, double delta);

    /** Members with score in [min, max], highest score first, skipping {@code offset} and returning at most {@code count}. */
    <T> List<T> zReverseRangeByScore(String key, double max, double min, long offset, long count, Class<T> type);

    <T> List<T> zRange(String key, long start, long end, Class<T> type);

    <T> List<T> zReverseRange(String key, long start, long end, Class<T> type);

    <T> List<T> zRangeByScore(String key, double min, double max, Class<T> type);

    /* HASH */
    void hSet(String key, String hashKey, Object value);

    void hSet(String key, String hashKey, Object value, Duration ttl);

    <T> Optional<T> hGet(String key, String hashKey, Class<T> type);

    void hMSet(String key, Map<String, ?> values);

    void hMSet(String key, Map<String, ?> values, Duration ttl);

    <T> List<T> hMGet(String key, List<String> hashKeys, Class<T> type);

    <T> Map<String, T> hGetAll(String key, Class<T> type);

    long hDelete(String key, Object... hashKeys);

    boolean hExists(String key, String hashKey);
}
