package com.k.medtour.domain.admin.entity;

import com.k.medtour.domain.admin.enums.MagicLinkTargetType;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MagicLink extends BaseEntity {


    private UUID token;


    private String targetEmail;


    private String targetPhone;



    private MagicLinkTargetType targetType;


    private String role;


    private String language = "en";


    private LocalDate birthDate;


    private LocalDateTime expiresAt;


    private LocalDateTime usedAt;



    private Member member;

    @Builder
    public MagicLink(UUID token, String targetEmail, String targetPhone,
                     MagicLinkTargetType targetType, String role, String language,
                     LocalDate birthDate, LocalDateTime expiresAt, Member member) {
        this.token = token != null ? token : UUID.randomUUID();
        this.targetEmail = targetEmail;
        this.targetPhone = targetPhone;
        this.targetType = targetType;
        this.role = role;
        this.language = language != null ? language : "en";
        this.birthDate = birthDate;
        this.expiresAt = expiresAt;
        this.member = member;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isUsed() {
        return this.usedAt != null;
    }

    public void markAsUsed() {
        this.usedAt = LocalDateTime.now();
    }

    public void linkMember(Member member) {
        this.member = member;
    }
}
