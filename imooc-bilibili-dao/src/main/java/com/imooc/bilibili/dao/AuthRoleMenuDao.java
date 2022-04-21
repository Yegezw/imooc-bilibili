package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.auth.AuthRole;
import com.imooc.bilibili.domain.auth.AuthRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface AuthRoleMenuDao {

    /**
     * 根据角色 id 列表查询页面访问权限
     */
    List<AuthRoleMenu> getRoleMenuByRoleIds(@Param("roleIdSet") Set<Long> roleIdSet);
}
