package com.k.medtour.domain.patient.repository;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.patient.entity.PatientPassport;
import com.k.medtour.domain.patient.enums.Gender;
import com.k.medtour.domain.patient.enums.PassportInputType;
import com.k.medtour.domain.patient.enums.VerificationStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class JdbcPatientPassportRepository implements PatientPassportRepository {

    private final DataSource dataSource;

    public JdbcPatientPassportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public PatientPassport save(PatientPassport passport) {
        if (passport.getId() == null) {
            return insert(passport);
        } else {
            return update(passport);
        }
    }

    private PatientPassport insert(PatientPassport passport) {
        passport.prePersist();
        String sql = """
            INSERT INTO patient_passport (member_id, passport_number, full_name, nationality, birth_date,
                                          expiry_date, gender, input_type, file_id, ocr_confidence,
                                          verification_status, verified_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, passport.getMember().getId());
            stmt.setString(2, passport.getPassportNumber());
            stmt.setString(3, passport.getFullName());
            stmt.setString(4, passport.getNationality());
            stmt.setObject(5, passport.getBirthDate());
            stmt.setObject(6, passport.getExpiryDate());
            stmt.setString(7, passport.getGender() != null ? passport.getGender().name() : null);
            stmt.setString(8, passport.getInputType() != null ? passport.getInputType().name() : null);
            stmt.setObject(9, passport.getFileId());
            stmt.setObject(10, passport.getOcrConfidence());
            stmt.setString(11, passport.getVerificationStatus().name());
            stmt.setObject(12, passport.getVerifiedAt() != null ? Timestamp.valueOf(passport.getVerifiedAt()) : null);
            stmt.setTimestamp(13, Timestamp.valueOf(passport.getCreatedAt()));
            stmt.setTimestamp(14, Timestamp.valueOf(passport.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) passport.setId(rs.getLong(1));
            }
            return passport;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private PatientPassport update(PatientPassport passport) {
        passport.preUpdate();
        String sql = """
            UPDATE patient_passport SET passport_number = ?, full_name = ?, nationality = ?,
                                        birth_date = ?, expiry_date = ?, gender = ?, input_type = ?,
                                        file_id = ?, ocr_confidence = ?, verification_status = ?,
                                        verified_at = ?, updated_at = ?, deleted_at = ?
            WHERE id = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passport.getPassportNumber());
            stmt.setString(2, passport.getFullName());
            stmt.setString(3, passport.getNationality());
            stmt.setObject(4, passport.getBirthDate());
            stmt.setObject(5, passport.getExpiryDate());
            stmt.setString(6, passport.getGender() != null ? passport.getGender().name() : null);
            stmt.setString(7, passport.getInputType() != null ? passport.getInputType().name() : null);
            stmt.setObject(8, passport.getFileId());
            stmt.setObject(9, passport.getOcrConfidence());
            stmt.setString(10, passport.getVerificationStatus().name());
            stmt.setObject(11, passport.getVerifiedAt() != null ? Timestamp.valueOf(passport.getVerifiedAt()) : null);
            stmt.setTimestamp(12, Timestamp.valueOf(passport.getUpdatedAt()));
            stmt.setObject(13, passport.getDeletedAt() != null ? Timestamp.valueOf(passport.getDeletedAt()) : null);
            stmt.setLong(14, passport.getId());
            stmt.executeUpdate();
            return passport;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<PatientPassport> findById(Long id) {
        String sql = "SELECT * FROM patient_passport WHERE id = ?";
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
    public Optional<PatientPassport> findByMemberIdAndDeletedAtIsNull(Long memberId) {
        String sql = "SELECT * FROM patient_passport WHERE member_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, memberId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean existsByMemberIdAndDeletedAtIsNull(Long memberId) {
        String sql = "SELECT COUNT(*) FROM patient_passport WHERE member_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, memberId);
            try (var rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private PatientPassport mapRow(ResultSet rs) throws SQLException {
        Member memberStub = Member.builder().build();
        memberStub.setId(rs.getLong("member_id"));

        String genderStr = rs.getString("gender");
        String inputTypeStr = rs.getString("input_type");

        PatientPassport passport = PatientPassport.builder()
                .member(memberStub)
                .passportNumber(rs.getString("passport_number"))
                .fullName(rs.getString("full_name"))
                .nationality(rs.getString("nationality"))
                .birthDate(rs.getObject("birth_date", LocalDate.class))
                .expiryDate(rs.getObject("expiry_date", LocalDate.class))
                .gender(genderStr != null ? Gender.valueOf(genderStr) : null)
                .inputType(inputTypeStr != null ? PassportInputType.valueOf(inputTypeStr) : null)
                .fileId(rs.getObject("file_id", Long.class))
                .ocrConfidence(rs.getObject("ocr_confidence", Double.class))
                .build();
        passport.setId(rs.getLong("id"));
        passport.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        passport.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        passport.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));

        String vsStr = rs.getString("verification_status");
        if (vsStr != null) {
            try {
                var field = PatientPassport.class.getDeclaredField("verificationStatus");
                field.setAccessible(true);
                field.set(passport, VerificationStatus.valueOf(vsStr));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return passport;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
