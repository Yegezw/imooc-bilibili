package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.UserFollowingDao;
import com.imooc.bilibili.domain.user.FollowingGroup;
import com.imooc.bilibili.domain.user.User;
import com.imooc.bilibili.domain.user.UserFollowing;
import com.imooc.bilibili.domain.user.UserInfo;
import com.imooc.bilibili.domain.constant.UserConstant;
import com.imooc.bilibili.domain.exception.ConditionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserFollowingService {

    @Autowired
    private UserFollowingDao userFollowingDao;

    @Autowired
    private FollowingGroupService followingGroupService;

    @Autowired
    private UserService userService;

    /**
     * 添加用户关注
     */
    @Transactional
    public void addUserFollowing(UserFollowing userFollowing) {
        Long groupId = userFollowing.getGroupId();
        if (groupId == null) {
            FollowingGroup followingGroup = followingGroupService.getByType(UserConstant.USER_FOLLOWING_GROUP_TYPE_DEFAULT);
            userFollowing.setGroupId(followingGroup.getId()); // 默认分组的 id
        } else {
            FollowingGroup followingGroup = followingGroupService.getById(groupId);
            if (followingGroup == null) {
                throw new ConditionException("关注分组不存在！");
            }
        }

        Long followingId = userFollowing.getFollowingId();
        User user = userService.getUserById(followingId);
        if (user == null) {
            throw new ConditionException("关注用户不存在！");
        }

        userFollowingDao.deleteUserFollowing(userFollowing.getUserId(), followingId); // 先删除
        userFollowing.setCreateTime(new Date());
        userFollowingDao.addUserFollowing(userFollowing); // 后添加
    }

    /**
     * 获取关注用户列表
     */
    public List<FollowingGroup> getUserFollowings(Long userId) {
        List<UserFollowing> list = userFollowingDao.getUserFollowings(userId); // 关注用户列表
        Set<Long> followingIdSet = list.stream().map(UserFollowing::getFollowingId).collect(Collectors.toSet()); // 关注用户 id 列表

        List<UserInfo> userInfoList = new ArrayList<>(); // 关注用户列表基本信息
        if (followingIdSet.size() > 0) {
            userInfoList = userService.getUserInfoByUserIds(followingIdSet);
        }

        // 关注用户列表中添加用户基本信息
        for (UserFollowing userFollowing : list) {
            for (UserInfo userInfo : userInfoList) {
                if (userFollowing.getFollowingId().equals(userInfo.getUserId())) {
                    userFollowing.setUserInfo(userInfo);
                }
            }
        }

        List<FollowingGroup> groupList = followingGroupService.getByUserId(userId); // 关注分组列表
        FollowingGroup allGroup = new FollowingGroup(); // 全部关注分组
        allGroup.setName(UserConstant.USER_FOLLOWING_GROUP_ALL_NAME);
        allGroup.setFollowingUserInfoList(userInfoList); // 全部关注分组中添加关注用户列表基本信息

        List<FollowingGroup> result = new ArrayList<>();
        result.add(allGroup);
        for (FollowingGroup group : groupList) {
            List<UserInfo> infoList = new ArrayList<>();
            for (UserFollowing userFollowing : list) {
                if (group.getId().equals(userFollowing.getGroupId())) {
                    infoList.add(userFollowing.getUserInfo());
                }
            }
            group.setFollowingUserInfoList(infoList);

            result.add(group);
        }

        return result;
    }

    /**
     * 获取粉丝列表
     */
    public List<UserFollowing> getUserFans(Long userId){
        List<UserFollowing> fanList = userFollowingDao.getUserFans(userId); // 粉丝用户列表
        Set<Long> fanIdSet = fanList.stream().map(UserFollowing::getUserId).collect(Collectors.toSet());

        List<UserInfo> userInfoList = new ArrayList<>(); // 粉丝基本信息列表
        if(fanIdSet.size() > 0){
            userInfoList = userService.getUserInfoByUserIds(fanIdSet);
        }

        List<UserFollowing> followingList = userFollowingDao.getUserFollowings(userId); // 关注用户列表

        for(UserFollowing fan : fanList){
            // 粉丝用户列表中添加粉丝基本信息, 设置已关注为 false
            for(UserInfo userInfo : userInfoList){
                if(fan.getUserId().equals(userInfo.getUserId())){
                    userInfo.setFollowed(false);
                    fan.setUserInfo(userInfo);
                }
            }

            for(UserFollowing following : followingList){
                if(following.getFollowingId().equals(fan.getUserId())){
                    fan.getUserInfo().setFollowed(true); // 设置已关注为 true
                }
            }
        }

        return fanList;
    }

    /**
     * 添加关注分组
     */
    public Long addUserFollowingGroups(FollowingGroup followingGroup) {
        followingGroup.setCreateTime(new Date());
        followingGroup.setType(UserConstant.USER_FOLLOWING_GROUP_TYPE_USER);

        followingGroupService.addFollowingGroup(followingGroup);
        return followingGroup.getId();
    }

    /**
     * 根据 userId 获取关注分组
     */
    public List<FollowingGroup> getUserFollowingGroups(Long userId) {
        return followingGroupService.getUserFollowingGroups(userId);
    }

    /**
     * 填充用户关注状态
     */
    public List<UserInfo> checkFollowingStatus(List<UserInfo> userInfoList, Long userId) {
        List<UserFollowing> followingList = userFollowingDao.getUserFollowings(userId); // 关注用户列表
        Set<Long> followingIdSet = followingList.stream().map(UserFollowing::getFollowingId).collect(Collectors.toSet()); // 关注用户 id 列表

        for (UserInfo userInfo : userInfoList) {
            userInfo.setFollowed(followingIdSet.contains(userInfo.getUserId()));
        }

        return userInfoList;
    }
}
