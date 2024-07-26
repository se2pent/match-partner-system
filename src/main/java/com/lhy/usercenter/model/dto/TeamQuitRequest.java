package com.lhy.usercenter.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TeamQuitRequest implements Serializable {


    private static final long serialVersionUID = 5953336849128286925L;
    private long teamId;

}
