## 下载到本地文件
更新时间：2025/09/17 产品信息
功能说明
可以把对象（Object）的内容下载到指定的本地文件中，如果指定的本地文件不存在，则会新建对象。

### 接口定义
|接口| 	说明     |
|-|---------|
|S3Object getObject(GetObjectRequest getObjectRequest, File destinationFile)| 	下载到本地文件|

### 请求参数说明
| 参数名称         | 参数类型         | 是否必选 | 参数解释         |
| ---------------- | ---------------- | -------- | ---------------- |
| getObjectRequest | GetObjectRequest | 必选     | 下载文件请求参数 |
| destinationFile  | File             | 必选     | 本地文件         |

GetObjectRequest 表

| 参数名称   | 参数类型 | 是否必选 | 参数解释 |
| ---------- | -------- | -------- | -------- |
| bucketName | String   | 必选     | 桶名     |
| key        | String   | 必选     | 对象名   |



### 返回结果说明
表 ObjectMetadata

| 参数名称     | 参数类型 | 参数解释       |
| ------------ | -------- | -------------- |
| metadata     | Map      | HTTP 标准属性  |
| userMetadata | Map      | 用户自定义属性 |

### 代码示例
#### 下载到本地文件：

```java
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.File;

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
        // 填写要下载的对象名，例如'object.txt'。
        String objectName = "<your-object-name>";
        // 填写本地文件路径，例如 linux 路径'/usr/object/object.txt'或 windows 路径'D:\object\object.txt'。
        String localFile = "<local-file-path>";

        // 创建 AmazonS3 实例。
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider).build();

        // 将文件（Object）下载到文件中，并返回文件（Object）的元数据。
        GetObjectRequest request = new GetObjectRequest(bucketName, objectName);
        ObjectMetadata meta = client.getObject(request, new File(localFile));

        // 关闭 client。
        client.shutdown();
    }
}

```