package com.capoo.chat.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.capoo.chat.dto.request.AddParticipantsRequest;
import com.capoo.chat.dto.request.ConversationRequest;
import com.capoo.chat.dto.request.GroupConversationRequest;
import com.capoo.chat.dto.request.UpdateParticipantRequest;
import com.capoo.chat.dto.response.ConversationResponse;
import com.capoo.chat.service.ConversationService;
import com.capoo.dto.ApiResponse;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@RequestMapping("conversations")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationController {
    ConversationService conversationService;

    @PostMapping("/create")
    ApiResponse<ConversationResponse> createConversation(@RequestBody @Valid ConversationRequest request) {
        return ApiResponse.<ConversationResponse>builder()
                .result(conversationService.create(request))
                .build();
    }

    @PostMapping("/group/create")
    ApiResponse<ConversationResponse> createGroup(@RequestBody @Valid GroupConversationRequest request) {
        return ApiResponse.<ConversationResponse>builder()
                .result(conversationService.createGroup(request))
                .build();
    }

    @PostMapping("/{conversationId}/participants/add")
    ApiResponse<ConversationResponse> addParticipants(
            @PathVariable("conversationId") String conversationId, @RequestBody @Valid AddParticipantsRequest request) {
        return ApiResponse.<ConversationResponse>builder()
                .result(conversationService.addParticipants(conversationId, request))
                .build();
    }

    @PostMapping("/{conversationId}/seen")
    ApiResponse<Boolean> markSeen(@PathVariable("conversationId") String conversationId) {
        conversationService.markSeen(conversationId);
        return ApiResponse.<Boolean>builder().result(true).build();
    }

    @GetMapping("/{conversationId}/seen")
    ApiResponse<Boolean> hasSeen(@PathVariable("conversationId") String conversationId) {
        return ApiResponse.<Boolean>builder()
                .result(conversationService.hasSeen(conversationId))
                .build();
    }

    @GetMapping("/my-conversations")
    ApiResponse<List<ConversationResponse>> myConversations() {
        return ApiResponse.<List<ConversationResponse>>builder()
                .result(conversationService.myConversations())
                .build();
    }

    @PostMapping("/participants/update")
    ApiResponse<Boolean> updateParticipant(@RequestBody UpdateParticipantRequest request) {
        conversationService.updateParticipant(request);
        return ApiResponse.<Boolean>builder().result(true).build();
    }
}
