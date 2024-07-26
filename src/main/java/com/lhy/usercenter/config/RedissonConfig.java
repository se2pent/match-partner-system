package com.lhy.usercenter.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.awt.print.PrinterJob;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private String host;
    private String port;

    @Bean
    public RedissonClient redissonClient(){
        Config config=new Config();
        String redissonAddress=String.format("redis://%s:%s",host,port);
        config.useSingleServer().setAddress(redissonAddress).setDatabase(3);
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }


}
