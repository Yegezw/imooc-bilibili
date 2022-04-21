package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.user.UserFollowing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserFollowingDao {

    /**
     * 根据 userId 和 followingId 删除用户关注
     */
    Integer deleteUserFollowing(@Param("userId") Long userId, @Param("followingId") Long followingId);

    /**
     * 添加用户关注
     */
    Integer addUserFollowing(UserFollowing userFollowing);

    /**
     * 根据 userId 查询关注用户列表
     */
    List<UserFollowing> getUserFollowings(Long userId);

    /**
     * 根据 userId 查询用户粉丝列表
     */
    List<UserFollowing> getUserFans(@Param("followingId") Long userId);
}
