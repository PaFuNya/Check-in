package org.example.repository;

import org.example.entity.ChatHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatHistoryRepository extends BaseRepository<ChatHistoryEntity> {

    List<ChatHistoryEntity> findTop20BySessionIdOrderByIdDesc(String sessionId);

    @Query(value = "SELECT * FROM chat_history WHERE session_id = :sessionId ORDER BY id DESC LIMIT 20", nativeQuery = true)
    List<ChatHistoryEntity> findBySessionIdWithNativeQuery(@Param("sessionId") String sessionId);

    Page<ChatHistoryEntity> findAllBySessionId(String sessionId, Pageable pageable);

    @Transactional
    @Modifying
    void deleteBySessionId(String sessionId);
}
