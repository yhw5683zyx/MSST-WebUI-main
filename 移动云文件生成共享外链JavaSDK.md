## 生成共享外链
更新时间：2025/09/17 产品信息
功能说明
利用 generatePresignedUrl 接口，可以为一个对象（Object）生成一个预签名的 URL 链接，使用浏览器访问该链接即可下载该对象（Object）。您可以将该链接共享给其他人，从而达到共享该对象的目的。

### 接口定义
|接口| 	说明    |
|-|--------|
|URL generatePresignedUrl(GeneratePresignedUrlRequest req)| 	生成共享外链|

### 接口约束
- 带有预签名的 URL 链接，都带有过期时间。 
- v2 签名有效期最长可设置为 30 年，v4 签名有效期最长可设置为 7 天。 
- 设置签名算法版本参考对象存储 EOS初始化配置客户端小节中的signerOverride参数。 
- 浏览器能否支持预览与其自身可支持预览的对象类型有关，如果有预览需求，在上传对象时需要指定对象类型 content-type。 
- 即使上传的是图片，但 content-type 不是 png、jpg 等对象类型时，浏览器可能无法进行预览对象。

### 请求参数说明
| 参数名称 | 参数类型                    | 是否必选 | 参数解释             |
| -------- | --------------------------- | -------- | -------------------- |
| req      | GeneratePresignedUrlRequest | 必选     | 生成共享外链请求参数 |

GeneratePresignedUrlRequest 表

| 参数名称        | 参数类型                | 是否必选 | 参数解释                                     |
| --------------- | ----------------------- | -------- | -------------------------------------------- |
| bucketName      | String                  | 必选     | 桶名                                         |
| key             | String                  | 必选     | 对象名                                       |
| expiration      | java.util.Date          | 可选     | 共享外链过期时间，单位毫秒，默认值为：900000 |
| method          | HttpMethod              | 可选     | 访问外链的方法类型，默认为 GET               |
| responseHeaders | ResponseHeaderOverrides | 可选     | HTTP 响应头                                  |

ResponseHeaderOverrides 表

| 参数名称           | 参数类型 | 是否必选 | 参数解释           |
| ------------------ | -------- | -------- | ------------------ |
| contentDisposition | String   | 可选     | 响应内容的展示类型 |

### 返回结果说明
java.net.URL

### 代码示例
#### 生成可下载预签名 URL：

```java
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainClass {

    public static void main(String[] args) {
        // 填写存储桶（Bucket）所在地域对应的 endpoint 和 Region。
        // 以华东 - 苏州为例，endpoint 填写 https://eos-wuxi-1.cmecloud.cn，Region 填写 wuxi1。
        String endpoint = "<your-endpoint>";
        String region = "<your-region>";

        // 填写 EOS 账号的认证信息，或者子账号的认证信息。
        String accessKey = "<your-access-key>";
        String secretKey = "<your-secret-access-key>";

        // 填写存储桶名称，例如'example-bucket'。
        String bucketName = "<your-bucket-name>";
        // 填写对象名，例如'object.txt'。
        String objectName = "<your-object-name>";

        // 创建 AmazonS3 实例。
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("S3SignerType");
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider).build();

        // 生成共享外链。
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucketName, objectName);
        // 设置过期时间，当到达该时间点时，URL 就会过期，其他人不再能访问该对象（Object）。
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date expiration = null;
        try {
            expiration = simpleDateFormat.parse("2022/12/31 23:59:59");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // 设置 1 小时后过期。
        // Date expiration = new java.util.Date();
        // long expTimeMillis = Instant.now().toEpochMilli();
        // expTimeMillis += 1000 * 60 * 60;
        // expiration.setTime(expTimeMillis);
        request.setExpiration(expiration);
        
        // 设置外链的访问方法，默认为 GET。
        request.setMethod(HttpMethod.GET);
        // 开启多版本的桶可填写 versionId
        // request.addRequestParameter("versionId", null);
        
        URL url = client.generatePresignedUrl(request);
        System.out.println(url);

        // 关闭 client。
        client.shutdown();
    }
}
```
#### 生成可上传预签名 URL：
```java
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class GeneratePresignedUrlAndUploadObject {

    public static void main(String[] args) throws IOException {
        // 填写存储桶（Bucket）所在地域对应的 endpoint 和 Region。
        // 以华东 - 苏州为例，endpoint 填写 https://eos-wuxi-1.cmecloud.cn，Region 填写 wuxi1。
        String endpoint = "<your-endpoint>";
        String region = "<your-region>";

        // 填写 EOS 账号的认证信息，或者子账号的认证信息。
        String accessKey = "<your-access-key>";
        String secretKey = "<your-secret-access-key>";

        // 填写存储桶名称，例如'example-bucket'。
        String bucketName = "<your-bucket-name>";
        // 填写对象名，例如'object.txt'。
        String objectName = "<your-object-name>";

        try {
            // 创建 AmazonS3 实例。
            AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
            BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setSignerOverride("S3SignerType");

            AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withClientConfiguration(clientConfiguration)
                    .withEndpointConfiguration(endpointConfiguration)
                    .withPathStyleAccessEnabled(false)
                    .withCredentials(credentialsProvider).build();

            // 设置访问过期时间
            Date expiration = new Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += 1000 * 60 * 60;
            expiration.setTime(expTimeMillis);

            // 生成预签名URL
            System.out.println("Generating pre-signed URL.");
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectName)
                    // 设置外链的访问方法为 PUT
                    .withMethod(HttpMethod.PUT)
                    .withExpiration(expiration);
            
            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
            System.out.println(url);
        } catch (SdkClientException e) {
            e.printStackTrace();
        }
    }
}
```
#### 使用签名的上传 URL 上传文件：
```java
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadByUrl {

    public static void main(String[] args) throws IOException {
        URL url = new URL("<your-Presigned-Url>");

        // 创建一个 HttpURLConnection 对象发送 HTTP 请求。
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        out.write("This text uploaded as an object via presigned URL.");
        out.close();
    }

}
```
#### 生成可预览的预签名 URL：
```java
浏览器使用共享外链下载对象时，根据对象类型，可以在浏览器中预览展示，或者直接以对象形式下载。一般来说，图片、视频等可以直接在浏览器中展示。
如果想控制浏览器的行为，可以按如下方式生成共享外链：

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainClass {

    public static void main(String[] args) {
        // 填写存储桶（Bucket）所在地域对应的 endpoint 和 Region。
        // 以华东 - 苏州为例，endpoint 填写 https://eos-wuxi-1.cmecloud.cn，Region 填写 wuxi1。
        String endpoint = "<your-endpoint>";
        String region = "<your-region>";

        // 填写 EOS 账号的认证信息，或者子账号的认证信息。
        String accessKey = "<your-access-key>";
        String secretKey = "<your-secret-access-key>";

        // 填写存储桶名称，例如'example-bucket'。
        String bucketName = "<your-bucket-name>";
        // 填写对象名，例如'object.txt'。
        String objectName = "<your-object-name>";

        // 创建 AmazonS3 实例。
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("S3SignerType");
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider).build();

        // 生成可预览的外链。
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucketName, objectName);

        // 设置过期时间，当到达该时间点时，URL 就会过期，其他人不再能访问该对象（Object）。
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date expiration = null;
        try {
            expiration = simpleDateFormat.parse("2022/12/31 23:59:59");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        request.setExpiration(expiration);

        // 设置返回头
        // 设置为 "inline" 时在浏览器中展示，设置为 "attachment" 时以文件形式下载。
        // 此外设置为 "attachment;filename=\"filename.jpg\"" ，还可以让下载的文件名字重命名为 "filename.jpg"。
        ResponseHeaderOverrides headerOverrides = new ResponseHeaderOverrides();
        headerOverrides.setContentDisposition("inline");
        request.setResponseHeaders(headerOverrides);

        URL url = client.generatePresignedUrl(request);
        System.out.println(url);

        // 关闭 client。
        client.shutdown();
    }
}
```