package com.imooc.bilibili.domain.video;

import com.imooc.bilibili.domain.user.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoComment {

    private Long id;

    private Long videoId; // 视频 id

    private Long userId; // 评论者的用户 id

    private String comment;  // 评论

    private Long replyUserId;  // 评论所回复的用户 id

    private Long rootId; // 根节点评论 id

    private Date createTime;

    private Date updateTime;

    private List<VideoComment> childList; // 二级评论

    private UserInfo userInfo; // 评论者的用户信息

    private UserInfo replyUserInfo; // 评论所回复的用户信息
}
