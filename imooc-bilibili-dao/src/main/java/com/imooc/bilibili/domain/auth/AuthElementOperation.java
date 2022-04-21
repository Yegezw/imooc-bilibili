package com.imooc.bilibili.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthElementOperation {

    private Long id;

    private String elementName;

    private String elementCode;

    private String operationType;

    private Date createTime;

    private Date updateTime;
}
