# ATOM「空集」客户端架构

> 定位：ATOM 生态中的 Android 操作入口，后续会从账号与控制台扩展到课表协作、随机抽签等日常工具。本文档描述当前已落地架构、明确边界和下一阶段方向。

## 1. 当前架构总览

- 单 `:app` Gradle 模块，按 `core / data / di / ui` 分包。
- UI 使用 Kotlin + Jetpack Compose + Material 3，不使用 XML 界面。
- 应用是单 Activity 架构，`MainActivity` 承载 `NavHost`。
- DI 使用 Hilt，依赖入口统一在 `SingletonComponent`。
- 数据层使用 Repository 接口、Retrofit/OkHttp 和 DataStore。
- 指令系统使用命令模式与注册表，控制台指令统一使用 `/` 前缀。
- 主题由 `AppThemeConfig` 提供，当前默认深色配色和内置字体，后续可替换预设或配置。

当前产品目标不是只有登录和控制台；账号体系与控制台只是后续功能模块的公共入口和调试基础。

## 2. 分层与数据流

```text
UI 层
  LoginScreen / ConsoleScreen / HomeScreen / SettingsScreen
  只展示 UiState、转发用户事件
        ↓
Presentation 状态层
  LoginViewModel / ConsoleViewModel
  StateFlow + UiState + viewModelScope
        ↓
Data 层
  SessionRepository、ConsoleHistoryRepository、ConfigLoader
  决定访问 Remote API 还是本地数据源
        ↓
数据源
  DaizyNightApi（Retrofit）
  SessionDataStore（DataStore Preferences）
```

### UI / Presentation 层

- `MainActivity` 使用 `ComponentActivity`、`enableEdgeToEdge`、`ATOMTheme` 和 `NavHost`。
- 登录页和控制台页分别是 `LoginScreen`、`ConsoleScreen`。
- 页面通过 `hiltViewModel()` 获取 ViewModel。
- ViewModel 持有 StateFlow 驱动的不可变 UiState，UI 不直接访问 Repository。
- 控制台输入框使用 Material 3 `ExposedDropdownMenuBox` 提供指令候选，候选按字母序过滤。
- 登录反馈使用 Material 3 `SnackbarHost`；登录成功后清除登录页返回栈并进入主页。
- 主页使用 Material 3 `ModalNavigationDrawer` 提供侧边栏，主页项固定在列表置顶，其余功能入口先作为占位栏展示。
- 主页与设置页共用 Material 3 `NavigationBar`；设置是独立顶层路由，不作为侧边栏项。
- 主页和设置页在主路由内并排布局，通过 `graphicsLayer` 平移共享同一版面；切换动画可被新的导航目标立即接管。

### Data 层

- `SessionRepository` 是会话业务接口，`RemoteSessionRepository` 是当前实现。
- `SessionDataStore` 只负责会话的读写和清除，不包含登录业务规则。
- 应用启动会先等待 DataStore 会话恢复，再根据 `SessionState` 决定初始进入登录页还是主页。
- `DaizyNightApi` 定义注册、登录、刷新访问令牌和获取用户信息接口。
- `AuthInterceptor` 从会话状态读取 access token，并统一添加 `Authorization: Bearer` 头。
- 用户信息接口使用 `/api/v1/user/{username}/me`；路径用户名来自持久化会话，仅用于服务端属主校验。
- access token 返回 401 时按配置自动刷新；refresh token 采用一次性轮换语义，成功后整体覆盖 access/refresh token 对。
- 登出会先保证本地持有可用 token 对，再把当前 refresh token 提交给服务端吊销，最后清空本地会话。
- `ConsoleHistoryRepository` 是进程内单例，保存控制台输出，避免页面返回后历史丢失。
- 应用日志通过 `AppLogger` 同时写入 Logcat、控制台历史和磁盘日志；控制台只保留最近 500 条，磁盘文件不主动清理。
- 每次 App 进程启动会在 `files/logs` 创建 `session-<序号>-<时间>.log`，序号按已有日志文件最大序号递增。
- HTTP 日志由 OkHttp 拦截器接入并按状态分级着色。
- `ConfigLoader.mustLoad()` 加载强类型 YAML 配置，解析或校验失败会快速失败。

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
- 应用进程内通过 `SessionState` 暴露状态，磁盘上通过 DataStore 恢复。
- `AuthInterceptor` 和 `TokenAuthenticator` 通过 `dagger.Lazy` 打破 OkHttp、Retrofit 与会话仓库之间的构建期循环依赖。
- `auth.autoRefresh` 控制是否注册 401 自动刷新器；当前默认与 debug 联调配置均已启用。

### 指令

- 当前指令包括 `/status`、`/no:login`、`/clear:data`、`/cls` 和内置 `/help`。
- `/no:login`、`/clear:data` 只在 debug 构建可见且可执行。
- `/cls` 清空控制台历史；`/clear:data` 清除本地会话数据，两者职责不同。
- 控制台历史保存在 `ConsoleHistoryRepository` 进程内单例中，导航返回后仍可显示。
- 输入 `/` 后展示当前构建可见的候选指令，继续输入会按指令名前缀过滤。

### 本地测试账号

- 测试账号只放在 `app/src/debug/assets/test-account.yaml`，该文件已被 `.gitignore` 排除。
- 文件存在且 `useLocalLogin: true` 时，debug 包内匹配账号密码后可建立本地会话并进入主页，不依赖服务端。
- 文件不存在时走正常 Retrofit 登录；release 包不包含该 debug 资源。

## 4. 目录映射

```text
app/src/main/java/net/atomreforge/nilset/
├─ core/
│  ├─ command/              # 指令契约、注册中心、具体指令
│  ├─ logging/              # 控制台日志模型、级别、统一日志入口与会话文件
├─ const/                   # 跨层表述常量：路由、接口、存储键、配置文件名、指令前缀
├─ data/
│  ├─ config/               # AppConfig、ConfigLoader、时长解析
│  ├─ remote/
│  │  ├─ api/               # Retrofit 接口
│  │  ├─ dto/               # 网络传输模型
│  │  └─ interceptor/       # AuthInterceptor 与 TokenAuthenticator
│  ├─ repository/           # 会话仓库与控制台历史仓库
│  └─ session/              # SessionState 与 DataStore 数据源
├─ di/                       # Hilt Module
└─ ui/
   ├─ login/                # 登录 Screen / ViewModel
   ├─ console/              # 控制台 Screen / ViewModel
   ├─ home/                 # 主页与侧边栏
   ├─ main/                 # 主页/设置共用的底部导航
   ├─ session/              # 会话状态提供给启动路由使用
   ├─ settings/             # 设置页
   └─ theme/                # Material 3 主题、颜色、字体
```

## 5. 技术选型现状

| 领域 | 当前选型 | 说明 |
| --- | --- | --- |
| UI | Jetpack Compose + Material 3 | 已全面替代 XML 界面方案 |
| 导航 | Navigation Compose | 单 Activity + `NavHost` |
| 异步 | Coroutines + Flow | ViewModel、Repository 和网络层统一使用 |
| DI | Hilt 2.59 + KSP 2.3.11 | 适配 AGP 9 内建 Kotlin |
| 持久化 | DataStore Preferences | 当前只存会话，Room 尚未引入 |
| 网络 | Retrofit + OkHttp + kotlinx.serialization | 连接 Daizy Night 服务端 |
| 配置 | KAML + 强类型 data class | YAML fail-fast 加载 |
| 构建 | Gradle Version Catalog + AGP 9 | 单模块工程 |

## 6. 明确边界与已知差距

- 没有独立 Domain 层：当前业务规模较小，UseCase 仍按需后置。
- 没有多模块拆分：仍保持单 `:app` 模块，功能增多后再拆 feature/core 模块。
- 控制台历史只保存在进程内：应用进程被杀或系统回收后不会恢复。
- 课表 API 尚未接入客户端业务层：接口契约已由服务端给出，但当前主页和设置页还没有对应功能。
- 测试还是模板为主：核心指令、Repository、ViewModel 和 Compose UI 的有效测试不足。
- release 优化未开启：R8/资源压缩尚未启用。
- debug 指令是运行时门控：release 中不可见、不可执行，但代码并未从包内物理移除。

## 7. 质量与发布路线

- 构建：release 开启 R8 和 keep rules，补齐签名与 CI 构建。
- 质量：为核心指令、会话仓库、ViewModel 补单元测试，再补 Compose UI 测试。
- 工程：接入 CI、静态检查、依赖更新和构建缓存。
- 发布：崩溃上报、baseline profile、版本签名和发布流水线。

## 8. 落地路线图

- **Phase 0（已完成）**：架构文档落地，目录按分层重构。
- **Phase 1（已完成）**：登录和控制台逻辑迁入 ViewModel + UiState。
- **Phase 2（已完成）**：会话迁移为 `SessionRepository` + DataStore 持久化。
- **Phase 3（已完成）**：指令系统重构为命令接口、注册表、结构化结果和 debug 门控。
- **Phase 4（已完成）**：迁移到 Jetpack Compose、Material 3、单 Activity 和 Navigation。
- **Phase 5（已完成）**：用 Hilt 替代手动 DI，建立 Hilt/KSP 构建链路。
- **Phase 6（进行中）**：访问令牌自动刷新已接入，仍需补有效测试和 CI。
- **Phase 7**：完善发布工程化，包括 R8、签名、崩溃上报和性能优化。
- **后续产品方向**：课表共享、共同空闲时间计算、随机抽签、分组和其他操作入口模块。

## 9. 演进原则

- 新界面优先复用 Compose + Material 3 现成组件，不重复造控件。
- 新业务先接入 Repository 与 UiState，避免 UI 直接持有业务状态。
- 指令和后续功能模块优先保持开闭原则，减少修改分发逻辑。
- 跨层表述常量统一放在 `const/` 包中，按 `AppRoutes`、`ApiExpressions`、`SessionStoreKeys` 等对象分组；业务文件不再新增顶层 `const`。
- 只有出现明确复用、隔离或编译时间收益时，再引入 Domain 层或多模块拆分。
