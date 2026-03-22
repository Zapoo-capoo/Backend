package com.capoo.post.repository.httpClient;

import com.capoo.post.configuration.AuthenticationRequestInterceptor;
import com.capoo.post.dto.ApiResponse;
import com.capoo.post.dto.response.FileReponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "file-service", url = "http://localhost:8084", configuration = { AuthenticationRequestInterceptor.class })
public interface FileClient {
    @PostMapping(value = "/file/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<FileReponse> uploadMedia(@RequestPart("file") MultipartFile file);
}

