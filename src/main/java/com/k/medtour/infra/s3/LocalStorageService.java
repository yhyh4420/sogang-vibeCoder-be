package com.k.medtour.infra.s3;

import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path uploadDir;
    private final String baseUrl;

    public LocalStorageService(String uploadDir, String baseUrl) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        createUploadDirectory();
    }

    private void createUploadDirectory() {
        try {
            Files.createDirectories(uploadDir);
            log.info("Upload directory initialized: {}", uploadDir);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadDir, e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @Override
    public StorageUploadResult upload(InputStream inputStream, String filename, String contentType,
                                      long size, String storedName, String category) {
        try {
            Path categoryDir = uploadDir.resolve(category.toLowerCase());
            Files.createDirectories(categoryDir);

            Path targetPath = categoryDir.resolve(storedName);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            String s3Key = category.toLowerCase() + "/" + storedName;
            String url = baseUrl + "/" + s3Key;

            log.debug("File uploaded to local storage: {}", targetPath);
            return new StorageUploadResult(s3Key, url);
        } catch (IOException e) {
            log.error("Failed to upload file: {}", storedName, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String generatePresignedUrl(String key) {
        return baseUrl + "/" + key;
    }

    @Override
    public void delete(String key) {
        try {
            Path filePath = uploadDir.resolve(key);
            Files.deleteIfExists(filePath);
            log.debug("File deleted from local storage: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file from local storage: {}", key, e);
        }
    }
}
