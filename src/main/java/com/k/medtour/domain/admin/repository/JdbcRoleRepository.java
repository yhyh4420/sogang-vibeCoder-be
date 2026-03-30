package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.Permission;
import com.k.medtour.domain.admin.entity.Role;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class JdbcRoleRepository implements RoleRepository {

    private final DataSource dataSource;

    public JdbcRoleRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Role save(Role role) {
        if (role.getId() == null) {
            String sql = "INSERT INTO role (name, description, created_at, updated_at) VALUES (?, ?, ?, ?)";
            try (var conn = dataSource.getConnection();
                 var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());
                stmt.setString(1, role.getName());
                stmt.setString(2, role.getDescription());
                stmt.setTimestamp(3, now);
                stmt.setTimestamp(4, now);
                stmt.executeUpdate();
                try (var rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        setField(role, "id", rs.getLong(1));
                    }
                }
                return role;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            String sql = "UPDATE role SET name = ?, description = ?, updated_at = ? WHERE id = ?";
            try (var conn = dataSource.getConnection();
                 var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, role.getName());
                stmt.setString(2, role.getDescription());
                stmt.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
                stmt.setLong(4, role.getId());
                stmt.executeUpdate();
                return role;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Optional<Role> findById(Long id) {
        String sql = "SELECT * FROM role WHERE id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Role role = mapRow(rs);
                    loadPermissions(role, conn);
                    return Optional.of(role);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Role> findByName(String name) {
        String sql = "SELECT * FROM role WHERE name = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Role role = mapRow(rs);
                    loadPermissions(role, conn);
                    return Optional.of(role);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Role> findAllWithPermissions() {
        String sql = "SELECT * FROM role ORDER BY id";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            List<Role> roles = new ArrayList<>();
            while (rs.next()) {
                Role role = mapRow(rs);
                loadPermissions(role, conn);
                roles.add(role);
            }
            return roles;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Role> findByIdWithPermissions(Long id) {
        return findById(id);
    }

    private Role mapRow(ResultSet rs) throws SQLException {
        Role role = Role.builder()
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .build();
        setField(role, "id", rs.getLong("id"));
        return role;
    }

    private void loadPermissions(Role role, Connection conn) throws SQLException {
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
                    setField(permission, "id", rs.getLong("id"));
                    permissions.add(permission);
                }
                setField(role, "permissions", permissions);
            }
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
