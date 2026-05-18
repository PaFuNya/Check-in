package org.example.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;



@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,streamingChatModel = "openAiStreamingChatModel",
        tools = {"chatHistoryTools","lostRegisterTools","queryLostPropertyTools"})
public interface QueryLostPropertyAssistant {
    @UserMessage("当前sessionId:{{sessionId}};当前用户消息:{{message}}")
    @SystemMessage(fromResource =  "/queryLostProperty.txt")
    Flux<String>  queryLostProperty(@V("sessionId") String id,@V("message") String message) ;
}
