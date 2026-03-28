package com.capoo.file.service;

import com.capoo.file.dto.FileInfo;
import com.capoo.file.dto.reponse.FileData;
import com.capoo.file.dto.reponse.FileReponse;
import com.capoo.file.entity.FileMgmt;
import com.capoo.file.exception.AppException;
import com.capoo.file.exception.ErrorCode;
import com.capoo.file.mapper.FileMgmtMapper;
import com.capoo.file.repository.FileMgmtRepository;
import com.capoo.file.repository.FileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileService {
    FileMgmtRepository fileMgmtRepository;
    FileRepository fileRepository;
    FileMgmtMapper fileMgmtMapper;
    public FileReponse uploadFile(MultipartFile file) {
        //store file to local
        FileInfo fileInfo= null;
        try {
            fileInfo = fileRepository.store(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //Create fileMgmt and save to db
        var fileMgmt=fileMgmtMapper.toFileMgmt(fileInfo);
        String userId= SecurityContextHolder.getContext().getAuthentication().getName();
        fileMgmt.setOwnerId(userId);

        fileMgmt= fileMgmtRepository.save(fileMgmt);
        return FileReponse.builder()
                .url(fileInfo.getUrl())
                .originalFileName(file.getOriginalFilename())
                .build();
    }
    public FileData downloadFile(String fileId) {
        FileMgmt fileMgmt=fileMgmtRepository.findById(fileId).orElseThrow(
                ()-> new AppException(ErrorCode.FILE_NOT_FOUND)
        );
        Resource resource= null;
        try {
            resource = fileRepository.read(fileMgmt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new FileData(fileMgmt.getContentType(), resource);
    }
    public void deleteFile(String fileId) {
        // 1. Tìm file trong DB
        FileMgmt fileMgmt = fileMgmtRepository.findById(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        // 2. Check quyền (chỉ owner được xóa)
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!Objects.equals(fileMgmt.getOwnerId(), userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }


        // 3. Xóa file vật lý
        try {
            fileRepository.delete(fileMgmt);
        } catch (IOException e) {
            throw new RuntimeException("Delete file failed", e);
        }

        // 4. Xóa metadata trong DB
        fileMgmtRepository.delete(fileMgmt);
    }
}
