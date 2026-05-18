package org.example.enitity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

/**
 * 这段Java代码使用了多个注解，功能如下：
 *
 * 1. `@EqualsAndHashCode(callSuper = true)`：Lombok 注解，自动生成 `equals` 和 `hashCode` 方法，并包含父类字段。
 * 2. `@Table(name = "lost_register")`：指定该实体类映射的数据库表名为 `lost_register`。
 * 3. `@Entity`：JPA 注解，标识该类为实体类，用于与数据库表映射。
 * 4. `@Data`：Lombok 注解，自动生成 getter、setter、toString 等方法。
 * 5. `@Comment("丢失登记")`：为该实体类添加注释，说明其用途为“丢失登记”。
 */
@EqualsAndHashCode(callSuper = true)
@Table(name = "lost_register")
@Entity
@Data
@Comment("丢失登记")
public class LostRegisterEntity extends BaseEntity {

    @Comment("用户姓名")
    private String username;

    @Comment("用户手机号")
    private String phone;

    @Comment("失物名称")
    private String lostName;

    @Comment("失物特征")
    private String lostType;

}