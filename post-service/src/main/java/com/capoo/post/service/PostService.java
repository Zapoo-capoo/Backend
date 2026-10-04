package com.capoo.post.service;

import com.capoo.post.dto.PageResponse;
import com.capoo.post.dto.request.PostRequest;
import com.capoo.post.dto.response.PostResponse;
import com.capoo.post.entity.Post;
import org.springframework.web.multipart.MultipartFile;

public interface PostService {
    PostResponse createPost(PostRequest postRequest);

    PostResponse createPostWithMedia(String content, MultipartFile file);

    void pulishCreatedPostEvent(Post post);

    PageResponse<PostResponse> getMyPosts(int page, int size);

    PageResponse<PostResponse> getFriendsPosts(int page, int size);

    void deletePost(String postId);
}
