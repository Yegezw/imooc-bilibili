package com.imooc.bilibili.domain.video;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoLike {

    private Long id;

    private Long userId;

    private Long videoId;

    private Date createTime;
}
