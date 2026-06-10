package org.example.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "openAiChatModel")
public interface SimpleChatAssistant {

    @SystemMessage("你是浙江国际海运职业技术学院的寝室签到助手。请根据提供的上下文信息回答用户问题，用正式友好的语气。回答时引用具体条文和页码。")
    @UserMessage("{{prompt}}")
    String chat(@V("prompt") String prompt);
}
