# ATOM「空集」Android Client

ATOM「空集」（Nilset） 是 ATOM 生态中的 Android 客户端，定位是面向日常协作和趣味工具的有机统一（）。

（还有很多很明显不是我写的，Mapher不直接对那些文字负责（））

客户端依赖 [atomreforge/daizy-night-server](https://github.com/atomreforge/daizy-night-server) 提供后端 API。该仓库是服务端项目；本仓库只包含 Android 客户端代码。

## 功能

### 当前能力

- 登录与注册 API 接入；注册提供与服务端规则一致的本地校验、确认密码和注册码输入，成功后返回登录页并预填用户名。
- 设置页顶部提供用户卡片，显示昵称和用户名；Logout 按钮默认灰色禁用，长按卡片后变为错误色可点击，点击其他位置会取消启用。头像当前仅保留 `avatar` 空字段。
- 基于 DataStore 的会话、主题、当前用户本地课表和课表查看偏好持久化，应用重启后可恢复状态。
- 类终端控制台页面，支持内部指令扩展。
- 控制台顶栏提供附属设置页入口；控制台设置页支持单独开关控制台背景和调整输出字号，返回层级与控制台保持一致。
- 控制台指令候选补齐、进程内历史保留和会话级文件日志。
- 控制台候选补全支持指令名和参数段：输入空格后展示当前下一段可选字段，选择候选只替换光标所在段并自动补空格。
- 控制台 `/config` 支持查看和持久化覆盖服务器地址；输入 `/` 展示全部可见指令并按名称排序，`/config get` 输出包含默认值，输出区在已处于底部时自动跟随新输出和键盘展开。
- 登录后的主页与设置页底边栏导航；主页提供侧边栏功能入口。
- 设置页提供“通知”入口，附属通知设置页当前为占位。
- 设置页提供“自定义”入口，附属自定义设置页当前为占位；主页使用内置 End Poem Markdown 作为临时文本占位。
- Markdown 读写仓库支持应用私有 Markdown 文件和 SAF 文档 URI 的文本读取、写入；后续随心记编辑器将复用该链路。
- 设置页右上角显示服务端连接状态；点击会提示当前状态，应用启动自动探测一次，失败后冷却结束前禁用实际重试、冷却结束后可手动重试。
- 主页侧边栏提供独立日历月历视图，支持按月切换和今天高亮；当前不与课表数据关联。
- 主页侧边栏最后一项提供 BiliNil 工具，顶部提供封面下载和视频下载选项卡切换：封面下载支持 av、BV、直播间和 `b23.tv` 输入解析，展示并保存封面到 `Downloads/Nilset/Cover`。
- 视频下载解析 DASH 流，仅展示当前视频支持的清晰度；请求优先 AVC/HEVC，目标画质只有 AV1 时会自动尝试更低画质，全部只有 AV1 时保持音视频分离并说明原因。下载支持进度、暂停/继续、取消和 1-4 并发任务设置，导出到 `Downloads/Nilset/Video`，文件名为 `标题{BV号}[画质].mp4`。
- BiliNil 设置页提供 B站登录入口和附属 WebView 登录页；登录态分为未登录、普通用户和大会员，并显示昵称、头像和 `mid`。Cookie 存在应用内加密存储中，仅发送给 `bilibili.com` 及其子域，不会发送到 B站媒体 CDN，也不会写入日志、异常信息或 UI 状态。WebView UI 需开启 JavaScript、DOM Storage 和第三方 Cookie，并建议从 UA 中移除 `wv` 标记；登出会清理库内凭据和 B站相关 WebView Cookie。
- 课表共建页当前用户的课表以本地 DataStore 为准，创建、编辑和删除先写本地；断开服务端时可离线修改，恢复连接后会对比本地与远端并以本地数据覆盖远端。老师、课室和备注通过私有 `roaming` 云同步，public 课表不会返回这些字段。本地从未初始化时优先采用远端课表，便于重装恢复；本地已初始化的空课表仍会覆盖远端。支持创建和编辑课程（标题、星期、分段时间、老师、课室和备注）、长按课程删除、问候、下一节课提示、星期筛选、课程列表和下拉刷新。查看他人课表时隐藏课程编辑入口并禁用长按操作。成员列表目前仍只显示当前登录用户；服务端已提供公共单用户课表读取，多人聚合和成员列表仍待后续 API。
- 内部指令带有 debug 门控，避免调试能力进入 release 行为。
- 单 Activity + Navigation Compose 的页面组织。
- Material 3 主题、自定义字体和可扩展的主题配置。
- 主题模式提供浅色和深色；默认使用枫糖，另含落樱、青碧、汀蓝、动态取色和自定义，内置配色均含浅色与深色版本，自定义配色结果通过 DataStore 恢复。
- 动态取色主题在 Android 12+ 按当前浅色/深色模式跟随系统配色。
- 主题页支持通过系统照片选择器选择图片并在应用内按应用比例裁剪后设为全局背景；背景图默认 100% 透明度，可调整为 0%-100%。卡片、顶栏和底部导航使用基于当前背景色派生的透明度遮罩，卡片遮罩可通过 0%-100% 滑块调整；侧边栏以当前背景色半透明遮罩覆盖共享全局背景与主页内容。主题配色主要影响图标与文字。文本缩放和 UI 缩放可通过 80%-120% 滑块调整，设置均通过 DataStore 恢复。
- 主题页支持浏览并选择 TTF/OTF 字体文件作为全局字体；文件会复制到应用私有目录并校验，卡片下方显示当前字体。清除自定义字体和清除自定义背景均需弹窗确认。
- 控制台设置可单独开关控制台页面的自定义背景；关闭后控制台与控制台设置页都会隐藏背景图，其他页面不受影响，背景显示切换使用淡入淡出过渡。
- 控制台设置可调整输出字号（10-24sp），仅影响控制台输出文本；滑块支持回正和重置。
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
| 构建 | Gradle Version Catalog、AGP 9、KSP；主工程 `:app`，BiliNil 能力在 `:lib-bilidownload` |

## 目录概览

```text
app/src/main/java/net/atomreforge/nilset/
├─ core/          # 纯 Kotlin 的指令与主题模型、命令注册中心
│  ├─ logging/    # Logcat、控制台和文件日志
│  └─ theme/      # 主题预设、颜色字段和 HEX 解析
├─ const/         # 跨层路由、API、存储键和配置文件表述
├─ data
│  ├─ config/     # YAML 配置模型与加载器
│  ├─ remote/     # Retrofit API、DTO、AuthInterceptor、TokenAuthenticator
│  ├─ repository/ # 会话、主题、控制台历史、课表、配置与 Markdown 仓库
│  └─ session/    # DataStore 会话数据源
├─ di/            # Hilt 模块
└─ ui/            # 登录、控制台、日历、课表、主页/设置导航、主题
lib-bilidownload/src/main/java/net/atomreforge/nilset/bili/
├─ api/       # B站 HTTP/WBI API 与域名隔离 CookieJar
├─ auth/      # WebView Cookie 导入、登录态校验与登出
├─ download/  # 分段下载、任务状态、文件名与导出前处理
├─ model/     # 视频模型与清晰度/编码选择
├─ mux/       # MediaMuxer 音视频合并
└─ store/     # MediaStore 导出
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

Release 签名、本地 keystore 生成和 GitHub Pre-release 发布流程见 [RELEASING.md](RELEASING.md)。

## 后端联调

客户端默认连接 Daizy Night 服务端。模拟器访问宿主机上的本机后端时，`baseUrl` 使用：

```yaml
api:
  baseUrl: http://10.0.2.2:4703
apiPrefix: /api/v1
```

控制台 `/config set host_addr <address>` 会在运行时覆盖所有 API 请求的 host 和端口，地址支持 IP、域名和可选协议/端口，并持久化到 `nilset_config`；`/config clear host_addr` 恢复默认 `syewiki.top:4703`。Retrofit 仍使用 YAML 中的 `baseUrl` 作为启动占位，实际请求由动态 BaseUrl 拦截器重写。

Android 默认禁止 release 包访问明文 HTTP。服务端联调应优先启用 HTTPS，并使用：

```text
/config set host_addr https://<server-domain>:<port>
```

本地例外仅覆盖模拟器宿主机 `10.0.2.2` 和本机回环 `127.0.0.1`。真机通过 USB 联调时，可先执行 `adb reverse tcp:4703 tcp:4703`，再将服务器地址设为 `127.0.0.1:4703`。不要在 release 构建中全局开放明文 HTTP。

当前客户端调用的接口包括：

- `POST /api/v1/register`
- `POST /api/v1/login`
- `POST /api/v1/refresh-access-token`
- `GET /api/v1/user/{username}/info`
- `GET /api/v1/public/user/{username}/info`
- `GET /api/v1/public/user/{username}/calendar`
- `GET /api/v1/user/{username}/calendar`
- `PUT /api/v1/user/{username}/calendar`
- `DELETE /api/v1/user/{username}/calendar`
- `GET /api/v1/public/health/db`
- `POST /api/v1/user/signout`

访问令牌 401 后会按 `auth.autoRefresh` 使用 refresh token 自动换发；当前默认启用。启动健康检查收到 401 时表示服务端可达但当前会话未认证，不会误报为离线。

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
| `/config` | 查看、设置、恢复配置；当前支持 `host_addr` | 所有构建 |
| `/no:login` | 跳过登录进入特殊模式 | 仅 debug |
| `/clear:data` | 清除本地会话数据 | 仅 debug |
| `/cls` | 清空控制台历史 | 所有构建 |

## 项目状态

- 当前基线为 v0.3.0-pre.1。
- Phase 0-6 已完成：架构分层、ViewModel、会话持久化、指令系统、Compose、Hilt、令牌自动刷新、文件日志、CI 和核心会话测试。
- v0.2.0 对齐服务端 v0.7.1/v0.7.2：补齐健康检查 401 状态、公共用户信息、私有课表 GET、私有课程信息 `roaming` 同步、重装后远端恢复和离线覆盖语义。
- v0.2.1 新增 BiliNil 模块：封面下载支持 av、BV、直播间和 `b23.tv` 输入解析，顶部提供封面下载/视频下载选项卡切换。
- v0.2.2 完善 BiliNil：补齐视频下载管线、B站 WebView Cookie 登录、清晰度选择和 Windows 兼容导出文件名。
- v0.3.0-pre.1 建立发布基线：支持本地/CI release 签名配置和 GitHub Pre-release 产物。
- Phase 7 计划完善发布工程化，包括 R8、签名、崩溃上报和 baseline profile。

架构设计与阶段规划见 [ARCHITECTURE.md](ARCHITECTURE.md)。
