package com.imooc.bilibili.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.video.*;
import com.imooc.bilibili.service.ElasticSearchService;
import com.imooc.bilibili.service.VideoService;
import org.apache.mahout.cf.taste.common.TasteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO 视频分组 t_collection_group
@RestController
public class VideoApi {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private ElasticSearchService elasticSearchService;

    /**
     * 视频投稿, 添加视频和视频标签
     */
    @PostMapping("/videos")
    public JsonResponse<String> addVideos(@RequestBody Video video) {
        Long userId = userSupport.getCurrentUserId();
        video.setUserId(userId);

        videoService.addVideos(video);
        elasticSearchService.addVideo(video); // 在 es 中添加视频
        return JsonResponse.success();
    }

    /**
     * 根据 area 分页查询视频
     */
    @GetMapping("/videos")
    public JsonResponse<PageResult<Video>> pageListVideos(@RequestParam(required = true) Integer no,
                                                          @RequestParam(required = true) Integer size,
                                                          String area) {
        PageResult<Video> result = videoService.pageListVideos(no, size, area);
        return new JsonResponse<>(result);
    }

    /**
     * 视频在线观看
     */
    @GetMapping("/video-slices")
    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String url) throws Exception {
        videoService.viewVideoOnlineBySlices(request, response, url);
    }

    /**
     * 视频点赞
     */
    @PostMapping("/video-likes")
    public JsonResponse<String> addVideoLike(@RequestParam(required = true) Long videoId) {
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoLike(videoId, userId);
        return JsonResponse.success();
    }

    /**
     * 取消视频点赞
     */
    @DeleteMapping("/video-likes")
    public JsonResponse<String> deleteVideoLike(@RequestParam(required = true) Long videoId) {
        Long userId = userSupport.getCurrentUserId();
        videoService.deleteVideoLike(videoId, userId);
        return JsonResponse.success();
    }

    /**
     * 查询视频点赞数量和当前用户是否对该视频点赞
     */
    @GetMapping("/video-likes")
    public JsonResponse<Map<String, Object>> getVideoLikes(@RequestParam(required = true) Long videoId) {
        Long userId = null;
        try {
            userId = userSupport.getCurrentUserId(); // 即使不登陆, 也可以查看视频, 也可以查看视频点赞数量
        } catch (Exception ignored) {
        }

        Map<String, Object> result = videoService.getVideoLikes(videoId, userId);
        return new JsonResponse<>(result);
    }

    /**
     * 收藏视频
     */
    @PostMapping("/video-collections")
    public JsonResponse<String> addVideoCollection(@RequestBody VideoCollection videoCollection) {
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoCollection(videoCollection, userId);
        return JsonResponse.success();
    }

    /**
     * 取消收藏视频
     */
    @DeleteMapping("/video-collections")
    public JsonResponse<String> deleteVideoCollection(@RequestParam(required = true) Long videoId) {
        Long userId = userSupport.getCurrentUserId();
        videoService.deleteVideoCollection(videoId, userId);
        return JsonResponse.success();
    }

    /**
     * 查询视频收藏数量和当前用户是否对该视频收藏
     */
    @GetMapping("/video-collections")
    public JsonResponse<Map<String, Object>> getVideoCollections(@RequestParam(required = true) Long videoId) {
        Long userId = null;
        try {
            userId = userSupport.getCurrentUserId(); // 即使不登陆, 也可以查看视频, 也可以查看视频收藏数量
        } catch (Exception ignored) {
        }

        Map<String, Object> result = videoService.getVideoCollections(videoId, userId);
        return new JsonResponse<>(result);
    }

    /**
     * 视频投币
     */
    @PostMapping("/video-coins")
    public JsonResponse<String> addVideoCoins(@RequestBody VideoCoin videoCoin) {
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoCoins(videoCoin, userId);
        return JsonResponse.success();
    }

    /**
     * 查询视频投币数量和当前用户是否对该视频投币
     */
    @GetMapping("/video-coins")
    public JsonResponse<Map<String, Object>> getVideoCoins(@RequestParam Long videoId) {
        Long userId = null;
        try {
            userId = userSupport.getCurrentUserId(); // 即使不登陆, 也可以查看视频, 也可以查看视频投币数量
        } catch (Exception ignored) {
        }

        Map<String, Object> result = videoService.getVideoCoins(videoId, userId);
        return new JsonResponse<>(result);
    }

    /**
     * 添加视频评论
     */
    @PostMapping("/video-comments")
    public JsonResponse<String> addVideoComment(@RequestBody VideoComment videoComment) {
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoComment(videoComment, userId);
        return JsonResponse.success();
    }

    /**
     * 根据 videoId 分页查询视频评论评论者用户基本信息
     */
    @GetMapping("/video-comments")
    public JsonResponse<PageResult<VideoComment>> pageListVideoComments(@RequestParam Integer size,
                                                                        @RequestParam Integer no,
                                                                        @RequestParam Long videoId) {
        PageResult<VideoComment> result = videoService.pageListVideoComments(size, no, videoId);
        return new JsonResponse<>(result);
    }

    /**
     * 根据 videoId 获取视频详情
     */
    @GetMapping("/video-details")
    public JsonResponse<Map<String, Object>> getVideoDetails(@RequestParam Long videoId) {
        Map<String, Object> result = videoService.getVideoDetails(videoId);
        return new JsonResponse<>(result);
    }

    /**
     * 添加视频观看记录
     */
    @PostMapping("/video-views")
    public JsonResponse<String> addVideoView(@RequestBody VideoView videoView, HttpServletRequest request) {
        Long userId;
        try {
            userId = userSupport.getCurrentUserId();
            videoView.setUserId(userId);
            videoService.addVideoView(videoView, request);
        } catch (Exception e) {
            videoService.addVideoView(videoView, request);
        }
        return JsonResponse.success();
    }

    /**
     * 根据 videoId 查询视频播放量
     */
    @GetMapping("/video-view-counts")
    public JsonResponse<Integer> getVideoViewCounts(@RequestParam Long videoId) {
        Integer count = videoService.getVideoViewCounts(videoId);
        return new JsonResponse<>(count);
    }

    /**
     * 根据 userId 进行视频内容推荐, 基于用户的协同推荐
     */
    @GetMapping("/recommendations")
    public JsonResponse<List<Video>> recommend() throws TasteException {
        Long userId = userSupport.getCurrentUserId();
        List<Video> list = videoService.recommend(userId);
        return new JsonResponse<>(list);
    }

    /**
     * 视频帧截取生成黑白剪影并保存到数据库, 返回视频二值图列表
     */
    @GetMapping("/video-frames")
    public JsonResponse<List<VideoBinaryPicture>> captureVideoFrame(@RequestParam Long videoId,
                                                                    @RequestParam String fileMd5) throws Exception {
        List<VideoBinaryPicture> list = videoService.convertVideoToImage(videoId, fileMd5);
        return new JsonResponse<>(list);
    }


    /**
     * 根据 videoId、videoTimestamp、frameNo 查询视频黑白剪影列表
     */
    @GetMapping("/video-binary-images")
    public JsonResponse<List<VideoBinaryPicture>> getVideoBinaryImages(@RequestParam Long videoId,
                                                                       Long videoTimestamp,
                                                                       String frameNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("videoId", videoId);
        params.put("videoTimestamp", videoTimestamp);
        params.put("frameNo", frameNo);
        List<VideoBinaryPicture> list = videoService.getVideoBinaryImages(params);
        return new JsonResponse<>(list);
    }

    /**
     * 根据 videoId 查询视频标签列表
     */
    @GetMapping("/video-tags")
    public JsonResponse<List<VideoTag>> getVideoTagsByVideoId(@RequestParam Long videoId) {
        List<VideoTag> list = videoService.getVideoTagsByVideoId(videoId);
        return new JsonResponse<>(list);
    }

    /**
     * 删除视频标签
     */
    @DeleteMapping("/video-tags")
    public JsonResponse<String> deleteVideoTags(@RequestBody JSONObject params) {
        String tagIdList = params.getString("tagIdList");
        Long videoId = params.getLong("videoId");
        videoService.deleteVideoTags(JSONArray.parseArray(tagIdList).toJavaList(Long.class), videoId);
        return JsonResponse.success();
    }

}
