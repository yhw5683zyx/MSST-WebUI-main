## 生成共享外链
更新时间：2025/09/17 产品信息
功能说明
利用 generatePresignedUrl 接口，可以为一个对象（Object）生成一个预签名的 URL 链接，使用浏览器访问该链接即可下载该对象（Object）。您可以将该链接共享给其他人，从而达到共享该对象的目的。

### 接口定义
|接口| 	说明    |
|-|--------|
|generate_presigned_url(clientMethod: str, params: dict, expiresIn: int, httpMethod: str)| 	生成共享外链|

### 接口约束
- 调用者必须拥有 EOS 账号的认证信息，或者子账号的认证信息。
- 带有预签名的 URL 链接，都带有过期时间。
- v2 签名有效期最长可设置为 30 年。
- v4 签名有效期最长可设置为 7 天。
- 浏览器能否支持预览与其自身可支持预览的对象类型有关，如果有预览需求，在上传对象时需要指定对象类型 content-type。
- 即使上传的是图片，但 content-type 不是 png、jpg 等对象类型时，浏览器可能无法进行预览对象。
- EOS 桶本身也可以有策略，这些策略进一步控制哪些用户或角色可以对该桶执行哪些操作。
- 预签名 URL 通常与生成它们的 EOS 客户端所在的区域相关联，如果尝试从另一个区域访问对象，可能会遇到区域限制问题；桶策略可以是对所有用户公开的（例如，用于静态网站托管），也可以是对特定用户或角色授权的。
- 对于 GET 操作，如果指定的对象在桶中不存在，则请求将失败，对于 DELETE 操作，如果对象不存在，则请求可能成功（取决于桶的配置和 IAM 策略），但不会有任何对象被删除。

### 请求参数说明
| 参数名称     | 参数类型 | 是否必选 | 参数解释                                                     |
| ------------ | -------- | -------- | ------------------------------------------------------------ |
| ClientMethod | str      | 必选     | 指定要预签名的eos操作，例如'get_object'、'put_object'等      |
| Params       | dict     | 非必选   | 一个字典，包含要传递给eos操作的参数。例如，对于'get_object'操作，通常需要包含'Bucket'和'Key'参数 |
| ExpiresIn    | int      | 非必选   | 预签名URL的有效期（以秒为单位）。默认值为3600秒（即1小时）   |
| HttpMethod   | str      | 非必选   | 指定HTTP方法（例如'GET'、'PUT'等）。如果未指定，则根据ClientMethod的默认值来确定 |

Params 表

| 参数名称  | 参数类型 | 是否必选 | 参数解释                                             |
| --------- | -------- | -------- | ---------------------------------------------------- |
| Bucket    | str      | 必选     | 指定要访问的eos存储桶的名称                          |
| Key       | str      | 必选     | 指定要访问的eos对象（文件）的键（即路径和文件名）    |
| VersionId | str      | 非必选   | 指定要访问的S3对象的特定版本ID（如果启用了版本控制） |

### 返回结果说明

| 参数名称 | 参数类型 | 参数解释                        |
| -------- | -------- | ------------------------------- |
| URL      | str      | 方法返回一个字符串，即预签名URL |


### 代码示例
#### 使用共享外链下载：
对于私有 bucket，可以生成共享外链（又称为预签名 URL）供用户访问，下面是生成共享外链下载，该链接在 3600 秒后失效。

```python
# -*- coding: utf-8 -*-
from boto3.session import Session
from botocore.exceptions import NoCredentialsError, PartialCredentialsError, ClientError

# Client 初始化
# 填写 EOS 账号的认证信息，或者子账号的认证信息
access_key = "your-access-key"
secret_key = "your-secret-access-key"
# 填写 url。例如：'https://IP:PORT' 或者 'https://eos-wuxi-1.cmecloud.cn'
url = "your-endpoint"
session = Session(access_key, secret_key)
s3_client = session.client('s3', endpoint_url=url)

try:
    # 生成预签名的 URL  
    url = s3_client.generate_presigned_url(
        ClientMethod='get_object',  # 指定要调用的 S3 客户端方法  
        Params={'Bucket': 'your-bucket-name', 'Key': 'you-object-name'},  # 指定 S3 桶和对象键  
        ExpiresIn=3600,  # URL 在 3600 秒（1 小时）后过期  
        HttpMethod='GET'  # 指定 HTTP 方法为 GET  
    )
    print(f"Pre-signed URL for downloading the object: {url}")
except (NoCredentialsError, PartialCredentialsError) as e:
    print(f"Credentials error: {e}")
except ClientError as e:
    print(f"Client error: {e}")
```
上面的python代码将输出一个预签名的URL，直接用 curl 和生成的签名连接下载对象：

这里的 -O 选项告诉 curl 将下载的文件保存到与对象键同名的本地文件中。如果您希望将文件保存到不同的名称，可以使用 -o 选项并指定文件名。

curl -O "生成的预签名的URL"

#### 使用共享外链上传：
```python
# -*- coding: utf-8 -*-
from boto3.session import Session
from botocore.exceptions import NoCredentialsError, PartialCredentialsError

# Client 初始化
# 填写 EOS 账号的认证信息，或者子账号的认证信息
access_key = "your-access-key"
secret_key = "your-secret-access-key"
# 填写 url。例如：'https://IP:PORT' 或者 'https://eos-wuxi-1.cmecloud.cn'
url = "your-endpoint"
session = Session(access_key, secret_key)
s3_client = session.client('s3', endpoint_url=url)

try:
    # 生成用于上传对象的预签名 URL  
    url = s3_client.generate_presigned_url(
        ClientMethod='put_object',  # 指定要调用的 S3 客户端方法为 put_object  
        Params={'Bucket': 'your-bucket-name', 'Key': 'you-object-name'},  # 指定 S3 桶和对象键  
        ExpiresIn=3600,  # URL 在 3600 秒（1 小时）后过期  
        HttpMethod='PUT'  # 指定 HTTP 方法为 PUT  
    )
    print(f"Pre-signed URL for uploading the object: {url}")
except (NoCredentialsError, PartialCredentialsError) as e:
    print(f"Credentials error: {e}")
except ClientError as e:
    print(f"Client error: {e}")

```
上面的python代码将输出一个预签名的URL，直接用 curl 和生成的签名连接上传对象：

这里的 -T 选项告诉 curl 要上传的文件。请确保将 "本地文件路径" 替换为您要上传的文件的实际路径。

curl -T "本地文件路径" "生成的预签名的URL"


#### 使用共享外链删除：
对于私有 bucket，可以生成共享外链（又称为预签名 URL）供用户访问，下面是生成共享外链删除对象，该链接在 3600 秒后失效。
```python
# -*- coding: utf-8 -*-
from boto3.session import Session
from botocore.exceptions import NoCredentialsError, PartialCredentialsError, ClientError

# Client 初始化
# 填写 EOS 账号的认证信息，或者子账号的认证信息
access_key = "your-access-key"
secret_key = "your-secret-access-key"
# 填写 url。例如：'https://IP:PORT' 或者 'https://eos-wuxi-1.cmecloud.cn'
url = "your-endpoint"
session = Session(access_key, secret_key)
s3_client = session.client('s3', endpoint_url=url)

try:
    # 生成预签名的 URL  
    url = s3_client.generate_presigned_url(
        ClientMethod='delete_object',
        Params={'Bucket': 'your-bucket-name', 'Key': 'you-object-name'},
        ExpiresIn=3600,  # URL 在 3600 秒（1 小时）后过期  
        HttpMethod='DELETE'
    )
    print(f"Delete URL: {url}")
except (NoCredentialsError, PartialCredentialsError) as e:
    print(f"Credentials error: {e}")
except ClientError as e:
    print(f"Client error: {e}")

```
上面的python代码将输出一个预签名的URL，直接用 curl 和生成的签名连接删除对象：

这里的 -X DELETE 选项告诉 curl 使用 DELETE 方法发送请求。请确保将 URL 中的查询参数（从 ? 开始的部分）保留，因为它们包含了必要的签名信息，以验证请求的合法性。

curl -X DELETE "生成的预签名的URL"
