package com.common.storage.controller;

import com.common.storage.exception.MaxUploadSizeExceedException;
import com.common.storage.model.dto.*;
import com.common.storage.service.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ws.schild.jave.EncoderException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@Validated
@RequestMapping(value = "/files")
public class FileController {

    @Value("${limit-size-in-mb.image:20}")
    private int MAX_IMAGE_SIZE;

    @Value("${limit-size-in-mb.audio:5}")
    private int MAX_AUDIO_SIZE;

    @Value("${limit-size-in-mb.office:5}")
    private int MAX_FILE_SIZE;

    @Value("${limit-size-in-mb.video:100}")
    private int MAX_VIDEO_SIZE;

    @Autowired
    private IFileService fileService;
    @Operation(description = "Upload file", summary = "Upload file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PostMapping(value = "/upload-file", consumes = {"multipart/form-data", "application/json"})
    public ResponseEntity<ResponseModel<FileDTO>> uploadFile(
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam("file") @Valid MultipartFile multipartFile)
            throws IOException, MaxUploadSizeExceedException, EncoderException {
        FileDTO response = this.fileService.uploadFile(multipartFile, description);
        return new ResponseEntity<>(new ResponseModel<>(true, response), HttpStatus.OK);
    }
    @Operation(description = "Upload files", summary = "Upload files")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PostMapping(value = "/upload-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseModel<List<FileDTO>>> uploadFiles(
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestPart("files") @Valid MultipartFile[] multipartFiles)
            throws EncoderException, MaxUploadSizeExceedException, IOException {
        List<FileDTO> response = this.fileService.uploadFiles(multipartFiles, description);
        return new ResponseEntity<>(new ResponseModel<>(true, response), HttpStatus.OK);
    }
    @Operation(description = "Get file info by id", summary = "Get file info by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseModel<FileDTO>> getFileById(@PathVariable String id)
            throws MethodArgumentTypeMismatchException {
        return new ResponseEntity<>(new ResponseModel<>(true, this.fileService.getFileById(id)), HttpStatus.OK);
    }
    @GetMapping("/size-configuration")
    public ResponseEntity<ResponseModel<SizeLimitedDTO>> getSizeConfiguration() {
        SizeLimitedDTO sizeLimitedDTO = new SizeLimitedDTO();
        sizeLimitedDTO.setImage(this.MAX_IMAGE_SIZE);
        sizeLimitedDTO.setVideo(this.MAX_VIDEO_SIZE);
        sizeLimitedDTO.setAudio(this.MAX_AUDIO_SIZE);
        sizeLimitedDTO.setFile(this.MAX_FILE_SIZE);
        return new ResponseEntity<>(new ResponseModel<>(true, sizeLimitedDTO), HttpStatus.OK);
    }


    @Operation(description = "Delete file by id", summary = "Delete file by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseModel<Void>> deleteFile(@PathVariable String id, HttpServletRequest request)
            throws MethodArgumentTypeMismatchException, IOException {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        log.info("deleteFile: {} from origin: {} - referer: {}", id, origin, referer);
        this.fileService.deleteFile(id);
        return new ResponseEntity<>(new ResponseModel<>(true, null), HttpStatus.OK);
    }

    @DeleteMapping("/batch")
    public ResponseEntity<ResponseModel<Void>> deleteFiles(@RequestBody BatchDeleteDTO fileIds) throws IOException {
        this.fileService.deleteFiles(fileIds);
        return new ResponseEntity<>(new ResponseModel<>(true, null), HttpStatus.OK);
    }

    @Operation(description = "Get file web", summary = "{file} = file_id + .webp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping(value = "/web/{file}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<StreamingResponseBody> getWebFile(
            @PathVariable(value = "file") String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {
        StreamingMediaDTO resourceDto = this.fileService.getWebFile(fileName, rangeHeader);
        HttpHeaders headers = resourceDto.getHeaders();
        headers.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
        return new ResponseEntity<>(resourceDto.getResponse_body(), headers, HttpStatus.OK);
    }

    @Operation(description = "Get file thumbnail", summary = "{file} = file_id + .webp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping(value = "/thumbnail/{file}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<StreamingResponseBody> getThumbnailByFileName(
            @PathVariable(value = "file") String fileName) throws IOException {
        StreamingMediaDTO resourceDto = this.fileService.getThumbnailImage(fileName);
        HttpHeaders headers = resourceDto.getHeaders();
        headers.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
        return new ResponseEntity<>(resourceDto.getResponse_body(), headers, HttpStatus.OK);
    }
    @Operation(description = "Get file origin", summary = "{file} = file_id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/download/{file}")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @PathVariable(value = "file") String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {
        StreamingMediaDTO resourceDto = this.fileService.downloadFile(fileName, rangeHeader);
        StreamingResponseBody responseBody = resourceDto.getResponse_body();
        HttpStatus status = resourceDto.getHeaders().containsKey(HttpHeaders.CONTENT_RANGE)
                ? HttpStatus.PARTIAL_CONTENT
                : HttpStatus.OK;
        return ResponseEntity.status(status).headers(resourceDto.getHeaders()).body(responseBody);
    }

    @Operation(description = "Stream video", summary = "{file} = file_id + extension")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/stream-video/{file}")
    public ResponseEntity<StreamingResponseBody> streamVideo(
            @PathVariable(value = "file") String fileName,
            @RequestParam(value = "quality", defaultValue = "") String quality,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {
        return this.fileService.streamVideoV2(fileName, quality, rangeHeader);
    }


}
