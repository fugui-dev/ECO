package com.example.eco.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName RedissonConfig
 * @Author lishiyou
 * @Date 2022/7/7 17:26
 * @Version 1.0
 **/
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:}")
    private String redisHost;

    @Value("${spring.redis.port:}")
    private String port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.database:8}")
    private Integer database;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redisHost + ":" + port);
        config.useSingleServer().setPassword(password);
        config.useSingleServer().setDatabase(database);

        return Redisson.create(config);
    }

}
