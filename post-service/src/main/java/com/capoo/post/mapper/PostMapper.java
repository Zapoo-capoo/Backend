package com.capoo.post.mapper;

import com.capoo.post.dto.response.PostResponse;
import com.capoo.post.entity.Post;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {
    PostResponse toPostResponse(Post post);
}
