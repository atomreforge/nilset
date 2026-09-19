# ATOM「空集」客户端架构

> 定位：ATOM 生态中的 Android 操作入口，后续会从账号与控制台扩展到课表协作、随机抽签等日常工具。本文档描述当前已落地架构、明确边界和下一阶段方向。

## 1. 当前架构总览

- 单 `:app` Gradle 模块，按 `core / data / di / ui` 分包。
- UI 使用 Kotlin + Jetpack Compose + Material 3，不使用 XML 界面。
- 应用是单 Activity 架构，`MainActivity` 承载 `NavHost`。
- DI 使用 Hilt，依赖入口统一在 `SingletonComponent`。
- 数据层使用 Repository 接口、Retrofit/OkHttp 和 DataStore。
- 指令系统使用命令模式与注册表，控制台指令统一使用 `/` 前缀。
- 主题由 `ThemeRepository` 提供 `StateFlow<UserThemeSettings>`，启动时从 DataStore 恢复；默认使用枫糖，另含落樱、青碧、汀蓝、动态取色和自定义主题。卡片与导航容器使用当前背景色派生的半透明遮罩，遮罩透明度由主题设置持久化；主题配色主要影响图标与文字。裁剪后的全局背景图按应用比例生成，透明度默认 100%；主页侧边栏使用当前背景色半透明遮罩，覆盖共享全局背景与下层主页内容。
- 自定义字体从系统文件选择器进入后复制到 `files/font`，先按 Android `Typeface` 校验再持久化路径和原始文件名；`ATOMTheme` 会基于该文件重建 Typography，恢复默认时删除文件并清空设置。
- 控制台背景开关保存在主题设置中；全局背景层在 `console` 和 `console/settings` 路由按该开关隐藏，其他页面仍使用全局背景。背景图切换使用淡入淡出过渡。
- 独立 `theme_settings` 页面将主题模式与主题配色分离；模式提供浅色和深色，动态取色是随当前模式区分浅色/深色的主题。
- 配色模型保留 `primary`、`secondary`、`background` 和 `surface` 四个来源色；设置页自定义只编辑 primary、secondary 和 surface，background 由预设与模式内部维护。`ATOMTheme` 依据所选模式派生 Material 3 `ColorScheme`，字体仍由 `AppThemeConfig` 提供。
- `ATOMTheme` 通过 `LocalDensity` 应用可选的文本缩放与 UI 缩放，两者均支持 80%-120% 并由主题仓库持久化。

当前产品目标不是只有登录和控制台；账号体系与控制台只是后续功能模块的公共入口和调试基础。

## 2. 分层与数据流

```text
UI 层
  LoginScreen / ConsoleScreen / HomeScreen / CalendarScreen / ScheduleScreen / SettingsScreen
  只展示 UiState、转发用户事件
        ↓
Presentation 状态层
  LoginViewModel / ConsoleViewModel
  StateFlow + UiState + viewModelScope
        ↓
Data 层
  SessionRepository、CalendarRepository、ScheduleViewRepository、ConsoleHistoryRepository、ConfigLoader
  决定访问 Remote API 还是本地数据源
        ↓
数据源
  DaizyNightApi（Retrofit）
  SessionDataStore（DataStore Preferences）
```

### UI / Presentation 层

- `MainActivity` 使用 `ComponentActivity`、`enableEdgeToEdge`、`ATOMTheme` 和 `NavHost`。
- 登录页和控制台页分别是 `LoginScreen`、`ConsoleScreen`。
- 注册页是登录页的附属路由；注册成功后返回登录页并预填用户名。
- 设置页首项是 `SettingsUserCard`；长按弹出退出按钮，退出完成后由 `MainActivity` 清除主页返回栈并回到登录页。
- 页面通过 `hiltViewModel()` 获取 ViewModel。
- ViewModel 持有 StateFlow 驱动的不可变 UiState，UI 不直接访问 Repository。
- 控制台输入框使用 Material 3 `ExposedDropdownMenuBox` 提供指令名和参数段候选；指令名按字母序过滤。
- 登录反馈使用 Material 3 `SnackbarHost`；登录成功后清除登录页返回栈并进入主页。
- 主页使用 Material 3 `ModalNavigationDrawer` 提供侧边栏；日历月历、课表共建和 BiliNil 封面工具已接入（含封面/视频选项卡切换），随心记和待办事项暂作占位栏展示。
- 主页与设置页共用 Material 3 `NavigationBar`；设置是独立顶层路由，不作为侧边栏项。
- 主页和设置页在主路由内并排布局，通过 `graphicsLayer` 平移共享同一版面；切换动画可被新的导航目标立即接管。

### Data 层

- `SessionRepository` 是会话业务接口，`RemoteSessionRepository` 是当前实现。
- `SessionDataStore` 只负责会话的读写和清除，不包含登录业务规则。
- 应用启动会先等待 DataStore 会话恢复，再根据 `SessionState` 决定初始进入登录页还是主页。
- `DaizyNightApi` 定义注册、登录、刷新访问令牌、获取私有和公开用户信息、读取私有/公共课表以及写入/删除个人课表接口。
- `AuthInterceptor` 从会话状态读取 access token，并统一添加 `Authorization: Bearer` 头。
- 用户信息接口使用 `/api/v1/user/{username}/info`；路径用户名来自持久化会话，仅用于服务端属主校验。
- access token 返回 401 时按配置自动刷新；refresh token 采用一次性轮换语义，成功后整体覆盖 access/refresh token 对。
- 登出会先保证本地持有可用 token 对，再把当前 refresh token 提交给服务端吊销，最后清空本地会话。
- `ConsoleHistoryRepository` 是进程内单例，保存控制台输出，避免页面返回后历史丢失。
- 应用日志通过 `AppLogger` 同时写入 Logcat、控制台历史和磁盘日志；控制台只保留最近 500 条，磁盘文件不主动清理。
- 每次 App 进程启动会在 `files/logs` 创建 `session-<序号>-<时间>.log`，序号按已有日志文件最大序号递增。
- HTTP 日志由 OkHttp 拦截器接入并按状态分级着色。
- `ConfigLoader.mustLoad()` 加载强类型 YAML 配置，解析或校验失败会快速失败。
- `CalendarRepository` 分成本地与远端数据源：当前登录用户的课表在远端 PUT 成功后由 `PreferencesLocalCalendarRepository` 在 DataStore 中按用户缓存，缺失时返回空表；远端仓库用于课表同步和他人课表数据源。`ScheduleViewModel` 负责选择数据源、创建/编辑/删除当前用户课程并触发全量 PUT、按星期筛选、排序和计算下一节课。
- 课表共建页的成员列表目前是只含登录用户的临时占位；服务端已提供公共单用户课表读取，成员列表和多人聚合仍待后续 API。

### Core 层

- `core.command` 不依赖 Android UI。
- `NilSetCommand` 定义指令契约，每个具体指令一个类。
- `CommandRegistry` 负责收集和过滤指令。
- `NilSetCommandCenter` 负责解析 `/` 前缀、分发 `/help` 和具体指令。
- `CommandResult` 用结构化结果区分成功与失败。

## 3. 关键机制

### 配置

- 模板：`app/src/main/assets/config.example.yaml`。
- 默认配置：`app/src/main/assets/config.yaml`。
- 本地联调配置：`app/src/debug/assets/config.yaml`，不入库。
- 临时测试配置：`ConfigLoader` 会优先探测 `config.test.yaml`。
- Retrofit baseUrl、OkHttp 连接/读取超时、HTTP 日志开关均来自 `AppConfig`。

### 会话

- 登录成功后保存 access token、refresh token 和用户信息。
- 登录接口换取 token 后立即请求当前用户信息；信息请求失败时不保留半完整会话。
- 应用进程内通过 `SessionState` 暴露状态，磁盘上通过 DataStore 恢复。
- `AuthInterceptor` 和 `TokenAuthenticator` 通过 `dagger.Lazy` 打破 OkHttp、Retrofit 与会话仓库之间的构建期循环依赖。
- `auth.autoRefresh` 控制是否注册 401 自动刷新器；当前默认与 debug 联调配置均已启用。

### 服务连接

- `ServerConnectionManager` 通过 `GET /api/v1/public/health/db` 判断服务端连接状态；`v0.7.1` 起该接口需要认证，401 表示服务端可达但会话未认证。未登录时不触发课表同步；已有本地登录会话时，该状态仍可执行本地优先核对。
- 应用启动时自动探测一次；启动链路只做一次探测，不进行自动轮询或自动重试。
- 设置页按钮始终可点击并反馈当前状态；只有失败且冷却结束时才发起手动重试。每次检查开始后进入 10 秒冷却，冷却结束前禁用实际重试。

### 课表

- 当前用户的本地课表以 JSON 片段保存在 `nilset_schedule` DataStore；本地记录不存在时显示空课表，并用 `isInitialized=false` 区分“从未初始化”和“刻意清空”。
- 当前用户课表的新增、编辑和删除先写本地 DataStore；离线时不会触发远端请求，恢复连接后对比本地与远端并以本地数据覆盖远端。
- 自己的远端核对使用认证接口 `GET /api/v1/user/{username}/calendar`；查看他人继续使用认证公共只读接口 `GET /api/v1/public/user/{username}/calendar`。
- 私有 `roaming.annotation` 映射到课程备注；`roaming.description` 由客户端写成结构化 JSON，映射到老师和课室。public 课表读取固定忽略 `roaming`，避免私人信息外泄。
- 客户端数据模型使用 `weekday`、`startMin`、`endMin`、`title`、`teacher`、`classroom` 和 `note`，展示层负责把分钟转换为 `HH:mm`；当前用户本地课表会固定写出老师、课室和备注字段，PUT 会固定写出课表级与课程级 `roaming`。
- 服务端已提供他人课表公共读取，但尚未提供成员列表和多人共享能力；客户端成员菜单不伪造数据。

### 指令

- 当前指令包括 `/status`、`/config`、`/no:login`、`/clear:data`、`/cls` 和内置 `/help`。
- `/config set host_addr` 将服务器地址持久化到 `nilset_config` DataStore，默认值为 `syewiki.top:4703`；`DynamicBaseUrlInterceptor` 在每次请求前把 Retrofit 请求重写到该地址，`clear` 移除覆盖键。
- `/no:login`、`/clear:data` 只在 debug 构建可见且可执行。
- `/cls` 清空控制台历史；`/clear:data` 清除本地会话数据，两者职责不同。
- 控制台历史保存在 `ConsoleHistoryRepository` 进程内单例中，导航返回后仍可显示。
- 输入 `/` 后展示当前构建可见的全部候选指令并按名称排序；继续输入会按指令名前缀过滤。参数段补全由各指令通过 `completeArgument` 声明，选择候选只替换光标所在 token。
- `console/settings` 是控制台的附属路由；当前承载控制台背景开关，不承载独立主导航入口。
- 控制台输出字号保存在主题设置中；该设置只应用于控制台输出文本，不影响输入框、TopBar或其他页面文字。
- 所有路由级返回和完成后的弹栈统一检查当前目标 route；快速重复点击时，只有仍在发起页时才执行一次返回，避免弹出发起页导致空白。

### BiliNil

- `BiliInputParser` 本地识别 av、BV、直播间和 `b23.tv` 输入；短链由独立客户端手动跟随并限制为 3 跳。
- av/BV 使用视频 view 接口，cv 使用专栏 viewinfo 接口，直播间使用表单请求的房间信息接口。
- BiliNil 的 OkHttp 客户端不接入 Nilset 认证拦截器或动态服务端地址；请求只携带浏览器风格的 UA/Referer。
- 封面必须来自 HTTPS `hdslb.com` 或其子域；响应先进入 `cacheDir/bili_nil`，预览按尺寸降采样，保存时流式复制到 `Downloads/Nilset`。
- B站登录模块不创建或管理 WebView；`BiliWebLoginScreen` 负责启用 JavaScript、DOM Storage 和第三方 Cookie，并从 UA 移除 `wv`。导入前调用系统 `CookieManager.flush()`，读取 passport/api/www 三个域，合并去重后交给共享 `BiliCookieStore`。
- 缺少 `buvid3` 或 `buvid4` 时由 B站指纹接口补齐；登录态通过 nav 接口校验为未登录、普通用户或大会员，状态包含昵称、头像 URL 和 `mid`，不包含 Cookie。
- `BiliCookieStore` 只接受并只向 `bilibili.com` 及其子域发送 B站 Cookie；媒体 CDN 域名返回空 Cookie。Cookie 持久化使用 `EncryptedSharedPreferences`，旧明文存储会迁移后清除。
- 登出清空库内加密凭据，并对 B站 WebView 相关 Cookie 过期处理后刷新；错误路径只暴露枚举错误，不记录或包装 Cookie 值。

### 本地测试账号

- 测试账号只放在 `app/src/debug/assets/test-account.yaml`，该文件已被 `.gitignore` 排除。
- 文件存在且 `useLocalLogin: true` 时，debug 包内匹配账号密码后可建立本地会话并进入主页，不依赖服务端。
- 匹配账号密码以外的输入仍走正常 Retrofit 登录。
- 文件不存在时走正常 Retrofit 登录；release 包不包含该 debug 资源。

## 4. 目录映射

```text
app/src/main/java/net/atomreforge/nilset/
├─ core/
│  ├─ command/              # 指令契约、注册中心、具体指令
│  ├─ logging/              # 控制台日志模型、级别、统一日志入口与会话文件
│  ├─ theme/                # 主题预设、颜色字段、HEX 解析
├─ const/                   # 跨层表述常量：路由、接口、存储键、配置文件名、指令前缀
├─ data/
│  ├─ config/               # AppConfig、ConfigLoader、时长解析
│  ├─ remote/
│  │  ├─ api/               # Retrofit 接口
│  │  ├─ dto/               # 网络传输模型
│  │  └─ interceptor/       # AuthInterceptor 与 TokenAuthenticator
│  ├─ repository/           # 会话、主题、控制台历史、课表、配置与 Markdown 仓库
│  └─ session/              # SessionState 与 DataStore 数据源
├─ di/                       # Hilt Module
└─ ui/
   ├─ login/                # 登录 Screen / ViewModel
   ├─ register/             # 注册 Screen / ViewModel 与表单校验
   ├─ console/              # 控制台 Screen / ViewModel
   ├─ calendar/             # 独立月历 Screen 与日期状态
   ├─ home/                 # 主页与侧边栏
   ├─ main/                 # 主页/设置共用的底部导航
   ├─ session/              # 会话状态提供给启动路由使用
   ├─ schedule/             # 课表共建 Screen、状态、问候与课程选择逻辑
   ├─ settings/             # 设置页、用户卡片与附属设置页
   └─ theme/                # Material 3 主题、颜色、字体
```

## 5. 技术选型现状

| 领域 | 当前选型 | 说明 |
| --- | --- | --- |
| UI | Jetpack Compose + Material 3 | 已全面替代 XML 界面方案 |
| 导航 | Navigation Compose | 单 Activity + `NavHost` |
| 异步 | Coroutines + Flow | ViewModel、Repository 和网络层统一使用 |
| DI | Hilt 2.59 + KSP 2.3.11 | 适配 AGP 9 内建 Kotlin |
| 持久化 | DataStore Preferences | 保存会话、主题、控制台配置、当前用户本地课表和课表查看偏好，Room 尚未引入 |
| 网络 | Retrofit + OkHttp + kotlinx.serialization | 连接 Daizy Night 服务端 |
| 配置 | KAML + 强类型 data class | YAML fail-fast 加载 |
| 构建 | Gradle Version Catalog + AGP 9 | 单模块工程 |

## 6. 明确边界与已知差距

- 没有独立 Domain 层：当前业务规模较小，UseCase 仍按需后置。
- 没有多模块拆分：仍保持单 `:app` 模块，功能增多后再拆 feature/core 模块。
- 控制台历史只保存在进程内：应用进程被杀或系统回收后不会恢复。
- 课表共建页当前用户课表为本地创建和本地持久化；公共单用户课表读取已接入，但成员列表和多人共享 API 尚未提供，客户端成员菜单是只含登录用户的临时占位。
- 离线课表修改不会进入同步队列；连接恢复或在线修改时按可同步字段（包括老师、课室和备注的 `roaming` 映射）对比并以本地覆盖远端，同步失败则等待下一次触发。
- 侧边栏日历当前只是独立月历浏览视图，不加载课表或日程数据，也不提供日期详情。
- 测试覆盖仍不完整：会话刷新、服务连接、课表仓库/视图模型、主题模型和指令配置补全已有测试，Compose UI 测试不足。
- release 优化未开启：R8/资源压缩尚未启用。
- debug 指令是运行时门控：release 中不可见、不可执行，但代码并未从包内物理移除。

## 7. 质量与发布路线

- 构建：release 开启 R8 和 keep rules，补齐签名与 CI 构建。
- 质量：为核心指令、会话仓库、ViewModel 补单元测试，再补 Compose UI 测试。
- 工程：在 CI 基础上接入静态检查、依赖更新和构建缓存优化。
- 发布：崩溃上报、baseline profile、版本签名和发布流水线。

## 8. 落地路线图

- **Phase 0（已完成）**：架构文档落地，目录按分层重构。
- **Phase 1（已完成）**：登录和控制台逻辑迁入 ViewModel + UiState。
- **Phase 2（已完成）**：会话迁移为 `SessionRepository` + DataStore 持久化。
- **Phase 3（已完成）**：指令系统重构为命令接口、注册表、结构化结果和 debug 门控。
- **Phase 4（已完成）**：迁移到 Jetpack Compose、Material 3、单 Activity 和 Navigation。
- **Phase 5（已完成）**：用 Hilt 替代手动 DI，建立 Hilt/KSP 构建链路。
- **Phase 6（已完成）**：访问令牌自动刷新、CI 和核心会话链路测试已接入。
- **v0.2.0（已完成）**：对齐服务端 v0.7.1/v0.7.2，支持健康检查未认证状态、公共用户信息、私有课表 GET、私有课程信息 `roaming` 云同步和未初始化本地课表恢复。
- **Phase 7**：完善发布工程化，包括 R8、签名、崩溃上报和性能优化。
- **后续产品方向**：课表共享、共同空闲时间计算、随机抽签、分组和其他操作入口模块。

## 9. 演进原则

- 新界面优先复用 Compose + Material 3 现成组件，不重复造控件。
- 新业务先接入 Repository 与 UiState，避免 UI 直接持有业务状态。
- 指令和后续功能模块优先保持开闭原则，减少修改分发逻辑。
- 跨层表述常量统一放在 `const/` 包中，按 `AppRoutes`、`ApiExpressions`、`SessionStoreKeys` 等对象分组；业务文件不再新增顶层 `const`。
- 只有出现明确复用、隔离或编译时间收益时，再引入 Domain 层或多模块拆分。
