package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.entity.CheckInRecordEntity;
import org.example.entity.LeaveRequestEntity;
import org.example.repository.CheckInRecordRepository;
import org.example.repository.LeaveRequestRepository;
import org.example.service.AiChatService;
import org.example.vo.ChatHistoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private CheckInRecordRepository checkInRecordRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=utf-8")
    public Flux<String> chatStream(@RequestParam(value = "message", defaultValue = "Hello") String message,
                                   @RequestParam(value = "userId", defaultValue = "111") String userId,
                                   HttpSession session) {
        String studentName = (String) session.getAttribute("studentName");
        String className = (String) session.getAttribute("className");
        return Flux.just(aiChatService.chatStream(userId,
                studentName != null ? studentName : "",
                className != null ? className : "",
                message));
    }

    @GetMapping(value = "/chat-history")
    public PagedModel<ChatHistoryVo> queryChatHistory(
            @RequestParam(value = "userId", defaultValue = "111") String userId,
            @PageableDefault(direction = Sort.Direction.DESC, sort = "id") Pageable page) {
        return new PagedModel<>(aiChatService.queryChatHistory(userId, page));
    }

    @PostMapping(value = "/clear-chat-history/{userId}")
    public void clearChatHistory(@PathVariable("userId") String userId) {
        aiChatService.clearChatHistory(userId);
    }

    @GetMapping(value = "/check-in-records")
    public List<CheckInRecordEntity> queryCheckInRecords(
            @RequestParam(value = "studentId") String studentId) {
        return checkInRecordRepository.findByStudentId(studentId);
    }

    @GetMapping(value = "/leave-requests")
    public List<LeaveRequestEntity> queryLeaveRequests(
            @RequestParam(value = "studentId") String studentId) {
        return leaveRequestRepository.findByStudentId(studentId);
    }

    @GetMapping(value = "/embedding-index")
    public String embeddingIndex() {
        return aiChatService.embeddingIndex();
    }

    @GetMapping(value = "/embedding-query")
    public List<String> embeddingQuery(@RequestParam String message) {
        return aiChatService.embeddingQuery(message);
    }
}
