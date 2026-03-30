package com.k.medtour.domain.aftercare.entity;

import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AftercareGuide extends BaseEntity {


    private Long journeyId;


    private String title;


    private String content;



    private Map<String, Object> instructions;


    private LocalDateTime publishedAt;

    @Builder
    public AftercareGuide(Long journeyId, String title, String content,
                          Map<String, Object> instructions) {
        this.journeyId = journeyId;
        this.title = title;
        this.content = content;
        this.instructions = instructions;
        this.publishedAt = LocalDateTime.now();
    }
}
