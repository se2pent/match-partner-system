package com.lhy.usercenter.utils;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lhy.usercenter.common.ResultUtil;
import com.lhy.usercenter.contant.UserContant;
import com.lhy.usercenter.mapper.UserMapper;
import com.lhy.usercenter.model.User;
import com.lhy.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ScheduConfig {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;


    private List<Long> mainUserList= Arrays.asList(2L);




    @Scheduled(cron = "0 26 * * * ? ")
    public void doCacheCommendUser(){
        System.out.println("执行了自动任务");
        RLock rLock=redissonClient.getLock("yupao:precachejob:docache:lock");
        try {
            if (rLock.tryLock(0,-1,TimeUnit.MILLISECONDS)) {
                for (Long userId : mainUserList) {
                    String redisKey = String.format("yupao:user:recommend:%s", userId);
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> userList = userService.page(new Page<>(1, 50), queryWrapper);
                    try {
                        redisTemplate.opsForValue().set(redisKey, userList, 30000, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.error("redis error message:", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            if (rLock.isHeldByCurrentThread()) {
                System.out.println("currentId:"+Thread.currentThread().getId());
                rLock.unlock();
            }
        }

    }

}
