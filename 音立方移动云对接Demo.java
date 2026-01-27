package com.yinlifang.ecloud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 音立方服务 - 移动云对接 Demo
 * 
 * 功能:
 * 1. 上传音频到移动云
 * 2. 调用 MSST API 处理
 * 3. 接收回调
 * 4. 通过预签名 URL 下载结果
 * 
 * 优势: 音立方无需移动云 SDK,直接通过 URL 下载!
 */
public class YinfangEcloudClient {

    private static final String MSST_API_BASE_URL = "http://117.50.182.134:8000";
    private static final String EOS_ENDPOINT = "https://eos-wuxi-1.cmecloud.cn";
    private static final String EOS_REGION = "wuxi1";
    private static final String EOS_ACCESS_KEY = "<your-access-key>";
    private static final String EOS_SECRET_KEY = "<your-secret-access-key>";
    private static final String EOS_BUCKET = "<your-bucket-name>";

    private final Gson gson = new Gson();

    // ==================== 移动云操作 ====================

    /**
     * 上传音频文件到移动云
     * 
     * @param audioFile 音频文件
     * @param objectName 对象名
     * @return ETag
     */
    public String uploadAudioToEcloud(File audioFile, String objectName) {
        try {
            EcloudUploader uploader = new EcloudUploader(
                EOS_ENDPOINT, 
                EOS_REGION, 
                EOS_ACCESS_KEY, 
                EOS_SECRET_KEY
            );
            
            return uploader.uploadFile(audioFile, objectName, EOS_BUCKET);
            
        } catch (Exception e) {
            throw new RuntimeException("上传到移动云失败", e);
        }
    }

    /**
     * 通过预签名 URL 下载文件
     * 
     * @param presignedUrl 预签名URL
     * @param savePath 保存路径
     */
    public void downloadByPresignedUrl(String presignedUrl, String savePath) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(presignedUrl);
            
            HttpResponse response = httpClient.execute(httpGet);
            
            if (response.getStatusLine().getStatusCode() == 200) {
                byte[] fileData = EntityUtils.toByteArray(response.getEntity());
                java.nio.file.Files.write(
                    java.nio.file.Paths.get(savePath), 
                    fileData
                );
                System.out.println("文件下载成功: " + savePath);
            } else {
                throw new IOException("下载失败: " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("下载失败", e);
        }
    }

    // ==================== MSST API 调用 ====================

    /**
     * 调用 MSST API 处理音频
     * 
     * @param ecloudKey 移动云对象Key
     * @param presetName Preset配置名
     * @param outputFormat 输出格式
     * @param callbackUrl 回调地址
     * @return 任务ID
     */
    public String processAudio(String ecloudKey, String presetName, 
                               String outputFormat, String callbackUrl) throws IOException {
        
        String taskId = UUID.randomUUID().toString();
        String url = MSST_API_BASE_URL + "/process_ecloud";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("task_id", taskId);
        requestBody.addProperty("preset_name", presetName);
        requestBody.addProperty("ecloud_key", ecloudKey);
        requestBody.addProperty("output_format", outputFormat);
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

    // ==================== 回调处理 ====================

    /**
     * 处理 MSST 回调 (Spring Boot Controller 示例)
     */
    public static class CallbackController {
        
        /**
         * 接收 MSST 回调
         */
        public String handleCallback(String callbackData) {
            System.out.println("收到 MSST 回调: " + callbackData);
            
            Gson gson = new Gson();
            CallbackData data = gson.fromJson(callbackData, CallbackData.class);
            
            if ("completed".equals(data.getStatus())) {
                System.out.println("任务 " + data.getTask_id() + " 完成!");
                System.out.println("移动云对象Keys: " + data.getEcloud_keys());
                System.out.println("预签名URLs: " + data.getDownload_urls());
                System.out.println("URL有效期: " + data.getUrl_expire_seconds() + " 秒");
                
                // 下载结果文件
                YinfangEcloudClient client = new YinfangEcloudClient();
                
                for (int i = 0; i < data.getDownload_urls().size(); i++) {
                    String url = data.getDownload_urls().get(i);
                    String filename = data.getEcloud_keys().get(i)
                        .substring(data.getEcloud_keys().get(i).lastIndexOf("/") + 1);
                    String savePath = "/path/to/save/" + filename;
                    
                    try {
                        client.downloadByPresignedUrl(url, savePath);
                        System.out.println("下载成功: " + savePath);
                    } catch (Exception e) {
                        System.err.println("下载失败: " + e.getMessage());
                    }
                }
                
            } else if ("failed".equals(data.getStatus())) {
                System.err.println("任务 " + data.getTask_id() + " 失败: " + data.getMessage());
            }
            
            return "OK";
        }
    }

    // ==================== 完整流程示例 ====================

    /**
     * 完整流程示例
     */
    public void completeWorkflow() {
        try {
            // 1. 上传音频到移动云
            File audioFile = new File("/path/to/audio.mp3");
            String ecloudKey = "input/" + audioFile.getName();
            
            System.out.println("步骤1: 上传音频到移动云");
            String etag = uploadAudioToEcloud(audioFile, ecloudKey);
            System.out.println("上传成功, ETag: " + etag);
            
            // 2. 调用 MSST API 处理
            String taskId = processAudio(
                ecloudKey,
                "my_preset",  // Preset配置名
                "wav",        // 输出格式
                "http://your-server/callback"  // 回调地址
            );
            
            System.out.println("步骤2: 任务已提交, Task ID: " + taskId);
            
            // 3. 等待回调 (或轮询查询)
            System.out.println("步骤3: 等待回调通知...");
            
            // 回调会自动触发下载,无需手动操作
            
        } catch (Exception e) {
            System.err.println("流程失败: " + e.getMessage());
            e.printStackTrace();
        }
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
        private List<String> ecloud_keys;      // 移动云对象Key列表
        private List<String> download_urls;    // 预签名URL列表
        private Integer url_expire_seconds;    // URL有效期(秒)
        private String message;
    }

    // ==================== 移动云上传工具类 ====================

    /**
     * 移动云上传工具类
     */
    public static class EcloudUploader {
        private final String endpoint;
        private final String region;
        private final String accessKey;
        private final String secretKey;

        public EcloudUploader(String endpoint, String region, String accessKey, String secretKey) {
            this.endpoint = endpoint;
            this.region = region;
            this.accessKey = accessKey;
            this.secretKey = secretKey;
        }

        public String uploadFile(File file, String objectName, String bucketName) {
            com.amazonaws.services.s3.AmazonS3 client = null;
            try {
                client = createS3Client();
                
                // 设置元数据
                com.amazonaws.services.s3.model.ObjectMetadata metadata = 
                    new com.amazonaws.services.s3.model.ObjectMetadata();
                metadata.setContentType("audio/mpeg");
                metadata.addUserMetadata("upload-time", 
                    String.valueOf(System.currentTimeMillis()));
                
                com.amazonaws.services.s3.model.PutObjectResult result = 
                    client.putObject(bucketName, objectName, file, metadata);
                
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

        private com.amazonaws.services.s3.AmazonS3 createS3Client() {
            com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration endpointConfiguration = 
                new com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration(endpoint, region);
            
            com.amazonaws.auth.BasicAWSCredentials credentials = 
                new com.amazonaws.auth.BasicAWSCredentials(accessKey, secretKey);
            com.amazonaws.auth.AWSCredentialsProvider credentialsProvider = 
                new com.amazonaws.auth.AWSStaticCredentialsProvider(credentials);
            
            return com.amazonaws.services.s3.AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(endpointConfiguration)
                    .withCredentials(credentialsProvider)
                    .build();
        }
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        YinfangEcloudClient client = new YinfangEcloudClient();
        
        System.out.println("========================================");
        System.out.println("   音立方 - 移动云对接 Demo");
        System.out.println("========================================\n");
        
        // 运行完整流程
        client.completeWorkflow();
        
        System.out.println("\n========================================");
        System.out.println("   Demo 执行完成!");
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
        return new YinfangEcloudClient.CallbackController().handleCallback(callbackData);
    }
}