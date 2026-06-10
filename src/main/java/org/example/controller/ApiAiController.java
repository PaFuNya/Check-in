package org.example.controller;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ApiResponse;
import org.example.entity.ChatHistoryEntity;
import org.example.entity.CheckInRecordEntity;
import org.example.repository.ChatHistoryRepository;
import org.example.repository.CheckInRecordRepository;
import org.example.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class ApiAiController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private CheckInRecordRepository checkInRecordRepository;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

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
                                   jakarta.servlet.http.HttpServletResponse response,
                                   HttpSession session) {
        // 禁用代理缓冲（Cloudflare/nginx）
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        String studentId = (String) session.getAttribute("studentId");
        String studentName = (String) session.getAttribute("studentName");
        String className = (String) session.getAttribute("className");

        // 从 session 获取 userId，若未登录则使用请求参数的默认值
        if (studentId != null && !studentId.isEmpty()) {
            userId = studentId;
        }

        // 调用同步 AI 对话获取完整回复
        String fullResponse;
        try {
            fullResponse = aiChatService.chatStream(userId,
                    studentName != null ? studentName : "",
                    className != null ? className : "",
                    message);
        } catch (Exception e) {
            log.error("AI对话异常: userId={}, message={}", userId, message, e);
            fullResponse = "抱歉，AI服务暂时不可用，请稍后重试。(" + e.getMessage() + ")";
        }

        // 拆分为逐字符 SSE 流
        // 注意：WebFlux 会自动添加 "data:" 前缀，所以 Flux 只返回原始内容
        List<String> chars = new ArrayList<>();
        for (int i = 0; i < fullResponse.length(); ) {
            int cp = fullResponse.codePointAt(i);
            chars.add(new String(Character.toChars(cp)));
            i += Character.charCount(cp);
        }

        Flux<String> charFlux = Flux.fromIterable(chars)
                .delayElements(Duration.ofMillis(20));

        Flux<String> doneFlux = Flux.just("[DONE]");

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

    /**
     * GET /api/ai/chat-history — 查询聊天历史
     */
    @GetMapping("/chat-history")
    public ApiResponse<?> chatHistory(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @RequestParam(value = "sort", defaultValue = "id,asc") String sort,
            HttpSession session) {
        String studentId = (String) session.getAttribute("studentId");
        if (studentId == null) {
            return ApiResponse.error(401, "未登录");
        }
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by(
                        sort.contains("desc") ? org.springframework.data.domain.Sort.Direction.DESC : org.springframework.data.domain.Sort.Direction.ASC,
                        sort.split(",")[0]));
        org.springframework.data.domain.Page<ChatHistoryEntity> chatPage =
                chatHistoryRepository.findByStudentId(studentId, pageable);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", chatPage.getContent());
        data.put("totalElements", chatPage.getTotalElements());
        data.put("totalPages", chatPage.getTotalPages());
        data.put("number", chatPage.getNumber());
        return ApiResponse.ok(data);
    }

    /**
     * DELETE /api/ai/clear-chat-history/{userId} — 清空聊天历史
     */
    @DeleteMapping("/clear-chat-history/{userId}")
    public ApiResponse<Void> clearChatHistory(@PathVariable String userId) {
        chatHistoryRepository.deleteByStudentId(userId);
        return ApiResponse.ok();
    }
}
