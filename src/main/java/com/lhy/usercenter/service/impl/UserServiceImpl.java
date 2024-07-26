package com.lhy.usercenter.service.impl;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lhy.usercenter.common.ErrorCode;
import com.lhy.usercenter.contant.UserContant;
import com.lhy.usercenter.exception.BusinessException;
import com.lhy.usercenter.model.User;
import com.lhy.usercenter.service.UserService;
import com.lhy.usercenter.mapper.UserMapper;
import com.lhy.usercenter.utils.AlgorithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

/**
* @author ser_chsh
* @description 针对表【user】的数据库操作Service实现
* @createDate 2024-07-01 09:55:17
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    @Autowired
    private UserMapper userMapper;

    final String Salt="yupi";



    @Override
    public User selectUserByUsername(String userAccount) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("user_account", userAccount));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public long registerUser(String userAccount, String password, String checkedPassword,String planetCode) {
        if (!password.equals(checkedPassword)){
            return -1;
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((Salt + password).getBytes());
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        int insert = userMapper.insert(user);
        if (insert==0){
            return -1;
        }
        return user.getUserId();
    }

    @Override
    public User selectUserByAccountAndPassword(String userAccount, String password, HttpServletRequest request) {

        String encryptPassword = DigestUtils.md5DigestAsHex((Salt + password).getBytes());
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("user_account", userAccount).eq("user_password", encryptPassword));
        if (user == null) {
            log.info("login failed,user is null");
            return null;
        }

        User safeUser = getSafeUser(user);


        request.getSession().setAttribute(UserContant.USER_LOGIN_STATE,safeUser);

        return safeUser;
    }

    @Override
    public List<User> getListByUsername(QueryWrapper queryWrapper) {
        return userMapper.selectList(queryWrapper);
    }

    @Override
    public boolean deleteById(long userId) {
        return userMapper.deleteById(userId)>0;
    }
    @Override
    public User getSafeUser(User user){
        if (ObjectUtil.isNull(user)){
            return null;
        }
        User safeUser = new User();
        safeUser.setUserId(user.getUserId());
        safeUser.setUserName(user.getUserName());
        safeUser.setUserAccount(user.getUserAccount());
        safeUser.setAvatarUrl(user.getAvatarUrl());
        safeUser.setGender(user.getGender());
        safeUser.setPhone(user.getPhone());
        safeUser.setUserRole(user.getUserRole());
        safeUser.setPlanetCode(user.getPlanetCode());
        safeUser.setEmail(user.getEmail());
        safeUser.setUserStatus(user.getUserStatus());
        safeUser.setCreateTime(LocalDateTime.now());
        safeUser.setTags(user.getTags());
        safeUser.setProfile(user.getProfile());
        return safeUser;
    }

    @Override
    public User selectUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public int logout(HttpServletRequest request) {
        request.getSession().removeAttribute(UserContant.USER_LOGIN_STATE);
        return 1;
    }

    @Override
    public User selectUserByPlanetCode(String planetCode) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("planet_code",planetCode));
    }

    /**
     * 通过缓存查询携带标签用户
     * @param tagList
     * @return
     */
    @Override
    public List<User> selectUserByTags(List<String> tagList){
        Gson gson=new Gson();
        QueryWrapper<User> queryWrapper=new QueryWrapper<User>();
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream().filter(user -> {
            String tag = user.getTags();
            if (StringUtils.isBlank(tag)){
                return false;
            }
            Set<String> tagNameSet = gson.fromJson(tag, new TypeToken<Set<String>>() {
            }.getType());
            tagNameSet=Optional.ofNullable(tagNameSet).orElse(new HashSet<>());//判断set是否为空
            for (String tagName : tagList) {
                if (!tagNameSet.contains(tagName)){
                    return false;
                }
            }
                return true;
        }).map(this::getSafeUser).collect(Collectors.toList());
    }

    @Override
    public Integer updateUser(User user,User loginUser) {
        long userId = user.getUserId();
        if (userId<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!isAdmin(loginUser)&&userId!=loginUser.getUserId()){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userId);
        if (ObjectUtil.isNull(oldUser)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (ObjectUtil.isNull(request)) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(UserContant.USER_LOGIN_STATE);
        if (ObjectUtil.isNull(userObj)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User) userObj;
    }

    /**
     * 判断是否为管理员
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request){
        User user = (User) request.getSession().getAttribute(UserContant.USER_LOGIN_STATE);
        if (ObjectUtil.isNull(user)||user.getUserRole()!=UserContant.ADMIN_ROLE){
            return false;
        }
        return true;
    }

    /**
     * 判断是否为管理员重载
     * @param loginUser
     * @return
     */
    @Override
    public boolean isAdmin(User loginUser){
        return loginUser != null && loginUser.getUserRole() == UserContant.ADMIN_ROLE;
    }

    /**
     * 通过sql查询携带标签用户
     * @param tagList
     * @return
     */
    @Deprecated
    public List<User> selectUserByTagsSQL(List<String> tagList){
    if (CollectionUtils.isEmpty(tagList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper=new QueryWrapper<User>();
        for (String tag : tagList) {
            queryWrapper=queryWrapper.like("tags",tag);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream().map(this::getSafeUser).collect(Collectors.toList());
    }

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("user_id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下标 => 相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        // 依次计算所有用户和当前用户的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            // 无标签或者为当前用户自己
            if (StringUtils.isBlank(userTags) || user.getUserId() == loginUser.getUserId()) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        // 按编辑距离由小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 原本顺序的 userId 列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getUserId()).collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("user_id", userIdList);
        // 1, 3, 2
        // User1、User2、User3
        // 1 => User1, 2 => User2, 3 => User3
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(user -> getSafeUser(user))
                .collect(Collectors.groupingBy(User::getUserId));
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }

}




