package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.admin.entity.RefreshToken;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class JdbcRefreshTokenRepository implements RefreshTokenRepository {

    private final DataSource dataSource;

    public JdbcRefreshTokenRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        if (refreshToken.getId() == null) {
            return insert(refreshToken);
        } else {
            return update(refreshToken);
        }
    }

    private RefreshToken insert(RefreshToken refreshToken) {
        refreshToken.prePersist();
        String sql = """
            INSERT INTO refresh_token (token, member_id, expires_at, revoked, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, refreshToken.getToken());
            stmt.setLong(2, refreshToken.getMember().getId());
            stmt.setTimestamp(3, Timestamp.valueOf(refreshToken.getExpiresAt()));
            stmt.setBoolean(4, refreshToken.getRevoked());
            stmt.setTimestamp(5, Timestamp.valueOf(refreshToken.getCreatedAt()));
            stmt.setTimestamp(6, Timestamp.valueOf(refreshToken.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    refreshToken.setId(rs.getLong(1));
                }
            }
            return refreshToken;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private RefreshToken update(RefreshToken refreshToken) {
        refreshToken.preUpdate();
        String sql = "UPDATE refresh_token SET revoked = ?, updated_at = ? WHERE id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, refreshToken.getRevoked());
            stmt.setTimestamp(2, Timestamp.valueOf(refreshToken.getUpdatedAt()));
            stmt.setLong(3, refreshToken.getId());
            stmt.executeUpdate();
            return refreshToken;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<RefreshToken> findById(Long id) {
        String sql = "SELECT * FROM refresh_token WHERE id = ? AND deleted_at IS NULL";
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
    public Optional<RefreshToken> findByToken(String token) {
        String sql = "SELECT * FROM refresh_token WHERE token = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void revokeAllByMemberId(Long memberId) {
        String sql = "UPDATE refresh_token SET revoked = true, updated_at = ? WHERE member_id = ? AND revoked = false";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, memberId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private RefreshToken mapRow(ResultSet rs) throws SQLException {
        // Create a minimal Member stub with just the ID
        Member memberStub = Member.builder().build();
        memberStub.setId(rs.getLong("member_id"));

        RefreshToken refreshToken = RefreshToken.builder()
                .token(rs.getString("token"))
                .member(memberStub)
                .expiresAt(toLocalDateTime(rs.getTimestamp("expires_at")))
                .build();
        refreshToken.setId(rs.getLong("id"));
        refreshToken.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        refreshToken.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        refreshToken.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));

        if (rs.getBoolean("revoked")) {
            refreshToken.revoke();
        }
        return refreshToken;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
