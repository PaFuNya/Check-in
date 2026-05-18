package org.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Table(name = "remember_token")
@Entity
@Data
@Comment("记住登录令牌")
public class RememberTokenEntity extends BaseEntity {

    @Comment("学生id")
    private String studentId;

    @Comment("令牌")
    private String token;

    @Comment("过期时间")
    private Date expiresAt;
}
