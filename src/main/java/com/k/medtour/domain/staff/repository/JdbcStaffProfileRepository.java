package com.k.medtour.domain.staff.repository;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.staff.entity.StaffProfile;
import com.k.medtour.domain.staff.enums.StaffType;
import com.k.medtour.server.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcStaffProfileRepository implements StaffProfileRepository {

    private final DataSource dataSource;

    public JdbcStaffProfileRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public StaffProfile save(StaffProfile profile) {
        if (profile.getId() == null) {
            return insert(profile);
        } else {
            return update(profile);
        }
    }

    private StaffProfile insert(StaffProfile profile) {
        profile.prePersist();
        String sql = """
            INSERT INTO staff_profile (member_id, staff_type, languages, vehicle_info, is_available,
                                       created_at, updated_at)
            VALUES (?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, profile.getMember().getId());
            stmt.setString(2, profile.getStaffType().name());
            stmt.setString(3, profile.getLanguages() != null ? JsonUtil.toJson(profile.getLanguages()) : null);
            stmt.setString(4, profile.getVehicleInfo() != null ? JsonUtil.toJson(profile.getVehicleInfo()) : null);
            stmt.setBoolean(5, profile.getIsAvailable());
            stmt.setTimestamp(6, Timestamp.valueOf(profile.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(profile.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) profile.setId(rs.getLong(1));
            }
            return profile;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private StaffProfile update(StaffProfile profile) {
        profile.preUpdate();
        String sql = """
            UPDATE staff_profile SET staff_type = ?, languages = ?::jsonb, vehicle_info = ?::jsonb,
                                     is_available = ?, updated_at = ?
            WHERE id = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, profile.getStaffType().name());
            stmt.setString(2, profile.getLanguages() != null ? JsonUtil.toJson(profile.getLanguages()) : null);
            stmt.setString(3, profile.getVehicleInfo() != null ? JsonUtil.toJson(profile.getVehicleInfo()) : null);
            stmt.setBoolean(4, profile.getIsAvailable());
            stmt.setTimestamp(5, Timestamp.valueOf(profile.getUpdatedAt()));
            stmt.setLong(6, profile.getId());
            stmt.executeUpdate();
            return profile;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<StaffProfile> findById(Long id) {
        String sql = "SELECT * FROM staff_profile WHERE id = ? AND deleted_at IS NULL";
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
    public Optional<StaffProfile> findByMemberIdWithMember(Long memberId) {
        return findByMemberId(memberId);
    }

    @Override
    public Optional<StaffProfile> findByMemberId(Long memberId) {
        String sql = "SELECT * FROM staff_profile WHERE member_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, memberId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs, conn));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<StaffProfile> findAll() {
        String sql = "SELECT * FROM staff_profile WHERE deleted_at IS NULL ORDER BY id";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            List<StaffProfile> profiles = new ArrayList<>();
            while (rs.next()) profiles.add(mapRow(rs, conn));
            return profiles;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private StaffProfile mapRow(ResultSet rs, Connection conn) throws SQLException {
        Member member = loadMember(rs.getLong("member_id"), conn);

        String languagesJson = rs.getString("languages");
        List<String> languages = null;
        if (languagesJson != null) {
            languages = JsonUtil.fromJson(languagesJson, new TypeReference<List<String>>() {});
        }

        String vehicleInfoJson = rs.getString("vehicle_info");
        Map<String, Object> vehicleInfo = null;
        if (vehicleInfoJson != null) {
            vehicleInfo = JsonUtil.fromJson(vehicleInfoJson, new TypeReference<Map<String, Object>>() {});
        }

        StaffProfile profile = StaffProfile.builder()
                .member(member)
                .staffType(StaffType.valueOf(rs.getString("staff_type")))
                .languages(languages)
                .vehicleInfo(vehicleInfo)
                .isAvailable(rs.getBoolean("is_available"))
                .build();
        profile.setId(rs.getLong("id"));
        profile.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        profile.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        profile.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        return profile;
    }

    private Member loadMember(Long memberId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM member WHERE id = ? AND deleted_at IS NULL";
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, memberId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Member member = Member.builder()
                            .email(rs.getString("email"))
                            .name(rs.getString("name"))
                            .phone(rs.getString("phone"))
                            .language(rs.getString("language"))
                            .profileImage(rs.getString("profile_image"))
                            .build();
                    member.setId(rs.getLong("id"));
                    return member;
                }
            }
        }
        Member stub = Member.builder().build();
        stub.setId(memberId);
        return stub;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
