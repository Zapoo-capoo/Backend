package com.capoo.chat.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import com.capoo.chat.dto.response.ChatMessageResponse;
import com.capoo.chat.entity.ParticipantInfo;

class RedisConfigTest {
    private final RedisTemplate<String, Object> template =
            new RedisConfig().redisTemplate(new LettuceConnectionFactory());

    @SuppressWarnings("unchecked")
    private <T> T roundTrip(Object value) {
        byte[] bytes = ((org.springframework.data.redis.serializer.RedisSerializer<Object>)
                        template.getValueSerializer())
                .serialize(value);
        return (T) ((org.springframework.data.redis.serializer.RedisSerializer<Object>) template.getValueSerializer())
                .deserialize(bytes);
    }

    @Test
    void messageResponse_roundTripsWithTypeAndInstant() {
        ChatMessageResponse message = ChatMessageResponse.builder()
                .id("m1")
                .conversationId("c1")
                .message("hello")
                .createdDate(Instant.parse("2026-10-04T05:00:00.123Z"))
                .sender(ParticipantInfo.builder()
                        .userId("u1")
                        .username("user01")
                        .build())
                .build();

        Object restored = roundTrip(message);

        ChatMessageResponse typed = assertInstanceOf(ChatMessageResponse.class, restored);
        assertEquals("m1", typed.getId());
        assertEquals(Instant.parse("2026-10-04T05:00:00.123Z"), typed.getCreatedDate());
        assertEquals("u1", typed.getSender().getUserId());
    }

    @Test
    void stringMember_roundTripsAsPlainString() {
        Object restored = roundTrip("6ac147320c1e7c8ab23556bc");

        assertEquals("6ac147320c1e7c8ab23556bc", assertInstanceOf(String.class, restored));
    }

    @Test
    void participantList_roundTripsAsList() {
        Object restored = roundTrip(new ArrayList<>(List.of("u1", "u2")));

        assertEquals(List.of("u1", "u2"), assertInstanceOf(List.class, restored));
    }
}
