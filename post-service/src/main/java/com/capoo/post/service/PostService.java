package com.capoo.post.service;

import com.capoo.post.dto.PageResponse;
import com.capoo.post.dto.request.PostRequest;
import com.capoo.post.dto.response.PostResponse;
import com.capoo.post.dto.response.UserProfileReponse;
import com.capoo.post.entity.Post;
import com.capoo.post.mapper.PostMapper;
import com.capoo.post.repository.PostRepository;
import com.capoo.post.repository.httpClient.ProfileClient;
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

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PostService {
    PostRepository postRepository;
    PostMapper postMapper;
    DateTimeFormater dateTimeFormater;
    ProfileClient profileClient;
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


        Sort sort = Sort.by(Sort.Direction.DESC,"createdDate");
        Pageable pageable = PageRequest.of(page-1,size,sort);

        Page<Post> postPage=postRepository.findAllByUserId(userId,pageable);
        var postList=postPage.getContent()
                .stream()
                .map(post -> {
                    var postResponse=postMapper.toPostResponse(post);
                    postResponse.setCreated(dateTimeFormater.format(post.getCreatedDate()));
                    postResponse.setUsername(userName);
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
