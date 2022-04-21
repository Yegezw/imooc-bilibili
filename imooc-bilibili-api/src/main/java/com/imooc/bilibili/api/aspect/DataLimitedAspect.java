package com.imooc.bilibili.api.aspect;

import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.user.UserMoment;
import com.imooc.bilibili.domain.auth.UserRole;
import com.imooc.bilibili.domain.constant.AuthRoleConstant;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.UserRoleService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Order(1) // 优先级
@Component
public class DataLimitedAspect {

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private UserRoleService userRoleService;

    /**
     * 切点是 @DataLimited
     */
    @Pointcut("@annotation(com.imooc.bilibili.domain.annotation.DataLimited)")
    public void check() {
    }

    @Before("check()")
    public void doBefore(JoinPoint joinPoint) {
        Long userId = userSupport.getCurrentUserId();
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId); // 根据 userId 查询用户角色
        Set<String> roleCodeSet = userRoleList.stream().map(UserRole::getRoleCode).collect(Collectors.toSet());

        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof UserMoment) {
                UserMoment userMoment = (UserMoment) arg;
                String type = userMoment.getType();
                // Lv1 可以发的动态类型可以是: (0 视频), 不能是: (1 直播), (2 专栏动态)
                if (roleCodeSet.contains(AuthRoleConstant.ROLE_LV1) && !"0".equals(type)) {
                    throw new ConditionException("参数异常！");
                }
            }
        }
    }

}
