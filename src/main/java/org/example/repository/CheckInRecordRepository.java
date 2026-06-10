package org.example.repository;

import org.example.entity.CheckInRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface CheckInRecordRepository extends JpaRepository<CheckInRecordEntity, Long> {

    List<CheckInRecordEntity> findByStudentId(String studentId);

    Page<CheckInRecordEntity> findByStudentId(String studentId, Pageable pageable);

    List<CheckInRecordEntity> findByCheckTimeBetween(Date start, Date end);

    List<CheckInRecordEntity> findByStudentIdAndCheckTimeAfter(String studentId, Date after);

    /**
     * 统计某个学生在指定时间段内的签到次数（用于每日签到限制）
     */
    long countByStudentIdAndCheckTimeBetween(String studentId, Date start, Date end);
}
