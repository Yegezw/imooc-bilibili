package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.AuthRoleDao;
import com.imooc.bilibili.domain.auth.AuthRole;
import com.imooc.bilibili.domain.auth.AuthRoleElementOperation;
import com.imooc.bilibili.domain.auth.AuthRoleMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AuthRoleService {

    @Autowired
    private AuthRoleElementOperationService authRoleElementOperationService;

    @Autowired
    private AuthRoleMenuService authRoleMenuService;

    @Autowired
    private AuthRoleDao authRoleDao;

    /**
     * 根据角色 id 列表查询页面元素操作权限
     */
    public List<AuthRoleElementOperation> getRoleElementOperationByRoleIds(Set<Long> roleIdSet) {
        return authRoleElementOperationService.getRoleElementOperationByRoleIds(roleIdSet);
    }

    /**
     * 根据角色 id 列表查询页面访问权限
     */
    public List<AuthRoleMenu> getRoleMenuByRoleIds(Set<Long> roleIdSet) {
        return authRoleMenuService.getRoleMenuByRoleIds(roleIdSet);
    }

    /**
     * 根据 code 查询角色
     */
    public AuthRole getRoleByCode(String code) {
        return authRoleDao.getRoleByCode(code);
    }
}
