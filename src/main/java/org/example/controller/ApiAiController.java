package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.common.ApiResponse;
import org.example.entity.CheckInRecordEntity;
import org.example.repository.CheckInRecordRepository;
import org.example.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class ApiAiController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private CheckInRecordRepository checkInRecordRepository;

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
     * GET /api/ai/records — 查询签到记录（非分页，供 AI 使用）
     */
    @GetMapping("/records")
    public ApiResponse<List<CheckInRecordEntity>> checkInRecords(
            @RequestParam("studentId") String studentId) {
        List<CheckInRecordEntity> records = checkInRecordRepository.findByStudentId(studentId);
        return ApiResponse.ok(records);
    }
}
