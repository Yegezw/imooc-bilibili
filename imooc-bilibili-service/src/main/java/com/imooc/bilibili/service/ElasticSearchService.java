package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.repository.UserInfoRepository;
import com.imooc.bilibili.dao.repository.VideoRepository;
import com.imooc.bilibili.domain.user.UserInfo;
import com.imooc.bilibili.domain.video.Video;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ElasticSearchService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 添加视频
     */
    public void addVideo(Video video) {
        videoRepository.save(video);
    }

    /**
     * 根据 title 模糊查询视频
     */
    public Video getVideos(String keyword) {
        return videoRepository.findByTitleLike(keyword);
    }

    /**
     * 删除所有视频
     */
    public void deleteAll() {
        videoRepository.deleteAll();
    }

    /**
     * 添加用户基本信息
     */
    public void addUserInfo(UserInfo userInfo) {
        userInfoRepository.save(userInfo);
    }

    /**
     * 根据 keyword 分页查询
     * <p>索引: videos、user-infos</p>
     * <p>字段: title、description、nick</p>
     */
    public List<Map<String, Object>> getContents(String keyword, Integer pageNo, Integer pageSize) throws Exception {
        String[] indices = {"videos", "user-infos"}; // 索引
        SearchRequest searchRequest = new SearchRequest(indices); // 请求

        // 查询条件
        String[] array = {"title", "description", "nick"};
        searchRequest.source().query(QueryBuilders.multiMatchQuery(keyword, array));
        // 分页
        searchRequest.source().from((pageNo - 1) * pageSize).size(pageSize);
        // 高亮
        searchRequest.source().highlighter(
                new HighlightBuilder()
                        .field("title")
                        .field("description")
                        .field("nick")
                        .requireFieldMatch(false)
                        .preTags("<span style=\"color:red\">")
                        .postTags("</span>")
        );
        // 超时时间 60 s
        searchRequest.source().timeout(new TimeValue(60, TimeUnit.SECONDS));

        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT); // 发送请求

        // 解析响应
        List<Map<String, Object>> arrayList = new ArrayList<>();
        for (SearchHit hit : response.getHits()) {
            // 处理高亮字段
            Map<String, HighlightField> highLightBuilderFields = hit.getHighlightFields();
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            for (String key : array) {
                HighlightField field = highLightBuilderFields.get(key);
                if (field != null) {
                    Text[] fragments = field.fragments();
                    String str = Arrays.toString(fragments);
                    str = str.substring(1, str.length() - 1); // 去除头尾中括号 []
                    sourceMap.put(key, str);
                }
            }

            arrayList.add(sourceMap);
        }

        return arrayList;
    }

}
