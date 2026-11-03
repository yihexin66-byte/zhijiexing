# 智解星

一款 Android 刷题应用，使用 Kotlin + Jetpack Compose 构建客户端，uniCloud 支付宝云云函数提供后端接口。

## 功能

- 题库浏览、分类筛选与试卷练习
- 答题进度实时保存，手动交卷、超时交卷
- 成绩结果、逐题解析与错题复盘
- 错题本、题目收藏、刷题记录、学习证书
- 知识点学习与阅读进度
- 成长统计：学习时长、连续打卡、正确率与考试成绩
- 登录会话持久化，支持昵称与头像自定义
- 防切屏：客户端上报切屏次数，服务端裁决，达到阈值自动交卷
- 四个主页面支持跟手横向滑动切换

## 技术栈

- Kotlin 2.2.10
- Jetpack Compose + Material 3
- Android Gradle Plugin 9.4.1，compileSdk 37，minSdk 24
- Compose HorizontalPager 实现主页面滑动
- uniCloud 支付宝云（Node.js 云函数 + 云数据库）

## 目录结构

```text
app/                           Android 客户端
  src/main/java/.../data/      网络、会话、本地存储、数据模型
  src/main/java/.../ui/        页面、组件与主题
unicloud/
  cloudfunctions/orbit-api/    考试后端云函数
  exam-seed.json               题库种子数据
  smoke_api.py                 接口冒烟脚本
gradle/                        Gradle 版本目录与 wrapper
```

## 本地构建

需要 JDK 21 与 Android SDK Platform 37。在 `local.properties` 中配置 SDK 路径，例如：

```properties
sdk.dir=D:/Android/Sdk
```

构建调试包：

```bash
./gradlew assembleDebug
```

Windows 下使用 `gradlew.bat assembleDebug`。产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 后端

`unicloud/cloudfunctions/orbit-api` 提供登录、试卷、答题、交卷、成绩与解析等接口，通过 uniCloud URL 化对外提供服务。云端地址在客户端的 `data/Store.kt` 中配置；可选的 AI 能力通过云函数环境变量 `COZE_API_TOKEN`、`COZE_BOT_ID` 注入，未配置时不影响刷题主流程。

题库种子数据见 `unicloud/exam-seed.json`，对应演示账号：

```text
用户名：student
密码：123456
```

## 说明

仓库仅包含源码与必要构建配置，不包含 APK、打包产物、验证截图和本地 IDE 配置。当前为调试构建，未配置正式签名，仅用于学习与作品展示。
