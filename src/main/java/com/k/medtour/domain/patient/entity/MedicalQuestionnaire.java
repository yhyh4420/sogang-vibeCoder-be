package com.k.medtour.domain.patient.entity;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.patient.enums.BloodType;
import com.k.medtour.domain.patient.enums.QuestionnaireStatus;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalQuestionnaire extends BaseEntity {



    private Member member;



    private BloodType bloodType;


    private Double height;


    private Double weight;



    private List<String> allergies;



    private List<String> currentMedications;



    private List<String> pastSurgeries;



    private List<String> chronicConditions;


    private String additionalNotes;



    private QuestionnaireStatus status = QuestionnaireStatus.SUBMITTED;

    @Builder
    public MedicalQuestionnaire(Member member, BloodType bloodType, Double height, Double weight,
                                 List<String> allergies, List<String> currentMedications,
                                 List<String> pastSurgeries, List<String> chronicConditions,
                                 String additionalNotes) {
        this.member = member;
        this.bloodType = bloodType;
        this.height = height;
        this.weight = weight;
        this.allergies = allergies;
        this.currentMedications = currentMedications;
        this.pastSurgeries = pastSurgeries;
        this.chronicConditions = chronicConditions;
        this.additionalNotes = additionalNotes;
        this.status = QuestionnaireStatus.SUBMITTED;
    }

    public void update(BloodType bloodType, Double height, Double weight,
                       List<String> allergies, List<String> currentMedications,
                       List<String> pastSurgeries, List<String> chronicConditions,
                       String additionalNotes) {
        this.bloodType = bloodType;
        this.height = height;
        this.weight = weight;
        this.allergies = allergies;
        this.currentMedications = currentMedications;
        this.pastSurgeries = pastSurgeries;
        this.chronicConditions = chronicConditions;
        this.additionalNotes = additionalNotes;
        this.status = QuestionnaireStatus.SUBMITTED;
    }

    public void markReviewed() {
        this.status = QuestionnaireStatus.REVIEWED;
    }
}
