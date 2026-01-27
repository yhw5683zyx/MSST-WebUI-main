import java.io.File;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class Demo {

    我来为你编写音立方 Java 程序调用 MSST API 服务的代码示例。

    Java 调用代码示例

  1. Maven 依赖 (pom.xml)

   <dependencies>
       <!-- HTTP 客户端 -->
       <dependency>
           <groupId>org.apache.httpcomponents</groupId>
           <artifactId>httpclient</artifactId>
           <version>4.5.14</version>
       </dependency>

       <!-- JSON 处理 -->
       <dependency>
           <groupId>com.google.code.gson</groupId>
           <artifactId>gson</artifactId>
           <version>2.10.1</version>
       </dependency>

       <!-- 文件上传 -->
       <dependency>
           <groupId>org.apache.httpcomponents</groupId>
           <artifactId>httpmime</artifactId>
           <version>4.5.14</version>
       </dependency>

       <!-- Lombok (可选,简化代码) -->
       <dependency>
           <groupId>org.projectlombok</groupId>
           <artifactId>lombok</artifactId>
           <version>1.18.30</version>
           <scope>provided</scope>
       </dependency>
   </dependencies>

            2. 完整的 Java 调用代码

   package com.yinlifang.msst.client;

   import com.google.gson.Gson;
   import com.google.gson.JsonObject;
   import com.google.gson.reflect.TypeToken;
   import lombok.Data;
   import org.apache.http.HttpEntity;
   import org.apache.http.HttpResponse;
   import org.apache.http.client.methods.*;
   import org.apache.http.entity.ContentType;
   import org.apache.http.entity.mime.MultipartEntityBuilder;
   import org.apache.http.impl.client.CloseableHttpClient;
   import org.apache.http.impl.client.HttpClients;
   import org.apache.http.util.EntityUtils;

   import java.io.File;
   import java.io.IOException;
   import java.nio.charset.StandardCharsets;
   import java.util.List;
   import java.util.Map;
   import java.util.UUID;

    /**
     * MSST API 客户端
     * 用于调用 MSST 音频分离服务
     */
    public class MsstApiClient {

        private static final String BASE_URL = "http://117.50.182.134:8000";
        private final Gson gson = new Gson();

        /**
         * 上传音频文件并启动分离任务
         *
         * @param audioFile     音频文件
         * @param presetName    Preset 配置名称
         * @param outputFormat  输出格式 (wav, mp3, flac)
         * @param callbackUrl   回调地址
         * @return 上传响应
         */
        public UploadResponse uploadAudio(File audioFile, String presetName,
                                          String outputFormat, String callbackUrl) throws IOException {
            String taskId = UUID.randomUUID().toString();
            String url = BASE_URL + "/upload";

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(url);

                // 构建 multipart 请求
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.addBinaryBody("file", audioFile,
                        ContentType.create("audio/mpeg"), audioFile.getName());
                builder.addTextBody("task_id", taskId);
                builder.addTextBody("preset_name", presetName);
                builder.addTextBody("output_format", outputFormat);
                if (callbackUrl != null && !callbackUrl.isEmpty()) {
                    builder.addTextBody("callback_url", callbackUrl);
                }

                httpPost.setEntity(builder.build());

                // 发送请求
                HttpResponse response = httpClient.execute(httpPost);
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (response.getStatusLine().getStatusCode() == 200) {
                    return gson.fromJson(responseBody, UploadResponse.class);
                } else {
                    throw new IOException("Upload failed: " + responseBody);
                }
            }
        }

        /**
         * 查询任务状态
         *
         * @param taskId 任务ID
         * @return 任务状态
         */
        public TaskStatusResponse getTaskStatus(String taskId) throws IOException {
            String url = BASE_URL + "/status/" + taskId;

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(url);

                HttpResponse response = httpClient.execute(httpGet);
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (response.getStatusLine().getStatusCode() == 200) {
                    return gson.fromJson(responseBody, TaskStatusResponse.class);
                } else {
                    throw new IOException("Get status failed: " + responseBody);
                }
            }
        }

        /**
         * 下载结果文件
         *
         * @param taskId   任务ID
         * @param filename 文件名
         * @param savePath 保存路径
         */
        public void downloadResult(String taskId, String filename, String savePath) throws IOException {
            String url = BASE_URL + "/download/" + taskId + "/" + filename;

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(url);

                HttpResponse response = httpClient.execute(httpGet);

                if (response.getStatusLine().getStatusCode() == 200) {
                    byte[] fileData = EntityUtils.toByteArray(response.getEntity());
                    java.nio.file.Files.write(
                            java.nio.file.Paths.get(savePath),
                            fileData
                    );
                    System.out.println("File downloaded: " + savePath);
                } else {
                    throw new IOException("Download failed: " + response.getStatusLine().getStatusCode());
                }
            }
        }

        /**
         * 获取任务所有结果列表
         *
         * @param taskId 任务ID
         * @return 结果列表
         */
        public JsonObject getResults(String taskId) throws IOException {
            String url = BASE_URL + "/results/" + taskId;

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(url);

                HttpResponse response = httpClient.execute(httpGet);
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (response.getStatusLine().getStatusCode() == 200) {
                    return gson.fromJson(responseBody, JsonObject.class);
                } else {
                    throw new IOException("Get results failed: " + responseBody);
                }
            }
        }

        /**
         * 清理任务文件
         *
         * @param taskId 任务ID
         */
        public void cleanupTask(String taskId) throws IOException {
            String url = BASE_URL + "/task/" + taskId;

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpDelete httpDelete = new HttpDelete(url);

                HttpResponse response = httpClient.execute(httpGet);

                if (response.getStatusLine().getStatusCode() == 200) {
                    System.out.println("Task cleaned up: " + taskId);
                } else {
                    throw new IOException("Cleanup failed: " + response.getStatusLine().getStatusCode());
                }
            }
        }

        /**
         * 健康检查
         */
        public JsonObject healthCheck() throws IOException {
            String url = BASE_URL + "/health";

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(url);

                HttpResponse response = httpClient.execute(httpGet);
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                return gson.fromJson(responseBody, JsonObject.class);
            }
        }

        // ==================== 数据模型 ====================

        @Data
        public static class UploadResponse {
            private String task_id;
            private String status;
            private String message;
        }

        @Data
        public static class TaskStatusResponse {
            private String task_id;
            private String status;  // processing, completed, failed
            private String message;
            private List<String> results;
        }

        @Data
        public static class CallbackData {
            private String task_id;
            private String status;
            private List<String> results;
            private List<String> download_urls;
            private String message;
        }
    }

  3. 使用示例

   package com.yinlifang.msst.example;

   import com.yinlifang.msst.client.MsstApiClient;
   import java.io.File;
   import java.io.IOException;

    public class MsstExample {

        public static void main(String[] args) {
            MsstApiClient client = new MsstApiClient();

            try {
                // 1. 健康检查
                System.out.println("Health check: " + client.healthCheck());

                // 2. 上传文件并启动任务
                File audioFile = new File("/path/to/your/audio.mp3");
                String presetName = "my_preset";  // 对应 presets/my_preset.json
                String outputFormat = "wav";
                String callbackUrl = "http://your-server/callback";  // 你的回调地址

                MsstApiClient.UploadResponse uploadResponse = client.uploadAudio(
                        audioFile, presetName, outputFormat, callbackUrl
                );

                String taskId = uploadResponse.getTask_id();
                System.out.println("Task started: " + taskId);

                // 3. 轮询查询状态 (如果不使用回调)
                while (true) {
                    MsstApiClient.TaskStatusResponse status = client.getTaskStatus(taskId);
                    System.out.println("Task status: " + status.getStatus());

                    if ("completed".equals(status.getStatus())) {
                        System.out.println("Task completed!");
                        break;
                    } else if ("failed".equals(status.getStatus())) {
                        System.out.println("Task failed: " + status.getMessage());
                        return;
                    }

                    // 等待 5 秒后再次查询
                    Thread.sleep(5000);
                }

                // 4. 获取结果列表
                var results = client.getResults(taskId);
                System.out.println("Results: " + results);

                // 5. 下载结果文件
                List<String> resultFiles = results.getAsJsonArray("results")
                        .asList()
                        .stream()
                        .map(jsonElement -> jsonElement.getAsString())
                        .map(path -> path.substring(path.lastIndexOf("/") + 1))
                        .toList();

                for (String filename : resultFiles) {
                    String savePath = "/path/to/save/" + filename;
                    client.downloadResult(taskId, filename, savePath);
                }

                // 6. 清理任务文件 (可选)
                client.cleanupTask(taskId);

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

  4. 回调接口实现 (Spring Boot 示例)

   package com.yinlifang.controller;

   import com.yinlifang.msst.client.MsstApiClient;
   import com.google.gson.Gson;
   import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/api")
    public class MsstCallbackController {

        private final Gson gson = new Gson();

        /**
         * 接收 MSST 服务回调
         */
        @PostMapping("/callback")
        public String handleCallback(@RequestBody String callbackData) {
            System.out.println("Received callback: " + callbackData);

            MsstApiClient.CallbackData data = gson.fromJson(callbackData,
                    MsstApiClient.CallbackData.class);

            if ("completed".equals(data.getStatus())) {
                System.out.println("Task " + data.getTask_id() + " completed!");
                System.out.println("Results: " + data.getResults());
                System.out.println("Download URLs: " + data.getDownload_urls());

                // 下载结果文件
                MsstApiClient client = new MsstApiClient();
                for (int i = 0; i < data.getResults().size(); i++) {
                    String filename = data.getResults().get(i)
                            .substring(data.getResults().get(i).lastIndexOf("/") + 1);
                    String savePath = "/path/to/save/" + filename;

                    try {
                        client.downloadResult(data.getTask_id(), filename, savePath);
                    } catch (IOException e) {
                        System.err.println("Download failed: " + e.getMessage());
                    }
                }

                // 清理任务
                try {
                    client.cleanupTask(data.getTask_id());
                } catch (IOException e) {
                    System.err.println("Cleanup failed: " + e.getMessage());
                }

            } else if ("failed".equals(data.getStatus())) {
                System.err.println("Task " + data.getTask_id() + " failed: " + data.getMessage());
            }

            return "OK";
        }
    }

  5. Spring Boot 集成 (更简洁的方式)

    如果你使用 Spring Boot,可以使用 RestTemplate 或 WebClient:

            package com.yinlifang.msst.service;

   import org.springframework.core.io.FileSystemResource;
   import org.springframework.http.*;
   import org.springframework.stereotype.Service;
   import org.springframework.util.LinkedMultiValueMap;
   import org.springframework.util.MultiValueMap;
   import org.springframework.web.client.RestTemplate;

   import java.io.File;

    @Service
    public class MsstService {

        private static final String BASE_URL = "http://117.50.182.134:8000";
        private final RestTemplate restTemplate = new RestTemplate();

        public UploadResponse uploadAudio(File audioFile, String presetName,
                                          String outputFormat, String callbackUrl) {

            String url = BASE_URL + "/upload";

            // 构建 multipart 请求
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(audioFile));
            body.add("task_id", java.util.UUID.randomUUID().toString());
            body.add("preset_name", presetName);
            body.add("output_format", outputFormat);
            if (callbackUrl != null) {
                body.add("callback_url", callbackUrl);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<UploadResponse> response = restTemplate.postForEntity(
                    url, requestEntity, UploadResponse.class
            );

            return response.getBody();
        }
    }

  ---

    关键点说明

  1. 服务器配置
   - 服务器 IP: 117.50.182.134
            - 端口: 8000
            - Base URL: http://117.50.182.134:8000

            2. 调用流程
   1. 上传文件 → 获取 task_id
   2. 等待回调 或 轮询状态
   3. 下载结果文件
   4. 清理任务 (可选)

  3. 注意事项
   - 确保服务器防火墙开放 8000 端口
   - 确保音立方服务器可以访问 MSST 服务器
   - 大文件上传可能需要设置超时时间
   - 建议使用回调方式而非轮询,减少服务器压力
}
