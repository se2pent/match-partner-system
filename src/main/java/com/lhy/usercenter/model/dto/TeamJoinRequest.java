package com.lhy.usercenter.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TeamJoinRequest implements Serializable {


    private static final long serialVersionUID = -4054909536179605597L;
    private Long teamId;


    /**
     * 密码
     */
    private String password;


}
