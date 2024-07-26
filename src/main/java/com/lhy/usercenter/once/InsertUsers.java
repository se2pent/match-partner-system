package com.lhy.usercenter.once;

import com.lhy.usercenter.mapper.UserMapper;
import com.lhy.usercenter.model.User;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;


@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

    /**
     * 批量插入用户
     */
//    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        System.out.println("goodgoodgood");
        stopWatch.start();
        final int INSERT_NUM = 1000;
        for (int i = 0; i < INSERT_NUM; i++) {
            User user=new User();
            user.setUserName("假数据");
            user.setUserAccount("fakedata");
            user.setAvatarUrl("https://cdnjson.com/images/2024/07/03/photomode_15122020_183521.png");
            user.setGender(0);
            user.setUserPassword("123456");
            user.setPhone("123");
            user.setProfile("哈哈大家好啊我是电棍");
            user.setEmail("123@qq.com");
            user.setUserRole(0);
            user.setUserStatus(0);
            user.setTags("[]");
            user.setPlanetCode("1111111");

            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
