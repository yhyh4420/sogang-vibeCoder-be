package com.k.medtour.global.common;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Base entity fields for JDBC-based entities.
 * Subclasses should include these fields in their table schemas.
 */
@Getter
@Setter
public abstract class BaseEntity {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Call before INSERT to set timestamps.
     */
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Call before UPDATE to refresh updatedAt.
     */
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
