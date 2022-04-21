package com.imooc.bilibili.domain.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoCoin {

    private Long id;

    private Long videoId;

    private Long userId;

    private Integer amount;

    private Date createTime;

    private Date updateTime;
}