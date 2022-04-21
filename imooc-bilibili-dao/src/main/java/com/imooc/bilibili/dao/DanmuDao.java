package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.video.Danmu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DanmuDao {

    /**
     * 添加弹幕
     */
    Integer addDanmu(Danmu danmu);

    /**
     * 根据 videoId 和 startTime <= createTime <= endTime条件查询弹幕
     */
    List<Danmu> getDanmus(Map<String, Object> params);
}
