package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.auth.AuthRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthRoleDao {

    /**
     * 根据 code 查询角色
     */
    AuthRole getRoleByCode(String code);
}
