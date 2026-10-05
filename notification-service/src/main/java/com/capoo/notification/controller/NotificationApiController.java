package com.capoo.notification.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.capoo.dto.ApiResponse;
import com.capoo.notification.dto.reponse.CursorResponse;
import com.capoo.notification.dto.reponse.NotificationResponse;
import com.capoo.notification.service.NotificationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationApiController {
    NotificationService notificationService;

    @GetMapping
    ApiResponse<CursorResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        return ApiResponse.<CursorResponse<NotificationResponse>>builder()
                .result(notificationService.getMyNotifications(cursor, size, unreadOnly))
                .build();
    }

    @GetMapping("/unread-count")
    ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.<Map<String, Long>>builder()
                .result(Map.of("count", notificationService.countMyUnread()))
                .build();
    }

    @PostMapping("/{id}/read")
    ApiResponse<Void> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/read-all")
    ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.<Void>builder().build();
    }
}
