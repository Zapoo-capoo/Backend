package com.capoo.chat.service.cache;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements ICacheService {
    private static final long SCAN_BATCH_SIZE = 1_000;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) return Optional.empty();
            return Optional.of(type.cast(value));
        } catch (ClassCastException e) {
            log.warn("Redis read type mismatch for key {}: {}", key, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis read failed for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.warn("Redis write failed for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        try {
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, value);
                return;
            }
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("Redis write failed for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Redis exists check failed for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean evict(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.warn("Redis delete failed for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public long evictByPattern(String pattern) {
        // KEYS is dangerous on production; use SCAN.
        ScanOptions options =
                ScanOptions.scanOptions().match(pattern).count(SCAN_BATCH_SIZE).build();

        List<String> keys = new ArrayList<>();
        if (redisTemplate.getConnectionFactory() == null) return 0;
        try (Cursor<byte[]> cursor =
                redisTemplate.getConnectionFactory().getConnection().scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.warn("Redis scan failed for pattern {}: {}", pattern, e.getMessage());
            return 0;
        }

        if (keys.isEmpty()) return 0;

        try {
            Long deleted = redisTemplate.delete(keys);
            return deleted == null ? 0 : deleted;
        } catch (Exception e) {
            log.warn("Redis delete failed for pattern {}: {}", pattern, e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        try {
            if (ttl == null) return false;
            Boolean ok = redisTemplate.expire(key, ttl);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("Redis expire failed for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean zAdd(String key, Object value, double score) {
        try {
            Boolean added = redisTemplate.opsForZSet().add(key, value, score);
            return Boolean.TRUE.equals(added);
        } catch (Exception e) {
            log.warn("Redis zadd failed for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public long zAddAll(String key, Map<Object, Double> values) {
        try {
            if (values == null || values.isEmpty()) {
                return 0L;
            }
            Set<ZSetOperations.TypedTuple<Object>> tuples = values.entrySet().stream()
                    .map(entry -> new DefaultTypedTuple<>(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toSet());
            Long added = redisTemplate.opsForZSet().add(key, tuples);
            return added == null ? 0L : added;
        } catch (Exception e) {
            log.warn("Redis zaddAll failed for key {}: {}", key, e.getMessage());
            return 0L;
        }
    }

    @Override
    public Double zIncrementScore(String key, Object value, double delta) {
        try {
            return redisTemplate.opsForZSet().incrementScore(key, value, delta);
        } catch (Exception e) {
            log.warn("Redis zincrby failed for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public long zRemove(String key, Object... values) {
        try {
            Long removed = redisTemplate.opsForZSet().remove(key, values);
            return removed == null ? 0 : removed;
        } catch (Exception e) {
            log.warn("Redis zrem failed for key {}: {}", key, e.getMessage());
            return 0;
        }
    }

    @Override
    public long zCard(String key) {
        try {
            Long size = redisTemplate.opsForZSet().zCard(key);
            return size == null ? 0 : size;
        } catch (Exception e) {
            log.warn("Redis zcard failed for key {}: {}", key, e.getMessage());
            return 0;
        }
    }

    @Override
    public Long zRank(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().rank(key, value);
        } catch (Exception e) {
            log.warn("Redis zrank failed for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public Double zScore(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().score(key, value);
        } catch (Exception e) {
            log.warn("Redis zscore failed for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> List<T> zReverseRangeByScore(
            String key, double max, double min, long offset, long count, Class<T> type) {
        try {
            Set<Object> values = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, offset, count);
            return castZSetMembers(key, values, type);
        } catch (Exception e) {
            log.warn("Redis zrevrangebyscore failed for key {}: {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public <T> List<T> zRange(String key, long start, long end, Class<T> type) {
        try {
            Set<Object> values = redisTemplate.opsForZSet().range(key, start, end);
            return castZSetMembers(key, values, type);
        } catch (Exception e) {
            log.warn("Redis zrange failed for key {}: {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public <T> List<T> zReverseRange(String key, long start, long end, Class<T> type) {
        try {
            Set<Object> values = redisTemplate.opsForZSet().reverseRange(key, start, end);
            return castZSetMembers(key, values, type);
        } catch (Exception e) {
            log.warn("Redis zrevrange failed for key {}: {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public <T> List<T> zRangeByScore(String key, double min, double max, Class<T> type) {
        try {
            Set<Object> values = redisTemplate.opsForZSet().rangeByScore(key, min, max);
            return castZSetMembers(key, values, type);
        } catch (Exception e) {
            log.warn("Redis zrangebyscore failed for key {}: {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

    private <T> List<T> castZSetMembers(String key, Set<Object> values, Class<T> type) {
        if (values == null || values.isEmpty()) return Collections.emptyList();
        List<T> result = new ArrayList<>(values.size());
        for (Object value : values) {
            try {
                result.add(type.cast(value));
            } catch (ClassCastException e) {
                log.warn("Redis zset type mismatch for key {}: {}", key, e.getMessage());
            }
        }
        return result;
    }
    //
    @Override
    public <T> List<T> mGet(List<String> keys, Class<T> type) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }
            return values.stream()
                    .map(value -> {
                        if (value == null) {
                            return null;
                        }
                        if (!type.isInstance(value)) {
                            log.warn(
                                    "Redis mget type mismatch: expected {}, actual {}",
                                    type.getName(),
                                    value.getClass().getName());
                            return null;
                        }
                        return type.cast(value);
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Redis mget failed for {} keys: {}", keys.size(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void hSet(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
        } catch (Exception e) {
            log.warn("Redis hset failed for key {}, hashKey {}: {}", key, hashKey, e.getMessage());
        }
    }

    @Override
    public void hSet(String key, String hashKey, Object value, Duration ttl) {
        hSet(key, hashKey, value);
        expire(key, ttl);
    }

    @Override
    public <T> Optional<T> hGet(String key, String hashKey, Class<T> type) {
        try {
            Object value = redisTemplate.opsForHash().get(key, hashKey);
            if (value == null) {
                return Optional.empty();
            }
            if (!type.isInstance(value)) {
                log.warn(
                        "Redis hget type mismatch for key {}, hashKey {}: expected {}, actual {}",
                        key,
                        hashKey,
                        type.getName(),
                        value.getClass().getName());
                return Optional.empty();
            }
            return Optional.of(type.cast(value));
        } catch (Exception e) {
            log.warn("Redis hget failed for key {}, hashKey {}: {}", key, hashKey, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void hMSet(String key, Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        try {
            redisTemplate.opsForHash().putAll(key, values);
        } catch (Exception e) {
            log.warn("Redis hmset failed for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public void hMSet(String key, Map<String, ?> values, Duration ttl) {
        hMSet(key, values);
        expire(key, ttl);
    }

    @Override
    public <T> List<T> hMGet(String key, List<String> hashKeys, Class<T> type) {
        if (hashKeys == null || hashKeys.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Object> values = redisTemplate.opsForHash().multiGet(key, new ArrayList<>(hashKeys));
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }
            return values.stream()
                    .map(value -> {
                        if (value == null) {
                            return null;
                        }
                        if (!type.isInstance(value)) {
                            log.warn(
                                    "Redis hmget type mismatch for key {}: expected {}, actual {}",
                                    key,
                                    type.getName(),
                                    value.getClass().getName());
                            return null;
                        }
                        return type.cast(value);
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Redis hmget failed for key {}: {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public <T> Map<String, T> hGetAll(String key, Class<T> type) {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries == null || entries.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<String, T> result = new LinkedHashMap<>();
            entries.forEach((hashKey, value) -> {
                if (value == null) {
                    return;
                }
                if (!type.isInstance(value)) {
                    log.warn(
                            "Redis hgetall type mismatch for key {}, hashKey {}: expected {}, actual {}",
                            key,
                            hashKey,
                            type.getName(),
                            value.getClass().getName());
                    return;
                }
                result.put(String.valueOf(hashKey), type.cast(value));
            });
            return result;
        } catch (Exception e) {
            log.warn("Redis hgetall failed for key {}: {}", key, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public long hDelete(String key, Object... hashKeys) {
        if (hashKeys == null || hashKeys.length == 0) {
            return 0L;
        }
        try {
            Long deleted = redisTemplate.opsForHash().delete(key, hashKeys);
            return deleted == null ? 0L : deleted;
        } catch (Exception e) {
            log.warn("Redis hdel failed for key {}: {}", key, e.getMessage());
            return 0L;
        }
    }

    @Override
    public boolean hExists(String key, String hashKey) {
        try {
            Boolean exists = redisTemplate.opsForHash().hasKey(key, hashKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Redis hexists failed for key {}, hashKey {}: {}", key, hashKey, e.getMessage());
            return false;
        }
    }
}
