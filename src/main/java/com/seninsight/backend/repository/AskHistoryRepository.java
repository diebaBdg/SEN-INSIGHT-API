package com.seninsight.backend.repository;

import com.seninsight.backend.entity.AskHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AskHistoryRepository extends JpaRepository<AskHistory, UUID> {
    Page<AskHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<AskHistory> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
