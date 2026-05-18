package org.example.aioutput;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class IntentionOutput {

    @Description("意图分析 1：签到打卡 2：请假报备 3：查询签到/请假状态 4：规则问答 5：其他")
    private Integer intention;

    @Description("大模型对用户的输出")
    private String output;
}
