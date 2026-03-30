package com.k.medtour.infra.s3;

import java.io.InputStream;

public interface StorageService {

    /**
     * 파일을 스토리지에 업로드하고, 저장된 key를 반환한다.
     */
    StorageUploadResult upload(InputStream inputStream, String filename, String contentType,
                               long size, String storedName, String category);

    /**
     * 파일 key로 Presigned(다운로드) URL을 생성한다.
     */
    String generatePresignedUrl(String key);

    /**
     * 파일 key로 스토리지에서 삭제한다.
     */
    void delete(String key);

    record StorageUploadResult(String s3Key, String url) {
    }
}
