package org.example.repository;

import org.example.entity.RememberTokenEntity;

import java.util.Optional;

public interface RememberTokenRepository extends BaseRepository<RememberTokenEntity> {

    // 根据 token 字符串查找
    Optional<RememberTokenEntity> findByToken(String token);

    // 根据 studentId 查找
    Optional<RememberTokenEntity> findByStudentId(String studentId);

    // 根据 studentId 删除所有 token (登出时清除)
    void deleteByStudentId(String studentId);
}
