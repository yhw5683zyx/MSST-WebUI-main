package com.yinlifang.ecloud;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 音立方服务 - 移动云管理 + MSST调用
 * 
 * 职责:
 * 1. 管理移动云存储 (上传/下载/生成预签名URL)
 * 2. 调用MSST API处理音频
 * 3. 接收MSST回调
 * 
 * 优势: MSST无需SDK,轻量化,只负责音频分离
 */
public class YinfangService {

    private static final String MSST_API_BASE_URL = "http://117.50.182.134:8000";
    
    // 移动云配置
    private static final String EOS_ENDPOINT = "https://eos-wuxi-1.cmecloud.cn";
    private static final String EOS_REGION = "wuxi1";
    private static final String EOS_ACCESS_KEY = "<your-access-key>";
    private static final String EOS_SECRET_KEY = "<your-secret-access-key>";
    private static final String EOS_BUCKET = "<your-bucket-name>";
    
    // 预签名URL有效期(秒)
    private static final int PRESIGNED_URL_EXPIRE_SECONDS = 3600; // 1小时

    private final Gson gson = new Gson();

    // ==================== 移动云管理 ====================

    /**
     * 上传音频到移动云
     */
    public String uploadAudioToEcloud(File audioFile, String objectName) {
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("audio/mpeg");
            metadata.addUserMetadata("upload-time", String.valueOf(System.currentTimeMillis()));
            
            PutObjectResult result = client.putObject(EOS_BUCKET, objectName, audioFile, metadata);
            
            System.out.println("上传成功: " + objectName);
            return result.getETag();
            
        } catch (Exception e) {
            throw new RuntimeException("上传到移动云失败", e);
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 生成下载预签名URL
     */
    public String generateDownloadPresignedUrl(String objectName, int expireSeconds) {
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            Date expiration = new Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += expireSeconds * 1000L;
            expiration.setTime(expTimeMillis);
            
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(EOS_BUCKET, objectName)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            
            URL url = client.generatePresignedUrl(request);
            System.out.println("生成下载URL: " + url);
            return url.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("生成下载URL失败", e);
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 生成上传预签名URL
     */
    public Map<String, String> generateUploadPresignedUrls(List<String> objectNames, int expireSeconds) {
        AmazonS3 client = null;
        Map<String, String> urls = new HashMap<>();
        
        try {
            client = createS3Client();
            
            Date expiration = new Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += expireSeconds * 1000L;
            expiration.setTime(expTimeMillis);
            
            for (String objectName : objectNames) {
                GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(EOS_BUCKET, objectName)
                        .withMethod(HttpMethod.PUT)
                        .withExpiration(expiration);
                
                URL url = client.generatePresignedUrl(request);
                urls.put(objectName, url.toString());
                System.out.println("生成上传URL: " + objectName + " -> " + url);
            }
            
            return urls;
            
        } catch (Exception e) {
            throw new RuntimeException("生成上传URL失败", e);
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    /**
     * 从移动云下载文件
     */
    public void downloadFromEcloud(String objectName, String localPath) {
        AmazonS3 client = null;
        try {
            client = createS3Client();
            
            GetObjectRequest request = new GetObjectRequest(EOS_BUCKET, objectName);
            ObjectMetadata metadata = client.getObject(request, new File(localPath));
            
            System.out.println("下载成功: " + localPath);
            
        } catch (Exception e) {
            throw new RuntimeException("从移动云下载失败", e);
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    // ==================== MSST API 调用 ====================

    /**
     * 调用MSST轻量化API处理音频
     * 
     * @param downloadUrl 下载预签名URL
     * @param uploadUrls 上传预签名URL (key: 文件名, value: URL)
     * @param presetName Preset配置名
     * @param callbackUrl 回调地址
     * @return 任务ID
     */
    public String processAudioWithPresignedUrls(
            String downloadUrl, 
            Map<String, String> uploadUrls,
            String presetName,
            String callbackUrl) throws IOException {
        
        String taskId = UUID.randomUUID().toString();
        String url = MSST_API_BASE_URL + "/process_lightweight";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("task_id", taskId);
        requestBody.addProperty("preset_name", presetName);
        requestBody.addProperty("download_url", downloadUrl);
        
        // 添加upload_urls
        JsonObject uploadUrlsJson = new JsonObject();
        for (Map.Entry<String, String> entry : uploadUrls.entrySet()) {
            uploadUrlsJson.addProperty(entry.getKey(), entry.getValue());
        }
        requestBody.add("upload_urls", uploadUrlsJson);
        
        if (callbackUrl != null) {
            requestBody.addProperty("callback_url", callbackUrl);
        }
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));
            
            HttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            if (response.getStatusLine().getStatusCode() == 200) {
                ProcessResponse processResponse = gson.fromJson(responseBody, ProcessResponse.class);
                return processResponse.getTask_id();
            } else {
                throw new IOException("MSST API 调用失败: " + responseBody);
            }
        }
    }

    /**
     * 查询任务状态
     */
    public TaskStatusResponse getTaskStatus(String taskId) throws IOException {
        String url = MSST_API_BASE_URL + "/status/" + taskId;
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            
            HttpResponse response = httpClient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            if (response.getStatusLine().getStatusCode() == 200) {
                return gson.fromJson(responseBody, TaskStatusResponse.class);
            } else {
                throw new IOException("查询状态失败: " + responseBody);
            }
        }
    }

    // ==================== 完整流程 ====================

    /**
     * 完整流程: 上传 -> 生成URL -> 调用MSST -> 等待回调
     */
    public void completeWorkflow() {
        try {
            // 1. 上传音频到移动云
            File audioFile = new File("/path/to/audio.mp3");
            String inputObjectName = "input/" + audioFile.getName();
            
            System.out.println("步骤1: 上传音频到移动云");
            String etag = uploadAudioToEcloud(audioFile, inputObjectName);
            System.out.println("上传成功, ETag: " + etag);
            
            // 2. 生成下载预签名URL
            String downloadUrl = generateDownloadPresignedUrl(inputObjectName, PRESIGNED_URL_EXPIRE_SECONDS);
            System.out.println("生成下载URL: " + downloadUrl);
            
            // 3. 预生成上传预签名URL (用于结果文件)
            List<String> resultObjectNames = Arrays.asList(
                "separated/" + UUID.randomUUID() + "/input_vocals.wav",
                "separated/" + UUID.randomUUID() + "/input_instrumental.wav"
            );
            
            Map<String, String> uploadUrls = new HashMap<>();
            for (String objectName : resultObjectNames) {
                String fileName = objectName.substring(objectName.lastIndexOf("/") + 1);
                String url = generateUploadPresignedUrl(
                    Arrays.asList(objectName), 
                    PRESIGNED_URL_EXPIRE_SECONDS
                ).get(objectName);
                uploadUrls.put(fileName, url);
            }
            
            System.out.println("生成上传URL数量: " + uploadUrls.size());
            
            // 4. 调用MSST API处理
            String taskId = processAudioWithPresignedUrls(
                downloadUrl,
                uploadUrls,
                "my_preset",  // Preset配置名
                "http://your-server/callback"  // 回调地址
            );
            
            System.out.println("步骤4: 任务已提交, Task ID: " + taskId);
            
            // 5. 等待回调
            System.out.println("步骤5: 等待回调通知...");
            
            // 回调会自动处理结果文件,无需手动操作
            
        } catch (Exception e) {
            System.err.println("流程失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 回调处理 ====================

    /**
     * 处理MSST回调
     */
    public String handleCallback(String callbackData) {
        System.out.println("收到MSST回调: " + callbackData);
        
        CallbackData data = gson.fromJson(callbackData, CallbackData.class);
        
        if ("completed".equals(data.getStatus())) {
            System.out.println("任务 " + data.getTask_id() + " 完成!");
            System.out.println("上传的文件: " + data.getUploaded_files());
            
            // 结果文件已经通过预签名URL上传到移动云
            // 可以从移动云下载或直接使用移动云URL
            
        } else if ("failed".equals(data.getStatus())) {
            System.err.println("任务 " + data.getTask_id() + " 失败: " + data.getMessage());
        }
        
        return "OK";
    }

    // ==================== 辅助方法 ====================

    private AmazonS3 createS3Client() {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = 
            new AwsClientBuilder.EndpointConfiguration(EOS_ENDPOINT, EOS_REGION);
        
        BasicAWSCredentials credentials = new BasicAWSCredentials(EOS_ACCESS_KEY, EOS_SECRET_KEY);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("S3SignerType");
        
        return AmazonS3ClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }

    // ==================== 数据模型 ====================

    @Data
    public static class ProcessResponse {
        private String task_id;
        private String status;
        private String message;
    }

    @Data
    public static class TaskStatusResponse {
        private String task_id;
        private String status;
        private String message;
    }

    @Data
    public static class CallbackData {
        private String task_id;
        private String status;
        private List<String> uploaded_files;
        private String message;
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        YinfangService service = new YinfangService();
        
        System.out.println("========================================");
        System.out.println("   音立方服务 - 移动云管理 + MSST调用");
        System.out.println("========================================\n");
        
        // 运行完整流程
        service.completeWorkflow();
        
        System.out.println("\n========================================");
        System.out.println("   流程执行完成!");
        System.out.println("========================================");
    }
}

/**
 * Spring Boot Controller 示例
 */
@RestController
@RequestMapping("/api")
class MsstCallbackController {

    @PostMapping("/callback")
    public String handleCallback(@RequestBody String callbackData) {
        return new YinfangService().handleCallback(callbackData);
    }
}