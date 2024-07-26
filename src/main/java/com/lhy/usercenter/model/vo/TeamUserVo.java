package com.lhy.usercenter.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TeamUserVo implements Serializable {


    private static final long serialVersionUID = 5009276458536877602L;
    /**
     * id
     */
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
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人用户信息
     */
    private UserVo createUser;


    private boolean hasJoin=false;

    /**
     * 已加入的用户数
     */
    private Integer hasJoinNum;
}
