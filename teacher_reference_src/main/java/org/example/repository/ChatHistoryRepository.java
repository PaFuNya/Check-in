package org.example.repository;


import org.example.enitity.ChatHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatHistoryRepository extends  BaseRepository<ChatHistoryEntity>{

    /**
     * 获取用户当前会话记录最新的20条
     * @param sessionId
     * @return
     */
    List<ChatHistoryEntity> findTop20BySessionIdOrderByIdDesc(String sessionId);
    
    /**
     * 调试用：使用原生SQL查询来对比
     */
    @Query(value = "SELECT * FROM chat_history WHERE session_id = :sessionId ORDER BY id DESC LIMIT 20", nativeQuery = true)
    List<ChatHistoryEntity> findBySessionIdWithNativeQuery(@Param("sessionId") String sessionId);


    /**
     * 按会话ID分页查询所有聊天记录
     * 分页查询用于处理大量数据，避免一次性加载过多记录导致性能问题
     *
     * @param sessionId 会话ID，用于标识特定的聊天会话
     * @param pageable 分页信息，包括当前页和每页大小
     * @return 分页的聊天记录列表
     */
    Page<ChatHistoryEntity> findAllBySessionId(String sessionId, Pageable pageable);

    /*
     * 通过会话ID删除所有相关的聊天记录
     * 此操作标记为事务性和修改性，意味着它将在一个数据库事务中执行，以保持数据一致性
     *
     * @param userId 用户ID，此处应为sessionId的笔误，需要更正为sessionId以匹配方法的预期用途
     */
    @Transactional
    //表示修改数据库的操作
    @Modifying
    void deleteBySessionId(String userId);
}
