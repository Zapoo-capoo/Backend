package com.capoo.notification.service;

import com.capoo.notification.dto.reponse.UserProfileReponse;
import com.capoo.notification.dto.request.EmailUserRequest;
import com.capoo.notification.dto.request.Recipient;
import com.capoo.notification.repository.httpClient.ProfileClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final ProfileClient profileClient;
    private final EmailService emailService;
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
                        .htmlContent("Hi " + friend.getUsername() + ",<br><br>" +
                                "Your friend " + sender.getUsername() + " just created a new post. Check it out on our platform!<br><br>" +
                                "Best regards,<br>" +
                                "Zapoo Team")
                        .build());
            }
        }
    }
}
