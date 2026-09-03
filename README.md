# ATOM「空集」Android Client

ATOM「空集」（Nilset） 是 ATOM 生态中的 Android 客户端，定位是面向日常协作和趣味工具的有机统一（）。

（还有很多很明显不是我写的，Mapher不直接对那些文字负责（））

客户端依赖 [atomreforge/daizy-night-server](https://github.com/atomreforge/daizy-night-server) 提供后端 API。该仓库是服务端项目；本仓库只包含 Android 客户端代码。

## 功能

### 当前能力

- 登录与注册 API 接入。
- 基于 DataStore 的会话持久化，应用重启后可恢复会话。
- 类终端控制台页面，支持内部指令扩展。
- 控制台指令候选补齐、进程内历史保留和会话级文件日志。
- 登录后的主页与设置页底边栏导航；主页提供侧边栏功能入口。
- 主页侧边栏提供独立日历月历视图，支持按月切换和今天高亮；当前不与课表数据关联。
- 个人课表数据层已接入服务端 GET / PUT / DELETE 接口；多人共享与具体课表 UI 暂未实现。
- 内部指令带有 debug 门控，避免调试能力进入 release 行为。
- 单 Activity + Navigation Compose 的页面组织。
- Material 3 主题、自定义字体和可扩展的主题配置。
- 主题模式提供浅色和深色；默认使用枫糖，另含落樱、青碧、汀蓝、动态取色和自定义，内置配色均含浅色与深色版本，自定义配色结果通过 DataStore 恢复。
- 动态取色主题在 Android 12+ 按当前浅色/深色模式跟随系统配色。
- 主题页支持通过系统照片选择器选择图片并在应用内按应用比例裁剪后设为全局背景；背景图默认 100% 透明度，可调整为 0%-100%。卡片、顶栏和底部导航使用基于当前背景色派生的透明度遮罩；侧边栏以当前背景色半透明遮罩覆盖共享全局背景与主页内容。主题配色主要影响图标与文字。文本缩放和 UI 缩放可通过 80%-120% 滑块调整，设置均通过 DataStore 恢复。
- 强类型 YAML 配置加载，配置错误时快速失败。

### 规划方向

- 课表共享：导入或维护个人课表，聚合展示多人课程安排。（不然总是找人找不到（））
- 空闲时间协调：根据多人课表计算共同空闲时段，减少约时间时的来回沟通。（终于可以很好地安排什么时候开黑了吗（））
- 趣味工具：随机抽签或其他轻量互动功能。（之前规划了一堆，但是我现在忘了（））
- 操作入口：随着功能增多，提供统一的模块入口、权限控制和个性化入口配置。

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose、Material 3 |
| 架构 | 单 Activity、UDF、ViewModel + StateFlow |
| 依赖注入 | Hilt |
| 网络 | Retrofit、OkHttp、kotlinx.serialization |
| 持久化 | DataStore Preferences |
| 构建 | Gradle Version Catalog、AGP 9、KSP |

## 目录概览

```text
app/src/main/java/net/atomreforge/nilset/
├─ core/          # 纯 Kotlin 的指令与主题模型、命令注册中心
│  ├─ logging/    # Logcat、控制台和文件日志
│  └─ theme/      # 主题预设、颜色字段和 HEX 解析
├─ const/         # 跨层路由、API、存储键和配置文件表述
├─ data/
│  ├─ config/     # YAML 配置模型与加载器
│  ├─ remote/     # Retrofit API、DTO、AuthInterceptor、TokenAuthenticator
│  ├─ repository/ # 会话、主题、控制台历史与课表仓库
│  └─ session/    # DataStore 会话数据源
├─ di/            # Hilt 模块
└─ ui/            # 登录、控制台、主页/设置导航、主题
```

## 环境要求

- Android Studio 或包含 AGP 9 支持的 Android 构建环境。
- Android SDK 37。
- 最低支持 Android 10（API 29）。
- Gradle Wrapper 会自动下载 Gradle 9.5.0。

## 构建与测试

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

也可以在 Android Studio 中直接运行 `app` 到模拟器或真机。

## 后端联调

客户端默认连接 Daizy Night 服务端。模拟器访问宿主机上的本机后端时，`baseUrl` 使用：

```yaml
api:
  baseUrl: http://10.0.2.2:4703
  apiPrefix: /api/v1
```

当前客户端调用的接口包括：

- `POST /api/v1/register`
- `POST /api/v1/login`
- `POST /api/v1/refresh-access-token`
- `GET /api/v1/user/{username}/me`
- `GET /api/v1/user/{username}/calendar`
- `PUT /api/v1/user/{username}/calendar`
- `DELETE /api/v1/user/{username}/calendar`
- `POST /api/v1/user/signout`

访问令牌 401 后会按 `auth.autoRefresh` 使用 refresh token 自动换发；当前默认启用。

## 配置

配置使用 YAML，加载顺序如下：

1. 如果 `assets/config.test.yaml` 存在，优先加载它，便于临时测试覆盖。
2. 否则加载 `assets/config.yaml`。
3. debug sourceSet 中的 `app/src/debug/assets/config.yaml` 会覆盖 main sourceSet 中的同名文件。
4. 缺失必填项、YAML 结构错误或校验失败会直接抛异常，不带病启动。

| 文件 | 用途 | 是否入库 |
| --- | --- | --- |
| `app/src/main/assets/config.example.yaml` | 配置模板 | 是 |
| `app/src/main/assets/config.yaml` | 默认基线配置 | 是 |
| `app/src/debug/assets/config.yaml` | 本机联调配置 | 否 |
| `app/src/main/assets/config.test.yaml` | 临时测试覆盖配置 | 按需，默认不应提交 |
| `app/src/debug/assets/test-account.yaml` | 本地测试账号，支持本地登录 | 否 |

主要配置字段：

```yaml
main:
  isDebugMode: true

api:
  baseUrl: http://10.0.2.2:4703
  apiPrefix: /api/v1
  timeouts:
    connect: 10s
    read: 15s

auth:
  autoRefresh: false

log:
  isHttpLoggingEnabled: true

theme:
  materialYou: false
```

本地网络联调使用明文 HTTP 时，请保持后端只部署在可信开发环境。生产环境应使用 HTTPS；客户端默认拒绝未显式放行域名的明文流量。

## 内部指令

控制台输入以 `/` 开头的内部指令可以触发调试或维护动作。输入 `/` 后会显示按字母排序的候选指令，继续输入可以继续过滤。控制台历史会在返回登录页后保留；需要清空当前输出时使用 `/cls`。常见指令包括：

| 指令 | 说明 | 可用范围 |
| --- | --- | --- |
| `/status` | 查看当前会话状态 | 所有构建 |
| `/no:login` | 跳过登录进入特殊模式 | 仅 debug |
| `/clear:data` | 清除本地会话数据 | 仅 debug |
| `/cls` | 清空控制台历史 | 所有构建 |

## 项目状态

- Phase 0-6 已完成：架构分层、ViewModel、会话持久化、指令系统、Compose、Hilt、令牌自动刷新、文件日志、CI 和核心会话测试。
- Phase 7 计划完善发布工程化，包括 R8、签名、崩溃上报和 baseline profile。

架构设计与阶段规划见 [ARCHITECTURE.md](ARCHITECTURE.md)。
