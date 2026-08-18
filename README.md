# 时笺 ShiJianApp

一款本地优先的 Android 生活记录应用：倒计时、记账开销、美食推荐、AI 资讯与个人数据管理。视觉与交互依据《时笺 12 个设计稿》实现，功能依据《时笺-产品需求文档PRD.md》实现。

- 版本：1.1.0（versionCode 110）
- 语言：Kotlin + Jetpack Compose
- 最低系统：Android 8.0（minSdk 26）

## 功能总览

底部 5 Tab：

| Tab | 功能 |
| --- | --- |
| 首页 | 目标/纪念日倒计时、月度收支统计、打卡日历、日历明细、支出构成 |
| 美食 | 随机推荐、分类浏览、搜索（高德 POI）、收藏/拉黑、位置选点 |
| 开销 | 收支列表（按类型/待报销筛选）、记账面板（分类/备注/时间/日期/报销标记）、日历明细 |
| 新闻 | AI 资讯列表、关键词与分类、内容长度控制、定时推送开关 |
| 我的 | 用户卡（可改昵称）、统计（记账天数/记录总数/连续打卡）、外观与习惯、美食设置、新闻设置、数据备份/导入/清空、更新公告、隐私说明 |

子页面：记账（新增/编辑）、日历明细、新闻设置、地址管理、美食设置、收藏/黑名单列表、数据统计（月度/年度图表）、更新公告、隐私政策。

## 技术栈

- UI：Jetpack Compose（BOM 2024.02.01，编译器 1.5.8）+ Material 3
- 数据：Room 2.6.1（kapt）+ SQLCipher 4.5.4 全库加密
- 安全：Android Keystore + EncryptedSharedPreferences（API Key / 口令）
- 后台：WorkManager 2.9.0（新闻定时生成）
- 网络：Retrofit 2.9.0 + OkHttp 4.12.0 + kotlinx.serialization（高德 Web API / DeepSeek API）
- 构建：AGP 8.1.2 / Gradle 8.2.1 / JDK 17 / compileSdk 34 / targetSdk 34

## 构建与运行

1. 使用 Android Studio（Hedgehog 及以后版本）打开本目录（含 `settings.gradle.kts` 的根目录）。
2. 确认本机 JDK 17（Settings → Build Tools → Gradle → Gradle JDK）。
3. 等待 Gradle Sync 完成（首次会下载依赖）。
4. 连接 Android 8.0+ 真机或创建模拟器，Run `app`。

> 项目不依赖任何远端私有仓库，断网后除「美食搜索 / AI 资讯」外全部功能可正常使用。

## 首次使用配置（可选）

- 美食搜索：需要高德 Web 服务 Key。进入「我的 → 美食设置 → 高德 Key」填写（使用 AES 加密存入 Keystore 保护的 EncryptedSharedPreferences，不落明文）。
- AI 资讯：需要 DeepSeek API Key。进入「我的 → 新闻设置」填写（同样加密存储）。
- 定位权限：美食按位置搜索会请求定位权限；未授权时可使用「地址管理」中的常用地址作为搜索中心。

## 数据与安全

- 全部本地数据（账单、分类、搜索地址、美食收藏/黑名单、新闻设置）存于 SQLCipher 加密数据库，口令由 Keystore 随机生成并保护，仅在内存中使用。
- 备份导出：JSON（可选 AES-256-GCM 加密），可保存到任意位置或复制为 Base64 文本；导入时自动识别加密并解密覆盖式恢复。
- 隐私：应用不采集、不上传任何个人信息；唯一网络请求是高德/DeepSeek 的按需 API 调用，Key 仅存在于本机。

## 目录结构（要点）

```
app/src/main/java/com/shijian/app
├── api/            Retrofit 接口与工厂（AmapApi / DeepSeekApi / ApiClient）
├── data/
│   ├── db/         Room 数据库、DAO、实体（账单/分类/地址/POI/新闻）
│   ├── prefs/      SettingsRepository（设置项）、SecurePrefs（加密存储）
│   ├── repo/       Transaction/Food/News/Address/Backup 仓库
│   └── UpdatesData 更新公告数据
├── ui/
│   ├── components/ SjCard/ListRow/SwitchRow/TopBars 等通用组件
│   ├── navigation/ Routes / AppNavHost / SjBottomBar
│   ├── screens/    home / expense / food / news / profile 五个页面模块
│   └── theme/      颜色、形状、字体（含深色模式）
├── util/           日期/金额格式化、加密、定位、通知、新闻调度
├── worker/         NewsWorker（定时生成新闻）
├── AppContainer.kt 依赖注入容器
└── MainActivity.kt 入口（读深色模式设置并启动）
```

## 已知说明

- 深色模式跟随「我的 → 深色模式」设置，重启后持久化。
- 图片与金额数字使用等宽数字特性，保证列表对齐。
- 无内置测试用例；如需新增，参考 AndroidX Test 目录结构补充。
