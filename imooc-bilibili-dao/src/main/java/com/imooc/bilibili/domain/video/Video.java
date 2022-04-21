package com.imooc.bilibili.domain.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "videos")
public class Video {

    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long userId; //用户 id

    private String url; // 视频链接

    private String thumbnail; // 封面链接

    @Field(type = FieldType.Text)
    private String title; // 视频标题

    private String type; // 视频类型: 0 自制, 1 转载

    private String duration; // 视频时长

    private String area; // 所在分区: 0 鬼畜, 1 音乐, 2 电影

    @Field(type = FieldType.Text)
    private String description; // 视频简介

    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Date)
    private Date updateTime;

    private List<VideoTag> videoTagList; // 标签列表
}
