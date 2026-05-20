package org.example.aioutput;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class IntentionOutput {

    @Description("意图分析 1：请假报备 2：状态查询 3：规则问答 4：其他/非相关 5：个人信息查询")
    private Integer intention;

    @Description("大模型对用户的输出")
    private String output;
}
