package com.capoo.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.capoo.chat.dto.request.AddParticipantsRequest;
import com.capoo.chat.dto.request.GroupConversationRequest;
import com.capoo.chat.dto.response.ConversationResponse;
import com.capoo.chat.dto.response.UserProfileResponse;
import com.capoo.chat.entity.Conversation;
import com.capoo.chat.entity.ParticipantInfo;
import com.capoo.chat.exception.AppException;
import com.capoo.chat.exception.ErrorCode;
import com.capoo.chat.mapper.ConversationMapperImpl;
import com.capoo.chat.repository.ConversationRepository;
import com.capoo.chat.repository.httpclient.ProfileClient;
import com.capoo.chat.service.cache.ChatCacheKeys;
import com.capoo.chat.service.cache.ICacheService;
import com.capoo.dto.ApiResponse;

import feign.FeignException;
import feign.Request;

class ConversationServiceImplGroupTest {
    private static final String ME = "u-me";

    private ConversationRepository repository;
    private ProfileClient profileClient;
    private ICacheService cacheService;
    private ConversationServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ConversationRepository.class);
        profileClient = mock(ProfileClient.class);
        cacheService = mock(ICacheService.class);
        service = new ConversationServiceImpl(repository, profileClient, cacheService, new ConversationMapperImpl());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(ME, null));

        when(repository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // every userId except "ghost" has a profile
        when(profileClient.getProfile(any())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            if (userId.equals("ghost")) {
                return ApiResponse.<UserProfileResponse>builder().build();
            }
            return ApiResponse.<UserProfileResponse>builder()
                    .result(UserProfileResponse.builder()
                            .userId(userId)
                            .username("name-" + userId)
                            .build())
                    .build();
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Conversation group(String id, String... memberIds) {
        List<ParticipantInfo> members = new ArrayList<>();
        for (String memberId : memberIds) {
            members.add(ParticipantInfo.builder()
                    .userId(memberId)
                    .username("name-" + memberId)
                    .build());
        }
        return Conversation.builder()
                .id(id)
                .type("GROUP")
                .name("Team")
                .createdBy(ME)
                .participantsHash("GROUP_x")
                .participants(members)
                .build();
    }

    private List<String> userIds(ConversationResponse response) {
        return response.getParticipants().stream()
                .map(ParticipantInfo::getUserId)
                .toList();
    }

    /* ------------------------------------------------ createGroup ------------------------------------------------ */

    @Test
    void createGroup_addsCreatorAndMembers_andUsesGroupName() {
        var response = service.createGroup(GroupConversationRequest.builder()
                .name("  Team A  ")
                .participantIds(List.of("u2", "u3"))
                .build());

        assertEquals("GROUP", response.getType());
        assertEquals("Team A", response.getConversationName());
        assertEquals(ME, response.getCreatedBy());
        assertEquals(List.of(ME, "u2", "u3"), userIds(response));
        assertTrue(response.getParticipantsHash().startsWith("GROUP_"));
    }

    @Test
    void createGroup_ignoresCreatorDuplicatesAndBlanks() {
        var response = service.createGroup(GroupConversationRequest.builder()
                .name("Team")
                .participantIds(java.util.Arrays.asList(ME, "u2", "u2", " ", null))
                .build());

        assertEquals(List.of(ME, "u2"), userIds(response));
    }

    @Test
    void createGroup_onlyCreatorListed_rejected() {
        var request = GroupConversationRequest.builder()
                .name("Team")
                .participantIds(List.of(ME))
                .build();

        var exception = assertThrows(AppException.class, () -> service.createGroup(request));

        assertEquals(ErrorCode.GROUP_MEMBERS_REQUIRED, exception.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void createGroup_unknownMember_rejectedAndNothingSaved() {
        var request = GroupConversationRequest.builder()
                .name("Team")
                .participantIds(List.of("u2", "ghost"))
                .build();

        var exception = assertThrows(AppException.class, () -> service.createGroup(request));

        assertEquals(ErrorCode.PARTICIPANT_NOT_FOUND, exception.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void createGroup_profileServiceAnswers400_treatedAsUnknownMember() {
        Request feignRequest = Request.create(
                Request.HttpMethod.GET, "/internal/users/x", Map.of(), null, StandardCharsets.UTF_8, null);
        when(profileClient.getProfile("bad")).thenThrow(new FeignException.BadRequest("bad", feignRequest, null, null));
        var request = GroupConversationRequest.builder()
                .name("Team")
                .participantIds(List.of("bad"))
                .build();

        var exception = assertThrows(AppException.class, () -> service.createGroup(request));

        assertEquals(ErrorCode.PARTICIPANT_NOT_FOUND, exception.getErrorCode());
    }

    /* ---------------------------------------------- addParticipants ---------------------------------------------- */

    @Test
    void addParticipants_addsNewMembersSkipsExisting_andDropsMemberCache() {
        when(repository.findById("c1")).thenReturn(Optional.of(group("c1", ME, "u2")));

        var response = service.addParticipants(
                "c1",
                AddParticipantsRequest.builder()
                        .participantIds(List.of("u2", "u3", "u4"))
                        .build());

        assertEquals(List.of(ME, "u2", "u3", "u4"), userIds(response));
        ArgumentCaptor<Conversation> saved = ArgumentCaptor.forClass(Conversation.class);
        verify(repository).save(saved.capture());
        assertEquals(4, saved.getValue().getParticipants().size());
        // otherwise u3/u4 could not read the messages for up to 5 minutes
        verify(cacheService).evict(ChatCacheKeys.participants("c1"));
    }

    @Test
    void addParticipants_everyoneAlreadyInGroup_changesNothing() {
        when(repository.findById("c1")).thenReturn(Optional.of(group("c1", ME, "u2")));

        var response = service.addParticipants(
                "c1",
                AddParticipantsRequest.builder()
                        .participantIds(List.of("u2", ME))
                        .build());

        assertEquals(List.of(ME, "u2"), userIds(response));
        verify(repository, never()).save(any());
        verify(cacheService, never()).evict(any());
    }

    @Test
    void addParticipants_nonMember_getsNotFound() {
        when(repository.findById("c1")).thenReturn(Optional.of(group("c1", "u2", "u3")));

        var exception = assertThrows(
                AppException.class,
                () -> service.addParticipants(
                        "c1",
                        AddParticipantsRequest.builder()
                                .participantIds(List.of("u9"))
                                .build()));

        assertEquals(ErrorCode.CONVERSATION_NOT_EXISTED, exception.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void addParticipants_directConversation_rejected() {
        Conversation direct = group("c1", ME, "u2");
        direct.setType("DIRECT");
        when(repository.findById("c1")).thenReturn(Optional.of(direct));

        var exception = assertThrows(
                AppException.class,
                () -> service.addParticipants(
                        "c1",
                        AddParticipantsRequest.builder()
                                .participantIds(List.of("u3"))
                                .build()));

        assertEquals(ErrorCode.NOT_GROUP_CONVERSATION, exception.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void addParticipants_unknownConversation_notFound() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        var exception = assertThrows(
                AppException.class,
                () -> service.addParticipants(
                        "nope",
                        AddParticipantsRequest.builder()
                                .participantIds(List.of("u3"))
                                .build()));

        assertEquals(ErrorCode.CONVERSATION_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void addParticipants_unknownUser_rejectedAndNothingSaved() {
        when(repository.findById("c1")).thenReturn(Optional.of(group("c1", ME)));

        var exception = assertThrows(
                AppException.class,
                () -> service.addParticipants(
                        "c1",
                        AddParticipantsRequest.builder()
                                .participantIds(List.of("u2", "ghost"))
                                .build()));

        assertEquals(ErrorCode.PARTICIPANT_NOT_FOUND, exception.getErrorCode());
        verify(repository, never()).save(any());
        verify(cacheService, never()).evict(any());
    }
}
