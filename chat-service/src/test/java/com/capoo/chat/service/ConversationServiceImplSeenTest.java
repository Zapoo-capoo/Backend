package com.capoo.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.capoo.chat.entity.Conversation;
import com.capoo.chat.entity.ParticipantInfo;
import com.capoo.chat.exception.AppException;
import com.capoo.chat.exception.ErrorCode;
import com.capoo.chat.mapper.ConversationMapperImpl;
import com.capoo.chat.repository.ConversationRepository;
import com.capoo.chat.repository.httpclient.ProfileClient;
import com.capoo.chat.service.cache.ICacheService;

class ConversationServiceImplSeenTest {
    private ConversationRepository repository;
    private ConversationServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ConversationRepository.class);
        service = new ConversationServiceImpl(
                repository, mock(ProfileClient.class), mock(ICacheService.class), new ConversationMapperImpl());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("u-me", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void markSeen_marksTheCurrentUser() {
        when(repository.markSeen("c1", "u-me")).thenReturn(true);

        assertDoesNotThrow(() -> service.markSeen("c1"));

        verify(repository).markSeen("c1", "u-me");
    }

    @Test
    void markSeen_notAParticipantOrUnknownConversation_notFound() {
        when(repository.markSeen("c1", "u-me")).thenReturn(false);

        var exception = assertThrows(AppException.class, () -> service.markSeen("c1"));

        assertEquals(ErrorCode.CONVERSATION_NOT_EXISTED, exception.getErrorCode());
    }

    /* ------------------------------------------------- hasSeen ------------------------------------------------- */

    private Conversation conversationWith(Boolean myHasSeen) {
        return Conversation.builder()
                .id("c1")
                .participants(new ArrayList<>(List.of(
                        ParticipantInfo.builder()
                                .userId("u-me")
                                .hasSeen(myHasSeen)
                                .build(),
                        // the other participant's flag must never influence the answer
                        ParticipantInfo.builder()
                                .userId("u2")
                                .hasSeen(!Boolean.TRUE.equals(myHasSeen))
                                .build())))
                .build();
    }

    @Test
    void hasSeen_true_whenCurrentUserMarkedSeen() {
        when(repository.findById("c1")).thenReturn(Optional.of(conversationWith(true)));

        assertTrue(service.hasSeen("c1"));
    }

    @Test
    void hasSeen_false_whenCurrentUserHasAnUnseenMessage() {
        when(repository.findById("c1")).thenReturn(Optional.of(conversationWith(false)));

        assertFalse(service.hasSeen("c1"));
    }

    @Test
    void hasSeen_true_forOlderConversationWithoutTheFlag() {
        when(repository.findById("c1")).thenReturn(Optional.of(conversationWith(null)));

        assertTrue(service.hasSeen("c1"));
    }

    @Test
    void hasSeen_notAParticipant_notFound() {
        Conversation others = Conversation.builder()
                .id("c1")
                .participants(List.of(
                        ParticipantInfo.builder().userId("u2").hasSeen(false).build()))
                .build();
        when(repository.findById("c1")).thenReturn(Optional.of(others));

        var exception = assertThrows(AppException.class, () -> service.hasSeen("c1"));

        assertEquals(ErrorCode.CONVERSATION_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void hasSeen_unknownConversation_notFound() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        var exception = assertThrows(AppException.class, () -> service.hasSeen("nope"));

        assertEquals(ErrorCode.CONVERSATION_NOT_EXISTED, exception.getErrorCode());
    }
}
