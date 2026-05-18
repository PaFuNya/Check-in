package org.example.repository;

import org.example.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<StudentEntity, String> {
    // 不需要额外方法, studentId 即主键, 使用内置 findById 即可
}
