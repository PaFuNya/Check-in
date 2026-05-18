package org.example.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import org.example.aioutput.LostPropertyOutput;
import org.example.aop.ChatFlow;
import reactor.core.publisher.Flux;


@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,streamingChatModel ="openAiStreamingChatModel",chatModel = "openAiChatModel",
tools = {"chatHistoryTools","registerFindLostPropertyTools"})
public interface RegisterFindLostPropertyAssistant {
//    @SystemMessage(fromResource =  "/registerLostProperty.txt")
//    @UserMessage("当前sessionId:{{sessionId}};当前用户消息:{{message}}")
//    Flux<String>  registerLostProperty(@V("sessionId") String id,@V("message") String message) ;
    @ChatFlow
    @SystemMessage(fromResource =  "/registerLostProperty.txt")
    @UserMessage("当前sessionId:{{sessionId}};当前用户消息:{{message}}")
    LostPropertyOutput registerLostProperty(@V("sessionId") String id, @V("message") String message) ;

}
