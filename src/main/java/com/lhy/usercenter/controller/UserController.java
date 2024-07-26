package com.lhy.usercenter.controller;

import ch.qos.logback.classic.joran.action.InsertFromJNDIAction;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.lhy.usercenter.common.BaseResponse;
import com.lhy.usercenter.common.ErrorCode;
import com.lhy.usercenter.common.ResultUtil;
import com.lhy.usercenter.contant.UserContant;
import com.lhy.usercenter.exception.BusinessException;
import com.lhy.usercenter.model.User;
import com.lhy.usercenter.model.login.UserLoginRequest;
import com.lhy.usercenter.model.request.UserRegisterRequest;
import com.lhy.usercenter.service.UserService;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RestController
@Validated
@Slf4j
@CrossOrigin(origins = "http://localhost:5173/", allowCredentials = "true")
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> registerUser(@Validated @RequestBody UserRegisterRequest userRegisterRequest){
        if (ObjectUtil.isNull(userRegisterRequest)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getUserPassword();
        String checkedPassword = userRegisterRequest.getCheckedPassword();
        String planetCode= userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount,password,checkedPassword)){
            return null;
        }
        if (ObjectUtil.isNotNull(userService.selectUserByUsername(userAccount))) {
            throw  new BusinessException(ErrorCode.NULL_ERROR,"该账户已存在");
        }
        if (ObjectUtil.isNotNull(userService.selectUserByPlanetCode(planetCode))){
            throw  new BusinessException(ErrorCode.NULL_ERROR,"该星球编号已存在");
        }
        long userId = userService.registerUser(userAccount, password, checkedPassword,planetCode);
        return ResultUtil.success(userId);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrent(HttpServletRequest request){
        User currentUser = (User) request.getSession().getAttribute(UserContant.USER_LOGIN_STATE);
        if (ObjectUtil.isNull(currentUser)){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = currentUser.getUserId();
        User user=userService.selectUserById(userId);
        //todo 校验用户是否合法
        User safeUser = userService.getSafeUser(user);
        return ResultUtil.success(safeUser);

    }

    /**
     * 用户登录接口
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if (ObjectUtil.isNull(userLoginRequest)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String password = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount,password)){
            return null;
        }
        User user=userService.selectUserByAccountAndPassword(userAccount, password, request);
        if (ObjectUtil.isNull(user)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtil.success(user);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> search(String username,HttpServletRequest request){
        if (!userService.isAdmin(request)){
            return ResultUtil.error(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)){
            queryWrapper.like("user_name",username);
        }
        List<User> userList = userService.getListByUsername(queryWrapper);
        List<User> collect = userList.stream().map(
                listuser -> {
                    listuser.setUserPassword(null);
                    return listuser;
                }
        ).collect(Collectors.toList());
        return ResultUtil.success(collect);
    }

    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize,long pageNum,HttpServletRequest request){
//        List<User> userList = userService.getListByUsername(new QueryWrapper<>());
        User loginUser = userService.getLoginUser(request);
        String redisKey=String.format("yupao:user:recommend:%s",loginUser.getUserId());
        ValueOperations<String,Object> valueOperations = redisTemplate.opsForValue();
        Page<User> userList = (Page<User>) valueOperations.get(redisKey);
        if (ObjectUtil.isNotNull(userList)){
            return ResultUtil.success(userList);
        }
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        userList=userService.page(new Page<>(pageSize,pageNum),queryWrapper);
        try {
            valueOperations.set(redisKey,userList,30000, TimeUnit.SECONDS);
        }catch (Exception e){
            log.error("redis error message:",e);
        }

        return ResultUtil.success(userList);
    }

    @DeleteMapping("/delete/{userId}")
    public BaseResponse<Boolean> delete(@PathVariable("userId") Long userId,HttpServletRequest request){
        if (!userService.isAdmin(request)){
            return ResultUtil.error(ErrorCode.NO_AUTH);
        }
        if (userId <= 0) {
            return ResultUtil.error(ErrorCode.NULL_ERROR);
        }
        boolean result = userService.deleteById(userId);
        return ResultUtil.success(result);
    }

//    private boolean isAdmin(HttpServletRequest request){
//        User user = (User) request.getSession().getAttribute(UserContant.USER_LOGIN_STATE);
//        if (ObjectUtil.isNull(user)||user.getUserRole()!=UserContant.ADMIN_ROLE){
//            return false;
//        }
//        return true;
//    }



    @PostMapping("/logout")
    public BaseResponse<Integer> logout(HttpServletRequest request){
        if (ObjectUtil.isNull(request)){
            return ResultUtil.error(ErrorCode.NULL_ERROR);
        }

        int logout = userService.logout(request);
        return ResultUtil.success(logout);

    }

    /**
     * 根据标签搜索
     * @param tagNameList
     * @return
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.selectUserByTags(tagNameList);
        return ResultUtil.success(userList);
    }

    /**更改用户
     *
     * @param user
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user,HttpServletRequest request){
        if (ObjectUtil.isNull(user)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtil.success(userService.updateUser(user,loginUser));
    }

    /**推荐用户
     *
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        return ResultUtil.success(userService.matchUsers(num, user));
    }
}
