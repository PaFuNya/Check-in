package org.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Table(name = "leave_request")
@Entity
@Data
@Comment("请假申请")
public class LeaveRequestEntity extends BaseEntity {

    @Comment("学生id")
    private String studentId;

    @Comment("学生姓名")
    private String studentName;

    @Comment("请假类型")
    private String leaveType;

    @Comment("开始时间")
    private Date startTime;

    @Comment("结束时间")
    private Date endTime;

    @Comment("请假原因")
    @Lob
    private String reason;

    @Comment("审核状态")
    private String auditStatus;

    @Comment("审核意见")
    private String auditorComment;
}
