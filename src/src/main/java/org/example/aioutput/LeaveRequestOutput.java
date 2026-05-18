package org.example.aioutput;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class LeaveRequestOutput {

    @Description("大模型对用户的输出")
    private String output;

    @Description("学生id")
    private String studentId;

    @Description("学生姓名")
    private String studentName;

    @Description("请假类型")
    private String leaveType;

    @Description("开始时间")
    private String startTime;

    @Description("结束时间")
    private String endTime;

    @Description("请假原因")
    private String reason;

    @Description("是否完成请假申请")
    private Boolean completed;
}
