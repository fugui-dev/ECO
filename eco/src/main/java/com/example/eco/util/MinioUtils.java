package com.example.eco.util;

import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.util.StringUtils;
import com.example.odyssey.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MinioUtils {

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioConfig minioConfig;


    /**
     * 判断bucket是否存在，不存在则创建
     *
     * @Description
     * @Param bucketName bucket名称
     * @Return java.lang.Boolean
     * @Author Administrator
     * @Date 2024/7/17 14:31
     **/
    @SneakyThrows
    public Boolean bucketExists(String bucketName) {
        boolean exists = false;
        exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            exists = true;
        }
        return exists;
    }

    /**
     * 创建存储bucket
     *
     * @Description
     * @Param bucketName
     * @Return java.lang.Boolean
     * @Author Administrator
     * @Date 2024/7/17 14:42
     **/
    @SneakyThrows
    public Boolean makeBucket(String bucketName, String directoryPath) {
        minioClient.makeBucket(MakeBucketArgs.builder()
                .bucket(bucketName)
                .build());

        return true;
    }

    /**
     * 删除存储bucket
     *
     * @Description
     * @Param bucketName
     * @Return java.lang.Boolean
     * @Author Administrator
     * @Date 2024/7/17 14:44
     **/
    @SneakyThrows
    public Boolean removeBucket(String bucketName) {
        minioClient.removeBucket(RemoveBucketArgs.builder()
                .bucket(bucketName)
                .build());
        return true;
    }

    /**
     * 获取全部bucket
     *
     * @Description
     * @Return java.util.List<io.minio.messages.Bucket>
     * @Author Administrator
     * @Date 2024/7/17 14:44
     **/
    @SneakyThrows
    public List<Bucket> getAllBuckets() {
        return minioClient.listBuckets();
    }

    /**
     * 上传文件
     *
     * @Description
     * @Param bucketName
     * @Param prefix
     * @Param file
     * @Return java.lang.String
     * @Author Administrator
     * @Date 2024/7/17 14:51
     **/
    @SneakyThrows
    public String upload(String prefix, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new RuntimeException();
        }
        boolean fileExists;
        try {
            fileExists = isFileExists(minioConfig.getBucketName(), prefix, originalFilename);
        } catch (Exception e) {
            fileExists = false;
        }
        if (fileExists) {
            log.info("当前文件存在同名文件,开始重命名保存. originalFilename={}", originalFilename);
            int dotIndex = originalFilename.lastIndexOf('.');
            // 获取不带后缀名的文件名
            String fileNameWithoutExtension = originalFilename.substring(0, dotIndex);
            // 获取文件后缀名
            String fileExtension = originalFilename.substring(dotIndex + 1);
            originalFilename = fileNameWithoutExtension + "_" + System.currentTimeMillis() + "." + fileExtension;
        }
        String objectName = StringUtils.isBlank(prefix) ? originalFilename : "/" + prefix + "/" + originalFilename;
        PutObjectArgs objectArgs = PutObjectArgs.builder().bucket(minioConfig.getBucketName()).object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1).contentType(file.getContentType()).build();
        //文件名称相同会覆盖
        minioClient.putObject(objectArgs);
        return minioConfig.getBucketName() + "/" + prefix + "/" + originalFilename;
    }

    /**
     * 上传图片
     *
     * @Description
     * @Param bucketName
     * @Param prefix
     * @Param suffix
     * @Param fileBytes
     * @Return java.lang.String
     * @Author Administrator
     * @Date 2024/7/23 16:38
     **/
    @SneakyThrows
    public String upload(String prefix, String suffix, byte[] fileBytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
        String originalFilename = IdUtil.fastUUID() + suffix;
        String objectName = StringUtils.isBlank(prefix) ? originalFilename : "/" + prefix + "/" + originalFilename;
        PutObjectArgs objectArgs = PutObjectArgs.builder().bucket(minioConfig.getBucketName()).object(objectName)
                .stream(inputStream, fileBytes.length, -1).build();
        //文件名称相同会覆盖
        minioClient.putObject(objectArgs);
        return originalFilename;
    }

    /**
     * 预览(预览链接默认7天后过期)
     *
     * @Description
     * @Param bucketName
     * @Param fileName
     * @Return java.lang.String
     * @Author Administrator
     * @Date 2024/7/17 14:54
     **/
    @SneakyThrows
    public String preview(String bucketName, String prefix, String fileName) {
        // 查看文件地址
        String objectName = StringUtils.isBlank(prefix) ? fileName : "/" + prefix + "/" + fileName;
        GetPresignedObjectUrlArgs build = GetPresignedObjectUrlArgs.builder().bucket(bucketName).object(objectName).method(Method.GET).build();
        return minioClient.getPresignedObjectUrl(build);
    }

    /**
     * 下载文件
     *
     * @Description
     * @Param bucketName
     * @Param fileName
     * @Param res
     * @Return void
     * @Author Administrator
     * @Date 2024/7/17 14:56
     **/
    @SneakyThrows
    public void download(String bucketName, String prefix, String fileName, HttpServletResponse res) {
        String objectName = StringUtils.isBlank(prefix) ? fileName : "/" + prefix + "/" + fileName;
        GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(bucketName).object(objectName).build();
        try (GetObjectResponse response = minioClient.getObject(objectArgs)) {
            byte[] buf = new byte[1024];
            int len;
            try (FastByteArrayOutputStream os = new FastByteArrayOutputStream()) {
                while ((len = response.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
                os.flush();
                byte[] bytes = os.toByteArray();
                res.setCharacterEncoding("utf-8");
                // 设置强制下载不打开
                // res.setContentType("application/force-download");
                res.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
                try (ServletOutputStream stream = res.getOutputStream()) {
                    stream.write(bytes);
                    stream.flush();
                }
            }
        } catch (Exception e) {
            log.error("下载文件异常. e={}", e);
        }
    }

    /**
     * 查看文件对象
     *
     * @Description
     * @Param bucketName
     * @Return java.util.List<io.minio.messages.Item>
     * @Author Administrator
     * @Date 2024/7/17 15:32
     **/
    @SneakyThrows
    public List<Item> listObjects(String bucketName) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).build());
        List<Item> items = new ArrayList<>();
        for (Result<Item> result : results) {
            items.add(result.get());
        }
        return items;
    }

    /**
     * 删除文件
     *
     * @Description
     * @Param bucketName
     * @Param fileName
     * @Return boolean
     * @Author Administrator
     * @Date 2024/7/17 15:32
     **/
    @SneakyThrows
    public boolean remove(String bucketName, String prefix, String fileName) {
        String objectName = StringUtils.isBlank(prefix) ? fileName : "/" + prefix + "/" + fileName;
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        return true;
    }

    /**
     * 生成一个带到期时间、签名的URL，这个地址可以提供给没有登录的第三方共享访问或者上传对象，针对于Bucket为私有的情况
     *
     * @Description
     * @Param bucketName
     * @Param fileName
     * @Return java.lang.String
     * @Author Administrator
     * @Date 2024/7/17 16:22
     **/
    @SneakyThrows
    public String getPresignedObjectUrl(String bucketName, String prefix, String fileName) {
        String objectName = StringUtils.isBlank(prefix) ? fileName : "/" + prefix + "/" + fileName;
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                // 设置过期时间，30分钟
                .expiry(30, TimeUnit.MINUTES)
                .method(Method.GET)
                .build());
    }

    /**
     * 判断文件是否存在
     *
     * @Description
     * @Param bucketName
     * @Param prefix
     * @Param fileName
     * @Return boolean
     * @Author Administrator
     * @Date 2024/7/22 17:42
     **/
    @SneakyThrows
    public boolean isFileExists(String bucketName, String prefix, String fileName) {
        String objectName = StringUtils.isBlank(prefix) ? fileName : "/" + prefix + "/" + fileName;
        minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
        return true;
    }

}
