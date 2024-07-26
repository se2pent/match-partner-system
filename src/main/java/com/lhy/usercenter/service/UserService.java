package com.lhy.usercenter.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.lhy.usercenter.contant.UserContant;
import com.lhy.usercenter.model.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author ser_chsh
* @description 针对表【user】的数据库操作Service
* @createDate 2024-07-01 09:55:17
*/
public interface UserService extends IService<User>{

    User selectUserByUsername(String username);

    long registerUser(String userAccount, String password, String checkedPassword,String planetCode);

    User selectUserByAccountAndPassword(String userAccount, String password,  HttpServletRequest request);

    List<User> getListByUsername(QueryWrapper queryWrapper);

    boolean deleteById(long userId);
    public User getSafeUser(User user);

    User selectUserById(Long userId);

    int logout(HttpServletRequest request);

    User selectUserByPlanetCode(String planetCode);

    List<User> selectUserByTags(List<String> tagList);

    Integer updateUser(User user,User loginUser);

    User getLoginUser(HttpServletRequest request);

    boolean isAdmin(HttpServletRequest request);

    boolean isAdmin(User loginUser);

    /**
     * 匹配用户
     * @param num
     * @param loginUser
     * @return
     */
    List<User> matchUsers(long num, User loginUser);
}
