## 简单上传
更新时间：2025/09/17 产品信息
功能说明
简单上传指通过 putObject 接口上传单个对象（Object），数据可以是二进制流或者本地文件。

### 接口定义
|接口| 	说明     |
|-|---------|
|PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata)| 	流式上传   |
|PutObjectResult putObject(String bucketName, String key, File file)| 	本地文件上传 |

### 接口约束
- 使用该接口上传的对象大小不能超过 5 GB。
- 在桶中若存在同名文件，将被新上传的文件覆盖。
- 文件名长度长度大于 0 且不超过 1023 字节。

### 请求参数说明
| 参数名称   | 参数类型            | 是否必选 | 参数解释           |
| ---------- | ------------------- | -------- | ------------------ |
| bucketName | String              | 必选     | 桶名               |
| key        | String              | 必选     | 对象名             |
| input      | java.io.InputStream | 可选     | 待上传文件的数据流 |
| metadata   | ObjectMetadata      | 可选     | 对象元数据         |
| file       | java.io.File        | 可选     | 待上传的本地文件   |

### 返回结果说明
表 PutObjectResult

| 参数名称      |参数类型|参数解释|
|-----------|-|-|
| etag      |String|对象的 etag 值，是对象内容的唯一标识，可以通过该值识别对象内容是否有变化。|
| versionId |String|对象的版本号。如果桶的多版本状态为开启，则会返回对象的版本号。 |

### 代码示例
#### 流式上传字符串：

```java
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.io.ByteArrayInputStream;

public class MainClass {
public static void main(String[] args) {
// 填写存储桶（Bucket）所在地域对应的 endpoint 和 Region。
// 以华东 - 苏州为例，endpoint 填写 https://eos-wuxi-1.cmecloud.cn，Region 填写 wuxi1。
String endpoint = "<your-endpoint>";
String region = "<your-region>";


        // 填写 EOS 账号的认证信息，或者子账号的认证信息。
        String accessKey = "<your-access-key>";
        String secretKey = "<your-secret-access-key>";

        // 填写要上传到的存储桶名称，例如'example-bucket'。
        String bucketName = "<your-bucket-name>";
        // 填写上传后要显示的对象名，例如'object.txt'。
        String objectName = "<your-object-name>";
        // 填写要上传的对象内容，例如'abcde'。
        String content = "<your-object-content>";

        // 创建 AmazonS3 实例。
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider).build();

        // 上传字符串。
        client.putObject(bucketName, objectName, new ByteArrayInputStream(content.getBytes()), null);

        // 关闭 client。
        client.shutdown();
    }

}
```

#### 流式上传 byte 数组：
```java
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.io.ByteArrayInputStream;

public class MainClass {

    public static void main(String[] args) {
        // 填写存储桶（Bucket）所在地域对应的 endpoint 和 Region。
        // 以华东 - 苏州为例，endpoint 填写 https://eos-wuxi-1.cmecloud.cn，Region 填写 wuxi1。
        String endpoint = "<your-endpoint>";
        String region = "<your-region>";

        // 填写 EOS 账号的认证信息，或者子账号的认证信息。
        String accessKey = "<your-access-key>";
        String secretKey = "<your-secret-access-key>";

        // 填写要上传到的存储桶名称，例如'example-bucket'。
        String bucketName = "<your-bucket-name>";
        // 填写上传后要显示的对象名，例如'object.txt'。
        String objectName = "<your-object-name>";
        // 填写要上传的对象内容，例如'abcde'。
        String content = "<your-object-content>";

        // 创建 AmazonS3 实例。
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider).build();

        // 上传 byte 数组。
        byte[] data = content.getBytes();
        client.putObject(bucketName, objectName, new ByteArrayInputStream(data), null);

        // 关闭 client。
        client.shutdown();
    }
}
```
#### 流式上传网络流：
```java
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MainClass {

    public static void main(String[] args) {
        // 填写存储桶（Bucket）所在地域对应的 endpoint 和 Region。
        // 以华东 - 苏州为例，endpoint 填写 https://eos-wuxi-1.cmecloud.cn，Region 填写 wuxi1。
        String endpoint = "<your-endpoint>";
        String region = "<your-region>";

        // 填写 EOS 账号的认证信息，或者子账号的认证信息。
        String accessKey = "<your-access-key>";
        String secretKey = "<your-secret-access-key>";

        // 填写要上传到的存储桶名称，例如'example-bucket'。
        String bucketName = "<your-bucket-name>";
        // 填写上传后要显示的对象名，例如'object.txt'。
        String objectName = "<your-object-name>";

        // 创建 AmazonS3 实例。
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider).build();

        // 上传网络流。
        InputStream in = null;
        try {
            in = new URL("https://ecloud.10086.cn/").openStream();
            client.putObject(bucketName, objectName, in, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in, null);
        }

        // 关闭 client。
        client.shutdown();
    }
}
```
#### 流式上传对象流：
```java
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.IOUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MainClass {

    public static void main(String[] args) {
        // 填写存储桶（Bucket）所在地域对应的 endpoint 和 Region。
        // 以华东 - 苏州为例，endpoint 填写 https://eos-wuxi-1.cmecloud.cn，Region 填写 wuxi1。
        String endpoint = "<your-endpoint>";
        String region = "<your-region>";

        // 填写 EOS 账号的认证信息，或者子账号的认证信息。
        String accessKey = "<your-access-key>";
        String secretKey = "<your-secret-access-key>";

        // 填写要上传到的存储桶名称，例如'example-bucket'。
        String bucketName = "<your-bucket-name>";
        // 填写上传后要显示的对象名，例如'object.txt'。
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

        // 上传对象流。
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(localFile);
            client.putObject(bucketName, objectName, fin, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(fin, null);
        }

        // 关闭 client。
        client.shutdown();
    }
}
```
注意

使用流式上传时，如果采用上述的方式，会有 Out Of Memory 的风险（数据会缓存在内存中）。

#### 如果事先知道对象（Object）的大小，可以采用如下的方式，来规避风险：
```java
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.ByteArrayInputStream;

public class MainClass {

    public static void main(String[] args) {
        // 填写存储桶（Bucket）所在地域对应的 endpoint 和 Region。
        // 以华东 - 苏州为例，endpoint 填写 https://eos-wuxi-1.cmecloud.cn，Region 填写 wuxi1。
        String endpoint = "<your-endpoint>";
        String region = "<your-region>";

        // 填写 EOS 账号的认证信息，或者子账号的认证信息。
        String accessKey = "<your-access-key>";
        String secretKey = "<your-secret-access-key>";

        // 填写要上传到的存储桶名称，例如'example-bucket'。
        String bucketName = "<your-bucket-name>";
        // 填写上传后要显示的对象名，例如'object.txt'。
        String objectName = "<your-object-name>";
        // 填写要上传的对象内容，例如'abcde'。
        String content = "<your-object-content>";

        // 创建 AmazonS3 实例。
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider).build();

        // 上传字符串。
        // 设置文件（Object）大小。
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(content.length());
        client.putObject(bucketName, objectName,
                new ByteArrayInputStream(content.getBytes()), meta);

        // 关闭 client。
        client.shutdown();
    }
}
```
#### 本地文件上传：
```java
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

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

        // 填写要上传到的存储桶名称，例如'example-bucket'。
        String bucketName = "<your-bucket-name>";
        // 填写上传后要显示的对象名，例如'object.txt'。
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

        // 本地文件上传。
        client.putObject(bucketName, objectName, new File(localFile));

        // 关闭 client。
        client.shutdown();
    }
}
```