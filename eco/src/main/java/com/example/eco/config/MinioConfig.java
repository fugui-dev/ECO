package com.example.eco.config;

import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.minio")
@Slf4j
public class MinioConfig {

    private String accessKey;

    private String secretKey;

    private String url;

    private String bucketName;


    @Bean
    public MinioClient minioClient(){
        return MinioClient
                .builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}
