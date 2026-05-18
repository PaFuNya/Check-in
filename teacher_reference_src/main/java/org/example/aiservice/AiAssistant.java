package org.example.aiservice;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import org.example.aioutput.IntentionOuput;
import org.example.aioutput.LostRegisterOutput;
import reactor.core.publisher.Flux;

/**
 * // 第一次调用后，会话历史：
 * [用户] 什么是量子计算？
 * [系统] 回答要简洁
 * [AI] 量子计算是利用量子力学现象的计算方式。（简洁回答）
 *
 * // 第二次调用后，会话历史：
 * [用户] 什么是量子计算？
 * [系统] 回答要简洁
 * [AI] 量子计算是利用量子力学现象的计算方式。
 * [用户] 请解释量子计算的原理
 * [系统] 回答要详细  // 覆盖了之前的提示词！
 * [AI] 量子计算基于量子比特...（详细回答）
 */
@AiService( wiringMode = AiServiceWiringMode.EXPLICIT,chatModel = "openAiChatModel",
//streamingChatModel = "openAiStreamingChatModel",
 tools = {"chatHistoryTools"})
public interface AiAssistant {
//    String chat(@MemoryId String id, @UserMessage String message);
//
//    Flux<String> chatStream(@MemoryId String id, @UserMessage String message);

    @SystemMessage(fromResource ="getIntention.txt")
    @UserMessage("当前sessionId:{{sessionId}}；用户当前消息：{{message}}")
    IntentionOuput intention(@V("sessionId") String sessionId, @V("message")String message);

//    @SystemMessage(fromResource ="/registerlost.txt")
//    @UserMessage("当前sessionId:{{sessionId}}；用户当前消息：{{message}}")
//    LostRegisterOutput registerLost(@V("sessionId") String sessionId,@V("message")String message);
}
