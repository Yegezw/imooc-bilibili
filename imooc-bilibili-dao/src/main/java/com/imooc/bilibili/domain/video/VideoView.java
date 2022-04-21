package com.imooc.bilibili.domain.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoView {

    private Long id;

    private Long videoId;

    private Long userId;

    private String clientId;

    private String ip;

    private Date createTime;
}