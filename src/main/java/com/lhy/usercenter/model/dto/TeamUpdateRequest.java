package com.lhy.usercenter.model.dto;

import lombok.Data;

import java.awt.print.PrinterGraphics;
import java.io.Serializable;
import java.time.LocalDateTime;
@Data
public class TeamUpdateRequest implements Serializable {

    private static final long serialVersionUID = 4929016420315966234L;
    private Long id;
    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 队伍过期时间
     */
    private LocalDateTime expireTime;



    /**
     * 队伍状态0为公开1为私有2为加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;


}
