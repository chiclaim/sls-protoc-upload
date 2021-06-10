# sls-protoc-upload

Aliyun SLS SDK 中的 Java 版本 SDK 是给 Java 后端使用的，没有给鸿蒙系统的 SDK。于是基于 SLS 官方开发指南，使用 Java HttpUrlConnection 向 SLS 服务器上传日志。

## 数据编码方式

SLS 日志服务支持使用 Protocol Buffer 格式作为标准的日志写入格式。

Protocol Buffer格式用于结构化数据交换格式，当用户需要写入日志时，需要把原始日志数据序列化成如下格式的 Protocol Buffer 数据流，然后才能通过 API 写入服务端。

SlsLog.proto :

```
message Log
{
    required uint32 Time = 1;// UNIX Time Format
    message Content
    {
        required string Key = 1;
        required string Value = 2;
    }  
    repeated Content Contents = 2;
}

message LogTag
{
    required string Key = 1;
    required string Value = 2;
}

message LogGroup
{
    repeated Log Logs= 1;
    optional string Reserved = 2; // reserved fields
    optional string Topic = 3;
    optional string Source = 4;
    repeated LogTag LogTags = 6;
}

message LogGroupList
{
    repeated LogGroup logGroupList = 1;
}
```


## 安装 Protocol Buffer 环境

安装 Protocol Buffer 环境后，使用 protoc 命令编译上面的 SlsLog.proto 文件，或者直接使用仓库中的 SlsLog.java 这样就可以省去安装 Protocol Buffer 环境了。


## 请求签名

请求签名的生成是整个封装过程中最繁琐的地方，只要有一个环境出错都会导致签名不匹配的问题出现。而且文档有的地方说的也不是很明确，需要去试错。

签名的生成涉及到的算法工具类：

- [MD5](https://github.com/chiclaim/sls-protoc-upload/blob/main/src/main/java/sls/http/upload/MD5.java)
- [HmacSha1](https://github.com/chiclaim/sls-protoc-upload/blob/main/src/main/java/sls/http/upload/HmacSha1Signature.java)
- [ZLibUtils](https://github.com/chiclaim/sls-protoc-upload/blob/main/src/main/java/sls/http/upload/ZLibUtils.java)

更多详细的内容请查看代码。

## 最后

如果大家也有这方面的需求，可以直接基于本仓库进行修改，节省大量的时间。
