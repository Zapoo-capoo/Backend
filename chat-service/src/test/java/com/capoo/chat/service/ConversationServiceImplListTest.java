package com.capoo.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.capoo.chat.entity.Conversation;
import com.capoo.chat.entity.ParticipantInfo;
import com.capoo.chat.mapper.ConversationMapperImpl;
import com.capoo.chat.repository.ConversationRepository;
import com.capoo.chat.repository.httpclient.ProfileClient;
import com.capoo.chat.service.cache.ICacheService;

class ConversationServiceImplListTest {
    private static final String ME = "u-me";

    private ConversationRepository conversationRepository;
    private ConversationServiceImpl service;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        service = new ConversationServiceImpl(
                conversationRepository,
                mock(ProfileClient.class),
                mock(ICacheService.class),
                new ConversationMapperImpl());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(ME, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Conversation conversation(String id, String type, String name) {
        List<ParticipantInfo> members = new ArrayList<>();
        members.add(ParticipantInfo.builder().userId(ME).username("me").build());
        members.add(ParticipantInfo.builder().userId("u2").username("bob").build());
        return Conversation.builder()
                .id(id)
                .type(type)
                .name(name)
                .participants(members)
                .build();
    }

    @Test
    void myConversations_isSortedByModifiedDateNewestFirst() {
        when(conversationRepository.findAllByParticipantIdsContains(eq(ME), any(Sort.class)))
                .thenReturn(List.of());

        service.myConversations();

        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(conversationRepository).findAllByParticipantIdsContains(eq(ME), sort.capture());
        assertEquals(
                Sort.Direction.DESC, sort.getValue().getOrderFor("modifiedDate").getDirection());
    }

    @Test
    void myConversations_keepsTheOrderTheRepositoryReturned_andNamesConversations() {
        when(conversationRepository.findAllByParticipantIdsContains(eq(ME), any(Sort.class)))
                .thenReturn(List.of(conversation("c1", "DIRECT", null), conversation("c2", "GROUP", "Team")));

        var result = service.myConversations();

        assertEquals(List.of("c1", "c2"), result.stream().map(r -> r.getId()).toList());
        assertEquals("bob", result.get(0).getConversationName()); // direct: the other participant
        assertEquals("Team", result.get(1).getConversationName()); // group: its own name
    }
}
