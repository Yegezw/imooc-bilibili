package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.VideoDao;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.domain.user.User;
import com.imooc.bilibili.domain.user.UserInfo;
import com.imooc.bilibili.domain.user.UserPreference;
import com.imooc.bilibili.domain.video.*;
import com.imooc.bilibili.service.util.FastDFSUtil;
import com.imooc.bilibili.service.util.ImageUtil;
import com.imooc.bilibili.service.util.IpUtil;
import eu.bitwalker.useragentutils.UserAgent;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Autowired
    private VideoDao videoDao;

    @Autowired
    private FastDFSUtil fastDFSUtil;

    @Autowired
    private UserCoinService userCoinService;

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private ImageUtil imageUtil;

    private static final int FRAME_NO = 256; // 每隔多少帧进行截取

    /**
     * 视频投稿, 添加视频和视频标签
     */
    @Transactional
    public void addVideos(Video video) {
        video.setCreateTime(new Date());
        videoDao.addVideos(video); // 添加视频

        Long videoId = video.getId();
        List<VideoTag> tagList = video.getVideoTagList();
        tagList.forEach(tag -> {
            tag.setVideoId(videoId);
            tag.setCreateTime(new Date());
        });
        videoDao.batchAddVideoTags(tagList); // 添加视频标签
    }

    /**
     * 根据 area 分页查询视频
     */
    public PageResult<Video> pageListVideos(Integer no, Integer size, String area) {
        if (no == null || size == null) {
            throw new ConditionException("参数异常！");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("start", (no - 1) * size);
        params.put("limit", size);
        params.put("area", area);

        Integer total = videoDao.pageCountVideos(params);
        List<Video> list = new ArrayList<>();
        if (total > 0) {
            list = videoDao.pageListVideos(params);
        }

        return new PageResult<>(total, list);
    }

    /**
     * 视频在线观看
     */
    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String url) throws Exception {
        fastDFSUtil.viewVideoOnlineBySlices(request, response, url);
    }

    /**
     * 添加视频点赞
     */
    public void addVideoLike(Long videoId, Long userId) {
        Video video = videoDao.getVideoById(videoId); // 通过 id 查询视频
        if (video == null) {
            throw new ConditionException("非法视频！");
        }

        VideoLike videoLike = videoDao.getVideoLikeByVideoIdAndUserId(videoId, userId); // 通过 videoId 和 userId 查询视频点赞
        if (videoLike != null) {
            throw new ConditionException("已经赞过！");
        }

        videoLike = new VideoLike();
        videoLike.setVideoId(videoId);
        videoLike.setUserId(userId);
        videoLike.setCreateTime(new Date());
        videoDao.addVideoLike(videoLike);
    }

    /**
     * 通过 videoId 和 userId 删除视频点赞
     */
    public void deleteVideoLike(Long videoId, Long userId) {
        videoDao.deleteVideoLike(videoId, userId);
    }

    /**
     * <p>通过 videoId 查询视频点赞总记录数</p>
     * 通过 videoId 和 userId 查询视频点赞, 根据结果是否为 null 判断当前用户是否对该视频点赞
     */
    public Map<String, Object> getVideoLikes(Long videoId, Long userId) {
        Long count = videoDao.getVideoLikes(videoId);

        VideoLike videoLike = videoDao.getVideoLikeByVideoIdAndUserId(videoId, userId);
        boolean like = (videoLike != null);

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    /**
     * 添加收藏视频
     */
    @Transactional
    public void addVideoCollection(VideoCollection videoCollection, Long userId) {
        Long videoId = videoCollection.getVideoId();
        Long groupId = videoCollection.getGroupId();
        if (videoId == null || groupId == null) {
            throw new ConditionException("参数异常！");
        }

        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("非法视频！");
        }

        // 删除原有视频收藏
        videoDao.deleteVideoCollection(videoId, userId);
        // 添加新的视频收藏
        videoCollection.setUserId(userId);
        videoCollection.setCreateTime(new Date());
        videoDao.addVideoCollection(videoCollection);
    }

    /**
     * 通过 videoId 和 userId 删除收藏视频
     */
    public void deleteVideoCollection(Long videoId, Long userId) {
        videoDao.deleteVideoCollection(videoId, userId);
    }

    /**
     * <p>通过 videoId 查询收藏视频总记录数</p>
     * 通过 videoId 和 userId 查询收藏视频, 根据结果是否为 null 判断当前用户是否对该视频收藏
     */
    public Map<String, Object> getVideoCollections(Long videoId, Long userId) {
        Long count = videoDao.getVideoCollections(videoId);

        VideoCollection videoCollection = videoDao.getVideoCollectionByVideoIdAndUserId(videoId, userId);
        boolean collection = (videoCollection != null);

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", collection);
        return result;
    }

    /**
     * 添加收藏投币
     */
    @Transactional
    public void addVideoCoins(VideoCoin videoCoin, Long userId) {
        Long videoId = videoCoin.getVideoId();
        Integer amount = videoCoin.getAmount();
        if (videoId == null) {
            throw new ConditionException("参数异常！");
        }

        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("非法视频！");
        }

        // 查询当前登录用户是否拥有足够的硬币
        Integer userCoinsAmount = userCoinService.getUserCoinsAmount(userId);
        userCoinsAmount = (userCoinsAmount == null ? 0 : userCoinsAmount);
        if (amount > userCoinsAmount) {
            throw new ConditionException("硬币数量不足！");
        }

        // 查询当前登录用户对该视频已经投了多少硬币
        VideoCoin dbVideoCoin = videoDao.getVideoCoinByVideoIdAndUserId(videoId, userId);

        if (dbVideoCoin == null) {
            videoCoin.setUserId(userId);
            videoCoin.setCreateTime(new Date());
            // 新增视频投币
            videoDao.addVideoCoin(videoCoin);
        } else {
            Integer dbAmount = dbVideoCoin.getAmount();
            dbAmount += amount;
            // 更新视频投币
            videoCoin.setUserId(userId);
            videoCoin.setAmount(dbAmount);
            videoCoin.setUpdateTime(new Date());
            videoDao.updateVideoCoin(videoCoin);
        }

        // 更新用户当前硬币总数
        userCoinService.updateUserCoinsAmount(userId, (userCoinsAmount - amount));
    }

    /**
     * <p>通过 videoId 查询视频投币总记录数的 sum(amount)</p>
     * 通过 videoId 和 userId 查询视频投币, 根据结果是否为 null 判断当前用户是否对该视频投币
     */
    public Map<String, Object> getVideoCoins(Long videoId, Long userId) {
        Long count = videoDao.getVideoCoinsAmount(videoId);

        VideoCoin videoCollection = videoDao.getVideoCoinByVideoIdAndUserId(videoId, userId);
        boolean coins = (videoCollection != null);

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", coins);
        return result;
    }

    /**
     * 添加视频评论
     */
    public void addVideoComment(VideoComment videoComment, Long userId) {
        Long videoId = videoComment.getVideoId();
        if (videoId == null) {
            throw new ConditionException("参数异常！");
        }

        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("非法视频！");
        }

        videoComment.setUserId(userId); // 评论者的用户 id
        videoComment.setCreateTime(new Date());
        videoDao.addVideoComment(videoComment);
    }

    /**
     * 根据 videoId 分页查询视频评论和评论者用户基本信息
     */
    public PageResult<VideoComment> pageListVideoComments(Integer size, Integer no, Long videoId) {
        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("非法视频！");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("start", (no - 1) * size);
        params.put("limit", size);
        params.put("videoId", videoId);

        Integer total = videoDao.pageCountVideoComments(params); // 根据 videoId 和 rootId is null 查询视频一级评论总记录数

        List<VideoComment> list = new ArrayList<>();
        if (total > 0) {
            list = videoDao.pageListVideoComments(params); // 根据 videoId 和 rootId is null 分页查询视频一级评论列表

            // 批量查询二级评论
            List<Long> parentIdList = list.stream().map(VideoComment::getId).collect(Collectors.toList()); // 一级评论的 id 列表
            List<VideoComment> childCommentList = videoDao.batchGetVideoCommentsByRootIds(parentIdList); // 根据 rootId 批量查询视频二级评论列表

            // 批量查询用户信息
            Set<Long> userIdList = list.stream().map(VideoComment::getUserId).collect(Collectors.toSet()); // 一级评论者的 userId 列表
            Set<Long> replyUserIdList = childCommentList.stream().map(VideoComment::getUserId).collect(Collectors.toSet()); // 二级评论者 userId 列表
            Set<Long> childUserIdList = childCommentList.stream().map(VideoComment::getReplyUserId).collect(Collectors.toSet()); // 二级评论所回复的 userId 列表
            userIdList.addAll(replyUserIdList);
            userIdList.addAll(childUserIdList);

            List<UserInfo> userInfoList = userService.batchGetUserInfoByUserIds(userIdList); // 所有评论者的用户信息列表
            Map<Long, UserInfo> userInfoMap = userInfoList.stream().collect(Collectors.toMap(UserInfo::getUserId, userInfo -> userInfo)); // 所有评论者的用户信息映射

            // 遍历一级评论, 一级评论的 id = 一级评论的二级评论的 rootId
            list.forEach(comment -> {
                Long id = comment.getId(); // 一级评论 id
                List<VideoComment> childList = new ArrayList<>(); // 一级评论的二级评论列表

                // 遍历二级评论
                childCommentList.forEach(child -> {
                    if (id.equals(child.getRootId())) { // 一级评论的 id = 一级评论的二级评论的 rootId
                        child.setUserInfo(userInfoMap.get(child.getUserId())); // 填充二级评论者的用户信息
                        child.setReplyUserInfo(userInfoMap.get(child.getReplyUserId())); // 填充二级评论所回复的用户信息
                        childList.add(child);
                    }
                });

                comment.setChildList(childList); // 填充一级评论的二级评论列表
                comment.setUserInfo(userInfoMap.get(comment.getUserId())); // 填充一级评论者的用户信息
            });
        }

        return new PageResult<>(total, list);
    }

    /**
     * 根据 videoId 获取视频详情
     */
    public Map<String, Object> getVideoDetails(Long videoId) {
        Video video = videoDao.getVideoDetails(videoId);

        Long userId = video.getUserId();
        User user = userService.getUserInfo(userId);
        UserInfo userInfo = user.getUserInfo();

        Map<String, Object> result = new HashMap<>();
        result.put("video", video);
        result.put("userInfo", userInfo);
        return result;
    }

    /**
     * 添加视频观看记录
     */
    public void addVideoView(VideoView videoView, HttpServletRequest request) {
        Long userId = videoView.getUserId();
        Long videoId = videoView.getVideoId();

        // 生成 clientId(操作系统 + 浏览器) 和 ip
        String agent = request.getHeader("User-Agent");
        UserAgent userAgent = UserAgent.parseUserAgentString(agent);
        String clientId = String.valueOf(userAgent.getId());
        String ip = IpUtil.getIP(request);

        Map<String, Object> params = new HashMap<>();
        if (userId != null) {
            params.put("userId", userId); // 用户模式
        } else {
            // 游客模式
            params.put("ip", ip);
            params.put("clientId", clientId);
        }

        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        params.put("today", sdf.format(now));
        params.put("videoId", videoId);

        // 添加观看记录
        VideoView dbVideoView = videoDao.getVideoView(params);
        if (dbVideoView == null) {
            videoView.setIp(ip);
            videoView.setClientId(clientId);
            videoView.setCreateTime(new Date());
            videoDao.addVideoView(videoView);
        }
    }

    /**
     * 根据 videoId 查询视频播放量
     */
    public Integer getVideoViewCounts(Long videoId) {
        return videoDao.getVideoViewCounts(videoId);
    }

    /**
     * 根据 userId 进行视频内容推荐, 基于用户的协同推荐
     */
    public List<Video> recommend(Long userId) throws TasteException {
        List<UserPreference> list = videoDao.getAllUserPreference(); // 根据 t_video_operation 查询用户偏好列表
        // 创建数据模型
        DataModel dataModel = this.createDataModel(list);
        // 获取用户相似程度
        UserSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
        System.out.println(similarity.userSimilarity(11, 12));
        // 获取用户邻居
        UserNeighborhood userNeighborhood = new NearestNUserNeighborhood(2, similarity, dataModel);
        long[] ar = userNeighborhood.getUserNeighborhood(userId);
        // 构建推荐器
        Recommender recommender = new GenericUserBasedRecommender(dataModel, userNeighborhood, similarity);
        // 推荐视频
        List<RecommendedItem> recommendedItems = recommender.recommend(userId, 5);
        List<Long> itemIds = recommendedItems.stream().map(RecommendedItem::getItemID).collect(Collectors.toList());
        return videoDao.batchGetVideosByIds(itemIds);
    }

    /**
     * 基于内容的协同推荐
     *
     * @param userId  用户 id
     * @param itemId  参考内容 id(根据该内容进行相似内容推荐)
     * @param howMany 需要推荐的数量
     */
    public List<Video> recommendByItem(Long userId, Long itemId, int howMany) throws TasteException {
        List<UserPreference> list = videoDao.getAllUserPreference();
        // 创建数据模型
        DataModel dataModel = this.createDataModel(list);
        // 获取内容相似程度
        ItemSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
        GenericItemBasedRecommender genericItemBasedRecommender = new GenericItemBasedRecommender(dataModel, similarity);
        // 物品推荐相拟度, 计算两个物品同时出现的次数, 次数越多任务的相拟度越高
        List<Long> itemIds = genericItemBasedRecommender.recommendedBecause(userId, itemId, howMany)
                .stream()
                .map(RecommendedItem::getItemID)
                .collect(Collectors.toList());
        // 推荐视频
        return videoDao.batchGetVideosByIds(itemIds);
    }

    /**
     * 根据用户偏好列表创建数据模型
     */
    private DataModel createDataModel(List<UserPreference> userPreferenceList) {
        FastByIDMap<PreferenceArray> fastByIdMap = new FastByIDMap<>();
        Map<Long, List<UserPreference>> map = userPreferenceList.stream().collect(Collectors.groupingBy(UserPreference::getUserId));
        Collection<List<UserPreference>> list = map.values();
        for (List<UserPreference> userPreferences : list) {
            GenericPreference[] array = new GenericPreference[userPreferences.size()];
            for (int i = 0; i < userPreferences.size(); i++) {
                UserPreference userPreference = userPreferences.get(i);
                GenericPreference item = new GenericPreference(userPreference.getUserId(), userPreference.getVideoId(), userPreference.getValue());
                array[i] = item;
            }
            fastByIdMap.put(array[0].getUserID(), new GenericUserPreferenceArray(Arrays.asList(array)));
        }
        return new GenericDataModel(fastByIdMap);
    }

    /**
     * 视频帧截取生成黑白剪影并保存到数据库, 返回视频二值图列表
     */
    public List<VideoBinaryPicture> convertVideoToImage(Long videoId, String fileMd5) throws Exception {
        // 下载视频文件
        com.imooc.bilibili.domain.video.File file = fileService.getFileByMd5(fileMd5);
        String filePath = "F:\\tmpfile\\imooc-bilibili\\fileForVideoId\\" + videoId + "." + file.getType();
        fastDFSUtil.downLoadFile(file.getUrl(), filePath);

        FFmpegFrameGrabber fFmpegFrameGrabber = FFmpegFrameGrabber.createDefault(filePath);
        fFmpegFrameGrabber.start();
        int ffLength = fFmpegFrameGrabber.getLengthInFrames();

        Frame frame;
        Java2DFrameConverter converter = new Java2DFrameConverter();
        int count = 1;

        List<VideoBinaryPicture> pictures = new ArrayList<>();
        for (int i = 1; i <= ffLength; i++) {
            long timestamp = fFmpegFrameGrabber.getTimestamp();
            frame = fFmpegFrameGrabber.grabImage();
            if (count == i) {
                if (frame == null) {
                    throw new ConditionException("无效帧");
                }
                BufferedImage bufferedImage = converter.getBufferedImage(frame);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", os);
                InputStream inputStream = new ByteArrayInputStream(os.toByteArray());
                // 输出黑白剪影文件
                java.io.File outputFile = java.io.File.createTempFile("convert-" + videoId + "-", ".png");
                BufferedImage binaryImg = imageUtil.getBodyOutline(bufferedImage, inputStream);
                ImageIO.write(binaryImg, "png", outputFile);
                // 有的浏览器或网站需要把图片白色的部分转为透明色, 使用以下方法可实现
                imageUtil.transferAlpha(outputFile, outputFile);
                // 上传视频剪影文件到 fastdfs
                String imgUrl = fastDFSUtil.uploadCommonFile(outputFile, "png");
                VideoBinaryPicture videoBinaryPicture = new VideoBinaryPicture();
                videoBinaryPicture.setFrameNo(i);
                videoBinaryPicture.setUrl(imgUrl);
                videoBinaryPicture.setVideoId(videoId);
                videoBinaryPicture.setVideoTimestamp(timestamp);
                pictures.add(videoBinaryPicture);
                count += FRAME_NO;
                // 删除临时文件
                outputFile.delete();
            }
        }
        // 删除临时文件
        java.io.File tmpFile = new File(filePath);
        tmpFile.delete();
        // 批量添加视频剪影文件
        videoDao.batchAddVideoBinaryPictures(pictures);
        return pictures;
    }

    /**
     * 根据 videoId、videoTimestamp、frameNo 查询视频黑白剪影列表
     */
    public List<VideoBinaryPicture> getVideoBinaryImages(Map<String, Object> params) {
        return videoDao.getVideoBinaryImages(params);
    }

    /**
     * 根据 videoId 查询视频标签
     */
    public List<VideoTag> getVideoTagsByVideoId(Long videoId) {
        return videoDao.getVideoTagsByVideoId(videoId);
    }

    /**
     * 删除视频标签
     */
    public void deleteVideoTags(List<Long> tagIdList, Long videoId) {
        videoDao.deleteVideoTags(tagIdList, videoId);
    }
}
