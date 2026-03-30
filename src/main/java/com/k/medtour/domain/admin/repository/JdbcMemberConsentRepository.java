package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.admin.entity.MemberConsent;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class JdbcMemberConsentRepository implements MemberConsentRepository {

    private final DataSource dataSource;

    public JdbcMemberConsentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public MemberConsent save(MemberConsent consent) {
        if (consent.getId() == null) {
            return insert(consent);
        }
        return consent;
    }

    private MemberConsent insert(MemberConsent consent) {
        consent.prePersist();
        String sql = """
            INSERT INTO member_consent (member_id, terms_of_service, privacy_policy,
                                        medical_data_consent, marketing_consent,
                                        consent_version, consented_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, consent.getMember().getId());
            stmt.setBoolean(2, consent.getTermsOfService());
            stmt.setBoolean(3, consent.getPrivacyPolicy());
            stmt.setBoolean(4, consent.getMedicalDataConsent());
            stmt.setBoolean(5, consent.getMarketingConsent());
            stmt.setString(6, consent.getConsentVersion());
            stmt.setTimestamp(7, Timestamp.valueOf(consent.getConsentedAt()));
            stmt.setTimestamp(8, Timestamp.valueOf(consent.getCreatedAt()));
            stmt.setTimestamp(9, Timestamp.valueOf(consent.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    consent.setId(rs.getLong(1));
                }
            }
            return consent;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<MemberConsent> findById(Long id) {
        String sql = "SELECT * FROM member_consent WHERE id = ? AND deleted_at IS NULL";
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
    public Optional<MemberConsent> findTopByMemberIdOrderByConsentedAtDesc(Long memberId) {
        String sql = """
            SELECT * FROM member_consent
            WHERE member_id = ? AND deleted_at IS NULL
            ORDER BY consented_at DESC
            LIMIT 1
            """;
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

    private MemberConsent mapRow(ResultSet rs) throws SQLException {
        Member memberStub = Member.builder().build();
        memberStub.setId(rs.getLong("member_id"));

        MemberConsent consent = MemberConsent.builder()
                .member(memberStub)
                .termsOfService(rs.getBoolean("terms_of_service"))
                .privacyPolicy(rs.getBoolean("privacy_policy"))
                .medicalDataConsent(rs.getBoolean("medical_data_consent"))
                .marketingConsent(rs.getBoolean("marketing_consent"))
                .consentVersion(rs.getString("consent_version"))
                .consentedAt(toLocalDateTime(rs.getTimestamp("consented_at")))
                .build();
        consent.setId(rs.getLong("id"));
        consent.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        consent.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        consent.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        return consent;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
