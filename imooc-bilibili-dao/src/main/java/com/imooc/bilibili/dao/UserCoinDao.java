package com.imooc.bilibili.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface UserCoinDao {

    /**
     * 根据 userId 查询用户 coinsAmount(硬币数)
     */
    Integer getUserCoinsAmount(Long userId);

    /**
     * 根据 userId 更新用户 coinsAmount(硬币数)
     */
    Integer updateUserCoinAmount(@Param("userId") Long userId, @Param("amount") Integer amount, @Param("updateTime") Date updateTime);
}