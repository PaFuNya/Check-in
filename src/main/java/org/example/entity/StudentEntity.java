package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Table(name = "student")
@Entity
@Data
@Comment("学生表")
public class StudentEntity {

    @Id
    @Comment("学生id")
    private String studentId;

    @Comment("学生姓名")
    private String studentName;

    @Comment("密码(BCrypt加密)")
    private String password;

    @Comment("宿舍楼栋")
    private String dormBuilding;

    @Comment("寝室号")
    private String roomNumber;

    @Comment("人脸图片URL")
    private String faceImageUrl;

    @Comment("创建时间")
    private Date createdAt;
}
