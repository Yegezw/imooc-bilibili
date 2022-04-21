package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.user.FollowingGroup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FollowingGroupDao {

    /**
     * 根据 type 查询关注分组
     */
    FollowingGroup getByType(String type);

    /**
     * 根据 id 查询关注分组
     */
    FollowingGroup getById(Long id);

    /**
     * 根据 userId 查询关注分组列表
     */
    List<FollowingGroup> getByUserId(Long userId);

    /**
     * 添加关注分组
     */
    Long addFollowingGroup(FollowingGroup followingGroup);

    /**
     * 根据 userId 查询关注分组
     */
    List<FollowingGroup> getUserFollowingGroups(Long userId);
}
