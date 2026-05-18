package org.example.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.LeaveRequestEntity;
import org.example.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LeaveRequestTools {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Tool("根据学生id查询请假记录")
    public List<LeaveRequestEntity> queryLeaveByStudentId(@P("学生id") String studentId) {
        log.info("🔧 [TOOL] queryLeaveByStudentId被调用, studentId: {}", studentId);
        return leaveRequestRepository.findByStudentId(studentId);
    }
}
