package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.user.RefreshTokenDetail;
import com.imooc.bilibili.domain.user.User;
import com.imooc.bilibili.domain.user.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface UserDao {

    /**
     * 根据 phone 查询用户账户信息
     */
    User getUserByPhone(String phone);

    /**
     * 添加用户账户信息
     */
    Integer addUser(User user);

    /**
     * 添加用户基本信息
     */
    Integer addUserInfo(UserInfo userInfo);

    /**
     * 根据 id 查询用户账户信息
     */
    User getUserById(Long id);

    /**
     * 根据 userId 用户基本信息
     */
    UserInfo getUserInfoByUserId(Long userId);

    /**
     * 更新用户基本信息
     */
    Integer updateUsers(User user);

    /**
     * 根据 phone 或 email 查询用户账户信息
     */
    User getUserByPhoneOrEmail(@Param("phone") String phone, @Param("email") String email);

    /**
     * 更新用户账户信息
     */
    Integer updateUserInfos(UserInfo userInfo);

    /**
     * 根据 userIdList 批量查询用户基本信息
     */
    List<UserInfo> getUserInfoByUserIds(Set<Long> userIdList);

    /**
     * 查询满足 params 条件的用户基本信息总记录数(根据 nick 模糊查询)
     */
    Integer pageCountUserInfo(Map<String, Object> params);

    /**
     * 分页查询满足 params 条件的用户基本信息列表(根据 nick 模糊查询)
     */
    List<UserInfo> pageListUserInfos(Map<String, Object> params);

    /**
     * 删除 userId 的 refreshToken
     */
    Integer deleteRefreshToken(@Param("userId") Long userId, @Param("refreshToken") String refreshToken);

    /**
     * 给 userId 添加 refreshToken
     */
    Integer addRefreshToken(@Param("userId") Long userId,
                            @Param("refreshToken") String refreshToken,
                            @Param("createTime") Date createTime);

    /**
     * 根据 refreshToken 获取 RefreshTokenDetail
     */
    RefreshTokenDetail getRefreshTokenDetail(String refreshToken);

    /**
     * 根据 userIdList 批量查询用户基本信息
     */
    List<UserInfo> batchGetUserInfoByUserIds(Set<Long> userIdList);

    /**
     * 根据 userId 查询 refreshToken
     */
    String getRefreshTokenByUserId(Long userId);
}
