package com.lhy.usercenter.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 4155834019304558038L;
    protected Integer pageSize=10;
    protected Integer pageNum=1;


}
