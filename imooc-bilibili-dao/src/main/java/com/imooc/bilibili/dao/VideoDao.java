package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.user.UserPreference;
import com.imooc.bilibili.domain.video.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface VideoDao {

    /**
     * 添加视频
     */
    Integer addVideos(Video video);

    /**
     * 添加视频标签
     */
    Integer batchAddVideoTags(List<VideoTag> videoTagList);

    /**
     * 查询满足 params 条件的视频总记录数(根据 area 查询)
     */
    Integer pageCountVideos(Map<String, Object> params);

    /**
     * 分页查询满足 params 条件的视频列表(根据 area 查询)
     */
    List<Video> pageListVideos(Map<String, Object> params);

    /**
     * 通过 id 查询视频
     */
    Video getVideoById(Long id);

    /**
     * 通过 videoId 和 userId 查询视频点赞
     */
    VideoLike getVideoLikeByVideoIdAndUserId(@Param("videoId") Long videoId, @Param("userId") Long userId);

    /**
     * 添加视频点赞
     */
    Integer addVideoLike(VideoLike videoLike);

    /**
     * 通过 videoId 和 userId 删除视频点赞
     */
    Integer deleteVideoLike(@Param("videoId") Long videoId, @Param("userId") Long userId);

    /**
     * 通过 videoId 查询视频点赞总记录数
     */
    Long getVideoLikes(Long videoId);

    /**
     * 添加收藏视频
     */
    Integer addVideoCollection(VideoCollection videoCollection);

    /**
     * 通过 videoId 和 userId 删除收藏视频
     */
    Integer deleteVideoCollection(@Param("videoId") Long videoId, @Param("userId") Long userId);

    /**
     * 通过 videoId 查询视频收藏总记录数
     */
    Long getVideoCollections(Long videoId);

    /**
     * 通过 videoId 和 userId 查询收藏视频
     */
    VideoCollection getVideoCollectionByVideoIdAndUserId(@Param("videoId") Long videoId, @Param("userId") Long userId);

    /**
     * 通过 videoId 和 userId 查询视频投币
     */
    VideoCoin getVideoCoinByVideoIdAndUserId(@Param("videoId") Long videoId, @Param("userId") Long userId);

    /**
     * 添加视频投币
     */
    Integer addVideoCoin(VideoCoin videoCoin);

    /**
     * 更新视频投币
     */
    Integer updateVideoCoin(VideoCoin videoCoin);

    /**
     * 通过 videoId 查询视频投币总记录数的 sum(amount)
     */
    Long getVideoCoinsAmount(Long videoId);

    /**
     * 添加视频评论
     */
    Integer addVideoComment(VideoComment videoComment);

    /**
     * 根据 videoId 和 rootId is null 查询视频一级评论总记录数
     */
    Integer pageCountVideoComments(Map<String, Object> params);

    /**
     * 根据 videoId 和 rootId is null 分页查询视频一级评论列表
     */
    List<VideoComment> pageListVideoComments(Map<String, Object> params);

    /**
     * 根据 rootId 批量查询视频二级评论列表
     */
    List<VideoComment> batchGetVideoCommentsByRootIds(@Param("rootIdList") List<Long> parentIdList);

    /**
     * 根据 id 获取视频详情
     */
    Video getVideoDetails(Long id);

    /**
     * 根据 params 条件查询视频观看记录
     */
    VideoView getVideoView(Map<String, Object> params);

    /**
     * 添加视频观看记录
     */
    Integer addVideoView(VideoView videoView);

    /**
     * 根据 videoId 查询视频播放量
     */
    Integer getVideoViewCounts(Long videoId);

    /**
     * 根据 t_video_operation 查询用户偏好列表
     */
    List<UserPreference> getAllUserPreference();

    /**
     * 根据 id 列表批量查询视频列表
     */
    List<Video> batchGetVideosByIds(@Param("idList") List<Long> idList);

    /**
     * 批量添加视频二值图
     */
    Integer batchAddVideoBinaryPictures(@Param("pictureList") List<VideoBinaryPicture> pictureList);

    /**
     * 根据 videoId、videoTimestamp、frameNo 查询视频黑白剪影列表
     */
    List<VideoBinaryPicture> getVideoBinaryImages(Map<String, Object> params);

    /**
     * 根据 videoId 查询视频标签
     */
    List<VideoTag> getVideoTagsByVideoId(Long videoId);

    /**
     * 删除视频标签
     */
    Integer deleteVideoTags(@Param("tagIdList") List<Long> tagIdList, @Param("videoId") Long videoId);
}
