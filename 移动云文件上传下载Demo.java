package com.yinlifang.ecloud;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import java.io.*;
import java.net.URL;

/**
 * 移动云(EOS)文件上传下载 Demo
 * 
 * 功能：
 * 1. 简单上传 - 支持字符串、byte数组、网络流、本地文件
 * 2. 下载到本地文件
 * 
 * 依赖：
 * <dependency>
 *     <groupId>com.amazonaws</groupId>
 *     <artifactId>aws-java-sdk-s3</artifactId>
 *     <version>1.12.700</version>
 * </dependency>
 */
public class EcloudFileDemo {

    // 配置信息 - 请根据实际情况修改
    private static final String ENDPOINT = "https://eos-wuxi-1.cmecloud.cn"; // 华东-苏州
    private static final String REGION = "wuxi1";
    private static final String ACCESS_KEY = "<your-access-key>";  // 替换为你的 Access Key
    private static final String SECRET_KEY = "<your-secret-access-key>";  // 替换为你的 Secret Key
    private static final String BUCKET_NAME = "<your-bucket-name>";  // 替换为你的桶名

    /**
     * 创建 AmazonS3 客户端
     */
    private static AmazonS3 createS3Client() {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = 
            new AwsClientBuilder.EndpointConfiguration(ENDPOINT, REGION);
        
        BasicAWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider)
                .build();
    }

    /**
     * 示例1: 上传字符串
     */
    public static void uploadString() {
        System.out.println("=== 示例1: 上传字符串 ===");
        
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            String objectName = "test-string.txt";
            String content = "Hello, 移动云! 这是测试字符串内容。";
            
            // 设置文件大小，避免 Out Of Memory 风险
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(content.length());
            
            // 上传字符串
            PutObjectResult result = client.putObject(
                BUCKET_NAME, 
                objectName, 
                new ByteArrayInputStream(content.getBytes()), 
                meta
            );
            
            System.out.println("上传成功!");
            System.out.println("ETag: " + result.getETag());
            System.out.println("VersionId: " + (result.getVersionId() != null ? result.getVersionId() : "无"));
            
        } catch (Exception e) {
            System.err.println("上传失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 示例2: 上传 byte 数组
     */
    public static void uploadByteArray() {
        System.out.println("\n=== 示例2: 上传 byte 数组 ===");
        
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            String objectName = "test-bytes.bin";
            String content = "这是 byte 数组内容";
            byte[] data = content.getBytes();
            
            // 设置文件大小
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(data.length);
            
            // 上传 byte 数组
            PutObjectResult result = client.putObject(
                BUCKET_NAME, 
                objectName, 
                new ByteArrayInputStream(data), 
                meta
            );
            
            System.out.println("上传成功!");
            System.out.println("ETag: " + result.getETag());
            
        } catch (Exception e) {
            System.err.println("上传失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 示例3: 上传网络流
     */
    public static void uploadFromUrl() {
        System.out.println("\n=== 示例3: 上传网络流 ===");
        
        AmazonS3 client = null;
        InputStream in = null;
        
        try {
            client = createS3Client();
            
            String objectName = "network-file.html";
            String urlStr = "https://www.baidu.com";
            
            // 打开网络流
            URL url = new URL(urlStr);
            in = url.openStream();
            
            // 上传网络流
            PutObjectResult result = client.putObject(BUCKET_NAME, objectName, in, null);
            
            System.out.println("上传成功!");
            System.out.println("ETag: " + result.getETag());
            
        } catch (Exception e) {
            System.err.println("上传失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 示例4: 上传本地文件
     */
    public static void uploadLocalFile(String localFilePath, String objectName) {
        System.out.println("\n=== 示例4: 上传本地文件 ===");
        
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            File localFile = new File(localFilePath);
            if (!localFile.exists()) {
                System.err.println("本地文件不存在: " + localFilePath);
                return;
            }
            
            // 检查文件大小(最大5GB)
            long fileSize = localFile.length();
            if (fileSize > 5L * 1024 * 1024 * 1024) {
                System.err.println("文件大小超过5GB限制: " + fileSize + " bytes");
                return;
            }
            
            // 上传本地文件
            PutObjectResult result = client.putObject(BUCKET_NAME, objectName, localFile);
            
            System.out.println("上传成功!");
            System.out.println("文件名: " + localFile.getName());
            System.out.println("文件大小: " + fileSize + " bytes");
            System.out.println("ETag: " + result.getETag());
            System.out.println("VersionId: " + (result.getVersionId() != null ? result.getVersionId() : "无"));
            
        } catch (Exception e) {
            System.err.println("上传失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 示例5: 下载到本地文件
     */
    public static void downloadToLocalFile(String objectName, String localFilePath) {
        System.out.println("\n=== 示例5: 下载到本地文件 ===");
        
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            File localFile = new File(localFilePath);
            
            // 下载到本地文件
            GetObjectRequest request = new GetObjectRequest(BUCKET_NAME, objectName);
            ObjectMetadata meta = client.getObject(request, localFile);
            
            System.out.println("下载成功!");
            System.out.println("下载路径: " + localFile.getAbsolutePath());
            System.out.println("文件大小: " + localFile.length() + " bytes");
            
            // 打印元数据
            System.out.println("\n元数据:");
            if (meta.getUserMetadata() != null) {
                meta.getUserMetadata().forEach((k, v) -> 
                    System.out.println("  " + k + ": " + v)
                );
            }
            
        } catch (Exception e) {
            System.err.println("下载失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 示例6: 列出桶中的所有对象
     */
    public static void listObjects() {
        System.out.println("\n=== 示例6: 列出桶中的所有对象 ===");
        
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(BUCKET_NAME);
            
            ListObjectsV2Result result = client.listObjectsV2(request);
            
            System.out.println("桶 '" + BUCKET_NAME + "' 中的对象:");
            System.out.println("对象数量: " + result.getObjectSummaries().size());
            System.out.println();
            
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                System.out.println("对象名: " + objectSummary.getKey());
                System.out.println("  大小: " + objectSummary.getSize() + " bytes");
                System.out.println("  最后修改: " + objectSummary.getLastModified());
                System.out.println("  ETag: " + objectSummary.getETag());
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("列出对象失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 示例7: 删除对象
     */
    public static void deleteObject(String objectName) {
        System.out.println("\n=== 示例7: 删除对象 ===");
        
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            client.deleteObject(BUCKET_NAME, objectName);
            
            System.out.println("删除成功: " + objectName);
            
        } catch (Exception e) {
            System.err.println("删除失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 主方法 - 运行所有示例
     */
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   移动云(EOS)文件上传下载 Demo");
        System.out.println("========================================\n");
        
        // 检查配置
        if (ACCESS_KEY.equals("<your-access-key>") || 
            SECRET_KEY.equals("<your-secret-access-key>") ||
            BUCKET_NAME.equals("<your-bucket-name>")) {
            System.err.println("请先配置 ACCESS_KEY, SECRET_KEY 和 BUCKET_NAME!");
            return;
        }
        
        // 示例1: 上传字符串
        uploadString();
        
        // 示例2: 上传 byte 数组
        uploadByteArray();
        
        // 示例3: 上传网络流
        uploadFromUrl();
        
        // 示例4: 上传本地文件
        String testFile = "/tmp/test-upload.txt";
        String objectName = "test-upload.txt";
        
        // 创建测试文件
        try {
            File file = new File(testFile);
            if (!file.exists()) {
                file.createNewFile();
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("这是一个测试文件，用于上传到移动云。\n");
                    writer.write("创建时间: " + new java.util.Date());
                }
            }
            
            uploadLocalFile(testFile, objectName);
            
            // 示例5: 下载到本地文件
            String downloadPath = "/tmp/test-download.txt";
            downloadToLocalFile(objectName, downloadPath);
            
            // 示例6: 列出对象
            listObjects();
            
            // 示例7: 删除对象
            // deleteObject(objectName);
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n========================================");
        System.out.println("   Demo 执行完成!");
        System.out.println("========================================");
    }
}

/**
 * 实际应用示例: 音立方服务调用
 */
class EcloudApiClient {

    private static final String ENDPOINT = "https://eos-wuxi-1.cmecloud.cn";
    private static final String REGION = "wuxi1";
    private static final String ACCESS_KEY = "<your-access-key>";
    private static final String SECRET_KEY = "<your-secret-access-key>";
    private static final String BUCKET_NAME = "<your-bucket-name>";

    private AmazonS3 createS3Client() {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = 
            new AwsClientBuilder.EndpointConfiguration(ENDPOINT, REGION);
        
        BasicAWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider)
                .build();
    }

    /**
     * 上传音频文件到移动云
     * 
     * @param localFilePath 本地文件路径
     * @param objectName 云端对象名
     * @return ETag
     */
    public String uploadAudioFile(String localFilePath, String objectName) {
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new FileNotFoundException("文件不存在: " + localFilePath);
            }
            
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("audio/mpeg");
            metadata.addUserMetadata("upload-time", String.valueOf(System.currentTimeMillis()));
            
            PutObjectResult result = client.putObject(BUCKET_NAME, objectName, file, metadata);
            
            System.out.println("上传成功: " + objectName);
            return result.getETag();
            
        } catch (Exception e) {
            System.err.println("上传失败: " + e.getMessage());
            throw new RuntimeException("上传失败", e);
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 从移动云下载音频文件
     * 
     * @param objectName 云端对象名
     * @param localFilePath 本地保存路径
     */
    public void downloadAudioFile(String objectName, String localFilePath) {
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            GetObjectRequest request = new GetObjectRequest(BUCKET_NAME, objectName);
            ObjectMetadata metadata = client.getObject(request, new File(localFilePath));
            
            System.out.println("下载成功: " + objectName);
            System.out.println("保存路径: " + localFilePath);
            
        } catch (Exception e) {
            System.err.println("下载失败: " + e.getMessage());
            throw new RuntimeException("下载失败", e);
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 获取对象的访问URL
     * 
     * @param objectName 对象名
     * @param expirationSeconds URL有效期(秒)
     * @return 访问URL
     */
    public String getPresignedUrl(String objectName, int expirationSeconds) {
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + expirationSeconds * 1000L);
            
            URL url = client.generatePresignedUrl(
                BUCKET_NAME, 
                objectName, 
                expiration, 
                HttpMethod.GET
            );
            
            return url.toString();
            
        } catch (Exception e) {
            System.err.println("生成URL失败: " + e.getMessage());
            throw new RuntimeException("生成URL失败", e);
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }
}

/**
 * Spring Boot 集成示例
 */
class EcloudService {

    private final String endpoint;
    private final String region;
    private final String accessKey;
    private final String secretKey;
    private final String bucketName;

    public EcloudService(String endpoint, String region, String accessKey, 
                         String secretKey, String bucketName) {
        this.endpoint = endpoint;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
    }

    private AmazonS3 createS3Client() {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = 
            new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider)
                .build();
    }

    /**
     * 上传文件
     */
    public String uploadFile(File file, String objectName) {
        AmazonS3 client = createS3Client();
        try {
            PutObjectResult result = client.putObject(bucketName, objectName, file);
            return result.getETag();
        } finally {
            client.shutdown();
        }
    }

    /**
     * 下载文件
     */
    public void downloadFile(String objectName, File destinationFile) {
        AmazonS3 client = createS3Client();
        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, objectName);
            client.getObject(request, destinationFile);
        } finally {
            client.shutdown();
        }
    }
}