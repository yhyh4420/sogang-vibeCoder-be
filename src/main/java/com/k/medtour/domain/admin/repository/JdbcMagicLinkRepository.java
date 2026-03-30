package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.MagicLink;
import com.k.medtour.domain.admin.enums.MagicLinkTargetType;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class JdbcMagicLinkRepository implements MagicLinkRepository {

    private final DataSource dataSource;

    public JdbcMagicLinkRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public MagicLink save(MagicLink magicLink) {
        if (magicLink.getId() == null) {
            return insert(magicLink);
        } else {
            return update(magicLink);
        }
    }

    private MagicLink insert(MagicLink magicLink) {
        magicLink.prePersist();
        String sql = """
            INSERT INTO magic_link (token, target_email, target_phone, target_type, role, language,
                                    birth_date, expires_at, used_at, member_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, magicLink.getToken());
            stmt.setString(2, magicLink.getTargetEmail());
            stmt.setString(3, magicLink.getTargetPhone());
            stmt.setString(4, magicLink.getTargetType().name());
            stmt.setString(5, magicLink.getRole());
            stmt.setString(6, magicLink.getLanguage());
            stmt.setObject(7, magicLink.getBirthDate());
            stmt.setTimestamp(8, Timestamp.valueOf(magicLink.getExpiresAt()));
            stmt.setObject(9, magicLink.getUsedAt() != null ? Timestamp.valueOf(magicLink.getUsedAt()) : null);
            stmt.setObject(10, magicLink.getMember() != null ? magicLink.getMember().getId() : null);
            stmt.setTimestamp(11, Timestamp.valueOf(magicLink.getCreatedAt()));
            stmt.setTimestamp(12, Timestamp.valueOf(magicLink.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    magicLink.setId(rs.getLong(1));
                }
            }
            return magicLink;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private MagicLink update(MagicLink magicLink) {
        magicLink.preUpdate();
        String sql = """
            UPDATE magic_link SET used_at = ?, member_id = ?, updated_at = ?
            WHERE id = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, magicLink.getUsedAt() != null ? Timestamp.valueOf(magicLink.getUsedAt()) : null);
            stmt.setObject(2, magicLink.getMember() != null ? magicLink.getMember().getId() : null);
            stmt.setTimestamp(3, Timestamp.valueOf(magicLink.getUpdatedAt()));
            stmt.setLong(4, magicLink.getId());
            stmt.executeUpdate();
            return magicLink;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<MagicLink> findById(Long id) {
        String sql = "SELECT * FROM magic_link WHERE id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<MagicLink> findByToken(UUID token) {
        String sql = "SELECT * FROM magic_link WHERE token = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, token);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countRecentByTargetEmail(String target, LocalDateTime since) {
        String sql = "SELECT COUNT(*) FROM magic_link WHERE target_email = ? AND created_at > ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, target);
            stmt.setTimestamp(2, Timestamp.valueOf(since));
            try (var rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countRecentByTargetPhone(String target, LocalDateTime since) {
        String sql = "SELECT COUNT(*) FROM magic_link WHERE target_phone = ? AND created_at > ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, target);
            stmt.setTimestamp(2, Timestamp.valueOf(since));
            try (var rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private MagicLink mapRow(ResultSet rs) throws SQLException {
        MagicLink magicLink = MagicLink.builder()
                .token(UUID.fromString(rs.getString("token")))
                .targetEmail(rs.getString("target_email"))
                .targetPhone(rs.getString("target_phone"))
                .targetType(MagicLinkTargetType.valueOf(rs.getString("target_type")))
                .role(rs.getString("role"))
                .language(rs.getString("language"))
                .birthDate(rs.getObject("birth_date", LocalDate.class))
                .expiresAt(toLocalDateTime(rs.getTimestamp("expires_at")))
                .build();
        magicLink.setId(rs.getLong("id"));
        magicLink.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        magicLink.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        magicLink.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));

        Timestamp usedAtTs = rs.getTimestamp("used_at");
        if (usedAtTs != null) {
            try {
                var field = MagicLink.class.getDeclaredField("usedAt");
                field.setAccessible(true);
                field.set(magicLink, usedAtTs.toLocalDateTime());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return magicLink;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
