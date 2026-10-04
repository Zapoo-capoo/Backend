package com.capoo.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
}
