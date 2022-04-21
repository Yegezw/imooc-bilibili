package com.imooc.bilibili.domain.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class File {

    private Long id;

    private String url;

    private String type;

    private String md5;

    private Date createTime;
}