package com.k.medtour.domain.file.service;

import com.k.medtour.domain.file.dto.FileDownloadResponse;
import com.k.medtour.domain.file.dto.FileUploadResponse;
import com.k.medtour.domain.file.entity.FileEntity;
import com.k.medtour.domain.file.enums.FileCategory;
import com.k.medtour.domain.file.repository.FileRepository;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import com.k.medtour.infra.s3.StorageService;
import com.k.medtour.infra.s3.StorageService.StorageUploadResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final FileRepository fileRepository;
    private final StorageService storageService;

    public FileUploadResponse upload(InputStream inputStream, String originalName, String contentType,
                                     long fileSize, String categoryStr, Long uploaderId) {
        validateFileSize(fileSize);
        validateMimeType(contentType);
        FileCategory category = parseCategory(categoryStr);

        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID() + extension;

        StorageUploadResult result = storageService.upload(inputStream, originalName, contentType,
                fileSize, storedName, category.name());

        FileEntity fileEntity = FileEntity.builder()
                .uploaderId(uploaderId)
                .originalName(originalName)
                .storedName(storedName)
                .mimeType(contentType)
                .fileSize(fileSize)
                .category(category)
                .s3Key(result.s3Key())
                .url(result.url())
                .build();

        FileEntity saved = fileRepository.save(fileEntity);
        log.info("File uploaded: id={}, name={}, category={}, uploaderId={}",
                saved.getId(), originalName, category, uploaderId);

        return FileUploadResponse.from(saved);
    }

    public FileDownloadResponse getDownloadUrl(Long fileId) {
        FileEntity fileEntity = findFileOrThrow(fileId);

        String downloadUrl = storageService.generatePresignedUrl(fileEntity.getS3Key());
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        return new FileDownloadResponse(
                fileEntity.getId(),
                fileEntity.getOriginalName(),
                downloadUrl,
                expiresAt
        );
    }

    public void delete(Long fileId, Long memberId, String role) {
        FileEntity fileEntity = findFileOrThrow(fileId);

        if (!isAdminRole(role) && !fileEntity.getUploaderId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
        }

        fileEntity.softDelete();
        storageService.delete(fileEntity.getS3Key());
        log.info("File deleted: id={}, by memberId={}", fileId, memberId);
    }

    private FileEntity findFileOrThrow(Long fileId) {
        return fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }

    private void validateFileSize(long fileSize) {
        if (fileSize > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    private void validateMimeType(String contentType) {
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private FileCategory parseCategory(String categoryStr) {
        try {
            return FileCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_FILE_CATEGORY);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private boolean isAdminRole(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "MASTER".equalsIgnoreCase(role);
    }
}
