package com.common.storage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamingMediaDTO {

    private StreamingResponseBody response_body;
    private String media_type;
    private HttpHeaders headers;
}
