package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.FollowingGroupDao;
import com.imooc.bilibili.domain.user.FollowingGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FollowingGroupService {

    @Autowired
    private FollowingGroupDao followingGroupDao;

    /**
     * 根据 type 查询关注分组
     */
    public FollowingGroup getByType(String type) {
        return followingGroupDao.getByType(type);
    }

    /**
     * 根据 id 查询关注分组
     */
    public FollowingGroup getById(Long id) {
        return followingGroupDao.getById(id);
    }

    /**
     * 根据 userId 查询关注分组列表
     */
    public List<FollowingGroup> getByUserId(Long userId) {
        return followingGroupDao.getByUserId(userId);
    }

    /**
     * 添加关注分组
     */
    public void addFollowingGroup(FollowingGroup followingGroup) {
        followingGroupDao.addFollowingGroup(followingGroup);
    }

    /**
     * 根据 userId 获取关注分组
     */
    public List<FollowingGroup> getUserFollowingGroups(Long userId) {
        return followingGroupDao.getUserFollowingGroups(userId);
    }
}
