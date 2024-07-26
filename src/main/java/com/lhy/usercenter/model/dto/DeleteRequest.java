package com.lhy.usercenter.model.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 1601077696889253989L;
    private long teamId;
}
