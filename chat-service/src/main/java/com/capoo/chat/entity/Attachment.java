package com.capoo.chat.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** An image or video attached to a chat message. The file itself lives in storage-service. */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attachment {
    public static final String IMAGE = "IMAGE";
    public static final String VIDEO = "VIDEO";

    /** Id of the file in storage-service. */
    String fileId;

    /** {@link #IMAGE} or {@link #VIDEO}. */
    String type;

    /** Link to display or play the file. */
    String url;

    /** Small preview of an image, or a poster frame of a video. */
    String thumbnailUrl;

    String name;
    Long size;
}
