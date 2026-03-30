package com.k.medtour.domain.admin.entity;

import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {


    private String token;



    private Member member;


    private LocalDateTime expiresAt;


    private Boolean revoked = false;

    @Builder
    public RefreshToken(String token, Member member, LocalDateTime expiresAt) {
        this.token = token;
        this.member = member;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !this.revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }
}
