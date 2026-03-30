package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.admin.entity.Permission;
import com.k.medtour.domain.admin.entity.Role;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class JdbcMemberRepository implements MemberRepository {

    private final DataSource dataSource;

    public JdbcMemberRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Member save(Member member) {
        if (member.getId() == null) {
            return insert(member);
        } else {
            return update(member);
        }
    }

    private Member insert(Member member) {
        member.prePersist();
        String sql = """
            INSERT INTO member (email, name, role_id, oauth_provider, oauth_id, phone, language, profile_image,
                                created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, member.getEmail());
            stmt.setString(2, member.getName());
            stmt.setObject(3, member.getRole() != null ? member.getRole().getId() : null);
            stmt.setString(4, member.getOauthProvider());
            stmt.setString(5, member.getOauthId());
            stmt.setString(6, member.getPhone());
            stmt.setString(7, member.getLanguage());
            stmt.setString(8, member.getProfileImage());
            stmt.setTimestamp(9, Timestamp.valueOf(member.getCreatedAt()));
            stmt.setTimestamp(10, Timestamp.valueOf(member.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    member.setId(rs.getLong(1));
                }
            }
            return member;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Member update(Member member) {
        member.preUpdate();
        String sql = """
            UPDATE member SET email = ?, name = ?, role_id = ?, oauth_provider = ?, oauth_id = ?,
                              phone = ?, language = ?, profile_image = ?, updated_at = ?
            WHERE id = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, member.getEmail());
            stmt.setString(2, member.getName());
            stmt.setObject(3, member.getRole() != null ? member.getRole().getId() : null);
            stmt.setString(4, member.getOauthProvider());
            stmt.setString(5, member.getOauthId());
            stmt.setString(6, member.getPhone());
            stmt.setString(7, member.getLanguage());
            stmt.setString(8, member.getProfileImage());
            stmt.setTimestamp(9, Timestamp.valueOf(member.getUpdatedAt()));
            stmt.setLong(10, member.getId());
            stmt.executeUpdate();
            return member;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Member> findById(Long id) {
        String sql = "SELECT * FROM member WHERE id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs, conn));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        String sql = "SELECT * FROM member WHERE email = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs, conn));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Member> findByOauthProviderAndOauthId(String oauthProvider, String oauthId) {
        String sql = "SELECT * FROM member WHERE oauth_provider = ? AND oauth_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, oauthProvider);
            stmt.setString(2, oauthId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs, conn));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM member WHERE email = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (var rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Member> findByIdWithRole(Long id) {
        return findById(id);
    }

    @Override
    public Optional<Member> findByEmailWithRole(String email) {
        return findByEmail(email);
    }

    @Override
    public List<Member> findAllByRoleName(String roleName, int page, int size) {
        String sql = """
            SELECT m.* FROM member m
            JOIN role r ON m.role_id = r.id
            WHERE r.name = ? AND m.deleted_at IS NULL
            ORDER BY m.created_at DESC
            LIMIT ? OFFSET ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleName);
            stmt.setInt(2, size);
            stmt.setInt(3, page * size);
            try (var rs = stmt.executeQuery()) {
                List<Member> members = new ArrayList<>();
                while (rs.next()) {
                    members.add(mapRow(rs, conn));
                }
                return members;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countByRoleName(String roleName) {
        String sql = """
            SELECT COUNT(*) FROM member m
            JOIN role r ON m.role_id = r.id
            WHERE r.name = ? AND m.deleted_at IS NULL
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleName);
            try (var rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Member mapRow(ResultSet rs, Connection conn) throws SQLException {
        Role role = null;
        long roleId = rs.getLong("role_id");
        if (!rs.wasNull()) {
            role = findRoleById(roleId, conn);
        }
        Member member = Member.builder()
                .email(rs.getString("email"))
                .name(rs.getString("name"))
                .role(role)
                .oauthProvider(rs.getString("oauth_provider"))
                .oauthId(rs.getString("oauth_id"))
                .phone(rs.getString("phone"))
                .language(rs.getString("language"))
                .profileImage(rs.getString("profile_image"))
                .build();
        member.setId(rs.getLong("id"));
        member.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        member.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        member.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        return member;
    }

    private Role findRoleById(long roleId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM role WHERE id = ?";
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roleId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Role role = Role.builder()
                            .name(rs.getString("name"))
                            .description(rs.getString("description"))
                            .build();
                    // Use reflection-free approach: Role has setId-like access via its own fields
                    // Role doesn't extend BaseEntity, so we use a helper
                    setRoleId(role, rs.getLong("id"));
                    loadRolePermissions(role, conn);
                    return role;
                }
            }
        }
        return null;
    }

    private void setRoleId(Role role, long id) {
        try {
            var field = Role.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(role, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadRolePermissions(Role role, Connection conn) throws SQLException {
        String sql = """
            SELECT p.* FROM permission p
            JOIN role_permission rp ON p.id = rp.permission_id
            WHERE rp.role_id = ?
            """;
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, role.getId());
            try (var rs = stmt.executeQuery()) {
                Set<Permission> permissions = new HashSet<>();
                while (rs.next()) {
                    Permission permission = Permission.builder()
                            .name(rs.getString("name"))
                            .description(rs.getString("description"))
                            .build();
                    setPermissionId(permission, rs.getLong("id"));
                    permissions.add(permission);
                }
                setRolePermissions(role, permissions);
            }
        }
    }

    private void setPermissionId(Permission permission, long id) {
        try {
            var field = Permission.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(permission, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setRolePermissions(Role role, Set<Permission> permissions) {
        try {
            var field = Role.class.getDeclaredField("permissions");
            field.setAccessible(true);
            field.set(role, permissions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
