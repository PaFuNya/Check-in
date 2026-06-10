package org.example.repository;

import org.example.entity.RememberTokenEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RememberTokenRepository extends BaseRepository<RememberTokenEntity> {

    // 根据 token 字符串查找
    Optional<RememberTokenEntity> findByToken(String token);

    // 根据 studentId 查找
    Optional<RememberTokenEntity> findByStudentId(String studentId);

    // 根据 studentId 删除所有 token (登出时清除)
    @Transactional
    @Modifying
    void deleteByStudentId(String studentId);
}
