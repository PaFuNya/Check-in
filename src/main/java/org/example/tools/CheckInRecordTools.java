package org.example.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.CheckInRecordEntity;
import org.example.repository.CheckInRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class CheckInRecordTools {

    @Autowired
    private CheckInRecordRepository checkInRecordRepository;

    @Tool("根据学生id查询签到记录")
    public List<CheckInRecordEntity> queryCheckInByStudentId(@P("学生id") String studentId) {
        log.info("🔧 [TOOL] queryCheckInByStudentId被调用, studentId: {}", studentId);
        return checkInRecordRepository.findByStudentId(studentId);
    }
}
