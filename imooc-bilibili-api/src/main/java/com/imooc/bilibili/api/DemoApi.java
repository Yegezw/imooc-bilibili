package com.imooc.bilibili.api;

import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.video.Video;
import com.imooc.bilibili.service.ElasticSearchService;
import com.imooc.bilibili.service.util.FastDFSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class DemoApi {

    @Autowired
    private FastDFSUtil fastDFSUtil;

    @Autowired
    private ElasticSearchService elasticSearchService;

    /**
     * 测试, 将文件进行分片, 并将分片文件存储到 tmpfile 目录下
     */
    @GetMapping("/slices")
    public void slices(MultipartFile file) throws Exception {
        fastDFSUtil.convertFileToSlices(file);
    }

    /**
     * 在 es 中, 根据 title 模糊查询视频
     */
    @GetMapping("/es-videos")
    public JsonResponse<Video> getEsVideos(@RequestParam String keyword) {
        Video videos = elasticSearchService.getVideos(keyword);
        return new JsonResponse<>(videos);
    }

    /**
     * 删除 es 中所有视频
     */
    @DeleteMapping("es-videos")
    public JsonResponse<String> deleteAllVideos() {
        elasticSearchService.deleteAll();
        return JsonResponse.success();
    }

    /**
     * 根据 keyword 分页查询
     * <p>索引: videos、user-infos</p>
     * <p>字段: title、description、nick</p>
     */
    @GetMapping("/contents")
    public JsonResponse<List<Map<String, Object>>> getContents(@RequestParam String keyword,
                                                         @RequestParam Integer pageNo,
                                                         @RequestParam Integer pageSize) throws Exception {
        List<Map<String, Object>> result = elasticSearchService.getContents(keyword, pageNo, pageSize);
        return new JsonResponse<>(result);
    }

}
