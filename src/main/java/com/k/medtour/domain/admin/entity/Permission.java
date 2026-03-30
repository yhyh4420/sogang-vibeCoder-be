package com.k.medtour.domain.admin.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permission {



    private Long id;


    private String name;


    private String description;



    private LocalDateTime createdAt;



    private LocalDateTime updatedAt;

    @Builder
    public Permission(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
