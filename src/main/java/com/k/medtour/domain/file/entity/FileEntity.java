package com.k.medtour.domain.file.entity;

import com.k.medtour.domain.file.enums.FileCategory;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileEntity extends BaseEntity {


    private Long uploaderId;


    private String originalName;


    private String storedName;


    private String mimeType;


    private Long fileSize;



    private FileCategory category;


    private String s3Key;


    private String url;

    @Builder
    public FileEntity(Long uploaderId, String originalName, String storedName,
                      String mimeType, Long fileSize, FileCategory category,
                      String s3Key, String url) {
        this.uploaderId = uploaderId;
        this.originalName = originalName;
        this.storedName = storedName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.category = category;
        this.s3Key = s3Key;
        this.url = url;
    }
}
