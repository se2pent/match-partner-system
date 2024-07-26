package com.lhy.usercenter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lhy.usercenter.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import javax.annotation.Resource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class UserServiceTest {
    @Resource
    private UserService userService;
    @Test
    void selectUserByTags(){

        List<String> tagList= Arrays.asList("java","python");
        List<User> userList = userService.selectUserByTags(tagList);
        assertNotNull(userList);

    }
}