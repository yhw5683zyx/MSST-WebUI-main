## 简单上传
更新时间：2025/09/17 产品信息
功能说明
简单上传指通过 putObject 接口上传单个对象（Object），数据可以是二进制流或者本地文件。

### 接口定义
|接口|	说明|
|-|-|
|put_object(bucket_name: str, key: str, body: str, storageClass: str)|	根据桶的名称获取存储桶所在的地域|

### 接口约束
- 使用该接口上传的对象大小不能超过 5 GB。
- 在桶中若存在同名文件，将被新上传的文件覆盖。
- 文件名长度长度大于 0 且不超过 1023 字节。

### 请求参数说明
| 参数名称     | 参数类型 | 是否必选 | 参数解释                                                     |
| ------------ | -------- | -------- | ------------------------------------------------------------ |
| bucket_name  | str      | 必选     | 指定要上传对象的EOS存储桶的名称                              |
| Key          | str      | 必选     | 指定对象在EOS存储桶中的唯一标识符（即对象键）                |
| Body         | str      | 必选     | 要上传的对象的内容。可以是二进制流（如文件内容）或字符串（如文本内容） |
| StorageClass | str      | 必选     | 指定对象的存储类别。例如，'STANDARD'、'STANDARD_IA'、'GLACIER'等。默认值为'STANDARD' |

### 返回结果说明
|参数名称|参数类型|参数解释|
|-|-|-|
|ETag|str|对象的唯一标识符，用于验证对象的完整性|
|VersionId|str|如果启用了版本控制，则此字段包含对象的版本ID。否则，此字段不存在|

### 代码示例
#### 上传字符串：
```python
# -*- coding: utf-8 -*-
from boto3.session import Session

# Client 初始化
# 填写 EOS 账号的认证信息，或者子账号的认证信息
access_key = "your-access-key"
secret_key = "your-secret-access-key"
# 填写 url。例如：'https://IP:PORT' 或者 'https://eos-wuxi-1.cmecloud.cn'
url = "your-endpoint"
session = Session(access_key, secret_key)
s3_client = session.client('s3', endpoint_url=url)

# 上传字符串。例如 Bucket="example-bucket", Key="object.txt", Body="abcde", StorageClass='STANDARD'
# 如果指定 StorageClass='STANDARD_IA'，则上传的对象为低频类型
resp = s3_client.put_object(Bucket="your-bucket-name",
                            Key="you-object-name", Body="上传的字符串内容",
                            StorageClass='STANDARD')
   ```                         
                            


#### 上传 byte 数组：
```python
# -*- coding: utf-8 -*-
from boto3.session import Session

# Client 初始化
# 填写 EOS 账号的认证信息，或者子账号的认证信息
access_key = "your-access-key"
secret_key = "your-secret-access-key"
# 填写 url。例如：'https://IP:PORT' 或者 'https://eos-wuxi-1.cmecloud.cn'
url = "your-endpoint"
session = Session(access_key, secret_key)
s3_client = session.client('s3', endpoint_url=url)

# 上传 byte 数组。例如 Bucket="example-bucket", Key="object.txt", Body="{00010110, 01010010, 10111000}", StorageClass='STANDARD'
# 如果指定 StorageClass='STANDARD_IA'，则上传的对象为低频类型
resp = s3_client.put_object(Bucket="your-bucket-name",
Key="you-object-name", Body=b"上传的 bytes",
StorageClass='STANDARD')


```

#### 指定为unicode：
```python
# -*- coding: utf-8 -*-
from boto3.session import Session

# Client 初始化
# 填写 EOS 账号的认证信息，或者子账号的认证信息
access_key = "your-access-key"
secret_key = "your-secret-access-key"
# 填写 url。例如：'https://IP:PORT' 或者 'https://eos-wuxi-1.cmecloud.cn'
url = "your-endpoint"
session = Session(access_key, secret_key)
s3_client = session.client('s3', endpoint_url=url)

# 上传 unicode。例如 Bucket="example-bucket", Key="object.txt", Body="0x61", StorageClass='STANDARD'
# 如果指定 StorageClass='STANDARD_IA'，则上传的对象为低频类型
resp = s3_client.put_object(Bucket="your-bucket-name",
                            Key="you-object-name", Body=u"上传的 unicode",
                            StorageClass='STANDARD')

```