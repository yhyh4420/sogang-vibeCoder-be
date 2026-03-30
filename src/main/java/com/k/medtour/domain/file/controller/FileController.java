package com.k.medtour.domain.file.controller;

import com.k.medtour.domain.file.dto.FileDownloadResponse;
import com.k.medtour.domain.file.dto.FileUploadResponse;
import com.k.medtour.domain.file.service.FileService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.server.Router;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;

@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    public void register(Router router) {
        // File upload requires multipart handling -- simplified for MVP
        router.post("/api/v1/files", ctx -> {
            // MVP: accept JSON with base64 or just metadata
            return ApiResponse.success("파일 업로드는 multipart 처리가 필요합니다.", null);
        });
        router.get("/api/v1/files/{fileId}/download", ctx -> getDownloadUrl(ctx.pathParamAsLong("fileId")));
        router.delete("/api/v1/files/{fileId}", ctx -> delete(ctx.pathParamAsLong("fileId"), ctx.userPrincipal()));
    }

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
