package org.example.aiservice;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import org.example.aioutput.LostRegisterOutput;

@AiService( wiringMode = AiServiceWiringMode.EXPLICIT,chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        tools = {"chatHistoryTools","lostRegisterTools"})
public interface RegisterLostAssistant {
    @SystemMessage(fromResource ="/registerlost.txt")
    @UserMessage("当前sessionId:{{sessionId}}；用户当前消息：{{message}}")
    LostRegisterOutput registerLost(@V("sessionId") String sessionId, @V("message")String message);
}
