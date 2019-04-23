# 服务端人脸识别

## QUICK START

1. 去虹软下载SDK,并获取注册码
2. 按照说明配置环境

> demo文件下都是案例

## 说明

虹软2.0版本是由JAVA案例的，但是使用时间有限制（1年），且获取的人脸特征信息不能与1.2版本的人脸特征信息相匹配，本文件基于虹软SDK1.2版本开发的。

> 由于我们服务只运行于linux系统，所以我只做了linux的案例

## 环境准备

* ubunt16.04 X64
* 依赖
    * GLIBC2.19以上， `ldd --version` 查看
    * GCC4.8.2以上 `gcc -v` 查看版本, `apt-get install gcc-5` 安装
    * libsqlite3 `apt-get install libsqlite3-dev`
    * libcurl `apt-get install libcurl4-openssl-dev`
    * `jna-4.4.0.jar` 已放在 `/src/main/resources/lib` 下,将其放在 JDK的 `jre/lib/ext` 下,可选择其他位置,JVM能加载就行了
    
* SDK
    * libarcsoft_fsdk_face_recognition.so 放在 `/usr/lib` 下
    * libarcsoft_fsdk_face_detection.so 放在  `/usr/lib` 下