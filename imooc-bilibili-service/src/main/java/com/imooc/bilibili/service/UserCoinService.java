package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.UserCoinDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserCoinService {

    @Autowired
    private UserCoinDao userCoinDao;

    /**
     * 根据 userId 查询用户 coinsAmount(硬币数)
     */
    public Integer getUserCoinsAmount(Long userId) {
        return userCoinDao.getUserCoinsAmount(userId);
    }

    /**
     * 根据 userId 更新用户 coinsAmount(硬币数)
     */
    public void updateUserCoinsAmount(Long userId, Integer amount) {
        Date updateTime = new Date();
        userCoinDao.updateUserCoinAmount(userId, amount, updateTime);
    }
}