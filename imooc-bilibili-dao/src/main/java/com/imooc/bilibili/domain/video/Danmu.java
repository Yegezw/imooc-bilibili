package com.imooc.bilibili.domain.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Danmu {

    private Long id;

    private Long userId; // 弹幕发送者的用户 id

    private Long videoId; // 视频 id

    private String content; // 弹幕内容

    private String danmuTime; // 弹幕出现时间

    private Date createTime;
}
