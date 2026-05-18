package org.example.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import org.example.aioutput.CheckInOutput;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "openAiChatModel",
        tools = {"chatHistoryTools", "checkInRecordTools"})
public interface CheckInAssistant {

    @SystemMessage(fromResource = "checkin.txt")
    @UserMessage("当前sessionId:{{sessionId}}；用户当前消息：{{message}}")
    CheckInOutput checkIn(@V("sessionId") String sessionId, @V("message") String message);
}
