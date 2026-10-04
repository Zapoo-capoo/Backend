package com.common.storage.service;

import com.common.storage.model.dto.StreamingMediaDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

public interface IFileMediaDeliveryService {



    StreamingMediaDTO getWebFile(String fileName, String rangeHeader) throws IOException;

    StreamingMediaDTO getThumbnailImage(String fileName) throws IOException;
    StreamingMediaDTO downloadFile(String fileName, String rangeHeader) throws IOException;

    ResponseEntity<StreamingResponseBody> streamVideoV2(String fileName, String quality, String rangeHeader)
            throws IOException;

}
