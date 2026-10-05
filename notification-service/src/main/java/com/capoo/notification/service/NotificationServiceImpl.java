package com.capoo.notification.service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.capoo.event.dto.NotificationEvent;
import com.capoo.notification.dto.reponse.CursorResponse;
import com.capoo.notification.dto.reponse.NotificationResponse;
import com.capoo.notification.dto.reponse.UserProfileReponse;
import com.capoo.notification.dto.request.EmailUserRequest;
import com.capoo.notification.dto.request.Recipient;
import com.capoo.notification.entity.Notification;
import com.capoo.notification.entity.NotificationType;
import com.capoo.notification.entity.TargetType;
import com.capoo.notification.exception.AppException;
import com.capoo.notification.exception.ErrorCode;
import com.capoo.notification.repository.httpClient.ProfileClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final int MAX_PAGE_SIZE = 50;

    private final ProfileClient profileClient;
    private final EmailService emailService;
    private final MongoTemplate mongoTemplate;

    @Override
    public void createFromEvent(NotificationEvent event) {
        NotificationType type = parseType(event.getType());
        if (type == null || event.getRecipientIds() == null) {
            log.warn("Ignoring notification event without a known type or recipients: {}", event);
            return;
        }
        TargetType targetType = parseTargetType(event.getTargetType());
        String dedupKey = dedupKey(type, event);
        Instant now = Instant.now();

        for (String recipientId : event.getRecipientIds()) {
            // Nobody is notified about their own action
            if (recipientId == null || recipientId.equals(event.getActorId())) continue;
            Query query = new Query(Criteria.where("recipientId").is(recipientId).and("dedupKey").is(dedupKey));
            // Upsert: the same event (a Kafka redelivery, a request sent again after a reject) never makes a second
            // notification, it brings the existing one back to the top as unread
            Update update = new Update()
                    .set("type", type)
                    .set("actorId", event.getActorId())
                    .set("actorName", event.getActorName())
                    .set("actorAvatar", event.getActorAvatar())
                    .set("targetType", targetType)
                    .set("targetId", event.getTargetId())
                    .set("data", event.getData())
                    .set("read", false)
                    .set("readAt", null)
                    .set("createdAt", now);
            try {
                mongoTemplate.upsert(query, update, Notification.class);
            } catch (DuplicateKeyException e) {
                // Two consumers upserted the same notification at the same moment, the other one won
                log.debug("Notification {} for {} already exists", dedupKey, recipientId);
            }
        }
    }

    @Override
    public CursorResponse<NotificationResponse> getMyNotifications(String cursor, int size, boolean unreadOnly) {
        int limit = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Criteria criteria = Criteria.where("recipientId").is(currentUserId());
        if (unreadOnly) criteria.and("read").is(false);
        if (cursor != null && !cursor.isBlank()) {
            try {
                criteria.and("createdAt").lt(Instant.parse(cursor));
            } catch (DateTimeParseException e) {
                throw new AppException(ErrorCode.INVALID_CURSOR);
            }
        }
        // One more than asked, to know whether there is a next page
        Query query = new Query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(limit + 1);
        List<Notification> found = mongoTemplate.find(query, Notification.class);

        boolean hasMore = found.size() > limit;
        List<Notification> page = hasMore ? found.subList(0, limit) : found;
        return CursorResponse.<NotificationResponse>builder()
                .size(page.size())
                .hasMore(hasMore)
                .nextCursor(
                        hasMore ? page.get(page.size() - 1).getCreatedAt().toString() : null)
                .data(page.stream().map(NotificationResponse::from).toList())
                .build();
    }

    @Override
    public long countMyUnread() {
        return mongoTemplate.count(
                new Query(Criteria.where("recipientId").is(currentUserId()).and("read").is(false)),
                Notification.class);
    }

    @Override
    public void markAsRead(String notificationId) {
        Query query = new Query(Criteria.where("_id").is(notificationId).and("recipientId").is(currentUserId()));
        var result = mongoTemplate.updateFirst(
                query, new Update().set("read", true).set("readAt", Instant.now()), Notification.class);
        if (result.getMatchedCount() == 0) throw new AppException(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Override
    public void markAllAsRead() {
        mongoTemplate.updateMulti(
                new Query(Criteria.where("recipientId").is(currentUserId()).and("read").is(false)),
                new Update().set("read", true).set("readAt", Instant.now()),
                Notification.class);
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // What makes two events "the same" for one recipient
    private String dedupKey(NotificationType type, NotificationEvent event) {
        String subject =
                switch (type) {
                    case FRIEND_REQUEST, FRIEND_ACCEPTED -> event.getActorId();
                    default -> event.getTargetId();
                };
        return type + ":" + subject;
    }

    private NotificationType parseType(String type) {
        try {
            return type == null ? null : NotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TargetType parseTargetType(String targetType) {
        try {
            return targetType == null ? TargetType.NONE : TargetType.valueOf(targetType);
        } catch (IllegalArgumentException e) {
            return TargetType.NONE;
        }
    }

    @Override
    @KafkaListener(topics = "post_created_event", groupId = "post-group")
    public void handlePostCreatedEvent(String userId) {

        log.info("Received post created event with userId: {}", userId);
        // TODO: xử lý logic ở đây
        // get user profile by userId
        UserProfileReponse sender = null;
        try {
            var resp = profileClient.getUserProfileByUserId(userId);
            if (resp != null) sender = resp.getResult();
        } catch (Exception ex) {
            log.warn("Failed to fetch profile for current user {}: {}", userId, ex.getMessage());
        }
        // get friends of user
        List<UserProfileReponse> friends = null;
        try {
            var resp = profileClient.getAllFriendById(userId);

            if (resp != null && resp.getResult() != null) friends = resp.getResult();
        } catch (Exception ex) {
            log.warn("Failed to fetch friends from profile service: {}", ex.getMessage());
        }
        // send email notification to friends
        if (sender != null && friends != null) {
            for (UserProfileReponse friend : friends) {
                emailService.sendEmail(EmailUserRequest.builder()
                        .to(Recipient.builder()
                                .email(friend.getEmail())
                                .name(friend.getUsername())
                                .build())
                        .subject("Your friend " + sender.getUsername() + " just created a new post!")
                        .htmlContent("Hi " + friend.getUsername() + ",<br><br>" + "Your friend "
                                + sender.getUsername()
                                + " just created a new post. Check it out on our platform!<br><br>"
                                + "Best regards,<br>"
                                + "Zapoo Team")
                        .build());
            }
        }
    }
}
