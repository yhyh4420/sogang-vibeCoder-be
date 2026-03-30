package com.k.medtour.domain.patient.entity;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.patient.enums.Gender;
import com.k.medtour.domain.patient.enums.PassportInputType;
import com.k.medtour.domain.patient.enums.VerificationStatus;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientPassport extends BaseEntity {



    private Member member;


    private String passportNumber;


    private String fullName;


    private String nationality;


    private LocalDate birthDate;


    private LocalDate expiryDate;



    private Gender gender;



    private PassportInputType inputType;


    private Long fileId;


    private Double ocrConfidence;



    private VerificationStatus verificationStatus = VerificationStatus.PENDING;


    private LocalDateTime verifiedAt;

    @Builder
    public PatientPassport(Member member, String passportNumber, String fullName,
                           String nationality, LocalDate birthDate, LocalDate expiryDate,
                           Gender gender, PassportInputType inputType, Long fileId,
                           Double ocrConfidence) {
        this.member = member;
        this.passportNumber = passportNumber;
        this.fullName = fullName;
        this.nationality = nationality;
        this.birthDate = birthDate;
        this.expiryDate = expiryDate;
        this.gender = gender;
        this.inputType = inputType;
        this.fileId = fileId;
        this.ocrConfidence = ocrConfidence;
        this.verificationStatus = VerificationStatus.PENDING;
    }

    public void update(String passportNumber, String fullName, String nationality,
                       LocalDate birthDate, LocalDate expiryDate, Gender gender,
                       PassportInputType inputType, Long fileId, Double ocrConfidence) {
        this.passportNumber = passportNumber;
        this.fullName = fullName;
        this.nationality = nationality;
        this.birthDate = birthDate;
        this.expiryDate = expiryDate;
        this.gender = gender;
        this.inputType = inputType;
        this.fileId = fileId;
        this.ocrConfidence = ocrConfidence;
        this.verificationStatus = VerificationStatus.PENDING;
        this.verifiedAt = null;
    }

    public void verify() {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }

    public void reject() {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.verifiedAt = null;
    }
}
