package com.common.storage.controller;

import com.common.storage.exception.MaxUploadSizeExceedException;
import com.common.storage.model.dto.BatchDeleteDTO;
import com.common.storage.model.dto.FileDTO;
import com.common.storage.model.dto.ResponseModel;
import com.common.storage.service.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.EncoderException;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@Validated
@RequestMapping(value = "/internal/files")
public class InternalFileController {

    @Autowired
    private IFileService fileService;

    @Operation(description = "Internal upload file", summary = "Upload file for internal callers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad request"),
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

    @Operation(description = "Internal upload files", summary = "Upload files for internal callers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PostMapping(value = "/upload-files", consumes = {"multipart/form-data", "application/json"})
    public ResponseEntity<ResponseModel<List<FileDTO>>> uploadFiles(
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam("files") @Valid MultipartFile[] multipartFiles)
            throws EncoderException, MaxUploadSizeExceedException, IOException {
        List<FileDTO> response = this.fileService.uploadFiles(multipartFiles, description);
        return new ResponseEntity<>(new ResponseModel<>(true, response), HttpStatus.OK);
    }

    @Operation(description = "Internal get file info by id", summary = "Get file info by id for internal callers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseModel<FileDTO>> getFileById(@PathVariable String id)
            throws MethodArgumentTypeMismatchException {
        return new ResponseEntity<>(new ResponseModel<>(true, this.fileService.getFileById(id)), HttpStatus.OK);
    }

    @Operation(description = "Internal delete file by id", summary = "Delete file by id for internal callers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseModel<Void>> deleteFile(@PathVariable String id)
            throws MethodArgumentTypeMismatchException, IOException {
        log.info("internal deleteFile: {}", id);
        this.fileService.deleteFile(id);
        return new ResponseEntity<>(new ResponseModel<>(true, null), HttpStatus.OK);
    }
}
