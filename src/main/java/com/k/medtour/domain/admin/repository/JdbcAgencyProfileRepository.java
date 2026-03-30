package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.AgencyProfile;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAgencyProfileRepository implements AgencyProfileRepository {

    private final DataSource dataSource;

    public JdbcAgencyProfileRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public AgencyProfile save(AgencyProfile profile) {
        if (profile.getId() == null) {
            return insert(profile);
        } else {
            return update(profile);
        }
    }

    private AgencyProfile insert(AgencyProfile profile) {
        profile.prePersist();
        String sql = """
            INSERT INTO agency_profile (name, license_number, license_verified, address, phone, website,
                                        description, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, profile.getName());
            stmt.setString(2, profile.getLicenseNumber());
            stmt.setBoolean(3, profile.getLicenseVerified());
            stmt.setString(4, profile.getAddress());
            stmt.setString(5, profile.getPhone());
            stmt.setString(6, profile.getWebsite());
            stmt.setString(7, profile.getDescription());
            stmt.setTimestamp(8, Timestamp.valueOf(profile.getCreatedAt()));
            stmt.setTimestamp(9, Timestamp.valueOf(profile.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) profile.setId(rs.getLong(1));
            }
            return profile;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AgencyProfile update(AgencyProfile profile) {
        profile.preUpdate();
        String sql = """
            UPDATE agency_profile SET name = ?, license_number = ?, license_verified = ?,
                                      address = ?, phone = ?, website = ?, description = ?, updated_at = ?
            WHERE id = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, profile.getName());
            stmt.setString(2, profile.getLicenseNumber());
            stmt.setBoolean(3, profile.getLicenseVerified());
            stmt.setString(4, profile.getAddress());
            stmt.setString(5, profile.getPhone());
            stmt.setString(6, profile.getWebsite());
            stmt.setString(7, profile.getDescription());
            stmt.setTimestamp(8, Timestamp.valueOf(profile.getUpdatedAt()));
            stmt.setLong(9, profile.getId());
            stmt.executeUpdate();
            return profile;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<AgencyProfile> findById(Long id) {
        String sql = "SELECT * FROM agency_profile WHERE id = ? AND deleted_at IS NULL";
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
    public Optional<AgencyProfile> findByLicenseNumber(String licenseNumber) {
        String sql = "SELECT * FROM agency_profile WHERE license_number = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, licenseNumber);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AgencyProfile> findAll() {
        String sql = "SELECT * FROM agency_profile WHERE deleted_at IS NULL ORDER BY id";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            List<AgencyProfile> profiles = new ArrayList<>();
            while (rs.next()) profiles.add(mapRow(rs));
            return profiles;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AgencyProfile mapRow(ResultSet rs) throws SQLException {
        AgencyProfile profile = AgencyProfile.builder()
                .name(rs.getString("name"))
                .licenseNumber(rs.getString("license_number"))
                .licenseVerified(rs.getBoolean("license_verified"))
                .address(rs.getString("address"))
                .phone(rs.getString("phone"))
                .website(rs.getString("website"))
                .description(rs.getString("description"))
                .build();
        profile.setId(rs.getLong("id"));
        profile.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        profile.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        profile.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        return profile;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
