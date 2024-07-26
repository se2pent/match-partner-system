package com.lhy.usercenter.model.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class UserRegisterRequest {

    @NotBlank(message = "账户不能为空")
    @Length(min = 4,message = "账户最少要4位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",message = "账户名不能包含除了下划线外的其他特殊字符")
    private String userAccount;

    @NotBlank(message = "用户密码不能为空")
    @Length(min = 6,message = "账户密码最少要6位")
    private String userPassword;
    @NotBlank(message = "确认密码不能为空")
    private String checkedPassword;

    @NotBlank(message = "编号不能为空")
    @Length(max = 5,message = "编号最大位不能为5")
    private String planetCode;


}
