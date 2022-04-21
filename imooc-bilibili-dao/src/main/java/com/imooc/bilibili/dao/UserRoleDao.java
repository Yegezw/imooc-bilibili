package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.auth.UserRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserRoleDao {

    /**
     * 根据 userId 查询用户角色
     */
    List<UserRole> getUserRoleByUserId(Long userId);

    /**
     * 添加用户角色
     */
    Integer addUserRole(UserRole userRole);
}
