package com.lhy.usercenter.model.enums;

import com.lhy.usercenter.service.TeamService;

public enum TeamStatusEnum {
    PUBLIC(0,"公开"),
    PRIVATE(1,"私有"),
    SECRET(2,"加密");


    private int value;

    private String text;

    public static TeamStatusEnum getTeamStatusEnum(Integer value){
        if (value == null) {
            return null;
        }
        TeamStatusEnum[] teamStatusEnums=TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : teamStatusEnums) {
            if (teamStatusEnum.getValue() == value) {
                return teamStatusEnum;
            }
        }
        return null;
    }


    TeamStatusEnum(int value,String text){
        this.value=value;
        this.text=text;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
