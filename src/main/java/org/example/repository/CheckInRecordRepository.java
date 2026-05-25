package org.example.repository;

import org.example.entity.CheckInRecordEntity;

import java.util.Date;
import java.util.List;

public interface CheckInRecordRepository extends BaseRepository<CheckInRecordEntity> {

    List<CheckInRecordEntity> findByStudentId(String studentId);

    List<CheckInRecordEntity> findByCheckTimeBetween(Date start, Date end);
}
