package com.k.medtour.domain.admin.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {



    private Long id;


    private String name;


    private String description;


    private Set<Permission> permissions = new HashSet<>();



    private LocalDateTime createdAt;



    private LocalDateTime updatedAt;

    @Builder
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
