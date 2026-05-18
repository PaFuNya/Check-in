 //package org.example.aiservice;
//
//import dev.langchain4j.service.SystemMessage;
//import dev.langchain4j.service.UserMessage;
//import dev.langchain4j.service.V;
//import dev.langchain4j.service.spring.AiService;
//import dev.langchain4j.service.spring.AiServiceWiringMode;
//import org.example.aioutput.IntentionOuput;
//
//@AiService( wiringMode = AiServiceWiringMode.EXPLICIT,chatModel = "qwenChatModel",
//        streamingChatModel = "qwenStreamingChatModel",chatMemoryProvider = "chatMemoryProvider",
//        tools = "testTools")
//@SystemMessage(fromResource ="/getIntention.txt")
//public interface AiIntentionAssistant {
//
//    @UserMessage("当前sessionId:{{sessionId}}；用户当前消息：{{message}}")
//    IntentionOuput intention(@V("sessionId") String sessionId, @V("message")String message);
//
//}
