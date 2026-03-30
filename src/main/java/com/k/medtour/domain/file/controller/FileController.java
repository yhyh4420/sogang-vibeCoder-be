package com.k.medtour.domain.file.controller;

import com.k.medtour.domain.file.dto.FileDownloadResponse;
import com.k.medtour.domain.file.dto.FileUploadResponse;
import com.k.medtour.domain.file.service.FileService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;

@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    public ApiResponse<FileUploadResponse> upload(
            InputStream inputStream,
            String originalName,
            String contentType,
            long fileSize,
            String category,
            UserPrincipal principal) {
        FileUploadResponse response = fileService.upload(inputStream, originalName, contentType,
                fileSize, category, principal.memberId());
        return ApiResponse.success("업로드 완료", response);
    }

    public ApiResponse<FileDownloadResponse> getDownloadUrl(
            Long fileId) {
        FileDownloadResponse response = fileService.getDownloadUrl(fileId);
        return ApiResponse.success("다운로드 URL 생성 완료", response);
    }

    public ApiResponse<Void> delete(
            Long fileId,
            UserPrincipal principal) {
        fileService.delete(fileId, principal.memberId(), principal.role());
        return ApiResponse.success("파일 삭제 완료", null);
    }
}
