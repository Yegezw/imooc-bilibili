package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.user.UserMoment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMomentsDao {

    /**
     * 发布动态
     */
    Integer addUserMoments(UserMoment userMoment);
}
