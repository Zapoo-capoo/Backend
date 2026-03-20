package com.capoo.post.service;

import com.capoo.post.dto.request.PostRequest;
import com.capoo.post.dto.response.PostResponse;
import com.capoo.post.entity.Post;
import com.capoo.post.mapper.PostMapper;
import com.capoo.post.repository.PostRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {
    PostRepository postRepository;
    PostMapper postMapper;

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
    public List<PostResponse> getMyPosts(){
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        String userId=auth.getName();

        return postRepository.findAllByUserId(userId)
                .stream()
                .map(postMapper::toPostResponse)
                .toList();
    }

}
