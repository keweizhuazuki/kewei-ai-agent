# kewei-ai-agent

这是一个基于 Spring Boot + Spring AI 的 AI Agent 学习/实践项目。  
本 README 主要用于记录项目推进过程，方便后续回顾每一步做了什么、增加了哪些文件、核心类和方法分别负责什么。

说明：

- 本文是根据当前仓库代码状态对前期步骤进行回填整理。
- 不包含代码示例，重点记录实现思路、目录结构和阶段性成果。
- 后续你继续在这个对话里同步进度，我可以持续补充到这里。

## 项目目标（当前阶段）

- 搭建 Spring Boot 项目基础运行环境
- 接入阿里百炼与 Ollama 的 `ChatModel`
- 使用 `ChatClient` 封装业务对话能力（文本 / 多模态）
- 统一接口响应格式与异常处理
- 实现并串联自定义 Advisor（含链路共享状态）
- 支持结构化输出（JSON -> Java 对象）
- 支持多种会话记忆存储方案（`file` / `mysql` / `redis`）并通过 yml 切换
- 支持基础 RAG（Markdown 文档加载、向量化、检索增强问答）
- 为 `pgvector + RAG` 实操完成配置准备（含多数据源与 AI Bean 冲突处理）
- 跑通 `pgvector + RAG` 检索链路（含 PgVector 数据源区分与测试验证）
- 增强自定义 RAG 能力（查询预处理、关键词增强、按用户状态过滤检索）
- 增加工具调用（Tools）能力，支持文件读写、网页搜索/抓取、资源下载、PDF 转图

## 目录结构（当前）

- `pom.xml`
  - Maven 依赖管理与构建配置（Spring Boot、Spring AI、DashScope、Ollama、校验、文档等）
- `src/main/java/com/kiwi/keweiaiagent`
  - `KeweiAiAgentApplication`：Spring Boot 启动入口
- `src/main/java/com/kiwi/keweiaiagent/app`
  - `LoveApp`：对话业务封装（文本对话、结构化输出、多模态、RAG、记忆、Advisor 链）
- `src/main/java/com/kiwi/keweiaiagent/advisor`
  - `MyLoggerAdvisor`：记录请求与响应日志
  - `ReReadingAdvisor`：对用户输入进行增强，并向 Advisor 链上下文写入原始输入
- `src/main/java/com/kiwi/keweiaiagent/chatmemory`
  - `FileBaseChatMemory`：基于本地文件的会话记忆持久化
  - `MySqlChatMemory`：基于 MySQL 的会话记忆持久化
  - `MyRedisChatMemory`：基于 Redis 的会话记忆持久化
  - `ChatMemoryTableInitializer`：MySQL 记忆表初始化（自动建表）
- `src/main/java/com/kiwi/keweiaiagent/chatmemory/entity`
  - `ChatMemoryMessageDO`：会话消息数据库实体对象
- `src/main/java/com/kiwi/keweiaiagent/chatmemory/mapper`
  - `ChatMemoryMessageMapper`：会话消息表 Mapper（MyBatis-Plus）
- `src/main/java/com/kiwi/keweiaiagent/common`
  - `BaseResponse`：统一响应体
- `src/main/java/com/kiwi/keweiaiagent/constant`
  - `FileConstant`：文件工具默认保存目录常量
- `src/main/java/com/kiwi/keweiaiagent/config`
  - `AiModelPrimaryConfig`：指定默认 AI 模型/Embedding Bean 优先级（解决多 Provider 并存冲突）
  - `ChatMemoryConfig`：会话记忆实现装配与切换配置（file/mysql/redis）
  - `ChatMemoryMySqlDataSourceConfig`：ChatMemory 专用 MySQL 数据源与 MyBatis 配置
  - `PgVectorPrimaryDataSourceConfig`：PgVector 使用的 PostgreSQL 主数据源与 `JdbcTemplate` 配置
  - `GlobalResponseBodyAdvice`：统一响应包装
  - `GlobalCorsConfig`：全局跨域配置
- `src/main/java/com/kiwi/keweiaiagent/controller`
  - `HealController`：健康检查接口
- `src/main/java/com/kiwi/keweiaiagent/exception`
  - `BusinessException`：业务异常定义
  - `GlobalExceptionHandler`：全局异常处理器
- `src/main/java/com/kiwi/keweiaiagent/rag`
  - `LoveAppDocumentLoader`：加载并解析恋爱知识库 Markdown 文档
  - `LoveAppVectorStoreConfig`：构建向量存储并导入知识库文档
  - `PgVectorVectorLoadMarkdownConfig`：将 Markdown 文档增量写入 PgVector（按内容哈希去重）
  - `MyKeywordEnricher`：对文档执行关键词元数据增强
- `src/main/java/com/kiwi/keweiaiagent/rag/factory/loveapp`
  - `LoveAppContextualQueryAugmenterFactory`：自定义上下文增强器工厂（空上下文兜底话术）
  - `LoveAppRetrievalAugmentationAdvisorFactory`：自定义检索增强 Advisor 工厂（状态过滤+检索参数）
- `src/main/java/com/kiwi/keweiaiagent/query`
  - `QueryPreprocessor`：查询预处理组件（重写、压缩、多查询扩展）
- `src/main/java/com/kiwi/keweiaiagent/demo/invoke`
  - `SdkAiInvoke`：阿里百炼 SDK 调用示例
  - `SpringAiAiInvoke`：Spring AI + DashScope 调用示例
  - `OllamaAiInvoke`：Spring AI + Ollama 调用示例
  - `DebugBeans`：调试 `ChatModel` Bean 注册情况
  - `TestApiKey`：本地测试 API Key 占位（仅测试用）
- `src/main/java/com/kiwi/keweiaiagent/tools`
  - `ToolRegistration`：统一注册工具回调
  - `FileOperationTool`：文件读取/写入工具
  - `WebSearchTool`：联网搜索工具（SearchAPI）
  - `WebScrapingTool`：网页抓取与摘要工具
  - `ResourceDownloadTool`：资源下载工具
  - `PdfConvertTool`：批量 PDF 转 JPG 工具
- `src/main/resources`
  - `application.yml`：应用、端口、Profile、AI 模型、pgvector 预配置与接口文档配置
  - `application-local.yml`：本地环境数据源/Redis/ChatMemory 等配置（含动态切换参数）
  - `static/images`：多模态与工具调用相关图片资源（如 `test.png`、`couple.png`）
- `src/test/java/com/kiwi/keweiaiagent`
  - 应用启动测试、`LoveApp` 对话能力测试（文本 / 结构化 / 多模态 / RAG / Tools）
- `src/test/java/com/kiwi/keweiaiagent/rag`
  - `LoveAppDocumentLoaderTest`：知识库 Markdown 文档加载测试
- `src/test/java/com/kiwi/keweiaiagent/tools`
  - 工具能力测试（文件工具、搜索工具、抓取工具、下载工具、PDF 转换工具）
- `src/test/resources`
  - `application-test.yml`：测试环境专用配置（简化自动装配，避免非必要外部依赖）

## 进度记录

## 1. 初始化项目

### 本阶段目标

- 创建可启动的 Spring Boot 工程
- 确立 Maven 构建方式与基础依赖结构
- 完成基本运行配置与健康检查入口（便于验证项目已正常启动）

### 主要新增/建立的文件

- `pom.xml`
  - 作用：项目依赖管理与打包构建配置
  - 当前包含的重点方向：
    - Spring Boot Web
    - Spring AI（后续步骤使用）
    - 校验能力（Validation）
    - 接口文档相关（springdoc / knife4j）
    - Lombok
- `src/main/java/com/kiwi/keweiaiagent/KeweiAiAgentApplication.java`
  - 类：`KeweiAiAgentApplication`
  - 方法：
    - `main(String[] args)`：项目启动入口，启动 Spring Boot 容器
- `src/main/resources/application.yml`
  - 作用：集中配置应用名、端口、上下文路径、Profile、AI 模型参数、接口文档配置等
- `src/main/java/com/kiwi/keweiaiagent/controller/HealController.java`
  - 类：`HealController`
  - 方法：
    - `healthCheck()`：健康检查接口，快速验证服务可用性

### 阶段结果

- 项目具备基础启动能力
- 可以通过健康检查接口确认服务启动正常
- 为后续接入模型和封装业务能力打好基础

## 2. 接入阿里百炼 + Ollama 模型（ChatModel）

### 本阶段目标

- 验证两种模型接入路径：
  - 阿里百炼（DashScope）
  - 本地 Ollama
- 打通最小调用链路，确认 `ChatModel` Bean 与模型调用可用

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/demo/invoke/SdkAiInvoke.java`
  - 类：`SdkAiInvoke`
  - 方法：
    - `callWithMessage()`：使用阿里百炼 SDK 构造消息并发起调用
    - `main(String[] args)`：本地运行入口，用于直接测试 SDK 调用结果
  - 作用：验证“原生 SDK 调用”链路，便于对比 Spring AI 封装方式
- `src/main/java/com/kiwi/keweiaiagent/demo/invoke/TestApiKey.java`
  - 接口：`TestApiKey`
  - 成员：
    - `API_KEY`：测试用 Key 常量（本地调试使用）
  - 作用：给 SDK 示例提供测试 Key 来源（后续建议改为环境变量）
- `src/main/java/com/kiwi/keweiaiagent/demo/invoke/SpringAiAiInvoke.java`
  - 类：`SpringAiAiInvoke`（`dashscope` Profile）
  - 方法：
    - `run(String... args)`：应用启动后执行一次 DashScope `ChatModel` 调用
  - 作用：验证 Spring AI 对 DashScope 模型的接入和调用
- `src/main/java/com/kiwi/keweiaiagent/demo/invoke/OllamaAiInvoke.java`
  - 类：`OllamaAiInvoke`（`ollama` Profile）
  - 方法：
    - `run(String... args)`：应用启动后执行一次 Ollama `ChatModel` 调用
  - 作用：验证 Spring AI 对 Ollama 模型的接入和调用
- `src/main/java/com/kiwi/keweiaiagent/demo/invoke/DebugBeans.java`
  - 类：`DebugBeans`
  - 方法：
    - `DebugBeans(ApplicationContext ctx)`：注入应用上下文
    - `run(String... args)`：打印 `ChatModel` 类型 Bean 名称，辅助排查 Bean 注册问题
  - 作用：解决多模型/多 Profile 场景下 Bean 不明确的问题
- `src/main/resources/application.yml`
  - 作用（本阶段重点）：增加 Ollama 地址、模型名称、启用 Profile 等配置项

### 阶段结果

- 两种模型接入路径都具备验证方式
- 已具备 `ChatModel` 级别的调用能力，为后续升级到 `ChatClient` 做准备

## 3. 初试 ChatClient

### 本阶段目标

- 从“直接调用 `ChatModel`”升级到“通过 `ChatClient` 组织对话”
- 封装业务层对话入口，方便 controller / service 后续接入
- 引入会话记忆能力，支持多轮对话上下文

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 类：`LoveApp`
  - 方法：
    - `LoveApp(ChatModel ollamaChatModel)`：构造业务对话组件，初始化 `ChatMemory` 与 `ChatClient`
    - `doChat(String message, String chatId)`：执行一次用户对话请求，并按会话 ID 维持上下文
  - 作用：
    - 封装“恋爱咨询”场景的系统提示词
    - 统一管理聊天记忆窗口（最近消息）
    - 对上层暴露简洁的业务调用方法
- `src/test/java/com/kiwi/keweiaiagent/app/LoveAppTest.java`
  - 类：`LoveAppTest`
  - 方法：
    - `testChat()`：验证多轮对话与会话记忆的基本可用性
  - 作用：用测试方式初步确认 `ChatClient` 封装有效

### 阶段结果

- 完成 `ChatClient` 的业务化封装
- 初步具备“同一会话连续对话”的能力
- 为后续接入 Advisor 链提供挂载位置（`ChatClient` 构建阶段）

## 4. 封装全局异常处理器、响应处理等

### 本阶段目标

- 统一接口响应格式，避免 controller 返回结构不一致
- 统一异常处理出口，减少重复 `try-catch`
- 对参数错误、请求格式错误、系统异常进行分类处理

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/common/BaseResponse.java`
  - 类：`BaseResponse<T>`
  - 方法：
    - `BaseResponse()`：默认构造方法
    - `BaseResponse(Integer code, String message, T data)`：完整响应构造方法
    - `success(T data)`：返回带数据的成功响应
    - `success()`：返回不带数据的成功响应
    - `fail(Integer code, String message)`：返回失败响应
    - `getCode()/setCode()`：响应码访问与设置
    - `getMessage()/setMessage()`：响应信息访问与设置
    - `getData()/setData()`：响应数据访问与设置
  - 作用：统一接口返回结构（`code` / `message` / `data`）
- `src/main/java/com/kiwi/keweiaiagent/config/GlobalResponseBodyAdvice.java`
  - 类：`GlobalResponseBodyAdvice`
  - 方法：
    - `GlobalResponseBodyAdvice(ObjectMapper objectMapper)`：注入序列化工具
    - `supports(...)`：声明是否启用响应拦截（当前对 controller 全量开启）
    - `beforeBodyWrite(...)`：在返回前统一包装为 `BaseResponse`，并兼容字符串返回场景
  - 作用：让 controller 即使返回普通对象或字符串，也能自动转为统一响应格式
- `src/main/java/com/kiwi/keweiaiagent/exception/BusinessException.java`
  - 类：`BusinessException`
  - 方法：
    - `BusinessException(String message)`：默认业务异常（默认业务错误码）
    - `BusinessException(Integer code, String message)`：自定义业务错误码与信息
    - `getCode()`：读取业务错误码
  - 作用：承载业务层可预期错误
- `src/main/java/com/kiwi/keweiaiagent/exception/GlobalExceptionHandler.java`
  - 类：`GlobalExceptionHandler`
  - 方法：
    - `handleBusinessException(BusinessException e)`：处理业务异常
    - `handleMethodArgumentNotValidException(MethodArgumentNotValidException e)`：处理 `@Valid` 参数校验异常
    - `handleBindException(BindException e)`：处理参数绑定异常
    - `handleHttpMessageNotReadableException(HttpMessageNotReadableException e)`：处理请求体格式错误
    - `handleException(Exception e)`：兜底处理未捕获异常并记录日志
  - 作用：统一异常出口与错误码返回策略
- `src/main/java/com/kiwi/keweiaiagent/config/GlobalCorsConfig.java`
  - 类：`GlobalCorsConfig`
  - 方法：
    - `addCorsMappings(CorsRegistry registry)`：配置全局跨域规则
  - 作用：降低前后端联调时的跨域阻碍（开发阶段尤其有用）

### 阶段结果

- controller 输出风格统一
- 常见异常具备统一错误码与提示信息
- 接口层可维护性明显提升

## 5. 完成 MyLoggerAdvisor 与 ReReadingAdvisor（完成 Advisor 链中共享状态）

### 本阶段目标

- 在 `ChatClient` 调用前后插入可复用的增强逻辑
- 实现日志观测能力（请求/响应）
- 实现输入增强能力（Re-reading）
- 在 Advisor 链中共享上下文状态（原始用户输入）

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/advisor/ReReadingAdvisor.java`
  - 类：`ReReadingAdvisor`
  - 方法：
    - `ReReadingAdvisor()`：使用默认重读模板初始化
    - `ReReadingAdvisor(String re2AdviseTemplate)`：使用自定义模板初始化
    - `before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain)`：在模型调用前增强用户输入，并将原始输入写入上下文
    - `after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain)`：当前保持透传，预留后处理扩展点
    - `getOrder()`：返回 Advisor 顺序
    - `withOrder(int order)`：设置 Advisor 顺序并返回当前实例（便于链式配置）
  - 作用：
    - 通过模板对问题进行“重复强调/重读”增强
    - 将原始 `userText` 放入 `context`，供后续 Advisor（例如日志）读取
- `src/main/java/com/kiwi/keweiaiagent/advisor/MyLoggerAdvisor.java`
  - 类：`MyLoggerAdvisor`
  - 方法：
    - `adviseCall(...)`：拦截普通同步调用，记录请求与响应
    - `adviseStream(...)`：拦截流式调用，聚合响应后记录日志
    - `logRequest(ChatClientRequest request)`：记录原始输入、当前输入及请求内容
    - `logResponse(ChatClientResponse chatClientResponse)`：记录模型响应内容
    - `getName()`：返回 Advisor 名称
    - `getOrder()`：返回执行顺序
  - 作用：
    - 统一记录调用链日志
    - 借助 `context` 读取 `ReReadingAdvisor` 写入的原始输入，实现链路共享状态的可观测性
- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 本阶段在该类中继续增强：
    - 在 `ChatClient` 构建时挂载默认 Advisor 链（记忆、日志、ReReading）
  - 作用：把记忆、日志、提示增强统一收敛到业务对话入口

### 阶段结果

- 完成自定义 Advisor 的接入与串联
- 实现了 Advisor 链上下文共享（原始输入 -> 日志观察）
- 为后续增加更多 Advisor（限流、审计、提示词安全、输出后处理）提供了稳定扩展点

## 6. 了解结构化输出转换器（为普通对话增加 JSON 结构化输出能力）

### 本阶段目标

- 学习并理解结构化输出转换器的使用方式
- 在普通对话能力基础上增加 JSON 结构化输出能力
- 为后续功能拓展（如工作流编排、前端渲染、数据落库、规则判断）提供稳定数据格式

### 本阶段价值（为什么做）

- 自然语言输出虽然可读，但不利于程序稳定消费
- 结构化 JSON 输出更适合：
  - 前端按字段展示
  - 后端做规则判断/路由分发
  - 持久化存储与日志分析
  - 后续接入更多 Agent 能力（工具调用、任务拆分、状态机）

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 本阶段在该类中新增结构化输出相关能力：
  - 记录类型（record）：
    - `ActorsFilms(String actor, List<String> movies)`：用于承接“演员 + 电影列表”的结构化输出结果
    - `LoveReport(String title, List<String> suggestions)`：用于承接“恋爱报告”的结构化输出结果
  - 方法：
    - `getActorsFilms(String actor, String chatId)`：通过 `ChatClient` 发起请求，并将模型输出直接转换为 `ActorsFilms` 对象
      - 作用：作为结构化输出转换器的入门示例，验证模型结果可被稳定解析为 Java 对象
    - `doChatWithReport(String message, String chatId)`：在普通对话基础上，要求模型输出“恋爱报告”结构，并转换为 `LoveReport`
      - 作用：将结构化输出应用到实际业务场景，为后续前端展示/业务处理提供 JSON 友好结果
- `src/test/java/com/kiwi/keweiaiagent/app/LoveAppTest.java`
  - 本阶段新增/补充测试方法：
    - `testGetActorsFilms()`：验证 `getActorsFilms(...)` 的结构化输出结果是否完整（演员名、电影列表、列表长度）
    - `doChatWithReport()`：验证 `doChatWithReport(...)` 能返回 `LoveReport` 结构对象
  - 作用：确保结构化输出能力在真实调用下可用，避免仅有方法但缺少验证

### 阶段结果

- 普通对话能力开始具备结构化输出方向的设计基础
- 为后续将 AI 输出接入业务流程提供更好的可扩展性
- 项目从“能对话”进一步走向“能被程序稳定消费”

## 7. 完成 FileBaseChatMemory（实现本地永久会话存储）

### 本阶段目标

- 实现基于文件的 `ChatMemory`（或记忆仓储）能力
- 将原本偏内存态的会话记录落到本地文件，实现持久化
- 为应用重启后的上下文恢复、多轮会话延续提供基础支持

### 本阶段价值（为什么做）

- 内存会话在服务重启后会丢失，不利于真实业务使用
- 本地文件持久化实现成本较低，适合作为早期阶段的可靠方案
- 为后续升级到数据库/对象存储/分布式缓存打下接口与抽象基础

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/chatmemory/FileBaseChatMemory.java`
  - 类：`FileBaseChatMemory`（实现 `ChatMemory`）
  - 方法：
    - `FileBaseChatMemory(String base_DIR)`：初始化本地存储目录；目录不存在时自动创建
    - `add(String conversationId, Message message)`：追加单条消息到指定会话
    - `add(String conversationId, List<Message> messages)`：批量追加消息并持久化到文件
    - `get(String conversationId)`：读取指定会话的全部消息（返回只读副本）
    - `clear(String conversationId)`：清空指定会话（删除对应文件）
    - `getOrCreateConversation(String conversationId)`：读取会话文件；不存在则创建空会话文件并返回空消息列表
    - `saveConversation(String conversationId, List<Message> messages)`：将会话消息写入 JSON 文件（先写临时文件再替换，降低写入中断风险）
    - `getConversationFile(String conversationId)`：根据会话 ID 生成安全文件名并定位会话文件
    - `fromSpringMessage(Message message)`：将 Spring AI 消息对象转换为可落盘的存储对象
    - `toSpringMessage(StoredMessage stored)`：将文件中的存储对象还原为 Spring AI 消息对象
  - 内部数据结构（内部类）：
    - `StoredMessage`：文件持久化用消息结构（类型、文本、元数据等）
    - `StoredToolCall`：持久化 Assistant 的工具调用信息
    - `StoredToolResponse`：持久化工具响应信息
  - 作用：
    - 将会话消息以 JSON 文件形式持久化到本地
    - 支持普通消息与工具调用/工具响应消息的保存与恢复
    - 为应用重启后恢复上下文提供基础能力
- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 本阶段在该类中继续增强：
    - 在构造方法中将 `chatMemory` 从内存实现切换为 `FileBaseChatMemory`
    - 为本地会话文件指定存储目录（`tmp/chat-memory`）
  - 作用：把文件持久化记忆真正接入 `ChatClient` 的对话链路，而不只是单独实现存储类

### 阶段结果

- 会话记忆能力从“运行期内存”升级为“本地文件持久化”
- 应用重启后保留历史上下文成为可能
- 项目在可用性和后续扩展性上进一步提升

## 8. 增加 MySQL 持久化记忆实现与可切换配置（ChatMemoryTableInitializer / MySqlChatMemory / ChatMemoryConfig）

### 本阶段目标

- 在文件持久化方案之外，增加基于 MySQL 的 `ChatMemory` 实现
- 通过配置实现 `file` / `mysql` 两种记忆实现的切换
- 为后续多实例部署、集中存储、数据分析提供数据库基础

### 本阶段价值（为什么做）

- 文件存储适合单机开发，但在多实例和运维场景下可扩展性有限
- MySQL 方案便于统一管理会话数据、查询历史记录、做后续统计分析
- 通过配置化切换存储实现，降低 `LoveApp` 等业务层对底层存储方式的耦合

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/chatmemory/ChatMemoryTableInitializer.java`
  - 类：`ChatMemoryTableInitializer`
  - 方法：
    - `init()`：应用启动后自动执行建表 SQL（`ai_chat_memory_message`），确保 MySQL 记忆表存在
  - 作用：
    - 在 `app.chat-memory.type=mysql` 条件下自动初始化表结构
    - 降低首次接入数据库记忆时的手工建表成本
- `src/main/java/com/kiwi/keweiaiagent/chatmemory/MySqlChatMemory.java`
  - 类：`MySqlChatMemory`（实现 `ChatMemory`）
  - 方法：
    - `add(String conversationId, Message message)`：追加单条消息
    - `add(String conversationId, List<Message> messages)`：批量追加消息到数据库
    - `get(String conversationId)`：按会话 ID 查询消息列表，并按主键升序恢复对话顺序
    - `clear(String conversationId)`：按会话 ID 删除历史消息
    - `serializeMessage(Message message)`：将 Spring AI 消息序列化为 JSON 字符串
    - `deserializeMessage(String payloadJson)`：将数据库中的 JSON 反序列化为 Spring AI 消息
    - `fromSpringMessage(Message message)`：将运行时消息转换为可存储结构
    - `toSpringMessage(StoredMessage stored)`：将存储结构恢复为运行时消息
  - 内部数据结构（内部类）：
    - `StoredMessage`：数据库持久化载荷结构（类型、文本、元数据、工具调用/响应）
    - `StoredToolCall`：持久化工具调用信息
    - `StoredToolResponse`：持久化工具响应信息
  - 作用：
    - 使用 MyBatis-Plus Mapper 将每条会话消息落库
    - 保持与 `FileBaseChatMemory` 一致的消息序列化/反序列化思路，便于维护
- `src/main/java/com/kiwi/keweiaiagent/config/ChatMemoryConfig.java`
  - 类：`ChatMemoryConfig`
  - 方法：
    - `chatMemory(...)`：根据配置项创建 `ChatMemory` Bean；支持 `mysql` / `file` 两种实现
  - 作用：
    - 将记忆实现选择逻辑集中到配置层
    - 当 `type=mysql` 时注入 `MySqlChatMemory`，否则回退为 `FileBaseChatMemory`
- `src/main/java/com/kiwi/keweiaiagent/chatmemory/entity/ChatMemoryMessageDO.java`
  - 类：`ChatMemoryMessageDO`
  - 字段（核心）：
    - `id`：主键
    - `conversationId`：会话 ID
    - `payloadJson`：消息 JSON 内容
    - `createTime` / `updateTime`：创建与更新时间
  - 作用：
    - 对应数据库表 `ai_chat_memory_message` 的 ORM 实体
    - 承载会话消息持久化记录
- `src/main/java/com/kiwi/keweiaiagent/chatmemory/mapper/ChatMemoryMessageMapper.java`
  - 接口：`ChatMemoryMessageMapper`（继承 `BaseMapper<ChatMemoryMessageDO>`）
  - 方法：
    - 继承 `BaseMapper` 提供通用 CRUD 能力（未额外自定义方法）
  - 作用：
    - 为 `MySqlChatMemory` 提供数据库读写入口
- `pom.xml`
  - 本阶段涉及：
    - 引入 MyBatis-Plus Spring Boot Starter（用于 Mapper 与实体持久化）
  - 作用：
    - 支撑 `ChatMemoryMessageMapper` 与 `MySqlChatMemory` 的数据库访问能力

### 阶段结果

- 会话记忆持久化方案从“仅文件”扩展为“文件 + MySQL”
- 记忆实现已支持配置化切换，业务层无需感知底层存储细节
- 为后续生产化部署与会话数据治理提供更稳妥基础

## 9. 新增 MyRedisChatMemory，完善 ChatMemoryConfig（支持通过 yml 一键切换记忆化方式）

### 本阶段目标

- 新增基于 Redis 的 `ChatMemory` 实现
- 继续完善 `ChatMemoryConfig`，支持 `file` / `mysql` / `redis` 三种记忆方式切换
- 通过 yml 配置项实现低成本切换，无需改业务代码

### 本阶段价值（为什么做）

- Redis 适合高频读写、低延迟场景，适合作为会话记忆存储方案
- 相比文件和 MySQL，Redis 在短时对话上下文读取性能上更有优势
- 通过配置切换存储实现，进一步提升项目在不同环境下的适配能力（本地开发 / 测试 / 服务部署）

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/chatmemory/MyRedisChatMemory.java`
  - 类：`MyRedisChatMemory`（实现 `ChatMemory`）
  - 方法：
    - `MyRedisChatMemory(StringRedisTemplate stringRedisTemplate, String keyPrefix)`：初始化 Redis 模板与 key 前缀配置
    - `add(String conversationId, Message message)`：追加单条消息
    - `add(String conversationId, List<Message> messages)`：批量追加消息到 Redis List
    - `get(String conversationId)`：从 Redis 读取指定会话的全部消息并反序列化恢复
    - `clear(String conversationId)`：删除指定会话的 Redis Key
    - `buildKey(String conversationId)`：构造会话对应的 Redis Key（前缀 + 会话 ID）
    - `serializeMessage(Message message)`：将 Spring AI 消息序列化为 JSON 字符串
    - `deserializeMessage(String payloadJson)`：将 Redis 中的 JSON 反序列化为 Spring AI 消息
    - `fromSpringMessage(Message message)`：将运行时消息转换为可存储结构
    - `toSpringMessage(StoredMessage stored)`：将存储结构恢复为运行时消息
  - 内部数据结构（内部类）：
    - `StoredMessage`：Redis 持久化载荷结构（类型、文本、元数据、工具调用/响应）
    - `StoredToolCall`：持久化工具调用信息
    - `StoredToolResponse`：持久化工具响应信息
  - 作用：
    - 使用 Redis List 保存会话消息序列，保持消息顺序
    - 延续与文件/MySQL 方案一致的序列化模型，便于维护和切换
- `src/main/java/com/kiwi/keweiaiagent/config/ChatMemoryConfig.java`
  - 类：`ChatMemoryConfig`（本阶段增强）
  - 方法：
    - `mysqlChatMemory(...)`：当 `app.chat-memory.type=mysql` 时装配 `MySqlChatMemory`
    - `redisChatMemory(...)`：当 `app.chat-memory.type=redis` 时装配 `MyRedisChatMemory`，并支持 `key-prefix` 配置
    - `fileChatMemory(...)`：作为兜底方案，在未命中其他 `ChatMemory` Bean 时装配 `FileBaseChatMemory`
  - 作用：
    - 通过条件装配 + 缺省兜底，实现记忆方式的一键切换
    - 将选择逻辑从“代码判断”升级为“Spring 配置驱动”
- `pom.xml`
  - 本阶段涉及：
    - 引入 `spring-boot-starter-data-redis`
  - 作用：
    - 提供 `StringRedisTemplate` 等 Redis 访问能力，支撑 `MyRedisChatMemory`
- `src/main/resources/application.yml`（或对应环境配置文件）
  - 本阶段配置约定：
    - 通过 `app.chat-memory.type` 切换记忆实现（如 `file` / `mysql` / `redis`）
    - Redis 方案支持 `app.chat-memory.redis.key-prefix` 配置 Key 前缀
  - 作用：
    - 实现“改配置即切换”的使用方式，减少代码改动

### 阶段结果

- 会话记忆方案扩展为“文件 + MySQL + Redis”
- 记忆实现选择全面配置化，可通过 yml 快速切换
- 为不同环境下的性能、持久化与运维需求提供更灵活的方案选择

## 10. 新增 doChatWithImage（支持多模态调用）

### 本阶段目标

- 在原有文本对话基础上增加图片输入能力
- 封装多模态对话方法，支持“图片 + 文本问题”联合输入
- 让会话记忆与多轮追问在图像场景下也能复用现有 `ChatClient` 流程

### 本阶段价值（为什么做）

- 多模态输入能覆盖更多真实场景（识图问答、截图分析、内容解释）
- 为后续图像理解相关能力（UI 分析、凭证识别、视觉问答）打基础
- 复用现有记忆与 Advisor 链，降低新增能力的接入成本

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 本阶段在该类中新增多模态对话方法：
  - 方法：
    - `doChatWithImage(String message, String chatId, String imagePath)`：通过 `ChatClient` 发送“文本 + 图片”请求，并返回模型文本回复
      - 作用：
        - 使用 `ClassPathResource(imagePath)` 读取项目资源目录中的图片
        - 使用 `media(MimeTypeUtils.IMAGE_PNG, ...)` 将图片作为用户输入的一部分发送给模型
        - 继续复用 `CONVERSATION_ID`，支持同一会话下的多轮图像问答
  - 本阶段相关改动（配套）：
    - 构造方法改为注入 `ChatMemory` Bean（而非固定某个实现）
  - 作用：
    - 让文本/图片对话都能共享统一的记忆配置（file/mysql/redis 可切换）
- `src/test/java/com/kiwi/keweiaiagent/app/LoveAppTest.java`
  - 本阶段新增测试方法：
    - `doChatWithImage()`：验证多模态调用可用，并测试同一图片场景下的连续追问
      - 首次问题：识别图片内容
      - 二次问题：基于上一轮结果继续追问（验证会话连续性）
  - 本阶段测试环境调整：
    - `@ActiveProfiles({"local"})`：指定测试使用本地配置环境
  - 作用：
    - 确认多模态能力已正确接入
    - 验证图像问答场景下仍能沿用已有会话上下文

### 阶段结果

- 项目从纯文本对话扩展为支持图片输入的多模态对话
- 多模态场景已接入统一 `ChatClient`、Advisor 链与记忆体系
- 为后续更复杂的视觉理解与业务化场景拓展提供基础能力

## 11. 新增文档读取与 RAG 检索问答（doChatWithRag）

### 本阶段目标

- 增加恋爱知识库文档读取能力（Markdown 文档）
- 构建向量存储并完成文档向量化入库（内存向量库）
- 在 `LoveApp` 中新增 RAG 问答方法，实现“对话 + 检索增强生成”

### 本阶段价值（为什么做）

- 仅依赖模型通用知识，回答不一定贴合业务场景
- RAG 能把回答约束在自有知识库内容上，提升相关性与可控性
- 为后续接入更大规模知识库、外部文档源、持久化向量库提供实现模板

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 本阶段新增字段：
    - `loveAppVectorStore`（`VectorStore`）：恋爱知识库向量存储实例
  - 本阶段新增方法：
    - `doChatWithRag(String message, String chatId)`：在普通对话流程基础上挂载 `QuestionAnswerAdvisor`，基于向量检索结果生成回答
      - 作用：
        - 复用已有 `ChatClient`、会话记忆与 `CONVERSATION_ID`
        - 在回答前通过 `QuestionAnswerAdvisor` 使用 `loveAppVectorStore` 做检索增强
        - 返回最终模型文本回复
- `src/main/java/com/kiwi/keweiaiagent/rag/LoveAppDocumentLoader.java`
  - 类：`LoveAppDocumentLoader`
  - 方法：
    - `LoveAppDocumentLoader(Resource[] resources)`：注入 `classpath:documents/*.md` 下的知识库文档资源
    - `loadMarkdown()`：读取并解析所有 Markdown 文档为 `Document` 列表
  - 作用：
    - 使用 `MarkdownDocumentReader` 将 Markdown 拆分为可向量化的文档片段
    - 配置读取选项（如分隔策略、是否包含代码块/引用块）
    - 为每个文档片段补充元数据（如 `filename`、`source`），便于后续追踪来源
- `src/main/java/com/kiwi/keweiaiagent/rag/LoveAppVectorStoreConfig.java`
  - 类：`LoveAppVectorStoreConfig`
  - 方法：
    - `loveAppVectorStore(EmbeddingModel ollamaEmbeddingModel)`：创建 `VectorStore` Bean，加载文档并写入向量库
  - 作用：
    - 基于 `EmbeddingModel` 构建 `SimpleVectorStore`
    - 在应用启动阶段完成知识库文档的向量化导入
    - 为 `LoveApp#doChatWithRag(...)` 提供可注入的向量检索能力
- `src/test/java/com/kiwi/keweiaiagent/rag/LoveAppDocumentLoaderTest.java`
  - 类：`LoveAppDocumentLoaderTest`
  - 方法：
    - `loadMarkdown()`：验证 Markdown 文档加载流程可执行
  - 作用：
    - 用测试确认知识库文档可被读取和解析（作为 RAG 前置能力验证）
- `src/test/java/com/kiwi/keweiaiagent/app/LoveAppTest.java`
  - 本阶段新增测试方法：
    - `doChatWithRag()`：验证 `LoveApp#doChatWithRag(...)` 能返回检索增强后的问答结果
  - 作用：
    - 验证 RAG 问答入口已经接入成功（检索 + 生成链路打通）

### 阶段结果

- 项目具备了基础 RAG 能力（文档读取、向量化、检索增强问答）
- `LoveApp` 新增面向业务的 RAG 问答方法，可直接用于知识库问答场景
- 为后续扩展文档类型、向量数据库和检索策略打下基础

## 12. 为 pgvector + RAG 实操做准备（多 AI Bean 优先级 + 多数据源拆分 + 配置化开启）

### 本阶段目标

- 在配置层加入 pgvector 相关参数，为后续接入 PostgreSQL/pgvector 向量库做准备
- 解决同时引入多个 AI Provider（Ollama + Spring AI Alibaba）导致的 Bean 选择冲突
- 在存在多个数据源（PostgreSQL + MySQL）时，为 ChatMemory 单独拆分 MySQL 数据源配置
- 通过条件配置动态开启相关组件，避免不同环境相互影响

### 本阶段价值（为什么做）

- 为后续 `pgvector + RAG` 实战提前清理基础设施问题，降低接入复杂度
- 多 AI Provider 并存时明确默认 `ChatModel / EmbeddingModel`，避免自动装配歧义
- 多数据源隔离后，业务主数据源与 ChatMemory 数据源职责更清晰，更易维护
- 条件化配置能够提升项目在本地/测试/不同部署环境下的稳定性

### 主要新增/涉及文件

- `src/main/resources/application.yml`
  - 本阶段新增/调整：
    - 增加 `spring.ai.vectorstore.pgvector` 相关配置项（如索引类型、距离类型、维度、批量大小等）
  - 作用：
    - 为后续切换到 `pgvector` 向量存储提供预设配置入口
    - 让 RAG 能力从 `SimpleVectorStore` 向持久化向量库演进时更顺滑
- `src/main/resources/application-local.yml`
  - 本阶段新增/调整（本地环境配置）：
    - PostgreSQL 数据源配置（用于主数据源 / pgvector 场景准备）
    - Redis 连接配置
    - `app.chat-memory.type` 及 `file/mysql/redis` 切换相关参数
    - `app.datasource.mysql.*`（ChatMemory 专用 MySQL 数据源配置）
  - 作用：
    - 在本地环境集中管理多数据源和记忆方式切换参数
    - 支持通过配置动态启用对应组件
- `src/main/java/com/kiwi/keweiaiagent/config/AiModelPrimaryConfig.java`
  - 类：`AiModelPrimaryConfig`
  - 方法：
    - `ollamaAsPrimaryBeanFactoryPostProcessor()`：通过 `BeanFactoryPostProcessor` 在 Bean 定义阶段将 `ollamaChatModel` 与 `ollamaEmbeddingModel` 标记为 `primary`
  - 作用：
    - 解决同时引入 Ollama 与 DashScope（Spring AI Alibaba）后出现的默认 Bean 选择冲突
    - 保证依赖 `ChatModel` / `EmbeddingModel` 的自动配置（尤其向量存储相关）优先使用 Ollama
- `src/main/java/com/kiwi/keweiaiagent/config/ChatMemoryMySqlDataSourceConfig.java`
  - 类：`ChatMemoryMySqlDataSourceConfig`
  - 条件：
    - `app.chat-memory.type=mysql`
    - `app.datasource.mysql.url` 存在
  - 方法：
    - `chatMemoryMySqlDataSourceProperties()`：加载 `app.datasource.mysql` 配置
    - `chatMemoryMySqlDataSource(...)`：创建 ChatMemory 专用 MySQL 数据源
    - `mysqlChatMemoryJdbcTemplate(...)`：创建 ChatMemory 专用 `JdbcTemplate`
    - `chatMemorySqlSessionFactory(...)`：创建 ChatMemory 专用 MyBatis `SqlSessionFactory`
    - `chatMemorySqlSessionTemplate(...)`：创建 ChatMemory 专用 `SqlSessionTemplate`
  - 作用：
    - 将 ChatMemory 的 MySQL 访问能力从主数据源中拆分出来
    - 配合专用 `@MapperScan`，确保 ChatMemory 的 Mapper 绑定到正确数据源
- `src/main/java/com/kiwi/keweiaiagent/chatmemory/ChatMemoryTableInitializer.java`
  - 类：`ChatMemoryTableInitializer`（本阶段增强）
  - 方法：
    - `init()`：继续负责 MySQL 记忆表自动建表
  - 本阶段改动重点：
    - 增加 `@ConditionalOnBean(name = "mysqlChatMemoryJdbcTemplate")`
    - 通过 `@Qualifier("mysqlChatMemoryJdbcTemplate")` 绑定专用 `JdbcTemplate`
  - 作用：
    - 避免多数据源场景下误用主数据源执行 ChatMemory 建表 SQL
    - 仅在 ChatMemory MySQL 组件已正确装配时才执行初始化
- `src/main/java/com/kiwi/keweiaiagent/chatmemory/mapper/ChatMemoryMessageMapper.java`
  - 接口：`ChatMemoryMessageMapper`
  - 本阶段改动重点：
    - 移除 `@Mapper`（由 `ChatMemoryMySqlDataSourceConfig` 中的专用 `@MapperScan` 统一接管）
  - 作用：
    - 避免 Mapper 被默认数据源错误扫描/绑定
    - 确保 ChatMemory Mapper 明确走 ChatMemory 专用 MySQL 配置链路

### 踩坑记录

- 问题 1：同时引入 Ollama 与 Spring AI Alibaba 后，出现多个 AI Bean 候选（`ChatModel` / `EmbeddingModel`）
  - 现象：
    - 自动装配在某些场景下无法稳定判断该使用哪个模型 Bean（尤其向量存储相关配置会更早参与装配）
  - 原因：
    - 类路径上同时存在多个 Provider，导致同类型 Bean 数量 > 1
  - 处理方式：
    - 新增 `AiModelPrimaryConfig`
    - 在 Bean 定义阶段将 `ollamaChatModel`、`ollamaEmbeddingModel` 标记为 `primary`
  - 收获：
    - 不需要删除其他 Starter，后续切换默认 Provider 也只需调整配置类中的 bean 名称

- 问题 2：项目同时存在 PostgreSQL（主数据源 / pgvector）和 MySQL（ChatMemory），Mapper/JdbcTemplate 容易绑错
  - 现象：
    - ChatMemory 建表或 Mapper 扫描可能误走主数据源
    - 不同环境下若某个数据源配置缺失，容易触发无效装配或启动异常
  - 原因：
    - 多数据源场景下默认自动装配行为不够明确，缺少专用限定与条件启用
  - 处理方式：
    - 新增 `ChatMemoryMySqlDataSourceConfig`，为 ChatMemory 单独配置 MySQL `DataSource` / `JdbcTemplate` / MyBatis `SqlSessionFactory`
    - `ChatMemoryTableInitializer` 使用 `@Qualifier("mysqlChatMemoryJdbcTemplate")` 并增加 `@ConditionalOnBean`
    - `ChatMemoryMessageMapper` 移除 `@Mapper`，改由专用 `@MapperScan` 托管
    - 通过 `@ConditionalOnProperty` 实现按 `app.chat-memory.type` 动态开启
  - 收获：
    - 主数据源与 ChatMemory 数据源职责明确，配置切换时更稳定
    - 为第十三步接入 `pgvector` 主数据源和 `JdbcTemplate` 优先级处理打下基础

### 阶段结果

- 已完成 `pgvector + RAG` 实操前的关键配置准备工作
- 多 AI Provider 并存的默认模型选择问题得到处理
- 多数据源场景下的 ChatMemory MySQL 配置已独立并支持条件启用
- 项目在复杂配置场景下的可维护性与可扩展性进一步提升

## 13. 添加 PgVectorPrimaryDataSourceConfig，跑通 pgvector + RAG 检索（含踩坑记录）

### 本阶段目标

- 为 PgVectorStore 提供明确的 PostgreSQL 主数据源与 `JdbcTemplate`
- 避免 PgVector 与 ChatMemory（MySQL）在多数据源场景下混用 `JdbcTemplate`
- 在测试中验证 `pgvector` 向量写入、相似度检索以及 `LoveApp` 的 RAG 问答链路可用

### 本阶段价值（为什么做）

- `pgvector` 落地后，RAG 从内存向量库演进到数据库向量库，结果更贴近实际部署形态
- 明确数据源优先级可以减少自动装配误判导致的问题，提升稳定性
- 通过测试跑通“向量写入 + 检索 + RAG问答”，为后续迭代提供可靠基线

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/config/PgVectorPrimaryDataSourceConfig.java`
  - 类：`PgVectorPrimaryDataSourceConfig`
  - 条件：
    - `spring.datasource.url` 存在时启用
  - 方法：
    - `dataSourceProperties()`：读取 `spring.datasource` 配置并声明为 `@Primary`
    - `dataSource(...)`：创建 PostgreSQL 主数据源并声明为 `@Primary`
    - `jdbcTemplate(...)`：创建 PostgreSQL 主 `JdbcTemplate` 并声明为 `@Primary`
  - 作用：
    - 为 PgVectorStore 提供明确的 PostgreSQL JDBC 访问入口
    - 与 ChatMemory 的 MySQL 专用数据源配置形成职责分离（通过 `@Primary` + `@Qualifier` 区分）
- `src/test/java/com/kiwi/keweiaiagent/app/LoveAppTest.java`
  - 本阶段新增/增强测试内容：
    - `testVectorStore()`：直接使用 `PgVectorStore` 写入文档并执行相似度检索，验证 pgvector 链路可用
    - `doChatWithRag()`：在当前配置下验证基于 RAG 的问答能力继续可用（作为 pgvector + RAG 的业务入口验证）
  - 相关注入：
    - `@Qualifier("vectorStore") private PgVectorStore pgVectorStore`：显式注入 PgVector 实现
  - 作用：
    - 验证底层向量存储能力（增/检）
    - 验证上层业务问答入口在 pgvector 场景下可正常工作
- `src/main/resources/application.yml`
  - 本阶段新增/调整：
    - Ollama `embedding` 模型配置调整为 `mxbai-embed-large:latest`
    - `spring.ai.vectorstore.pgvector.dimensions` 调整为 `1024`
    - 保留 `index-type: HNSW` 配置并使其与 embedding 维度兼容
  - 作用：
    - 解决 embedding 维度与 pgvector HNSW 索引限制不兼容问题
    - 为 pgvector 检索性能与可用性提供稳定配置基础

### 踩坑记录

- 问题 1：PgVector 也依赖 JDBC，但项目里已经存在 MySQL 的 `JdbcTemplate`
  - 现象：
    - 自动装配可能优先拿到 MySQL 的 `JdbcTemplate`，导致 PgVector 相关组件走错数据源
  - 原因：
    - 多数据源场景下未明确 PostgreSQL 数据源/JdbcTemplate 的主优先级
  - 处理方式：
    - 新增 `PgVectorPrimaryDataSourceConfig`
    - 将 PostgreSQL 的 `DataSourceProperties` / `DataSource` / `JdbcTemplate` 标记为 `@Primary`
    - 同时让 ChatMemory 继续通过专用 `@Qualifier` 使用 MySQL 数据源

- 问题 2：Embedding 模型维度与 `HNSW` 索引限制不兼容
  - 现象：
    - 之前使用的 embedding 模型维度为 `4096`，而当前 `index-type: HNSW` 场景下最多支持约 `2000` 列（维度）
  - 可选方案：
    - 去掉索引（不使用 HNSW）
    - 更换 embedding 模型（降低维度）
  - 最终处理：
    - 将 embedding 模型切换为 `mxbai-embed-large`
    - 对应维度调整为 `1024`
    - 保留 `HNSW` 索引配置，最终跑通 pgvector 检索

### 阶段结果

- 已完成 PgVector 使用的 PostgreSQL 主数据源与 `JdbcTemplate` 配置
- 多数据源场景下 PgVector（PostgreSQL）与 ChatMemory（MySQL）职责分离更清晰
- `LoveAppTest` 已验证 pgvector 向量检索与 RAG 问答链路可用
- `pgvector + RAG` 实操链路已成功跑通

## 14. 自定义 RAG 增强链路（查询预处理 + 状态过滤检索 + 文档关键词增强）

### 本阶段目标

- 升级 `LoveApp#doChatWithRag`，支持可选查询改写与 PgVector 检索增强
- 新增应用级自定义 RAG 工厂类，支持按用户状态过滤知识库内容
- 新增查询预处理工具，覆盖重写、压缩、多查询扩展
- 新增 PgVector 文档增量加载配置，引入关键词增强并按内容哈希去重

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 本阶段改动：
    - `doChatWithRag` 方法签名升级为 `doChatWithRag(String query, String chatId, boolean withQueryReform)`
    - 接入 `QueryPreprocessor`，在 `withQueryReform=true` 时先执行查询重写
    - RAG 检索基于 `PgVector` 对应 `vectorStore` 执行（`QuestionAnswerAdvisor`）
  - 作用：
    - 提供“是否启用查询改写”的开关，增强复杂查询召回能力
    - 让业务 RAG 问答链路与 PgVector 保持一致
- `src/main/java/com/kiwi/keweiaiagent/rag/factory/loveapp/LoveAppContextualQueryAugmenterFactory.java`
  - 类：`LoveAppContextualQueryAugmenterFactory`
  - 方法：
    - `createInstance()`：创建 `ContextualQueryAugmenter`，并配置空上下文兜底提示模板
  - 作用：
    - 当检索不到上下文时输出可控提示，避免模型自由发挥偏题
- `src/main/java/com/kiwi/keweiaiagent/rag/factory/loveapp/LoveAppRetrievalAugmentationAdvisorFactory.java`
  - 类：`LoveAppRetrievalAugmentationAdvisorFactory`
  - 方法：
    - `createLoveAppRagCustomAdvisor(VectorStore vectorStore, String status)`：构建支持 `status` 过滤、阈值与 `topK` 的自定义 `RetrievalAugmentationAdvisor`
  - 作用：
    - 基于应用状态（如单身/恋爱/已婚）过滤检索文档，提高回答相关性
    - 将检索增强策略模块化，便于后续按场景扩展
- `src/main/java/com/kiwi/keweiaiagent/rag/MyKeywordEnricher.java`
  - 类：`MyKeywordEnricher`
  - 方法：
    - `enrichDocument(List<Document> documents)`：使用 `KeywordMetadataEnricher` 为文档补充关键词元数据
  - 作用：
    - 在文档入库前增加语义标签，提升后续检索质量
- `src/main/java/com/kiwi/keweiaiagent/query/QueryPreprocessor.java`
  - 类：`QueryPreprocessor`
  - 方法：
    - `rewriteQueryTransform(Query query)`：查询重写转换器
    - `compressionQueryTransform(Query query)`：查询压缩转换器
    - `multiQueryExpand(Query query)`：多查询扩展器（默认扩展 3 条）
  - 作用：
    - 在检索前优化用户问题表达，提高召回率和检索稳定性
- `src/main/java/com/kiwi/keweiaiagent/rag/PgVectorVectorLoadMarkdownConfig.java`
  - 类：`PgVectorVectorLoadMarkdownConfig`
  - 方法：
    - `pgVectorVectorStoreConfig()`：应用启动时执行 Markdown 文档加载、去重判断、关键词增强、向量写入
    - `existsByContentHash(String contentHash)`：按 `content_hash` 判断文档是否已入库
    - `sanitizeTableName(String configuredTableName)`：校验 pgvector 表名安全性
    - `extractDocumentText(Document document)`：兼容不同 Spring AI 版本提取文档文本
    - `sha256Hex(String value)`：生成内容哈希用于去重
  - 作用：
    - 保证知识库文档向量化加载可重复执行且不重复入库
    - 在 PgVector 入库链路中加入关键词增强与安全检查
- `src/test/java/com/kiwi/keweiaiagent/app/LoveAppTest.java`
  - 本阶段新增/调整测试：
    - `doChatWithRag()`：验证普通 RAG 问答（不启用查询改写）
    - `doChatWithRagWithQueryReform()`：验证开启查询改写后的 RAG 问答
  - 作用：
    - 确认 `doChatWithRag` 新参数分支行为可用
    - 验证查询预处理与 RAG 组合链路可运行

### 阶段结果

- `LoveApp` 已支持可选查询改写的 RAG 问答入口
- 已具备应用级自定义检索增强工厂能力（空上下文兜底 + 状态过滤检索）
- 已建立 PgVector 文档增量入库链路（内容哈希去重 + 关键词增强）
- 查询预处理能力（重写/压缩/扩展）可用于持续优化 RAG 召回效果

## 15. 增加 Tools 调用能力（文件/网页/下载/PDF）并接入 LoveApp

### 本阶段目标

- 在 `LoveApp` 中增加工具调用入口，支持模型按需调用外部工具
- 新增一批可复用工具类，覆盖文件操作、网页搜索/抓取、资源下载、PDF 转图
- 补齐工具能力测试与测试环境配置，提升可回归性

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 本阶段新增：
    - 字段 `ToolCallback[] allTools`
    - 方法 `doChatWithTools(String message, String chatId)`
  - 作用：
    - 通过 `chatClient.prompt()...toolCallbacks(allTools)...call()` 挂载工具能力
    - 让对话在保留会话记忆的同时，触发工具执行并返回工具结果
- `src/main/java/com/kiwi/keweiaiagent/tools/ToolRegistration.java`
  - 类：`ToolRegistration`
  - 方法：
    - `allTools()`：统一返回工具回调数组（`ToolCallback[]`）
  - 作用：
    - 集中维护工具注册入口，避免在业务类中分散装配
- `src/main/java/com/kiwi/keweiaiagent/tools/FileOperationTool.java`
  - 方法：
    - `readFile(String fileName)`：读取文件内容
    - `writeFile(String fileName, String content)`：写入文件内容
  - 作用：
    - 提供本地文件读写能力给模型调用
- `src/main/java/com/kiwi/keweiaiagent/tools/WebSearchTool.java`
  - 方法：
    - `searchWebsite(...)`：调用 SearchAPI 进行网页检索并输出简要结果
    - `resolveApiKey()`：解析 API Key 来源（配置/环境变量/系统参数）
    - `toConciseResult(...)`：将原始检索结果转换为简明文本
  - 作用：
    - 为模型提供联网搜索能力
- `src/main/java/com/kiwi/keweiaiagent/tools/WebScrapingTool.java`
  - 方法：
    - `scrapeWebsite(...)`：抓取网页标题、描述、正文与链接
    - `buildScrapeResult(...)`：构建标准化抓取结果
  - 作用：
    - 为模型提供网页内容抽取能力
- `src/main/java/com/kiwi/keweiaiagent/tools/ResourceDownloadTool.java`
  - 方法：
    - `downloadResource(String url, String fileName)`：下载资源到本地目录
  - 作用：
    - 支持模型触发文件下载操作
- `src/main/java/com/kiwi/keweiaiagent/tools/PdfConvertTool.java`
  - 方法：
    - `batchConvertPdfToJpg(String pdfDirectoryPath, Integer dpi)`：批量将目录内 PDF 转换为 JPG
  - 作用：
    - 为模型提供批量文档转图能力
- `src/main/java/com/kiwi/keweiaiagent/constant/FileConstant.java`
  - 常量：
    - `File_SAVE_DIR`：工具输出基础目录（`${user.dir}/tmp`）
  - 作用：
    - 统一工具类文件落盘目录
- `src/test/java/com/kiwi/keweiaiagent/app/LoveAppTest.java`
  - 本阶段新增测试：
    - `doChatWithTools()`：验证工具调用对话链路
  - 作用：
    - 验证 `LoveApp` 与工具注册的集成效果
- `src/test/java/com/kiwi/keweiaiagent/tools/*.java`
  - 本阶段新增测试：
    - `FileOperationToolTest`
    - `WebSearchToolTest`
    - `WebScrapingToolTest`
    - `ResourceDownloadToolTest`
    - `PdfConvertToolTest`
  - 作用：
    - 分别验证工具参数校验、结果结构和基础能力
- `src/test/resources/application-test.yml`
  - 本阶段新增：
    - 测试环境自动装配精简（排除部分数据库/向量库/Provider 自动配置）
  - 作用：
    - 降低测试环境对外部依赖的耦合，提高测试稳定性

### 踩坑记录

- 问题 1：DeepSeek 不支持 tools 调用方式
  - 现象：
    - 在 DeepSeek 模型下启用 tools 能力会报错
  - 原因：
    - 模型侧对 tools 调用能力支持不完整/不兼容
  - 处理方式：
    - 切换为支持工具调用（tools/function calling）的模型

- 问题 2：Spring AI 新版本下 `.tools(allTools)` 会报错（不限模型）
  - 现象：
    - 在当前升级后的 Spring AI 版本中，`.tools(allTools)` 统一报错
    - `spring-ai 1.0.0` 版本中该写法可用
  - 原因：
    - Spring AI 工具调用 API 在版本升级后发生变化，工具注入方式调整
  - 处理方式：
    - 统一改为 `.toolCallbacks(allTools)` 调用
  - 收获：
    - 工具调用链路与当前 Spring AI 版本保持一致，兼容性更稳定

### 阶段结果

- `LoveApp` 已具备工具调用能力，支持模型触发实际外部操作
- 工具模块已形成可扩展结构（统一注册 + 独立工具类 + 对应测试）
- 多轮对话、RAG 与工具能力可以在同一应用内协同演进

## 当前里程碑总结

- 基础工程与运行环境已搭建完成
- 模型接入已覆盖阿里百炼与 Ollama 两种路径
- `ChatClient` 已完成业务封装，并具备会话记忆能力
- 接口层已具备统一响应与全局异常处理能力
- Advisor 链已支持自定义增强与共享上下文
- 已开始引入结构化输出思路，为后续业务编排和扩展做准备
- 会话记忆已向本地持久化方向演进（文件存储）
- 会话记忆已扩展到 MySQL 持久化，并支持配置化切换存储实现
- 会话记忆已新增 Redis 实现，并形成 `file/mysql/redis` 三种可切换方案
- 已支持图片输入的多模态对话能力（`doChatWithImage`）
- 已具备基础 RAG 能力（文档读取、向量化入库、检索增强问答）
- 已完成 `pgvector + RAG` 实操前的配置治理（AI Bean 优先级、多数据源拆分、条件化启用）
- 已跑通 `pgvector + RAG` 检索与问答链路（含多数据源/JDBC 冲突处理与维度适配）
- 已引入自定义 RAG 增强链路（查询预处理、状态过滤检索、关键词增强、增量入库）
- 已完成工具调用能力接入（文件、搜索、抓取、下载、PDF 转图）并验证基础测试链路

## 后续进度补充方式（约定）

后续你在这个对话里继续发我“第 X 步完成了什么”，我会按同样格式补充：

- 步骤目标
- 主要新增/修改文件
- 每个文件的类与方法职责
- 阶段结果与下一步衔接
