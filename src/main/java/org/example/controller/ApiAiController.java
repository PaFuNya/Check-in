package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.common.ApiResponse;
import org.example.entity.CheckInRecordEntity;
import org.example.entity.LeaveRequestEntity;
import org.example.repository.CheckInRecordRepository;
import org.example.repository.LeaveRequestRepository;
import org.example.service.AiChatService;
import org.example.vo.ChatHistoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class ApiAiController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private CheckInRecordRepository checkInRecordRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    /**
     * GET /api/ai/chat-stream — SSE 流式 AI 对话
     *
     * 使用 text/event-stream 逐字符输出 AI 回复，配合 EventSource 前端消费。
     * 由于当前 AiChatService.chatStream() 返回同步 String，
     * 这里将完整响应拆分为逐字符 Flux 并以 SSE data: 格式推送。
     */
    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=utf-8")
    public Flux<String> chatStream(@RequestParam("message") String message,
                                   @RequestParam(value = "userId", defaultValue = "111") String userId,
                                   HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        String studentName = (String) session.getAttribute("studentName");
        String className = (String) session.getAttribute("className");

        // 从 session 获取 userId，若未登录则使用请求参数的默认值
        if (studentId != null && !studentId.isEmpty()) {
            userId = studentId;
        }

        // 调用同步 AI 对话获取完整回复
        String fullResponse = aiChatService.chatStream(userId,
                studentName != null ? studentName : "",
                className != null ? className : "",
                message);

        // 拆分为逐字符 SSE 流，每个 chunk 格式: data: <char>\n\n
        // 最后发送 data: [DONE]\n\n
        Flux<String> charFlux = Flux.fromArray(fullResponse.split(""))
                .delayElements(Duration.ofMillis(20))
                .map(c -> "data: " + c + "\n\n");

        Flux<String> doneFlux = Flux.just("data: [DONE]\n\n");

        return Flux.concat(charFlux, doneFlux);
    }

    /**
     * GET /api/ai/chat-history — 分页查询聊天历史
     */
    @GetMapping("/chat-history")
    public ApiResponse<Map<String, Object>> chatHistory(
            @RequestParam(value = "userId", defaultValue = "111") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId != null && !studentId.isEmpty()) {
            userId = studentId;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<ChatHistoryVo> pageResult = aiChatService.queryChatHistory(userId, pageable);

        Map<String, Object> data = Map.of(
                "content", pageResult.getContent(),
                "totalPages", pageResult.getTotalPages(),
                "totalElements", pageResult.getTotalElements(),
                "currentPage", pageResult.getNumber(),
                "size", pageResult.getSize()
        );
        return ApiResponse.ok(data);
    }

    /**
     * POST /api/ai/clear-chat-history/{userId} — 清除聊天历史
     */
    @PostMapping("/clear-chat-history/{userId}")
    public ApiResponse<Void> clearChatHistory(@PathVariable("userId") String userId) {
        aiChatService.clearChatHistory(userId);
        return ApiResponse.ok();
    }

    /**
     * GET /api/ai/check-in-records — 查询签到记录（非分页，供 AI 使用）
     */
    @GetMapping("/check-in-records")
    public ApiResponse<List<CheckInRecordEntity>> checkInRecords(
            @RequestParam("studentId") String studentId) {
        List<CheckInRecordEntity> records = checkInRecordRepository.findByStudentId(studentId);
        return ApiResponse.ok(records);
    }

    /**
     * GET /api/ai/leave-requests — 查询请假记录（供 AI 使用）
     */
    @GetMapping("/leave-requests")
    public ApiResponse<List<LeaveRequestEntity>> leaveRequests(
            @RequestParam("studentId") String studentId) {
        List<LeaveRequestEntity> requests = leaveRequestRepository.findByStudentId(studentId);
        return ApiResponse.ok(requests);
    }
}
