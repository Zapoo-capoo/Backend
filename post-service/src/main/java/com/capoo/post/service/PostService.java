package com.capoo.post.service;

import com.capoo.post.dto.PageResponse;
import com.capoo.post.dto.request.PostRequest;
import com.capoo.post.dto.response.PostResponse;
import com.capoo.post.entity.Post;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService {
    PostResponse createPost(PostRequest postRequest);

    PostResponse createPostWithMedia(String content, MultipartFile file);

    void pulishCreatedPostEvent(Post post);

    PageResponse<PostResponse> getMyPosts(int page, int size);

    PageResponse<PostResponse> getFriendsPosts(int page, int size);

    void deletePost(String postId);

    /** Posts of the friends of the current user (and the user's own) that match the text, best first */
    List<PostResponse> searchPosts(String query, int size);
}
