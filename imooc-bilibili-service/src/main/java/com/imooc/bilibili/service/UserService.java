package com.imooc.bilibili.service;

import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.dao.UserDao;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.user.RefreshTokenDetail;
import com.imooc.bilibili.domain.constant.UserConstant;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.domain.user.User;
import com.imooc.bilibili.domain.user.UserInfo;
import com.imooc.bilibili.service.util.MD5Util;
import com.imooc.bilibili.service.util.RSAUtil;
import com.imooc.bilibili.service.util.TokenUtil;
import com.mysql.cj.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    private ElasticSearchService elasticSearchService;

    /**
     * 用户注册, 添加用户账户信息和默认基本信息, 添加默认用户角色
     */
    public void addUser(User user) {
        String phone = user.getPhone();
        if (StringUtils.isNullOrEmpty(phone)) {
            throw new ConditionException("手机号不能为空！");
        }

        User dbUser = getUserByPhone(phone);
        if (dbUser != null) {
            throw new ConditionException("该手机号已经注册！");
        }

        Date now = new Date();
        String salt = String.valueOf(now.getTime());
        String password = user.getPassword();
        String rawPassword;
        try {
            rawPassword = RSAUtil.decrypt(password);
        } catch (Exception e) {
            throw new ConditionException("密码解密失败！");
        }
        String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");

        user.setSalt(salt);
        user.setPassword(md5Password);
        user.setCreateTime(now);

        userDao.addUser(user); // 主键自增回填

        // 添加用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setNick(UserConstant.DEFAULT_NICK);
        userInfo.setBirth(UserConstant.DEFAULT_BIRTH);
        userInfo.setGender(UserConstant.GENDER_MALE);
        userInfo.setCreateTime(now);
        userDao.addUserInfo(userInfo);

        // 添加用户信息到 es
        elasticSearchService.addUserInfo(userInfo);

        // 根据 usrId 添加默认用户角色(code = Lv0 的角色)
        userAuthService.addUserDefaultRole(user.getId());
    }

    /**
     * 根据 phone 查询用户账户信息
     */
    public User getUserByPhone(String phone) {
        return userDao.getUserByPhone(phone);
    }

    /**
     * 用户登录, 查询用户账户信息并返回 token
     */
    public String login(User user) throws Exception {
        String phone = user.getPhone() == null ? "" : user.getPhone();
        String email = user.getEmail() == null ? "" : user.getEmail();
        if (StringUtils.isNullOrEmpty(phone) && StringUtils.isNullOrEmpty(email)) {
            throw new ConditionException("参数异常！");
        }

        User dbUser = userDao.getUserByPhoneOrEmail(phone, email);
        if (dbUser == null) {
            throw new ConditionException("当前用户不存在！");
        }

        String password = user.getPassword();
        String rawPassword;
        try {
            rawPassword = RSAUtil.decrypt(password);
        } catch (Exception e) {
            throw new ConditionException("密码解密失败！");
        }
        String salt = dbUser.getSalt();
        String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");

        if (!md5Password.equals(dbUser.getPassword())) {
            throw new ConditionException("密码错误！");
        }

        user.setId(dbUser.getId()); // 使 loginForDts() 中的 user 有 userId
        return TokenUtil.generateToken(dbUser.getId());
    }

    /**
     * 根据 userId 获取用户账户信息和基本信息
     */
    public User getUserInfo(Long userId) {
        User user = userDao.getUserById(userId);
        UserInfo userInfo = userDao.getUserInfoByUserId(userId);

        user.setUserInfo(userInfo);
        return user;
    }

    /**
     * 更新用户账户信息
     */
    public void updateUsers(User user) throws Exception {
        Long id = user.getId();

        User dbUser = userDao.getUserById(id);
        if (dbUser == null) {
            throw new ConditionException("用户不存在！");
        }

        if (!StringUtils.isNullOrEmpty(user.getPassword())) {
            String rawPassword = RSAUtil.decrypt(user.getPassword());
            String md5Password = MD5Util.sign(rawPassword, dbUser.getSalt(), "UTF-8");
            user.setPassword(md5Password);
        }
        user.setUpdateTime(new Date());

        userDao.updateUsers(user);
    }

    /**
     * 更新用户基本信息
     */
    public void updateUserInfos(UserInfo userInfo) {
        userInfo.setUpdateTime(new Date());
        userDao.updateUserInfos(userInfo);
    }

    /**
     * 根据 id 查询用户账户信息
     */
    public User getUserById(Long id) {
        return userDao.getUserById(id);
    }

    /**
     * 根据 userIdList 批量查询用户基本信息
     */
    public List<UserInfo> getUserInfoByUserIds(Set<Long> userIdList) {
        return userDao.getUserInfoByUserIds(userIdList);
    }

    /**
     * 根据 nick 模糊分页查询用户列表
     */
    public PageResult<UserInfo> pageListUserInfos(JSONObject params) {
        Integer no = params.getInteger("no");
        Integer size = params.getInteger("size");
        params.put("start", (no - 1) * size);
        params.put("limit", size);

        Integer total = userDao.pageCountUserInfo(params);
        List<UserInfo> list = new ArrayList<>();
        if (total > 0) {
            list = userDao.pageListUserInfos(params);
        }

        return new PageResult<>(total, list);
    }

    /**
     * 用户登录, 查询用户账户信息并返回双 token
     */
    public Map<String, Object> loginForDts(User user) throws Exception {
        String accessToken = login(user);
        Long userId = user.getId();
        String refreshToken = TokenUtil.generateRefreshToken(userId);

        // 保存 refreshToken 到数据库
        userDao.deleteRefreshToken(userId, refreshToken);
        userDao.addRefreshToken(userId, refreshToken, new Date());

        Map<String, Object> map = new HashMap<>();
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        return map;
    }

    /**
     * 退出登录, 根据 userId 删除 refreshToken
     */
    public void logout(Long userId, String refreshToken) {
        userDao.deleteRefreshToken(userId, refreshToken);
    }

    /**
     * 根据 refreshToken 刷新 accessToken
     */
    public String refreshAccessToken(String refreshToken) throws Exception {
        RefreshTokenDetail refreshTokenDetail = userDao.getRefreshTokenDetail(refreshToken);
        if (refreshTokenDetail == null) {
            throw new ConditionException("555", "token 过期！");
        }

        Long userId = refreshTokenDetail.getUserId();
        return TokenUtil.generateToken(userId);
    }

    /**
     * 根据 userIdList 批量查询用户基本信息
     */
    public List<UserInfo> batchGetUserInfoByUserIds(Set<Long> userIdList) {
        return userDao.batchGetUserInfoByUserIds(userIdList);
    }

    /**
     * 根据 userId 查询 refreshToken
     */
    public String getRefreshTokenByUserId(Long userId) {
        return userDao.getRefreshTokenByUserId(userId);
    }
}
