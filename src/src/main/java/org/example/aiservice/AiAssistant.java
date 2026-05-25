package org.example.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import org.example.aioutput.IntentionOutput;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "openAiChatModel",
        tools = {"chatHistoryTools"})
public interface AiAssistant {

    @SystemMessage(fromResource = "getIntention.txt")
    @UserMessage("当前sessionId:{{sessionId}}；用户当前消息：{{message}}")
    IntentionOutput intention(@V("sessionId") String sessionId, @V("message") String message);
}
