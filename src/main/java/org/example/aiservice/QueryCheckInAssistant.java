package org.example.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, streamingChatModel = "openAiStreamingChatModel",
        tools = {"chatHistoryTools", "checkInRecordTools", "leaveRequestTools"})
public interface QueryCheckInAssistant {

    @SystemMessage(fromResource = "queryCheckIn.txt")
    @UserMessage("当前sessionId:{{sessionId}}；用户当前消息：{{message}}")
    Flux<String> queryCheckIn(@V("sessionId") String sessionId, @V("message") String message);
}
