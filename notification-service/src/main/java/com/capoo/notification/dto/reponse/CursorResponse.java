package com.capoo.notification.dto.reponse;

import java.util.Collections;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CursorResponse<T> {
    int size;
    boolean hasMore;

    /** Pass this value as the {@code cursor} of the next request to get the following (older) page. */
    String nextCursor;

    @Builder.Default
    private List<T> data = Collections.emptyList();
}
