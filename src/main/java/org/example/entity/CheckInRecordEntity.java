package org.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Table(name = "check_in_record")
@Entity
@Data
@Comment("签到记录")
public class CheckInRecordEntity extends BaseEntity {

    @Comment("学生id")
    private String studentId;

    @Comment("学生姓名")
    private String studentName;

    @Comment("寝室号")
    private String roomNumber;

    @Comment("宿舍楼栋")
    private String dormBuilding;

    @Comment("签到状态")
    private String status;

    @Comment("位置信息")
    private String locationInfo;

    @Comment("签到时间")
    private Date checkTime;
}
