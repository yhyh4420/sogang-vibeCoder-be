package com.k.medtour.domain.file.repository;

import com.k.medtour.domain.file.entity.FileEntity;
import com.k.medtour.domain.file.enums.FileCategory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcFileRepository implements FileRepository {

    private final DataSource dataSource;

    public JdbcFileRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public FileEntity save(FileEntity file) {
        if (file.getId() == null) return insert(file);
        else return update(file);
    }

    private FileEntity insert(FileEntity f) {
        f.prePersist();
        String sql = """
            INSERT INTO file_entity (uploader_id, original_name, stored_name, mime_type, file_size,
                                     category, s3_key, url, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, f.getUploaderId());
            stmt.setString(2, f.getOriginalName());
            stmt.setString(3, f.getStoredName());
            stmt.setString(4, f.getMimeType());
            stmt.setLong(5, f.getFileSize());
            stmt.setString(6, f.getCategory().name());
            stmt.setString(7, f.getS3Key());
            stmt.setString(8, f.getUrl());
            stmt.setTimestamp(9, Timestamp.valueOf(f.getCreatedAt()));
            stmt.setTimestamp(10, Timestamp.valueOf(f.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) f.setId(rs.getLong(1));
            }
            return f;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private FileEntity update(FileEntity f) {
        f.preUpdate();
        String sql = "UPDATE file_entity SET updated_at = ?, deleted_at = ? WHERE id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(f.getUpdatedAt()));
            stmt.setObject(2, f.getDeletedAt() != null ? Timestamp.valueOf(f.getDeletedAt()) : null);
            stmt.setLong(3, f.getId());
            stmt.executeUpdate();
            return f;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<FileEntity> findById(Long id) {
        return findOne("SELECT * FROM file_entity WHERE id = ?", id);
    }

    @Override
    public Optional<FileEntity> findByIdAndDeletedAtIsNull(Long id) {
        return findOne("SELECT * FROM file_entity WHERE id = ? AND deleted_at IS NULL", id);
    }

    @Override
    public List<FileEntity> findByUploaderIdAndDeletedAtIsNull(Long uploaderId) {
        return findList("SELECT * FROM file_entity WHERE uploader_id = ? AND deleted_at IS NULL ORDER BY id", uploaderId);
    }

    @Override
    public List<FileEntity> findByUploaderIdAndCategoryAndDeletedAtIsNull(Long uploaderId, FileCategory category) {
        String sql = "SELECT * FROM file_entity WHERE uploader_id = ? AND category = ? AND deleted_at IS NULL ORDER BY id";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, uploaderId);
            stmt.setString(2, category.name());
            try (var rs = stmt.executeQuery()) {
                List<FileEntity> files = new ArrayList<>();
                while (rs.next()) files.add(mapRow(rs));
                return files;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FileEntity> findByCategoryAndDeletedAtIsNull(FileCategory category) {
        String sql = "SELECT * FROM file_entity WHERE category = ? AND deleted_at IS NULL ORDER BY id";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.name());
            try (var rs = stmt.executeQuery()) {
                List<FileEntity> files = new ArrayList<>();
                while (rs.next()) files.add(mapRow(rs));
                return files;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<FileEntity> findOne(String sql, Long param) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, param);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<FileEntity> findList(String sql, Long param) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, param);
            try (var rs = stmt.executeQuery()) {
                List<FileEntity> files = new ArrayList<>();
                while (rs.next()) files.add(mapRow(rs));
                return files;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private FileEntity mapRow(ResultSet rs) throws SQLException {
        FileEntity f = FileEntity.builder()
                .uploaderId(rs.getLong("uploader_id"))
                .originalName(rs.getString("original_name"))
                .storedName(rs.getString("stored_name"))
                .mimeType(rs.getString("mime_type"))
                .fileSize(rs.getLong("file_size"))
                .category(FileCategory.valueOf(rs.getString("category")))
                .s3Key(rs.getString("s3_key"))
                .url(rs.getString("url"))
                .build();
        f.setId(rs.getLong("id"));
        f.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        f.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        f.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        return f;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
