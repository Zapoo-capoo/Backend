package com.capoo.post.search;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Asks the indexer to bring the search index in line with one post. Small on purpose: the post is read from Mongo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostSearchEvent {
    public enum Action {
        INDEX,
        DELETE
    }

    String postId;
    Action action;
}
