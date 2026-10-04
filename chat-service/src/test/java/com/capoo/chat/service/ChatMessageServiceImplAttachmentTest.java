package com.capoo.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.capoo.chat.dto.request.ChatMessageRequest;
import com.capoo.chat.dto.response.UserProfileResponse;
import com.capoo.chat.entity.Attachment;
import com.capoo.chat.entity.ChatMessage;
import com.capoo.chat.entity.Conversation;
import com.capoo.chat.entity.ParticipantInfo;
import com.capoo.chat.exception.AppException;
import com.capoo.chat.exception.ErrorCode;
import com.capoo.chat.mapper.ChatMessageMapperImpl;
import com.capoo.chat.repository.ChatMessageRepository;
import com.capoo.chat.repository.ConversationRepository;
import com.capoo.chat.repository.WebSocketSessionRepository;
import com.capoo.chat.repository.httpclient.ProfileClient;
import com.capoo.chat.service.cache.ICacheService;
import com.capoo.dto.ApiResponse;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class ChatMessageServiceImplAttachmentTest {
    private static final String ME = "u-me";

    private ChatMessageRepository chatMessageRepository;
    private ConversationRepository conversationRepository;
    private AttachmentResolver attachmentResolver;
    private BroadcastOperations room;
    private ChatMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        conversationRepository = mock(ConversationRepository.class);
        attachmentResolver = mock(AttachmentResolver.class);
        ProfileClient profileClient = mock(ProfileClient.class);
        SocketIOServer socketIOServer = mock(SocketIOServer.class);
        room = mock(BroadcastOperations.class);
        when(socketIOServer.getRoomOperations(any())).thenReturn(room);
        WebSocketSessionRepository sessions = mock(WebSocketSessionRepository.class);
        when(sessions.findAllByUserIdIn(any())).thenReturn(List.of());
        when(profileClient.getProfile(ME))
                .thenReturn(ApiResponse.<UserProfileResponse>builder()
                        .result(UserProfileResponse.builder()
                                .userId(ME)
                                .username("me")
                                .build())
                        .build());
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId("generated-id");
            return saved;
        });
        List<ParticipantInfo> members = new ArrayList<>();
        members.add(ParticipantInfo.builder().userId(ME).build());
        members.add(ParticipantInfo.builder().userId("u2").build());
        when(conversationRepository.findById("c1"))
                .thenReturn(Optional.of(
                        Conversation.builder().id("c1").participants(members).build()));

        service = new ChatMessageServiceImpl(
                chatMessageRepository,
                profileClient,
                socketIOServer,
                sessions,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                new ChatMessageMapperImpl(),
                conversationRepository,
                attachmentResolver,
                mock(ICacheService.class));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(ME, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Attachment image(String id) {
        return Attachment.builder()
                .fileId(id)
                .type(Attachment.IMAGE)
                .url("http://gw/web/" + id + ".webp")
                .thumbnailUrl("http://gw/thumbnail/" + id + ".webp")
                .name(id + ".png")
                .size(10L)
                .build();
    }

    private Attachment video(String id) {
        return Attachment.builder()
                .fileId(id)
                .type(Attachment.VIDEO)
                .url("http://gw/stream-video/" + id + ".mp4")
                .name(id + ".mp4")
                .size(99L)
                .build();
    }

    private ChatMessageRequest request(String text, String... fileIds) {
        return ChatMessageRequest.builder()
                .conversationId("c1")
                .message(text)
                .fileIds(List.of(fileIds))
                .build();
    }

    @Test
    void messageWithImagesAndAVideo_isSavedAndReturnedWithAllAttachments() {
        when(attachmentResolver.resolve(any())).thenReturn(List.of(image("i1"), video("v1"), image("i2")));

        var response = service.create(request("look", "i1", "v1", "i2"));

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(saved.capture());
        assertEquals(3, saved.getValue().getAttachments().size());
        assertEquals(3, response.getAttachments().size());
        assertEquals(
                List.of("i1", "v1", "i2"),
                response.getAttachments().stream().map(Attachment::getFileId).toList());
        assertEquals("look", response.getMessage());
    }

    @Test
    void imgUrl_mirrorsTheFirstImage_notAVideo() {
        when(attachmentResolver.resolve(any())).thenReturn(List.of(video("v1"), image("i1"), image("i2")));

        var response = service.create(request("x", "v1", "i1", "i2"));

        assertEquals("http://gw/web/i1.webp", response.getImgUrl());
    }

    @Test
    void onlyVideos_leaveImgUrlEmpty() {
        when(attachmentResolver.resolve(any())).thenReturn(List.of(video("v1")));

        var response = service.create(request("", "v1"));

        assertNull(response.getImgUrl());
        assertEquals(1, response.getAttachments().size());
    }

    @Test
    void attachmentsOnly_noText_isAllowed() {
        when(attachmentResolver.resolve(any())).thenReturn(List.of(image("i1")));

        assertDoesNotThrow(() -> service.create(request(null, "i1")));
    }

    @Test
    void textOnly_hasAnEmptyAttachmentList() {
        when(attachmentResolver.resolve(any())).thenReturn(List.of());

        var response = service.create(request("hello"));

        assertEquals("hello", response.getMessage());
        assertNotNull(response.getAttachments());
        assertTrue(response.getAttachments().isEmpty());
    }

    @Test
    void neitherTextNorAttachments_isRejected_andNothingIsSaved() {
        when(attachmentResolver.resolve(any())).thenReturn(List.of());

        var blank = assertThrows(AppException.class, () -> service.create(request("   ")));
        var missing = assertThrows(AppException.class, () -> service.create(request(null)));

        assertEquals(ErrorCode.MESSAGE_EMPTY, blank.getErrorCode());
        assertEquals(ErrorCode.MESSAGE_EMPTY, missing.getErrorCode());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void invalidAttachments_rejectTheMessage_nothingSavedOrSent() {
        when(attachmentResolver.resolve(any())).thenThrow(new AppException(ErrorCode.ATTACHMENT_NOT_FOUND));

        var exception = assertThrows(AppException.class, () -> service.create(request("hi", "ghost")));

        assertEquals(ErrorCode.ATTACHMENT_NOT_FOUND, exception.getErrorCode());
        verify(chatMessageRepository, never()).save(any());
        verify(conversationRepository, never()).markUnread(any(), any());
        verifyNoInteractions(room);
    }

    @Test
    void attachmentsAreCheckedOnlyForMembers() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("stranger", null));

        var exception = assertThrows(AppException.class, () -> service.create(request("hi", "i1")));

        assertEquals(ErrorCode.CONVERSATION_NOT_EXISTED, exception.getErrorCode());
        verifyNoInteractions(attachmentResolver);
    }

    @Test
    void socketPayload_carriesTheAttachments() {
        when(attachmentResolver.resolve(any())).thenReturn(List.of(image("i1"), video("v1")));

        service.create(request("hi", "i1", "v1"));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(room, atLeastOnce()).sendEvent(eq("message"), payload.capture());
        String json = payload.getValue().toString();
        assertTrue(json.contains("\"attachments\""));
        assertTrue(json.contains("stream-video/v1.mp4"));
        assertTrue(json.contains("web/i1.webp"));
    }
}
