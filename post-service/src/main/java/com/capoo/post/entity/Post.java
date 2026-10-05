package com.capoo.post.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Getter
@Setter
@Builder
@Document(value = "post")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Post {
    @MongoId
    String id;
    String userId;
    String content;
    // Id of the file in storage-service (not a link: the host of the link depends on where the app runs)
    String mediaId;

    // IMAGE, VIDEO or FILE
    String mediaType;
    Instant createdDate;
    Instant modifiedDate;
}
