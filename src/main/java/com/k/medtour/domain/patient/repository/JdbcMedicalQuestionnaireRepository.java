package com.k.medtour.domain.patient.repository;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.patient.entity.MedicalQuestionnaire;
import com.k.medtour.domain.patient.enums.BloodType;
import com.k.medtour.domain.patient.enums.QuestionnaireStatus;
import com.k.medtour.server.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class JdbcMedicalQuestionnaireRepository implements MedicalQuestionnaireRepository {

    private final DataSource dataSource;

    public JdbcMedicalQuestionnaireRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public MedicalQuestionnaire save(MedicalQuestionnaire q) {
        if (q.getId() == null) return insert(q);
        else return update(q);
    }

    private MedicalQuestionnaire insert(MedicalQuestionnaire q) {
        q.prePersist();
        String sql = """
            INSERT INTO medical_questionnaire (member_id, blood_type, height, weight, allergies, current_medications,
                                               past_surgeries, chronic_conditions, additional_notes, status,
                                               created_at, updated_at)
            VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, q.getMember().getId());
            stmt.setString(2, q.getBloodType() != null ? q.getBloodType().name() : null);
            stmt.setObject(3, q.getHeight());
            stmt.setObject(4, q.getWeight());
            stmt.setString(5, q.getAllergies() != null ? JsonUtil.toJson(q.getAllergies()) : null);
            stmt.setString(6, q.getCurrentMedications() != null ? JsonUtil.toJson(q.getCurrentMedications()) : null);
            stmt.setString(7, q.getPastSurgeries() != null ? JsonUtil.toJson(q.getPastSurgeries()) : null);
            stmt.setString(8, q.getChronicConditions() != null ? JsonUtil.toJson(q.getChronicConditions()) : null);
            stmt.setString(9, q.getAdditionalNotes());
            stmt.setString(10, q.getStatus().name());
            stmt.setTimestamp(11, Timestamp.valueOf(q.getCreatedAt()));
            stmt.setTimestamp(12, Timestamp.valueOf(q.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) q.setId(rs.getLong(1));
            }
            return q;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private MedicalQuestionnaire update(MedicalQuestionnaire q) {
        q.preUpdate();
        String sql = """
            UPDATE medical_questionnaire SET blood_type = ?, height = ?, weight = ?,
                                             allergies = ?::jsonb, current_medications = ?::jsonb,
                                             past_surgeries = ?::jsonb, chronic_conditions = ?::jsonb,
                                             additional_notes = ?, status = ?, updated_at = ?, deleted_at = ?
            WHERE id = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, q.getBloodType() != null ? q.getBloodType().name() : null);
            stmt.setObject(2, q.getHeight());
            stmt.setObject(3, q.getWeight());
            stmt.setString(4, q.getAllergies() != null ? JsonUtil.toJson(q.getAllergies()) : null);
            stmt.setString(5, q.getCurrentMedications() != null ? JsonUtil.toJson(q.getCurrentMedications()) : null);
            stmt.setString(6, q.getPastSurgeries() != null ? JsonUtil.toJson(q.getPastSurgeries()) : null);
            stmt.setString(7, q.getChronicConditions() != null ? JsonUtil.toJson(q.getChronicConditions()) : null);
            stmt.setString(8, q.getAdditionalNotes());
            stmt.setString(9, q.getStatus().name());
            stmt.setTimestamp(10, Timestamp.valueOf(q.getUpdatedAt()));
            stmt.setObject(11, q.getDeletedAt() != null ? Timestamp.valueOf(q.getDeletedAt()) : null);
            stmt.setLong(12, q.getId());
            stmt.executeUpdate();
            return q;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<MedicalQuestionnaire> findById(Long id) {
        String sql = "SELECT * FROM medical_questionnaire WHERE id = ?";
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
    public Optional<MedicalQuestionnaire> findByMemberIdAndDeletedAtIsNull(Long memberId) {
        String sql = "SELECT * FROM medical_questionnaire WHERE member_id = ? AND deleted_at IS NULL";
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
        String sql = "SELECT COUNT(*) FROM medical_questionnaire WHERE member_id = ? AND deleted_at IS NULL";
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

    private MedicalQuestionnaire mapRow(ResultSet rs) throws SQLException {
        Member memberStub = Member.builder().build();
        memberStub.setId(rs.getLong("member_id"));

        String btStr = rs.getString("blood_type");
        String allergiesJson = rs.getString("allergies");
        String medsJson = rs.getString("current_medications");
        String surgeriesJson = rs.getString("past_surgeries");
        String conditionsJson = rs.getString("chronic_conditions");

        MedicalQuestionnaire q = MedicalQuestionnaire.builder()
                .member(memberStub)
                .bloodType(btStr != null ? BloodType.valueOf(btStr) : null)
                .height(rs.getObject("height", Double.class))
                .weight(rs.getObject("weight", Double.class))
                .allergies(allergiesJson != null ? JsonUtil.fromJson(allergiesJson, new TypeReference<List<String>>() {}) : null)
                .currentMedications(medsJson != null ? JsonUtil.fromJson(medsJson, new TypeReference<List<String>>() {}) : null)
                .pastSurgeries(surgeriesJson != null ? JsonUtil.fromJson(surgeriesJson, new TypeReference<List<String>>() {}) : null)
                .chronicConditions(conditionsJson != null ? JsonUtil.fromJson(conditionsJson, new TypeReference<List<String>>() {}) : null)
                .additionalNotes(rs.getString("additional_notes"))
                .build();
        q.setId(rs.getLong("id"));
        q.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        q.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        q.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                var field = MedicalQuestionnaire.class.getDeclaredField("status");
                field.setAccessible(true);
                field.set(q, QuestionnaireStatus.valueOf(statusStr));
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        return q;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
