package com.example.demo.shortenurl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for managing pre-generated short code pool.
 * Stores available short codes that can be assigned to URLs.
 */
@Entity
@Table(name = "CODE_POOL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodePool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODE", unique = true, nullable = false, length = 10)
    private String code;

    @Column(name = "ISUSED", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "ASSIGNEDUSERID")
    private Long assignedUserId;

    @Column(name = "CREATEDAT")
    private LocalDateTime createdAt = LocalDateTime.now();

    public CodePool(String code) {
        this.code = code;
        this.isUsed = false;
        this.createdAt = LocalDateTime.now();
    }

    public CodePool(String code, Long assignedUserId) {
        this.code = code;
        this.isUsed = false;
        this.assignedUserId = assignedUserId;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
