package com.lhy.usercenter.model;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long userId;

    /**
     * 
     */
    private String userName;

    /**
     * 
     */
    @NotNull(message = "账户不能为空")
    @Min(value = 4,message = "账户不能小于4位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",message = "账户名不能包含除了下划线外的其他特殊字符")
    private String userAccount;

    /**
     * 
     */
    private String avatarUrl;

    /**
     * 
     */
    private Integer gender;

    /**
     * 
     */
    @NotBlank(message = "用户密码不能为空")
    @Min(value = 8,message = "密码长度不能小于8位")
    private String userPassword;

    /**
     * 
     */
    private String phone;

    private String profile;

    /**
     * 
     */
    private String email;

    /**
     * 0代表普通用户 1代表管理员
     */
    private Integer userRole;
    private Integer userStatus;

    private String tags;

    /**
     * 
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 
     */
    private String planetCode;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}