package com.k.medtour.domain.file.repository;

import com.k.medtour.domain.file.entity.FileEntity;
import com.k.medtour.domain.file.enums.FileCategory;

import java.util.List;
import java.util.Optional;

public interface FileRepository {

    FileEntity save(FileEntity fileEntity);

    Optional<FileEntity> findById(Long id);

    Optional<FileEntity> findByIdAndDeletedAtIsNull(Long id);

    List<FileEntity> findByUploaderIdAndDeletedAtIsNull(Long uploaderId);

    List<FileEntity> findByUploaderIdAndCategoryAndDeletedAtIsNull(Long uploaderId, FileCategory category);

    List<FileEntity> findByCategoryAndDeletedAtIsNull(FileCategory category);
}
