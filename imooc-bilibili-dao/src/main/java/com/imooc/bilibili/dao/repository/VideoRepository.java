package com.imooc.bilibili.dao.repository;

import com.imooc.bilibili.domain.video.Video;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VideoRepository extends ElasticsearchRepository<Video, Long> {

    // find by title like keyword
    /**
     * 根据 title 模糊查询视频
     */
    Video findByTitleLike(String keyword);
}
