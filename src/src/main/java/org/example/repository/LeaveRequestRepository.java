package org.example.repository;

import org.example.entity.LeaveRequestEntity;

import java.util.List;

public interface LeaveRequestRepository extends BaseRepository<LeaveRequestEntity> {

    List<LeaveRequestEntity> findByStudentId(String studentId);

    List<LeaveRequestEntity> findByAuditStatus(String auditStatus);
}
