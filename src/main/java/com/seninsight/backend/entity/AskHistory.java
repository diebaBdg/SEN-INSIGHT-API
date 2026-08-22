package com.seninsight.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ask_history")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AskHistory {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(nullable = false)
    private String query;

    private String insight;

    @Column(columnDefinition = "text")
    private String dataPoints;

    @Column(columnDefinition = "text")
    private String sources;

    @Column(name = "region_id")
    private String regionId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
