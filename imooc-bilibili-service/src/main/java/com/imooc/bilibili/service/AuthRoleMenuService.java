package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.AuthRoleMenuDao;
import com.imooc.bilibili.domain.auth.AuthRole;
import com.imooc.bilibili.domain.auth.AuthRoleMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AuthRoleMenuService {

    @Autowired
    private AuthRoleMenuDao authRoleMenuDao;

    /**
     * 根据角色 id 列表查询页面访问权限
     */
    public List<AuthRoleMenu> getRoleMenuByRoleIds(Set<Long> roleIdSet) {
        return authRoleMenuDao.getRoleMenuByRoleIds(roleIdSet);
    }

}
