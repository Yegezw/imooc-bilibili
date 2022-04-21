package com.imooc.bilibili.domain.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoBinaryPicture {

    private Long id;

    private Long videoId; // 视频 id

    private Integer frameNo; // 帧数

    private String url; // 图片链接

    private Long videoTimestamp; // 视频时间戳

    private Date createTime;
}