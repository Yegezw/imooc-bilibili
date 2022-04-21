package com.imooc.bilibili.domain.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenDetail {

    private Long id;

    private String refreshToken;

    private Long userId;

    private Date createTime;
}