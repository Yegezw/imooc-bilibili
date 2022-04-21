package com.imooc.bilibili.service;

import com.imooc.bilibili.domain.auth.*;
import com.imooc.bilibili.domain.constant.AuthRoleConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAuthService {

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private AuthRoleService authRoleService;

    /**
     * 根据 userId 获取用户权限
     */
    public UserAuthorities getUserAuthorities(Long userId) {
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId); // 根据 userId 查询用户角色
        Set<Long> roleIdSet = userRoleList.stream().map(UserRole::getRoleId).collect(Collectors.toSet()); // 角色 id 列表

        List<AuthRoleElementOperation> roleElementOperationList = authRoleService.getRoleElementOperationByRoleIds(roleIdSet);
        List<AuthRoleMenu> roleMenuList = authRoleService.getRoleMenuByRoleIds(roleIdSet);

        return new UserAuthorities(roleElementOperationList, roleMenuList);
    }

    /**
     * 根据 usrId 添加默认用户角色(code = Lv0 的角色)
     */
    public void addUserDefaultRole(Long userId) {
        AuthRole authRole = authRoleService.getRoleByCode(AuthRoleConstant.ROLE_LV0); // 根据 code 查询角色

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(authRole.getId());

        userRoleService.addUserRole(userRole);
    }
}
