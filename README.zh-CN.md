[English](README.md) | **中文**

[主页：Etai 应用集 — 轻记账](https://etais.dev/#qingjizhang)

# 轻记账

本地优先的个人记账 Android 应用。数据保存在手机上的 SQLite（Room）里，无需登录、无需服务器。

- **GitHub**：[gnatecheng/easy-ledger](https://github.com/gnatecheng/easy-ledger)（原 `qingjizhang` 仓库已重命名，旧 URL 会跳转）
- **Releases**：[下载 APK](https://github.com/gnatecheng/easy-ledger/releases)
- **应用名**：轻记账（英文界面：**Easy Ledger**）
- **包名**：`com.qingjizhang.app`
- **版本**：1.3.2
- **最低系统**：Android 8.0（API 26）
- **技术**：Kotlin、Jetpack Compose、Material 3、Room、Navigation Compose

## 功能

- **流水**：金额、日期时间、收支、分类、账户、备注、标签；快速记一笔；按月切换；备注/标签全文搜索，可按分类、账户、标签、金额区间、收支/转账类型筛选，一键清除筛选。
- **转账**：账户之间互转（转出/转入、金额、日期、备注）；两端余额同步更新；不计入月度收入/支出；明细中以独立样式展示。
- **分类**：支出 / 收入两套，可编辑名称与颜色；首次启动带中文默认分类（餐饮、交通、购物、住房、工资、理财…）。
- **账户**：现金、银行卡、信用卡、支付宝、微信等，余额随流水更新。
- **导入导出**
  - 导出 CSV（表格友好）和 JSON（完整备份：流水、分类、账户、预算、提醒设置）。
  - 导入时预览、识别常见列名（日期、金额、分类、备注等），并处理重复：跳过重复 / 全部导入 / 覆盖。
  - 可用系统分享或「保存文件」完成一键备份 / 恢复。
- **统计**：月度收支与结余（对比上月）、分类饼图、近 12 个月趋势；可按时间、账户、标签筛选；可分享月报图片或导出 PDF。
- **预算与提醒**：每月总预算 + 分类预算；达到 80% 警告、超过 100% 超支；可配置大额单笔阈值；多日未记账会在首页轻提醒。
- **外观与语言**：跟随系统 / 浅色 / 深色主题；中文与 English 界面切换（我的 → 设置）。
- **关于**：版本号、构建时间、GitHub 开源仓库链接（我的 → 关于）。
- **周期记账**：每天 / 每周 / 每月规则；打开应用时自动生成到期流水；可编辑、暂停、删除。
- **桌面小组件**：本月收入、支出、结余（绿/红），点按打开应用，记账后自动刷新。
- **收据拍照**：记账时可拍照或从相册选图，照片只保存在本机，支持缩略图、更换和删除。
- **演示数据**：第一次打开会写入示例流水，可在「我的 → 提醒设置」中清除或恢复。

## 更新记录

### 1.3.2

- 修复应用内切换 English/中文无效的问题（AppCompat 语言、系统「应用语言」同步）。

### 1.3.1

- 英文底部导航与统计图例排版优化（标签单行、分类名可读）。
- 深色模式下首页预算/大额提醒卡片使用适配配色。

### 1.3.0

- 完整英文界面（`values-en` 字符串资源，设置中可切换语言）。
- 主题：跟随系统、浅色、深色（替代单一深色开关）。
- 「关于」页：版本、构建时间与项目链接。

### 1.2.0

- 明细搜索与筛选、账户转账、深色模式等（见历史提交）。

## 在本地编译

需要 JDK 17+ 与 Android SDK（compileSdk 35）。

```bash
# 配置 SDK 路径
echo "sdk.dir=/path/to/Android/sdk" > local.properties

# 调试包
./gradlew :app:assembleDebug

# 可安装的签名 Release 包（仓库内含演示用 keystore）
./gradlew :app:assembleRelease
```

产物位置：

- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release.apk`

演示签名仅用于自行安装，**不能**用于上架。密码见 `app/build.gradle.kts` 中的 `signingConfigs.release`。

推送 `v*` 标签时，[Release APK 工作流](https://github.com/gnatecheng/easy-ledger/blob/main/.github/workflows/release-apk.yml) 会发布调试包，附件文件名为 `easy-ledger-{versionName}-{tag}.apk`（例如 `easy-ledger-1.3.2-v1.3.2.apk`）。

## 相关开源项目

同一作者维护的其它仓库（GitHub 重命名后请用新路径）：

- [gnatecheng/c-week](https://github.com/gnatecheng/c-week)（原 `C-week`）
- [gnatecheng/group-matters](https://github.com/gnatecheng/group-matters)（原 `class-activity-record`）

## 安装到手机

1. 把 `app-release.apk` 拷到手机。
2. 打开系统设置，允许当前文件管理器 / 浏览器「安装未知来源应用」。
3. 点开 APK 安装。若系统提示「未发现威胁」或「Play 保护机制」，选择仍要安装即可（这是自签证书，不是商店包）。

## 导入导出说明

### CSV 列

推荐表头（中英均可）：

`日期,时间,类型,金额,分类,账户,备注,标签,转入账户`

- 类型：`支出` / `收入` / `转账`（也识别 expense / income / transfer）
- 金额：`35.50`，不要带货币符号；表格可直接求和
- 日期：`2026-09-01`、`2026/09/01`、`2026年9月1日` 等

导入时若分类或账户不存在，会自动创建。

### JSON 备份

由本应用「保存 JSON 备份」生成，包含全部账本。恢复时建议先预览：

- **导入并跳过重复**：按「日期 + 金额 + 类型 + 备注」去重
- **全部导入**：允许重复流水
- **覆盖现有数据**：先清空再写入备份（请确认已另存一份）

## 项目结构

```
app/src/main/java/com/qingjizhang/app/
  data/      Room 实体、DAO、仓库、备份导入导出、种子数据
  domain/    业务模型与金额/日期工具
  ui/        Compose 界面（首页 / 明细 / 统计 / 预算 / 我的）
```

## 隐私

没有账号系统，没有网络同步。备份文件只有你分享或保存时才会离开本机。
