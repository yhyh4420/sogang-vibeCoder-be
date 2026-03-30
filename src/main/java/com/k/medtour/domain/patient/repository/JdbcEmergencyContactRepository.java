package com.k.medtour.domain.patient.repository;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.patient.entity.EmergencyContact;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcEmergencyContactRepository implements EmergencyContactRepository {

    private final DataSource dataSource;

    public JdbcEmergencyContactRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public EmergencyContact save(EmergencyContact contact) {
        if (contact.getId() == null) return insert(contact);
        else return update(contact);
    }

    private EmergencyContact insert(EmergencyContact c) {
        c.prePersist();
        String sql = """
            INSERT INTO emergency_contact (member_id, name, relationship, phone, email, is_primary,
                                           created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, c.getMember().getId());
            stmt.setString(2, c.getName());
            stmt.setString(3, c.getRelationship());
            stmt.setString(4, c.getPhone());
            stmt.setString(5, c.getEmail());
            stmt.setBoolean(6, c.getIsPrimary());
            stmt.setTimestamp(7, Timestamp.valueOf(c.getCreatedAt()));
            stmt.setTimestamp(8, Timestamp.valueOf(c.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) c.setId(rs.getLong(1));
            }
            return c;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private EmergencyContact update(EmergencyContact c) {
        c.preUpdate();
        String sql = """
            UPDATE emergency_contact SET name = ?, relationship = ?, phone = ?, email = ?,
                                         is_primary = ?, updated_at = ?, deleted_at = ?
            WHERE id = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, c.getName());
            stmt.setString(2, c.getRelationship());
            stmt.setString(3, c.getPhone());
            stmt.setString(4, c.getEmail());
            stmt.setBoolean(5, c.getIsPrimary());
            stmt.setTimestamp(6, Timestamp.valueOf(c.getUpdatedAt()));
            stmt.setObject(7, c.getDeletedAt() != null ? Timestamp.valueOf(c.getDeletedAt()) : null);
            stmt.setLong(8, c.getId());
            stmt.executeUpdate();
            return c;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<EmergencyContact> findById(Long id) {
        String sql = "SELECT * FROM emergency_contact WHERE id = ?";
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
    public List<EmergencyContact> findAllByMemberIdAndDeletedAtIsNull(Long memberId) {
        String sql = "SELECT * FROM emergency_contact WHERE member_id = ? AND deleted_at IS NULL ORDER BY id";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, memberId);
            try (var rs = stmt.executeQuery()) {
                List<EmergencyContact> contacts = new ArrayList<>();
                while (rs.next()) contacts.add(mapRow(rs));
                return contacts;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<EmergencyContact> findByIdAndMemberIdAndDeletedAtIsNull(Long id, Long memberId) {
        String sql = "SELECT * FROM emergency_contact WHERE id = ? AND member_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, memberId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private EmergencyContact mapRow(ResultSet rs) throws SQLException {
        Member memberStub = Member.builder().build();
        memberStub.setId(rs.getLong("member_id"));

        EmergencyContact c = EmergencyContact.builder()
                .member(memberStub)
                .name(rs.getString("name"))
                .relationship(rs.getString("relationship"))
                .phone(rs.getString("phone"))
                .email(rs.getString("email"))
                .isPrimary(rs.getBoolean("is_primary"))
                .build();
        c.setId(rs.getLong("id"));
        c.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        c.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        c.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        return c;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
