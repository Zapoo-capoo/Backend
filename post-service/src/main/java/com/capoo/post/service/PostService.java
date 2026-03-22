package com.capoo.post.service;

import com.capoo.post.dto.PageResponse;
import com.capoo.post.dto.request.PostRequest;
import com.capoo.post.dto.response.PostResponse;
import com.capoo.post.dto.response.UserProfileReponse;
import com.capoo.post.entity.Post;
import com.capoo.post.mapper.PostMapper;
import com.capoo.post.repository.PostRepository;
import com.capoo.post.repository.httpClient.ProfileClient;
import com.capoo.post.repository.httpClient.FileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PostService {
    PostRepository postRepository;
    PostMapper postMapper;
    DateTimeFormater dateTimeFormater;
    ProfileClient profileClient;
    FileClient fileClient;
    public PostResponse createPost(PostRequest postRequest) {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();

        Post post= Post.builder()
                .content(postRequest.getContent())
                .userId(auth.getName())
                .createdDate(Instant.now())
                .modifiedDate(Instant.now())
                .build();
        postRepository.save(post);
        return postMapper.toPostResponse(post);
    }

    public PostResponse createPostWithMedia(String content, MultipartFile file) {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        String mediaUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                var resp = fileClient.uploadMedia(file);
                if (resp != null && resp.getResult() != null) {
                    mediaUrl = resp.getResult().getUrl();
                }
            } catch (Exception ex) {
                log.warn("Failed to upload media: {}", ex.getMessage());
            }
        }

        Post post= Post.builder()
                .content(content)
                .userId(auth.getName())
                .mediaUrl(mediaUrl)
                .createdDate(Instant.now())
                .modifiedDate(Instant.now())
                .build();
        postRepository.save(post);
        var postResponse = postMapper.toPostResponse(post);
        postResponse.setMediaUrl(mediaUrl);
        return postResponse;
    }

    public PageResponse<PostResponse> getMyPosts(int page, int size){
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        String userId=auth.getName();
        UserProfileReponse userProfile=null;
        try {
            var response = profileClient.getUserProfileByUserId(userId);
            if (response != null) userProfile = response.getResult();
        } catch (Exception e) {
            // Don't fail the entire request if profile service is down; log and continue with fallback username
            log.warn("Failed to fetch user profile for userId {}: {}", userId, e.getMessage());
        }

        String userName = userProfile != null && userProfile.getUsername() != null ? userProfile.getUsername() : "Unknown";
        String avatar = userProfile != null ? userProfile.getAvatar() : null;


        Sort sort = Sort.by(Sort.Direction.DESC,"createdDate");
        Pageable pageable = PageRequest.of(page-1,size,sort);

        Page<Post> postPage=postRepository.findAllByUserId(userId,pageable);
        var postList=postPage.getContent()
                .stream()
                .map(post -> {
                    var postResponse=postMapper.toPostResponse(post);
                    postResponse.setCreated(dateTimeFormater.format(post.getCreatedDate()));
                    postResponse.setUsername(userName);
                    postResponse.setAvatar(avatar);
                    postResponse.setMediaUrl(post.getMediaUrl());
                    return postResponse;
                }).toList();
        return PageResponse.<PostResponse>builder()
                .currentPage(page)
                .pageSize(postPage.getSize())
                .totalPages(postPage.getTotalPages())
                .totalElements(postPage.getTotalElements())
                .data(postList)
                .build();
    }

    public PageResponse<PostResponse> getFriendsPosts(int page, int size) {
        // fetch friends via profile service
        List<UserProfileReponse> friends = null;
        try {
            var resp = profileClient.getMyFriends();
            if (resp != null && resp.getResult() != null) friends = resp.getResult();
        } catch (Exception ex) {
            log.warn("Failed to fetch friends from profile service: {}", ex.getMessage());
        }
        if (friends == null || friends.isEmpty()) {
            return PageResponse.<PostResponse>builder()
                    .currentPage(page)
                    .pageSize(0)
                    .totalPages(0)
                    .totalElements(0)
                    .data(List.of())
                    .build();
        }

        List<String> friendUserIds = friends.stream().map(UserProfileReponse::getUserId).collect(Collectors.toList());

        // build map of userId -> username/avatar to enrich posts without additional calls
        Map<String, UserProfileReponse> profileMap = friends.stream().collect(Collectors.toMap(UserProfileReponse::getUserId, p -> p));

        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Pageable pageable = PageRequest.of(page-1, size, sort);

        Page<Post> postPage = postRepository.findAllByUserIdIn(friendUserIds, pageable);

        var postList = postPage.getContent().stream().map(post -> {
            var postResponse = postMapper.toPostResponse(post);
            postResponse.setCreated(dateTimeFormater.format(post.getCreatedDate()));
            var prof = profileMap.get(post.getUserId());
            if (prof != null) {
                postResponse.setUsername(prof.getUsername());
                postResponse.setAvatar(prof.getAvatar());
            } else {
                postResponse.setUsername("Unknown");
            }
            postResponse.setMediaUrl(post.getMediaUrl());
            return postResponse;
        }).toList();

        return PageResponse.<PostResponse>builder()
                .currentPage(page)
                .pageSize(postPage.getSize())
                .totalPages(postPage.getTotalPages())
                .totalElements(postPage.getTotalElements())
                .data(postList)
                .build();
    }

}
