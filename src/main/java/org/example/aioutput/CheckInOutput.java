package org.example.aioutput;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class CheckInOutput {

    @Description("大模型对用户的输出")
    private String output;

    @Description("学生id")
    private String studentId;

    @Description("学生姓名")
    private String studentName;

    @Description("寝室号")
    private String roomNumber;

    @Description("宿舍楼栋")
    private String dormBuilding;

    @Description("位置信息")
    private String locationInfo;

    @Description("是否完成签到")
    private Boolean completed;
}
