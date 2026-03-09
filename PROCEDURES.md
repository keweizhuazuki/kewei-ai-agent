# kewei-ai-agent
## author: Kewei
这是一个基于 Spring Boot + Spring AI 的 AI Agent 学习与实践项目。

这份 README 不打算写成宣传文案，更像一份项目工作日志：
- 记录这个项目是怎么一步步搭起来的
- 说明关键目录、核心类和主要方法各自负责什么
- 方便以后自己回头看，也方便别人快速接手

说明：
- 这份文档是根据当前仓库状态回填整理出来的
- 不放大段代码，重点讲实现思路、结构变化和阶段成果

## 项目目标（当前阶段）

目前这个项目的目标，不是做一个“能回一句话”的聊天 demo，而是逐步搭出一套比较完整的 AI Agent 实践骨架。重点包括：

- 用 Spring Boot 搭起基础运行环境
- 接入阿里百炼和 Ollama 的 `ChatModel`
- 用 `ChatClient` 封装文本、多模态等对话能力
- 把接口响应、异常处理、Advisor 链这些基础设施先打稳
- 支持结构化输出（JSON -> Java 对象）
  支持多种会话记忆存储方案（`file` / `mysql` / `redis`）并通过 yml 切换
- 支持基础 RAG（Markdown 文档加载、向量化、检索增强问答）
- 为 `pgvector + RAG` 实操完成配置准备（含多数据源与 AI Bean 冲突处理）
- 跑通 `pgvector + RAG` 检索链路（含 PgVector 数据源区分与测试验证）
- 增强自定义 RAG 能力（查询预处理、关键词增强、按用户状态过滤检索）
- 增加工具调用（Tools）能力，支持文件读写、网页搜索/抓取、资源下载、PDF 转图
- 持续扩展工具能力（邮件发送、时间查询），完善工具注册与测试覆盖
- 接入 MCP 客户端与独立 MCP 工具服务（图像搜索 / 图像生成），支持跨服务工具编排
- 引入多步 Agent 执行框架（ReAct + Tool Calling + 终止机制）并开放控制器接口
- 增加图片上传与图片对话处理链路，支持基于文件路径的同步与流式图像聊天
- 新增前端工程 `kewei-ai-agent-frontend`，用于承载应用界面与前后端联调
- 升级到 Spring AI `2.0.0-M2`，补齐 Skills 模式、AskUserQuestion 两段式交互、PPT 生成能力与 Manus 分域执行链路
- 新增 TodoWrite 任务规划能力，支持独立 demo 与 Manus 执行中的 todo 进度输出
- 增加 Spring AI 主控 + OpenClaw 调研执行代理协作链路，支持将网页调研任务委派给独立 Agent 返回结构化结果

## 目录结构（当前）

- `pom.xml`
  - Maven 依赖管理与构建配置（Spring Boot、Spring AI、DashScope、Ollama、校验、文档等）
- `src/main/java/com/kiwi/keweiaiagent`
  - `KeweiAiAgentApplication`：Spring Boot 启动入口
- `src/main/java/com/kiwi/keweiaiagent/app`
  - `LoveApp`：对话业务封装（文本对话、结构化输出、多模态、图片流式、RAG、Tools、MCP、记忆、Advisor 链）
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
  - 包含图片上传目录常量 `IMAGE_UPLOAD_DIR`
- `src/main/java/com/kiwi/keweiaiagent/config`
  - `AiModelPrimaryConfig`：指定默认 AI 模型/Embedding Bean 优先级（解决多 Provider 并存冲突）
  - `ChatMemoryConfig`：会话记忆实现装配与切换配置（file/mysql/redis）
  - `ChatMemoryMySqlDataSourceConfig`：ChatMemory 专用 MySQL 数据源与 MyBatis 配置
  - `JacksonCompatibilityConfig`：补充 `ObjectMapper` 配置，兼容新版 Jackson / Spring AI 的序列化与 SSE JSON 场景
  - `PgVectorPrimaryDataSourceConfig`：PgVector 使用的 PostgreSQL 主数据源与 `JdbcTemplate` 配置
  - `GlobalResponseBodyAdvice`：统一响应包装
  - `GlobalCorsConfig`：全局跨域配置
- `src/main/java/com/kiwi/keweiaiagent/controller`
  - `HealController`：健康检查接口
  - `AiController`：统一 AI 能力入口（同步/流式/SSE/图片上传/Agent/MCP）
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
  - `TodoWriteToolAdapter`：适配社区版 `TodoWriteTool` 与 Spring AI 当前参数绑定行为
  - `TerminateTool`：Agent 终止工具（用于多步任务结束信号）
  - `AskQuestionTool`：将控制台提问改造为 Web 可恢复的用户追问工具
  - `OpenClawResearchTool`：通过 `openclaw agent --json` 把网页调研任务委派给 OpenClaw，并回收最终文本结果
  - `OpenClawCommandRunner` / `ShellOpenClawCommandRunner`：封装 OpenClaw CLI 子进程调用与超时控制
  - `PptWriterTool`：根据结构化幻灯片描述在本地生成 `.pptx` 文件
  - `EmailTool`：邮件发送工具（SMTP）
  - `TimeTool`：时间查询工具（支持时区）
  - `FileOperationTool`：文件读取/写入工具
  - `WebSearchTool`：联网搜索工具（SearchAPI）
  - `WebScrapingTool`：网页抓取与摘要工具
  - `ResourceDownloadTool`：资源下载工具
  - `PdfConvertTool`：批量 PDF 转 JPG 工具
- `src/main/resources/.claude/skills`
  - `ppt-writer/SKILL.md`：PPT 制作 skill 说明，约束澄清问题、内容规划、文件输出规则
  - `ppt-writer/assets/default-theme-notes.md`：PPT 默认主题与内容组织说明
- `src/main/resources`
  - `application.yml`：应用、端口、Profile、AI 模型、pgvector 预配置与接口文档配置
  - `application-local.yml`：本地环境数据源/Redis/ChatMemory 等配置（含动态切换参数）
  - 新增 `openclaw.agent.*` 配置：用于声明 OpenClaw CLI 命令、默认 agent、超时和研究会话前缀
  - `static/images`：多模态与工具调用相关图片资源（如 `test.png`、`couple.png`）
- `src/test/java/com/kiwi/keweiaiagent`
  - 应用启动测试、`LoveApp` 对话能力测试（文本 / 结构化 / 多模态 / RAG / Tools）
- `src/test/java/com/kiwi/keweiaiagent/rag`
  - `LoveAppDocumentLoaderTest`：知识库 Markdown 文档加载测试
- `src/test/java/com/kiwi/keweiaiagent/agent`
  - Agent 能力测试（`BaseAgentTest`、`ToolCallAgentTest`、`KeweiManusTest`）
- `src/test/java/com/kiwi/keweiaiagent/controller`
  - `AiControllerTest`：流式与 SSE 接口测试
- `src/test/java/com/kiwi/keweiaiagent/tools`
  - 工具能力测试（文件工具、搜索工具、抓取工具、下载工具、PDF 转换工具）
- `src/main/java/com/kiwi/keweiaiagent/agent`
  - `BaseAgent`：Agent 执行生命周期抽象（run/runStream/step/状态管理）
  - `ReActAgent`：ReAct 抽象层（think + act）
  - `ToolCallAgent`：工具调用型 Agent 实现
  - `KeweiManus`：应用级多工具 Agent
  - `ManusSessionStore`：保存 Manus 会话、中间问题和用户补充答案
  - `ManusSessionService`：负责 Manus 启动、续跑、任务分域与工具子集选择
  - `PendingUserQuestionException`：用户补充信息中断信号，用于触发 `question` SSE 事件
  - 运行中新增 `todo` SSE 事件：向前端输出最新 todo 列表快照
- `src/main/java/com/kiwi/keweiaiagent/agent/todo`
  - `TodoItem`：前端消费用的单条 todo 数据结构
  - `TodoSnapshot`：当前任务 todo 列表快照
  - `CommunityTodoMapper`：把社区版 `TodoWriteTool` 的数据结构映射为本项目快照结构
- `src/main/java/com/kiwi/keweiaiagent/app`
  - `TodoDemoApp`：最小 TodoWrite 演示入口，验证复杂任务先拆 todo 再执行
- `src/main/java/com/kiwi/keweiaiagent/agent/model`
  - `AgentState`：Agent 状态枚举（IDLE/RUNNING/WAITING_FOR_USER_INPUT/FINISHED/ERROR）
- `src/test/resources`
  - `application-test.yml`：测试环境专用配置（简化自动装配，避免非必要外部依赖）
- `kewei-image-search-mcp-server`
  - 独立 MCP 服务工程：提供 `ImageSearchTool`（基于 Pexels 的图片搜索）
  - 含 `application.yml` / `application-sse.yml` / `application-stdio.yml` 与对应测试
- `kewei-image-generation-mcp-server`
  - 独立 MCP 服务工程：提供 `ImageGenerationTool`（基于 Draw Things 的文生图）
  - 含 `application.yml` / `application-sse.yml` / `application-stdio.yml` 与对应测试
- `kewei-ai-agent-frontend`
  - 独立前端工程：用于承载对话界面、图片上传交互、流式输出展示以及前后端联调
  - 当前已承接 Manus 的 `question` 事件消费、补充信息表单提交与流式续跑交互

## TodoWrite 使用方式（当前实现）

- 最小 demo：`GET /ai/love_app/chat/sse_emitter?option=todo-demo&chatId=demo-1&message=帮我拆解一个三步任务`
- Manus 集成：`GET /ai/manus/chat?chatId=manus-1&message=帮我完成一个复杂任务`
- 当模型调用 `TodoWrite` 后，Manus SSE 流会增加 `event:todo`，数据体中包含当前 todo 快照

## OpenClaw 调研代理（当前实现）

- 默认调研主链路采用“Spring AI 主控 + OpenClaw 执行代理”分层：
  - Spring AI / Manus 负责任务拆解、工具路由、结果汇总
  - OpenClaw 负责网页调研执行，并通过 `openclaw agent --json` 返回最终结果
- 当前 PPT 与 research 类任务默认注入 `delegateResearchToOpenClaw`
- 本地 `WebSearchTool` / `WebScrapingTool` 仍保留实现和测试，但不再作为默认调研主工具集

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

## 16. 扩展工具能力（EmailTool / TimeTool）并完善工具注册方式

### 本阶段目标

- 在现有工具体系上新增“邮件发送”和“时间查询”能力
- 优化 `ToolRegistration`，由“手动 new 工具对象”调整为“基于 Spring Bean 注入统一注册”
- 补充新增工具对应测试，保障工具扩展后的可用性

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/tools/EmailTool.java`
  - 类：`EmailTool`
  - 方法：
    - `sendEmail(String to, String subject, String content, Boolean html)`：发送邮件（支持纯文本/HTML）
    - `readConfig(...)`：按优先级读取 SMTP 配置（Spring 配置、系统参数、环境变量）
    - `parsePortOrDefault(...)`：解析端口并提供默认值
    - `parseBooleanOrDefault(...)`：解析布尔配置并提供默认值
    - `maskPresent(...)`：返回配置项是否存在的状态文本
  - 作用：
    - 为 Agent 提供 SMTP 邮件发送能力
    - 对参数和配置进行前置校验，降低误调用风险
- `src/main/java/com/kiwi/keweiaiagent/tools/TimeTool.java`
  - 类：`TimeTool`
  - 方法：
    - `getCurrentDateTime(String zoneId)`：按指定时区（或系统默认时区）返回当前日期、时间、时间戳等信息
  - 作用：
    - 提供可被模型调用的时间查询能力
    - 支持 IANA 时区 ID，便于跨时区场景
- `src/main/java/com/kiwi/keweiaiagent/tools/ToolRegistration.java`
  - 类：`ToolRegistration`（本阶段增强）
  - 方法：
    - `allTools(...)`：通过 Spring 注入的工具 Bean 统一组装 `ToolCallback[]`
  - 本阶段改动重点：
    - 从 `new XxxTool()` 改为方法参数注入（`EmailTool`、`TimeTool`、`FileOperationTool`、`PdfConvertTool`、`ResourceDownloadTool`、`WebSearchTool`、`WebScrapingTool`）
  - 作用：
    - 保证工具实例走 Spring 生命周期与配置注入链路
    - 让工具扩展更可维护，避免手工实例化带来的配置失效问题
- `src/test/java/com/kiwi/keweiaiagent/tools/EmailToolTest.java`
  - 方法：
    - `shouldReturnErrorWhenReceiverMissing()`
    - `shouldReturnErrorWhenReceiverInvalid()`
  - 作用：
    - 验证邮件工具核心参数校验逻辑
- `src/test/java/com/kiwi/keweiaiagent/tools/EmailToolManualTest.java`
  - 方法：
    - `sendEmailManually()`（默认 `@Disabled`）
  - 作用：
    - 提供手动联调入口，避免自动化测试误发邮件
- `src/test/java/com/kiwi/keweiaiagent/tools/TimeToolTest.java`
  - 方法：
    - `shouldReturnCurrentDateTimeWithDefaultZone()`
    - `shouldReturnErrorWhenZoneInvalid()`
  - 作用：
    - 验证时间工具在默认时区和非法时区输入下的行为

### 阶段结果

- 工具能力从“文件/网页/下载/PDF”扩展到“邮件/时间”场景
- `ToolRegistration` 已切换到 Spring Bean 注入式注册，扩展性和稳定性更高
- 新增工具具备基础测试保障，后续可继续按同一模式快速扩展工具集

## 17. 接入 MCP 图像能力（图片搜索 + 图片生成）并在 LoveApp 打通调用链路

### 本阶段目标

- 在主应用中接入 `spring-ai-starter-mcp-client`，支持通过 MCP 调用外部工具服务
- 新增两个独立 MCP 服务工程：
  - 图片搜索 MCP 服务（Pexels）
  - 图片生成 MCP 服务（Draw Things）
- 在 `LoveApp` 中增加 MCP 对话入口，完成端到端调用与测试验证

### 本阶段价值（为什么做）

- 将重工具型能力从主应用拆分为独立 MCP 服务，降低主应用耦合
- 便于后续独立扩展、部署和维护图像相关能力
- 为后续接入更多第三方能力（地图、日历、知识平台等）提供标准化接入模式

### 主要新增/涉及文件

- `pom.xml`
  - 本阶段新增：
    - `spring-ai-starter-mcp-client`
  - 作用：
    - 让主应用具备 MCP 客户端能力，可通过 `ToolCallbackProvider` 调用外部 MCP 工具
- `src/main/resources/application.yml`
  - 本阶段新增/调整：
    - 增加 `spring.ai.mcp.client.sse.connections.*` 与 `request-timeout` 等配置
  - 作用：
    - 配置主应用与 MCP 服务的连接方式（SSE）
    - 控制 MCP 请求超时与连接行为
- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 本阶段新增：
    - 字段：`ToolCallbackProvider toolCallbackProvider`
    - 方法：`doChatWithMCP(String message, String chatId)`
  - 作用：
    - 在对话链路中通过 `.toolCallbacks(toolCallbackProvider)` 调用 MCP 工具
    - 在保留会话上下文的同时，将图像类任务委托给外部 MCP 服务
- `src/test/java/com/kiwi/keweiaiagent/app/LoveAppTest.java`
  - 本阶段新增测试：
    - `doChatWithMCP()`
  - 作用：
    - 验证 MCP 工具调用链路可用
    - 对图片生成场景校验返回路径是否存在且文件非空

- `kewei-image-search-mcp-server/src/main/java/com/kiwi/keweiimagesearchmcpserver/KeweiImageSearchMcpServerApplication.java`
  - 类：`KeweiImageSearchMcpServerApplication`
  - 方法：
    - `imageSearchTools(ImageSearchTool imageSearchTool)`：注册 MCP 工具提供器
  - 作用：
    - 暴露图片搜索工具为 MCP 可调用能力
- `kewei-image-search-mcp-server/src/main/java/com/kiwi/keweiimagesearchmcpserver/tools/ImageSearchTool.java`
  - 类：`ImageSearchTool`
  - 方法：
    - `searchImage(String query)`：按关键词调用 Pexels API 并返回图片 URL 列表
    - `resolveApiKey()`：读取 API Key（配置或环境变量）
  - 作用：
    - 提供独立可复用的图片搜索能力
- `kewei-image-search-mcp-server/src/main/resources/application.yml`
- `kewei-image-search-mcp-server/src/main/resources/application-sse.yml`
- `kewei-image-search-mcp-server/src/main/resources/application-stdio.yml`
  - 作用：
    - 提供 MCP 服务运行模式配置（SSE / stdio）
- `kewei-image-search-mcp-server/src/test/java/com/kiwi/keweiimagesearchmcpserver/tools/ImageSearchToolTest.java`
  - 作用：
    - 验证图片搜索工具基本可用

- `kewei-image-generation-mcp-server/src/main/java/com/kiwi/keweiimagegenerationmcpserver/KeweiImageGenerationMcpServerApplication.java`
  - 类：`KeweiImageGenerationMcpServerApplication`
  - 方法：
    - `imageGenerationTools(ImageGenerationTool imageGenerationTool)`：注册 MCP 工具提供器
  - 作用：
    - 暴露图片生成工具为 MCP 可调用能力
- `kewei-image-generation-mcp-server/src/main/java/com/kiwi/keweiimagegenerationmcpserver/tools/ImageGenerationTool.java`
  - 类：`ImageGenerationTool`
  - 方法：
    - `generateImage(...)`：调用 Draw Things 文生图接口生成图片
    - `saveImageToFile(String base64Image)`：将 Base64 图片落盘并返回绝对路径
  - 作用：
    - 将高耗时/长返回的图片生成能力放到独立服务处理
    - 返回可直接访问的本地路径，便于主应用后续处理
- `kewei-image-generation-mcp-server/src/main/resources/application.yml`
- `kewei-image-generation-mcp-server/src/main/resources/application-sse.yml`
- `kewei-image-generation-mcp-server/src/main/resources/application-stdio.yml`
  - 作用：
    - 提供 MCP 服务运行模式配置（SSE / stdio）
- `kewei-image-generation-mcp-server/src/test/java/com/kiwi/keweiimagegenerationmcpserver/tools/ImageGenerationToolTest.java`
- `kewei-image-generation-mcp-server/src/test/java/com/kiwi/keweiimagegenerationmcpserver/tools/ImageGenerationToolRealTest.java`
  - 作用：
    - 覆盖模拟接口与真实环境联调两类验证场景

### 踩坑记录

- 问题：图像生成时直接把超长结果返回给 AI，处理耗时很长且效果不稳定
  - 现象：
    - image generation 返回内容过长（尤其大段 Base64）时，主对话链路处理时间明显增加，交互体验变差
  - 当前解决方案：
    - 在图片生成 MCP 服务里将结果落盘，仅返回“本地存储路径”
  - 后续优化方向：
    - 上传到云存储（对象存储/网盘），返回可访问链接给用户，减少本地路径耦合并提升跨端可用性

### 启动与联调手册

### 启动顺序

1. 启动图片生成 MCP 服务（默认端口 `8129`）
2. 启动图片搜索 MCP 服务（默认端口 `8127`）
3. 启动主应用 `kewei-ai-agent`（默认端口 `8123`）

### 启动命令（建议）

1. 启动图片生成 MCP 服务（SSE）

```bash
cd /Users/zhukewei/Downloads/dev/codes/kewei-ai-agent/kewei-image-generation-mcp-server
mvn spring-boot:run
```

2. 启动图片搜索 MCP 服务（SSE）

```bash
cd /Users/zhukewei/Downloads/dev/codes/kewei-ai-agent/kewei-image-search-mcp-server
./mvnw spring-boot:run
```

3. 启动主应用

```bash
cd /Users/zhukewei/Downloads/dev/codes/kewei-ai-agent
./mvnw spring-boot:run
```

### 关键配置检查

- 图片生成 MCP：
  - `kewei-image-generation-mcp-server/src/main/resources/application.yml`
  - `server.port=8129`
  - `spring.ai.mcp.server.*` 位于 `application-sse.yml` / `application-stdio.yml`
- 图片搜索 MCP：
  - `kewei-image-search-mcp-server/src/main/resources/application.yml`
  - `server.port=8127`
  - `spring.ai.mcp.server.*` 位于 `application-sse.yml` / `application-stdio.yml`
  - 需要配置 `PEXELS_API_KEY` 或 `pexels.api-key`
- 主应用 MCP 客户端：
  - `src/main/resources/application.yml`
  - `spring.ai.mcp.client.sse.connections.server1.url=http://localhost:8129`
  - 如果需要同时连接图片搜索服务，可再新增一个连接（例如 `server2.url=http://localhost:8127`）

### 最小联调步骤

1. 在主应用执行 `LoveAppTest#doChatWithMCP()`，先验证图片生成链路
2. 检查返回值是否为本地文件路径，且文件存在且非空
3. 放开 `doChatWithMCP()` 中图片搜索测试语句后，验证图片搜索链路
4. 若联调超时，优先检查：
   - MCP 服务是否已启动且端口一致
   - Draw Things 服务是否可用（图片生成依赖）
   - Pexels API Key 是否正确（图片搜索依赖）

### 阶段结果

- 主应用已具备通过 MCP 调用外部图像工具服务的能力
- 图片搜索与图片生成能力已完成服务化拆分并可独立运行
- `LoveApp` 侧已打通 MCP 调用与验证流程，为后续跨服务工具编排打下基础

## 18. 引入 Agent 执行框架与控制器扩展（ReAct + Tool Calling + 多种流式返回）

### 本阶段目标

- 在主应用中引入可多步执行的 Agent 框架，支持“思考-行动”循环
- 将工具调用能力从单轮对话升级为 Agent 多步任务执行
- 扩展 `AiController`，统一提供同步、SSE、SseEmitter、Manus Agent 入口
- 补齐 Agent 与控制器相关测试

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/agent/model/AgentState.java`
  - 枚举：`IDLE`、`RUNNING`、`FINISHED`、`ERROR`
  - 作用：
    - 统一 Agent 生命周期状态，作为并发与异常控制基础

- `src/main/java/com/kiwi/keweiaiagent/agent/BaseAgent.java`
  - 类：`BaseAgent`（抽象）
  - 核心方法：
    - `run(String userPrompt)`：同步多步执行主流程
    - `runStream(String userPrompt)`：基于 `SseEmitter` 的流式多步执行
    - `step()`：单步执行抽象方法
    - `cleanup()`：资源清理与生命周期收尾
  - 内部能力：
    - prompt/state 校验
    - 最大步数控制
    - 错误事件回传（stream）
  - 作用：
    - 定义 Agent 统一执行模板，减少子类重复逻辑

- `src/main/java/com/kiwi/keweiaiagent/agent/ReActAgent.java`
  - 类：`ReActAgent`（抽象，继承 `BaseAgent`）
  - 方法：
    - `think()`：决策是否需要执行动作
    - `act()`：执行动作并返回结果
    - `step()`：封装 ReAct 单步流程（think -> act）
  - 作用：
    - 将 Agent 行为标准化为 ReAct 模式

- `src/main/java/com/kiwi/keweiaiagent/agent/ToolCallAgent.java`
  - 类：`ToolCallAgent`（继承 `ReActAgent`）
  - 核心方法：
    - `think()`：调用大模型解析是否需要工具调用
    - `act()`：执行工具调用并回写对话历史
    - `summarizeToolPlan(...)`：工具计划可读化摘要
    - `buildSystemPrompt()`：系统提示与下一步提示合并
  - 作用：
    - 提供“工具驱动的多步 Agent”核心实现
    - 支持终止工具检测并将 Agent 状态置为 `FINISHED`

- `src/main/java/com/kiwi/keweiaiagent/agent/KeweiManus.java`
  - 类：`KeweiManus`（继承 `ToolCallAgent`）
  - 作用：
    - 基于业务场景预置系统提示词、下一步提示词、最大步数
    - 作为可直接调用的应用级 Agent

- `src/main/java/com/kiwi/keweiaiagent/tools/TerminateTool.java`
  - 类：`TerminateTool`
  - 方法：
    - `doTerminate()`：返回任务结束信号
  - 作用：
    - 在多步 Agent 链路中显式结束任务，避免无意义循环

- `src/main/java/com/kiwi/keweiaiagent/exception/ErrorCode.java`
  - 类：`ErrorCode`（枚举）
  - 本阶段重点：
    - 增加 Agent 相关错误码（如 `AGENT_BUSY`、`AGENT_RUN_FAILED`）
  - 作用：
    - 为 Agent 执行状态异常提供统一错误语义

- `src/main/java/com/kiwi/keweiaiagent/tools/ToolRegistration.java`
  - 本阶段改动：
    - 将 `TerminateTool` 纳入 `allTools(...)`
  - 作用：
    - 让 Agent 在工具调用链路中可使用“终止”动作

- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 本阶段新增/增强：
    - `doChatWithStream(String message, String chatId)`：流式内容输出
    - `doChatWithMCP(String message, String chatId)`：MCP 工具调用入口
    - `doChatWithTools(...)` 保持工具回调链路
  - 作用：
    - 为控制层提供统一调用接口（同步 / 流式 / 工具 / MCP）

- `src/main/java/com/kiwi/keweiaiagent/controller/AiController.java`
  - 本阶段新增/增强接口：
    - `/ai/love_app/chat/sync`
    - `/ai/love_app/chat/sse`
    - `/ai/love_app/chat/server_sent_event`
    - `/ai/love_app/chat/sse_emitter`
    - `/ai/manus/chat`
  - 作用：
    - 对外暴露多种输出协议与 Agent 能力入口
    - 支持“打字机效果”SSE 输出

### 主要测试覆盖

- `src/test/java/com/kiwi/keweiaiagent/controller/AiControllerTest.java`
  - 验证 `sse_emitter` 的事件流输出和 done 收尾事件
- `src/test/java/com/kiwi/keweiaiagent/agent/BaseAgentTest.java`
  - 覆盖状态校验、空参数、同步执行、流式执行、异常分支
- `src/test/java/com/kiwi/keweiaiagent/agent/ToolCallAgentTest.java`
  - 覆盖“next step 不重复注入”和工具计划摘要逻辑
- `src/test/java/com/kiwi/keweiaiagent/agent/KeweiManusTest.java`
  - 验证 Manus Agent 端到端执行可用

### 阶段结果

- 主应用已具备可复用的 Agent 抽象层与 ReAct 执行模型
- 工具调用从“单轮工具触发”升级为“多步 Agent 任务执行”
- 控制器已统一支持同步/流式/Agent 入口，接口能力更完整
- Agent 关键分支具备测试覆盖，后续扩展基础更稳固

## 19. 增加图片上传与图片处理链路（控制器参数扩展 + 文件路径图像对话）

### 本阶段目标

- 为对话接口增加“图片模式”参数，支持基于本地图片路径的图像问答
- 增加图片上传接口，将前端上传文件保存到本地目录
- 让同步聊天、流式聊天、SSE Emitter 三种模式都兼容图片输入

### 主要新增/涉及文件

- `src/main/java/com/kiwi/keweiaiagent/controller/AiController.java`
  - 本阶段新增/增强接口：
    - `GET /ai/love_app/chat/sync`
      - 新增参数：`option`、`imagePath`
      - 作用：当 `option=image` 且 `imagePath` 有值时，切换为图片对话模式
    - `GET /ai/love_app/chat/sse`
      - 新增参数：`option`、`imagePath`
      - 作用：支持图片场景下的流式输出
    - `GET /ai/love_app/chat/sse_emitter`
      - 新增参数：`option`、`imagePath`
      - 作用：支持图片场景下的打字机式 SSE 输出
    - `POST /ai/love_app/image/upload`
      - 入参：`chatId`、`file`
      - 作用：接收图片上传，保存到本地并返回文件信息
  - 本阶段新增内部方法：
    - `resolveExtension(String originalFilename)`：解析文件后缀，兜底为 `.png`
    - `shouldUseImageOption(String option, String imagePath)`：判断是否启用图片模式
    - `sendChunkAsTypewriter(...)`：复用 SSE 打字机输出逻辑
  - 作用：
    - 把“图片上传”和“图片对话”连接成一条完整控制器链路
    - 统一处理文本模式和图片模式的入口切换

- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 本阶段改动：
    - `doChatWithImage(String message, String chatId, String imagePath)`
      - 从类路径图片读取切换为 `FileSystemResource`
      - 通过 `MediaTypeFactory` 自动识别文件类型
    - `doChatWithImageStream(String message, String chatId, String imagePath)`
      - 新增图片流式聊天能力
  - 作用：
    - 支持使用上传后的本地文件路径进行图像对话
    - 让图片输入也能走同步与流式两套聊天链路

- `src/main/java/com/kiwi/keweiaiagent/constant/FileConstant.java`
  - 本阶段新增常量：
    - `IMAGE_UPLOAD_DIR`
  - 作用：
    - 统一图片上传目录，当前固定为 `${user.dir}/tmp/file`

### 数据流说明

1. 前端先调用 `/ai/love_app/image/upload` 上传图片
2. 服务端将图片保存到 `IMAGE_UPLOAD_DIR`
3. 接口返回 `chatId`、`fileName`、`filePath`、`relativePath`
4. 前端再调用聊天接口，并传入：
   - `option=image`
   - `imagePath=<上传返回的 filePath 或可解析路径>`
5. 控制器根据 `shouldUseImageOption(...)` 切换到图片对话逻辑

### 输入输出行为

- 上传接口返回：
  - `chatId`
  - `fileName`
  - `filePath`
  - `relativePath`
- 聊天接口在图片模式下：
  - 同步接口返回完整文本结果
  - `sse` / `sse_emitter` 返回流式文本结果

### 本阶段结果

- 图片处理链路从“只支持固定资源文件”升级为“支持真实上传文件”
- 同步聊天、流式聊天、SSE Emitter 三种输出方式都已兼容图片模式
- 为后续前端接入“上传图片后直接发起图像问答”提供了完整接口基础

## 20. 新增前端工程 `kewei-ai-agent-frontend`

### 本阶段目标

- 在后端能力逐步完善后，新增独立前端工程承接界面层开发
- 为聊天、图片上传、流式返回、Agent 调用等能力提供统一前端入口
- 让前后端联调从“接口验证”进入“完整应用交互”阶段

### 主要新增/涉及内容

- 根目录新增：
  - `kewei-ai-agent-frontend`
  - 作用：
    - 作为独立前端工程存在，与主后端工程并列管理
    - 承担页面展示、交互流程、接口联调和用户侧体验实现

### 实现定位（当前阶段）

- 当前 README 只在根目录级别记录该前端工程的引入
- 不展开内部目录、文档和具体实现细节
- 从职责上看，它主要会服务于：
  - 文本聊天页面
  - 图片上传与图片问答页面
  - 流式输出展示
  - Agent / MCP 能力的前端交互封装

### 本阶段结果

- 项目结构从“后端能力建设”扩展为“前后端协同开发”
- 为后续 UI、交互体验和联调流程提供了独立承载工程

## 21. 升级 Spring AI 2.0 + 引入 Skill 模式 + AskUserQuestion 两段式交互 + Manus 分域执行

### 本阶段目标

- 将主工程升级到较新的 Spring AI 版本，兼容新的工具调用方式与 Skill 能力
- 为 Agent 增加 Skill 模式，先落地一个可直接产出文件的 PPT 生成能力
- 把原本依赖控制台 `stdin` 的提问流程改造成适用于 Web 的“两段式提问/续跑”
- 修复 Manus 在 SSE 场景下“服务端看得到提问、前端收不到、线程卡住”的交互时机问题
- 将 Manus 从“大而全工具集一次性硬塞给模型”调整为“先分域，再给小工具集执行”，降低大模型对 JSON Schema / Tool Calling 的不稳定响应

### 主要新增/修改文件

- `pom.xml`
  - 作用：升级 Spring AI 依赖体系
  - 关键调整：
    - `spring-ai.version` 升级到 `2.0.0-M2`
    - 保持 `spring-ai-agent-utils`、`spring-ai-starter-mcp-client`、`spring-ai-rag`、`spring-ai-starter-model-ollama` 等依赖与新版能力对齐
  - 本阶段意义：
    - 为 Skill、Tool Callback、新版 Agent 工具链和后续扩展打基础

- `src/main/resources/application.yml`
  - 作用：同步新版 Spring AI 运行配置
  - 本阶段重点：
    - 保留 Ollama、MCP、PgVector 等主能力配置
    - 继续通过自动装配排除项控制 DashScope 与其他模型能力的装配边界，避免升级后额外冲突

- `src/main/java/com/kiwi/keweiaiagent/config/JacksonCompatibilityConfig.java`
  - 类：`JacksonCompatibilityConfig`
  - 方法：
    - `objectMapper()`：注册带有 `findAndRegisterModules()` 的 `ObjectMapper`
  - 作用：
    - 给新版 Spring AI、SSE 事件序列化以及前后端 JSON 交互提供更稳定的 Jackson 兼容配置
    - 避免在 `question` 事件、技能结果对象等结构化输出场景下出现序列化兼容问题

- `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`
  - 类：`LoveApp`
  - 新增/强化的方法：
    - `callWithSkills(String message, String chatId)`：通过 `toolCallbacks(allTools)` + `SkillsTool` 进入 skill 模式，让模型可以结合技能说明文件和工具完成任务
    - `streamWithSkills(String message, String chatId)`：对 skill 模式提供流式封装
    - `doChatWithTools(String message, String chatId)`：基于 `SkillChatResult` 统一处理文本、问题、文件路径三种返回形态
    - `toSkillChatResult(String text)`：把模型输出规范化为 `TEXT / QUESTION / FILE` 三种业务结果
    - `resolveSkillsDirectory()`：定位 classpath 下的 `.claude/skills` 目录，供 `SkillsTool` 加载
  - 新增的数据结构：
    - `SkillChatResultType`：定义 Skill 结果类型
    - `SkillChatResult`：统一承载普通文本、追问、文件产物路径
  - 作用：
    - 让后端在一个统一入口中同时支持“普通回答”“让用户补充信息”“生成本地文件”三种 Agent 输出

- `src/main/resources/.claude/skills/ppt-writer/SKILL.md`
  - 作用：定义 `ppt-writer` 技能的行为规范
  - 主要约束：
    - 识别“做 PPT / 幻灯片 / 演示稿”等需求时启用该 skill
    - 缺少主题、受众、语气、页数、输出路径时，通过 `AskUserQuestionTool` 一次性追问
    - 获取到足够信息后，再调用 `PptWriterTool`
    - 输出最终文件路径，并补充每页概览说明

- `src/main/resources/.claude/skills/ppt-writer/assets/default-theme-notes.md`
  - 作用：给 PPT 生成提供默认版式、内容密度和备注页风格参考
  - 价值：
    - 降低模型在没有明确视觉规范时的发挥波动
    - 让生成出来的 PPT 更像“可直接演示的成品”，而不是简单文本堆砌

- `src/main/java/com/kiwi/keweiaiagent/tools/PptWriterTool.java`
  - 类：`PptWriterTool`
  - 核心数据结构：
    - `SlideSpec`：单页幻灯片的数据描述（标题、要点、备注）
    - `PptSpec`：整份 PPT 的输入描述（总标题、页面 Markdown、输出路径）
  - 方法：
    - `create_pptx(PptSpec spec)`：真正生成 `.pptx` 文件，并返回生成结果、路径和页数
    - `validateSpec(PptSpec spec)`：校验标题、内容、输出路径是否合法
    - `parseSlides(String slidesMarkdown)`：把统一约定的 Markdown 文本拆成多页结构化内容
    - `nonBlankOrDefault(...)`：处理默认标题等兜底逻辑
  - 作用：
    - 把“模型生成的页面规划文本”稳定转换成实际可下载的 PowerPoint 文件

- `src/test/java/com/kiwi/keweiaiagent/tools/PptWriterToolTest.java`
  - 类：`PptWriterToolTest`
  - 方法：
    - `createPptxFromFlatSlidesMarkdown()`：验证 Markdown 页面描述可以成功生成 `.pptx` 文件，且文件存在、页数正确
  - 作用：
    - 证明 PPT 工具不是停留在 Prompt 设计层，而是已经具备真实文件落盘能力

- `src/main/java/com/kiwi/keweiaiagent/tools/AskQuestionTool.java`
  - 类：`AskQuestionTool`
  - 方法：
    - `askUserQuestionTool()`：注册 `AskUserQuestionTool` Bean
    - `handleQuestions(List<Question> questions)`：处理模型发出的追问请求
  - 作用变化：
    - 不再通过控制台 `System.out.println` + `Scanner(System.in)` 阻塞等待用户输入
    - 改为优先从 `ManusSessionStore` 中消费前端已提交的答案
    - 若当前没有答案，则保存待回答问题，并抛出 `PendingUserQuestionException`
  - 本质变化：
    - 工具不再自己负责“提问 + 等待 + 继续”整条链路，而是只负责发出“现在需要用户补充信息”的信号

- `src/main/java/com/kiwi/keweiaiagent/agent/PendingUserQuestionException.java`
  - 类：`PendingUserQuestionException`
  - 方法：
    - `getQuestions()`：返回当前待回答问题列表
  - 作用：
    - 作为 AskUserQuestion 的中断信号，向外层 Agent / Controller 明确告知“当前不是报错，也不是完成，而是需要暂停等待用户回答”

- `src/main/java/com/kiwi/keweiaiagent/agent/model/AgentState.java`
  - 枚举：`AgentState`
  - 新增状态：
    - `WAITING_FOR_USER_INPUT`
  - 作用：
    - 让 Agent 的状态机从“空闲 / 运行 / 完成 / 错误”扩展为“可显式暂停等待用户补充信息”

- `src/main/java/com/kiwi/keweiaiagent/agent/ManusSessionStore.java`
  - 类：`ManusSessionStore`
  - 记录结构：
    - `PendingOption`：前端可展示的单个选项
    - `PendingQuestion`：适合前端渲染的待回答问题结构
    - `ManusSession`：会话快照，包含 `chatId`、原始任务、当前 agent、待回答问题和待提交答案
  - 方法：
    - `putSession(...)`：创建并保存会话
    - `getSession(String chatId)`：获取当前会话
    - `removeSession(String chatId)`：移除会话
    - `savePendingQuestions(...)`：保存模型发出的追问，并转换成前端可用结构
    - `submitAnswers(...)`：保存前端提交的答案
    - `consumeAnswers(String chatId)`：消费答案，并把前端问题 id 映射回模型原始问题文本
    - `clearPendingQuestions(String chatId)`：清理当前待答问题
    - `activateSession(String chatId)` / `clearActiveSession()` / `currentSessionId()`：维护当前线程对应的活动会话
    - `getPendingQuestions(String chatId)`：给 SSE `question` 事件提供可直接输出的前端结构
  - 作用：
    - 解决“提问”和“用户补答”不在同一个 HTTP 请求里的状态持久问题
    - 当前实现是进程内内存版，适合本地联调和单机运行

- `src/main/java/com/kiwi/keweiaiagent/agent/BaseAgent.java`
  - 类：`BaseAgent`
  - 新增/强化的方法与能力：
    - `runStream(String userPrompt)`：启动流式执行
    - `resumeStream()`：支持从等待状态继续执行
    - `executeStreamLoop(...)`：统一封装首次执行和继续执行的循环逻辑
    - `validateResumeState()`：校验当前是否允许继续
    - `sendQuestionEvent(...)`：在捕获 `PendingUserQuestionException` 后发送 `event: question`
    - `sendErrorEvent(...)`：统一发送错误事件
    - `cleanup()`：在完成/失败后清理资源，并在必要时清理 Manus 会话
  - 本阶段变化重点：
    - 新增 `sessionId`、`manusSessionStore`，把 Agent 执行与某个 chatId 绑定
    - `runStream()` 不再只是“执行 step 后发消息”，而是显式捕获等待用户输入的中断状态
    - 一旦进入等待状态，就通过 SSE 发送 `question` 事件，而不是继续卡在 `step()` 内部
  - 作用：
    - 修复了“服务端控制台能看到追问，但前端一直收不到 question 事件”的发送时机问题

- `src/test/java/com/kiwi/keweiaiagent/agent/BaseAgentTest.java`
  - 类：`BaseAgentTest`
  - 方法：
    - `runStreamShouldWaitForUserInputWhenQuestionIsRaised()`：验证 Agent 在收到 `PendingUserQuestionException` 时会进入 `WAITING_FOR_USER_INPUT`
  - 作用：
    - 覆盖本阶段最关键的状态流转

- `src/main/java/com/kiwi/keweiaiagent/agent/ToolCallAgent.java`
  - 类：`ToolCallAgent`
  - 方法：
    - `think()`：驱动模型思考，获取 Tool Call 计划
    - `act()`：执行工具调用并处理返回
    - `step()`：组合执行单步思考与行动
    - `resumeStep()`：保留“基于已有 Tool Call 恢复执行”的能力
    - `summarizeToolPlan(...)`：把纯 Tool Call 计划整理为更易读的文字摘要
    - `hasPendingUserInput(...)`：检测工具返回是否意味着“正在等待用户补充信息”
  - 本阶段说明：
    - 代码里保留了 `resumeStep()` 与基于 `toolCallChatResponse` 的续跑思路
    - 但从最终服务编排看，Manus 已不再依赖“复用旧 pending tool call 原地继续”作为主方案，而是改为“重新组装 prompt 后启动一个新的 agent”来续跑
  - 原因：
    - 旧方案在复杂 Tool Calling 场景下状态不稳定，容易受历史上下文和 pending 响应影响

- `src/test/java/com/kiwi/keweiaiagent/agent/ToolCallAgentTest.java`
  - 类：`ToolCallAgentTest`
  - 方法：
    - `think_shouldNotAppendNextStepPromptAsUserMessageOnEveryStep()`：验证不会在每一步重复污染用户消息
    - `summarizeToolPlan_shouldIncludeToolNamesAndResultHint()`：验证工具计划摘要可读性
    - `shouldDetectPendingUserInputFromAskUserQuestionToolResponse()`：验证能识别待用户输入状态
  - 作用：
    - 为 Tool Calling 的稳定性改造补充回归保障

- `src/main/java/com/kiwi/keweiaiagent/agent/ManusSessionService.java`
  - 类：`ManusSessionService`
  - 枚举：
    - `TaskDomain`：把任务分成 `GENERAL / PPT / PDF / EMAIL`
  - 方法：
    - `startChatStream(String chatId, String message)`：根据用户任务选择工具域，创建新的 `KeweiManus` 并启动执行
    - `continueChatStream(String chatId, Map<String, String> answers)`：接收用户补充信息，重新组装 follow-up prompt，再创建新的 `KeweiManus` 继续执行
    - `selectToolsForPrompt(String prompt)`：按任务域筛选一小组工具
    - `routeTaskDomain(String prompt)`：从 prompt 判断任务属于 PPT、PDF、邮件还是通用任务
    - `buildFollowupPrompt(...)`：把“原始任务 + 用户补充答案”组装成新的继续执行提示词
  - 本阶段核心作用：
    - 从“让模型背着全量工具和 skill 直接开跑”改成“先识别任务域，再只给相关工具”
    - 当前续跑策略不是恢复旧 agent 内部状态，而是重组 prompt 后启动新的 agent，规避旧状态不稳定问题

- `src/test/java/com/kiwi/keweiaiagent/agent/ManusSessionServiceTest.java`
  - 类：`ManusSessionServiceTest`
  - 方法：
    - `shouldSelectPptToolSubsetForPptPrompt()`：验证 PPT 任务只拿到 PPT 相关工具集
    - `shouldSelectEmailToolSubsetForEmailPrompt()`：验证邮件任务工具子集筛选
    - `shouldSelectPdfToolSubsetForPdfPrompt()`：验证 PDF 任务工具子集筛选
    - `shouldKeepAllToolsForGeneralPrompt()`：验证普通任务仍可使用全量工具
    - `shouldRoutePromptToExpectedDomain()`：验证任务域路由逻辑
  - 作用：
    - 给“两阶段分域执行”增加明确的可回归测试

- `src/main/java/com/kiwi/keweiaiagent/tools/ToolRegistration.java`
  - 类：`ToolRegistration`
  - 方法：
    - `allTools(...)`：统一收集并注册当前可用的 `ToolCallback[]`
  - 本阶段新增注册：
    - `AskUserQuestionTool`
    - `PptWriterTool`
  - 作用：
    - 让 PPT skill 和 AskUserQuestion 模式成为统一工具体系的一部分，而不是单独拼接的临时能力

- `src/main/java/com/kiwi/keweiaiagent/controller/AiController.java`
  - 类：`AiController`
  - 本阶段重点方法：
    - `doChatWithLoveAppSync(...)`：增加 `option=skills` 分支，走 `LoveApp.callWithSkills(...)`
    - `doChatWithLoveAppSSE(...)`：增加 skill 模式流式返回
    - `doChatWithLoveAppSseEmitter(...)`：兼容 skill 模式下的 SSE Emitter 输出
    - `doChatWithManus(...)`：启动 Manus 的第一段任务执行
    - `continueChatWithManus(...)`：接收前端补充答案，继续执行 Manus
    - `ManusContinueRequest`：封装继续执行接口的请求体（`chatId + answers`）
  - 作用：
    - 后端正式提供“第一次发起任务”和“补充答案后继续执行”两个独立入口

- `src/test/java/com/kiwi/keweiaiagent/controller/AiControllerTest.java`
  - 类：`AiControllerTest`
  - 方法：
    - `shouldUseSkillSyncResponseWhenOptionIsSkills()`：验证同步 skill 返回 `FILE`
    - `shouldUseSkillFluxResponseWhenOptionIsSkills()`：验证流式 skill 返回 `QUESTION`
    - `shouldUseSkillSseEmitterWhenOptionIsSkills()`：验证 skill 模式下 `SseEmitter` 输出
    - `shouldUseManusSessionServiceForStartChat()`：验证 `/ai/manus/chat` 可以输出 `question` 事件
    - `shouldUseManusSessionServiceForContinueChat()`：验证 `/ai/manus/chat/continue` 可以继续返回结果
  - 作用：
    - 证明 Controller 层已经具备完整的两段式问答协议

- `kewei-ai-agent-frontend/src/api/modules/ai.js`
  - 方法：
    - `continueManusChat(payload)`：调用 `/ai/manus/chat/continue`
  - 作用：
    - 把 Manus 续跑能力显式接到前端 API 层

- `kewei-ai-agent-frontend/src/api/sse.js`
  - 方法：
    - `openSSE(...)`：支持命名事件 `message / question / done`
    - `openFetchSSE(...)`：支持用 `fetch + POST` 方式消费 `/ai/manus/chat/continue` 这类需要请求体的 SSE 接口
  - 作用：
    - 解决 EventSource 只擅长 GET 的限制，让前端也能以流式方式续跑 Manus

- `kewei-ai-agent-frontend/src/views/ChatView.vue`
  - 主要交互点：
    - `send()`：发起第一次对话或 Agent 任务
    - `sendViaSSE(...)`：统一处理 `message / question / done` 事件
    - `handlePendingQuestion(payload)`：接收并展示待回答问题
    - `submitPendingAnswers()`：收集用户补充信息，调用 `/ai/manus/chat/continue`
    - `clearPendingQuestions()` / `resetPendingQuestions()`：清理或重启当前问答流程
  - 作用：
    - 前端不再把 `question` 当成普通聊天文本，而是展示为表单面板并支持用户填写后续跑

- `kewei-ai-agent-frontend/src/components/ChatMessage.vue`
  - 作用：
    - 优化消息展示，支持图片附件和 Markdown 渲染
    - 为 Agent 的阶段化输出、工具结果和最终回答提供更好的阅读体验

- `kewei-ai-agent-frontend/src/views/ConsoleView.vue`
  - 作用：
    - 补充接口调试与会话管理视角，方便联调 SSE 和 Manus 流程

### 本阶段的关键设计调整

- 从“命令行阻塞式提问”改为“Web 两段式提问/续跑”
  - 原来 `AskUserQuestionTool` 在控制台打印问题并阻塞等 `stdin`
  - 现在改为：
    - 第一次请求只负责把问题通过 SSE `question` 事件发给前端
    - 第二次请求由前端提交答案到 `/ai/manus/chat/continue`
    - 后端再基于补充信息继续执行任务

- 从“同一个 Agent 内部强行恢复旧状态”调整为“重新组装 Prompt 后新建 Agent”
  - 代码里保留了 `resumeStream()` / `resumeStep()` 的恢复能力
  - 但最终 `ManusSessionService` 选择的主链路是：
    - 保存原始任务
    - 收集用户补充答案
    - 组装 follow-up prompt
    - 重新创建 `KeweiManus`
    - 用新 Agent 继续执行
  - 这样可以绕开旧 pending tool-call 和内部状态在复杂场景下的不稳定行为

- 从“全工具集硬塞给模型”改为“任务分域 + 小工具集执行”
  - 先判断任务更像 PPT、PDF、Email 还是普通任务
  - 再只把该领域必要的工具子集交给模型
  - 这样可以显著降低模型在大量 tools/skills 下对 JSON Schema、Tool Calling 响应不稳定的问题

### 踩坑记录

#### 1. 服务端控制台能看到提问，但前端收不到 `question` 事件

- 问题表现：
  - 后端已经触发了提问逻辑
  - IDEA 控制台能看到问题
  - 前端却收不到 `question` 事件
  - 整个 Agent 看起来像“卡死”
- 原因：
  - 旧的提问方式依赖控制台 `stdin`
  - 同时 `SseEmitter` 原本要等 `step()` 整体执行完才会发送消息
  - 但 AskUserQuestion 恰好发生在 `step()` 内部，所以还没来得及把问题发出去就已经阻塞
- 处理方式：
  - 新增 `WAITING_FOR_USER_INPUT`
  - 用 `PendingUserQuestionException` 打断执行
  - 在 `BaseAgent` 中捕获异常后立刻发送 `event: question`
  - 前端改为监听命名 SSE 事件，而不是只收普通 `message`

#### 2. 整个 Agent 卡在等待 `stdin`

- 问题表现：
  - 服务端线程被阻塞
  - HTTP / SSE 请求长时间不结束
  - 前端既拿不到问题，也无法继续执行
- 原因：
  - AskUserQuestion 的实现本质上还是面向命令行，不适合 Web 请求链路
- 处理方式：
  - 去掉控制台阻塞式输入依赖
  - 改成“第一次请求发问题，第二次请求提交答案”的 Web 两段式协议
  - 用 `ManusSessionStore` 暂存问题与答案

#### 3. 同一个 Agent 的 resume 状态不稳定

- 问题表现：
  - 理论上可以在原 Agent 上直接恢复上一次未完成的 Tool Call
  - 但真实测试中，旧 `resume` 链路容易受 pending tool-call、历史状态和上下文波动影响
- 处理方式：
  - 不再把“恢复旧 agent 内部状态”作为主方案
  - 改成把原始任务和补充答案重新组装为 follow-up prompt
  - 然后新建一个 `KeweiManus` 继续执行

#### 4. Manus 携带大量 tools / skills 时，大模型对 JSON Schema 响应不稳定

- 问题表现：
  - 模型在全量工具环境下容易选错工具、输出结构不稳，甚至导致整体执行链路波动
- 处理方式：
  - 把 Agent 拆成两阶段思路：
    - 阶段 A：做规划、补参、任务分域
    - 阶段 B：只在选定的小工具集里继续执行
  - 当前落地在 `ManusSessionService` 中表现为“按任务域筛工具子集”

### 本阶段结果

- 主工程已升级到 Spring AI `2.0.0-M2` 体系
- 已引入基于 Skills 的 Agent 模式，并落地第一个可生成实际文件的 PPT skill
- AskUserQuestion 已从控制台阻塞模式改造成适合 Web 的两段式交互协议
- Manus 已补齐 `question` 事件发送链路，前端可以真正接收、展示并提交补充问题
- Manus 的续跑策略已从“恢复旧状态”转向“重组 prompt 后启动新 agent”，整体更稳定
- Manus 已具备按任务域筛选工具子集的能力，为后续继续扩展 PDF、Email 等领域 agent 做好了结构准备

## 22. 接入 TodoWrite 任务规划能力，增加多应用场景下的分层任务执行

### 本阶段目标

- 在复杂任务场景下先生成 todo 清单，再逐步执行具体工具，减少模型“做到一半忘任务”的问题
- 将社区版 `TodoWriteTool` 接入当前工程，并用于 Manus 和独立 demo 两条路径
- 在后端执行过程中持续输出 todo 快照，让前端可以实时看到任务拆解和进度变化
- 解决 Spring AI `2.0.0-M2` 下社区版 `TodoWriteTool` 参数绑定不兼容的问题

### 主要新增/修改文件

- `src/main/java/com/kiwi/keweiaiagent/tools/TodoWriteToolAdapter.java`
  - 类：`TodoWriteToolAdapter`
  - 方法：
    - `todoWrite(List<TodoWriteTool.Todos.TodoItem> todos)`：对外继续暴露 `TodoWrite` 工具名，但方法签名改为直接接收内部 todo 数组项列表
  - 作用：
    - 这是本阶段最关键的兼容层
    - Spring AI 当前版本对单参数工具方法的绑定行为，会把 `{"todos":[...]}` 中的内部数组抽出来再做反序列化
    - 社区版 `TodoWriteTool` 原本要的是包装对象 `TodoWriteTool.Todos`
    - 适配器先用 `List<TodoItem>` 接住 Spring AI 实际传入的数组，再在方法内部手动包装成 `new TodoWriteTool.Todos(todos)`，最后调用社区版真正的 `delegate.todoWrite(...)`
  - 本阶段意义：
    - 修复点不在 Prompt，也不在 Jackson 本身，而在“社区版方法签名”和“Spring AI 当前绑定行为”之间的兼容层

- `src/main/java/com/kiwi/keweiaiagent/tools/ToolRegistration.java`
  - 类：`ToolRegistration`
  - 方法：
    - `todoWriteTool(ManusSessionStore manusSessionStore)`：实例化社区版 `TodoWriteTool`，并在 todo 变更时把快照写入当前会话
    - `allTools(...)`：把 `TodoWriteToolAdapter` 纳入全局工具注册
  - 作用：
    - 保留社区版工具实现不动
    - 通过事件回调把 todo 状态同步到 `ManusSessionStore`
    - 通过适配器对外暴露稳定的工具调用入口

- `src/main/java/com/kiwi/keweiaiagent/agent/todo/TodoItem.java`
  - 记录：`TodoItem`
  - 字段：
    - `id`：本地生成的 todo 标识
    - `content`：任务内容
    - `status`：任务状态
  - 作用：
    - 作为前端展示和 SSE 输出时的最小单元，避免前端直接依赖社区版复杂结构

- `src/main/java/com/kiwi/keweiaiagent/agent/todo/TodoSnapshot.java`
  - 记录：`TodoSnapshot`
  - 字段：
    - `items`：当前任务的 todo 列表快照
  - 作用：
    - 用于表示某一时刻的完整 todo 状态，方便后端保存、SSE 推送和前端渲染

- `src/main/java/com/kiwi/keweiaiagent/agent/todo/CommunityTodoMapper.java`
  - 类：`CommunityTodoMapper`
  - 方法：
    - `toSnapshot(TodoWriteTool.Todos todos)`：把社区版 `TodoWriteTool` 的包装对象映射为项目内部的 `TodoSnapshot`
  - 作用：
    - 将社区版数据结构和项目自己的前端协议解耦
    - 把社区版的 todo 列表转换为更稳定、更轻量的前端快照结构

- `src/main/java/com/kiwi/keweiaiagent/agent/ManusSessionStore.java`
  - 类：`ManusSessionStore`
  - 新增内容：
    - `TodoSnapshotListener`：todo 快照监听器接口
    - `todoSnapshot`：加入到 `ManusSession` 会话快照中的 todo 状态
  - 新增/强化的方法：
    - `saveTodoSnapshot(String chatId, TodoSnapshot todoSnapshot)`：保存并广播当前会话的 todo 快照
    - `getTodoSnapshot(String chatId)`：读取当前快照
    - `registerTodoSnapshotListener(...)` / `unregisterTodoSnapshotListener(...)`：为当前会话注册或移除 SSE 推送监听器
  - 作用：
    - 让 todo 状态和提问状态一样，成为会话级中间状态的一部分
    - 支撑后端执行时持续向前端推送 todo 变化

- `src/main/java/com/kiwi/keweiaiagent/agent/BaseAgent.java`
  - 类：`BaseAgent`
  - 新增/强化的方法与能力：
    - `sendTodoEvent(...)`：发送 `event: todo`
    - `buildTodoEventPayload()`：构建当前 todo 快照的事件载荷
    - 在 `executeStreamLoop(...)` 中注册 `TodoSnapshotListener`，并在有历史 todo 时先补发一次
  - 作用：
    - 把 TodoWrite 从“工具内部状态”提升为“Agent 执行期可见的流式进度事件”
    - 让前端能实时看到任务从 `pending -> in_progress -> completed` 的变化过程

- `src/main/java/com/kiwi/keweiaiagent/agent/KeweiManus.java`
  - 类：`KeweiManus`
  - 本阶段变化：
    - 在 system prompt 中明确要求复杂任务先调用 `TodoWrite`
    - 在 next-step prompt 中要求每完成一个子任务或进入下一阶段时，及时刷新 todo 清单
    - `maxSteps` 调整为 `20`
  - 作用：
    - 把 todo 规划从“可选能力”提升为 Manus 的默认工作方式

- `src/main/java/com/kiwi/keweiaiagent/agent/ManusSessionService.java`
  - 类：`ManusSessionService`
  - 本阶段变化：
    - 各任务域工具子集中增加 `TodoWrite`
  - 作用：
    - 让 PPT / PDF / Email 等复杂任务在执行前都能先做任务拆解
    - 维持“先分域，再给小工具集”的稳定策略，同时加入 todo 规划能力

- `src/main/java/com/kiwi/keweiaiagent/app/TodoDemoApp.java`
  - 类：`TodoDemoApp`
  - 方法：
    - `call(String message, String chatId)`：最小同步演示入口
    - `stream(String message, String chatId)`：最小流式演示入口
  - 作用：
    - 提供一个只保留 `TodoWrite / AskUserQuestionTool / doTerminate` 的轻量 demo 环境
    - 便于单独验证“先拆 todo 再执行”的工作方式，而不受其他工具干扰

- `src/main/java/com/kiwi/keweiaiagent/controller/AiController.java`
  - 类：`AiController`
  - 本阶段变化：
    - `doChatWithLoveAppSync(...)`：增加 `option=todo-demo` 分支
    - `doChatWithLoveAppSSE(...)`：增加 `todo-demo` 流式分支
    - `doChatWithLoveAppSseEmitter(...)`：增加 `todo-demo` 在 `sse_emitter` 模式下的支持
    - `shouldUseTodoDemoOption(String option)`：识别 todo demo 模式
  - 作用：
    - 让 TodoWrite 不仅在 Manus 中可用，也有独立演示入口，便于联调和说明

- `src/test/java/com/kiwi/keweiaiagent/controller/AiControllerTest.java`
  - 类：`AiControllerTest`
  - 方法：
    - `shouldUseTodoDemoAppWhenOptionIsTodoDemo()`：验证 `option=todo-demo` 时会走 `TodoDemoApp`
  - 作用：
    - 确认新的 demo 入口已被控制器正确接入

- `src/test/java/com/kiwi/keweiaiagent/tools/TodoWriteToolTest.java`
  - 类：`TodoWriteToolTest`
  - 方法：
    - `shouldAcceptStructuredJsonArgumentsAndSaveTodoSnapshot()`：验证适配器可以正确接收结构化 JSON，并把 todo 快照写入会话
    - `shouldRejectMultipleInProgressItems()`：验证社区版 TodoWrite 的业务约束仍然生效（一次只能有一个 `in_progress`）
  - 作用：
    - 证明适配层没有破坏社区版工具原本的校验逻辑

- `src/test/java/com/kiwi/keweiaiagent/agent/KeweiManusPromptTest.java`
  - 类：`KeweiManusPromptTest`
  - 方法：
    - `shouldMentionTodoPlanningForComplexTasks()`：验证 Manus 的 prompt 已明确要求复杂任务优先调用 `TodoWrite`
  - 作用：
    - 保证 Prompt 层的规划约束不会被后续修改意外删掉

- `src/test/java/com/kiwi/keweiaiagent/agent/ManusSessionStoreTest.java`
  - 类：`ManusSessionStoreTest`
  - 方法：
    - `shouldSaveAndReadTodoSnapshotForSession()`：验证 todo 快照可写可读
    - `shouldClearTodoSnapshotWhenSessionRemoved()`：验证会话删除时快照一起清理
    - `shouldNotifyTodoSnapshotListenersImmediately()`：验证监听器会立即收到 todo 更新
    - `shouldRemoveTodoSnapshotListenersWithSession()`：验证会话移除后监听器也会清理
  - 作用：
    - 给 todo 状态存储与事件通知机制提供回归保障

- `kewei-ai-agent-frontend/src/api/sse.js`
  - 方法：
    - `openSSE(...)`：新增 `todo` 事件监听
    - `openFetchSSE(...)`：新增 `todo` 事件解析
  - 作用：
    - 前端可以像消费 `message / question / done` 一样，消费后端新增的 `todo` 事件

- `kewei-ai-agent-frontend/src/views/ChatView.vue`
  - 主要新增逻辑：
    - `pinnedTodo`：保存当前置顶展示的 todo 快照
    - `handleTodoUpdate(payload)`：接收并更新当前任务 todo
    - `archivePinnedTodo()`：在任务结束后把置顶 todo 归档到聊天历史
    - `sendViaSSE(...)`：统一接入 `onTodo`
  - 作用：
    - 在聊天界面顶部固定展示当前任务拆解情况
    - 任务完成后再把 todo 快照作为一条历史消息存档

- `kewei-ai-agent-frontend/src/components/ChatMessage.vue`
  - 作用：
    - 增加 todo 卡片渲染能力
    - 可直观看到每一项任务的状态、完成数与当前进度

### 踩坑记录

#### 1. 社区版 `TodoWriteTool` 的方法签名与 Spring AI 当前参数绑定行为不兼容

- 问题表现：
  - 报错信息：
    - `Cannot deserialize value of type org.springaicommunity.agent.tools.TodoWriteTool$Todos from Array value`
  - 模型已经开始正确调用 `TodoWrite`
  - 但工具真正执行前，在参数绑定阶段就失败了

- 根因：
  - 社区版工具原本的方法签名是接收包装对象：
    - `TodoWriteTool.Todos`
  - 理论结构应该是：
    - `{"todos":[...]}`
  - 但在当前 Spring AI `2.0.0-M2` 的工具参数绑定行为下，单参数方法会把内部字段先抽出来，再做反序列化
  - 最终实际传给 Java 方法的不是整个对象，而是内部数组：
    - `[ ... ]`
  - 于是就出现了：
    - 目标类型是对象 `TodoWriteTool.Todos`
    - 实际 JSON 却是数组 `[ ... ]`
  - Jackson 无法把数组直接反序列化成包装对象，所以报错

- 处理方式：
  - 不修改社区版 `TodoWriteTool`
  - 新增 `TodoWriteToolAdapter`
  - 对外暴露的工具名仍然是 `TodoWrite`
  - 但方法签名改成：
    - `List<TodoWriteTool.Todos.TodoItem> todos`
  - 这样 Spring AI 实际传入的数组就能被正常接住
  - 然后在 Java 内部手动包装为：
    - `new TodoWriteTool.Todos(todos)`
  - 再委托给社区版真正的：
    - `delegate.todoWrite(...)`

#### 2. 问题不在模型不调用，而在“调用后参数绑定炸掉”

- 问题表现：
  - 前期模型没有调用 `TodoWrite`
  - 后来调整 prompt 和工具名后，模型已经会调用 `TodoWrite`
  - 但一调用就失败

- 根因：
  - Prompt 层和工具选择已经打通
  - 真正的问题发生在 Java 工具方法的入参绑定层，而不是模型输出层

- 处理方式：
  - 不去强行改模型输出结构
  - 而是通过适配层调整 Java 方法签名，让它匹配 Spring AI 当前真实传入的数据形态

### 本阶段结果

- 已接入社区版 `TodoWriteTool`，并完成适配层封装
- Manus 在复杂任务里已具备“先列 todo，再逐步执行”的默认工作方式
- Todo 状态已能通过 `ManusSessionStore` 保存，并以 `event:todo` 的形式实时推送到前端
- 前端已支持置顶展示当前任务 todo，并在任务完成后归档到历史消息
- 新增 `todo-demo` 演示入口，便于独立验证 TodoWrite 工作流
- 已解决 Spring AI `2.0.0-M2` 下社区版 TodoWrite 方法签名不兼容的问题

## 23. 接入 OpenClaw 远程调研委托能力，完善多进程管线并发处理

### 本阶段目标

- 把默认研究类任务从本地网页搜索/抓取，调整为委托给独立的 OpenClaw 执行代理
- 让 Manus 在复杂任务里保持“编排层”职责，只负责拆解、委托和汇总，不在本地重复伪造远程调研的内部步骤
- 为 OpenClaw 命令执行增加稳定的进程读写处理，避免 `stdin/stdout/stderr` 管线阻塞
- 为整个项目收官阶段补齐一套更清晰的研究任务架构

### 主要新增/修改文件

- `src/main/java/com/kiwi/keweiaiagent/tools/OpenClawCommandRunner.java`
  - 接口：`OpenClawCommandRunner`
  - 方法：
    - `run(List<String> command, Duration timeout)`：统一封装 OpenClaw 命令执行入口
  - 数据结构：
    - `CommandResult`：返回进程退出码、标准输出、标准错误
  - 作用：
    - 把“如何执行外部 OpenClaw 命令”从业务工具中抽离，方便后续替换实现或单元测试模拟

- `src/main/java/com/kiwi/keweiaiagent/tools/ShellOpenClawCommandRunner.java`
  - 类：`ShellOpenClawCommandRunner`
  - 方法：
    - `run(...)`：启动进程、并发读取 `stdout/stderr`、等待结束、处理超时和异常
    - `readStreamAsync(...)`：异步读取单个输出流
    - `awaitStream(...)`：等待异步读取结果
    - `firstNonBlank(...)`、`preview(...)`：错误信息与日志预览辅助方法
  - 作用：
    - 这是最终解决进程阻塞问题的关键类
    - 通过并发读取标准输出和标准错误，避免子进程因为某个输出缓冲区未被及时消费而卡死

- `src/main/java/com/kiwi/keweiaiagent/tools/OpenClawResearchTool.java`
  - 类：`OpenClawResearchTool`
  - 方法：
    - `delegateResearchToOpenClaw(String task, String locale)`：对外暴露给大模型的研究委托工具
    - `buildCommand(String task, String locale)`：拼装 `openclaw agent` 命令参数
    - `buildResearchPrompt(String task, String locale)`：构造发送给 OpenClaw 执行代理的中文调研提示词
    - `extractResearchText(String stdout)`：从 OpenClaw 的 JSON 响应中提取最终调研结果
    - `buildSessionId()`：生成独立的委托会话 id
  - 作用：
    - 将“网页搜索、打开页面、收集来源、汇总摘要”这一整段研究过程委托给外部 OpenClaw 执行代理
    - 当前 Spring AI 主应用只保留编排层角色，不直接暴露本地网页搜索/抓取给默认 Agent 主路径

- `src/main/java/com/kiwi/keweiaiagent/tools/ToolRegistration.java`
  - 类：`ToolRegistration`
  - 本阶段变化：
    - `allTools(...)` 中加入 `OpenClawResearchTool`
    - 默认工具集中移除 `WebSearchTool`、`WebScrapingTool`
  - 作用：
    - 把默认研究路径统一切到远程委托工具
    - 保持默认工具集更聚焦，减少本地工具和远程研究路径的职责重叠

- `src/main/java/com/kiwi/keweiaiagent/agent/ManusSessionService.java`
  - 类：`ManusSessionService`
  - 本阶段变化：
    - 新增 `TaskDomain.RESEARCH`
    - `RESEARCH` 任务域工具子集调整为：
      - `AskUserQuestionTool`
      - `TodoWrite`
      - `delegateResearchToOpenClaw`
      - `doTerminate`
    - `PPT` 任务域也加入 `delegateResearchToOpenClaw`
    - `routeTaskDomain(...)` 增加“调研 / 搜集资料 / 网页来源 / 研究一下”等关键词识别
  - 作用：
    - 让研究类任务和带资料搜集需求的 PPT 任务优先走 OpenClaw 远程调研路径
    - 保持“先分域，再给小工具集”的稳定策略继续成立

- `src/main/java/com/kiwi/keweiaiagent/agent/KeweiManus.java`
  - 类：`KeweiManus`
  - 本阶段变化：
    - 在 `systemPrompt` 中明确规定：
      - 如果选择 `delegateResearchToOpenClaw`，就把这次远程委托视为一个原子级 todo 步骤
      - 不要把远程调研内部再拆成本地伪步骤
    - 在 `nextStepPrompt` 中明确要求：
      - todo 只保留编排层任务，如“澄清目标 / 委托调研 / 汇总结果”
  - 作用：
    - 防止模型一边调用远程研究代理，一边又在本地 todo 里伪造“搜索、抓取、比价”等并不存在的内部步骤

- `src/test/java/com/kiwi/keweiaiagent/tools/OpenClawResearchToolTest.java`
  - 类：`OpenClawResearchToolTest`
  - 方法：
    - `shouldReturnDelegatedResearchTextFromJsonPayload()`：验证可从 OpenClaw JSON 载荷中提取研究结果，并正确拼装命令参数
    - `shouldReturnErrorWhenOpenClawCommandFails()`：验证命令失败时的错误兜底
    - `shouldReturnErrorWhenTaskMissing()`：验证缺少任务描述时的参数校验
  - 作用：
    - 覆盖远程研究委托工具的核心输入输出行为

- `src/test/java/com/kiwi/keweiaiagent/tools/ShellOpenClawCommandRunnerTest.java`
  - 类：`ShellOpenClawCommandRunnerTest`
  - 方法：
    - `shouldDrainLargeStdoutAndStderrWithoutBlocking()`：验证在大量 `stdout/stderr` 并发输出时不会阻塞
  - 作用：
    - 直接覆盖你这次最后一期最核心的难点：进程管线阻塞

- `src/test/java/com/kiwi/keweiaiagent/tools/ToolRegistrationTest.java`
  - 类：`ToolRegistrationTest`
  - 方法：
    - `shouldUseRemoteResearchToolInDefaultToolSet()`：验证默认工具集包含 `delegateResearchToOpenClaw`，且不再包含本地 `searchWebsite` / `scrapeWebsite`
  - 作用：
    - 确认默认工具注册策略已经切换到远程调研委托

- `src/test/java/com/kiwi/keweiaiagent/agent/ManusSessionServiceTest.java`
  - 类：`ManusSessionServiceTest`
  - 新增/强化的方法：
    - `shouldSelectRemoteResearchToolSubsetForResearchPrompt()`：验证研究类任务只拿到研究域工具集
    - 其他已有测试同步更新，确认 `PPT / PDF / EMAIL` 任务域都与新的 `TodoWrite + OpenClaw` 策略兼容
  - 作用：
    - 保证任务分域和工具集筛选在最终阶段仍然稳定

- `src/test/java/com/kiwi/keweiaiagent/agent/KeweiManusPromptTest.java`
  - 类：`KeweiManusPromptTest`
  - 方法：
    - `shouldTreatOpenClawDelegationAsAtomicTodoStep()`：验证 Prompt 已明确把远程研究委托当作原子任务处理
  - 作用：
    - 防止后续改 Prompt 时丢失这条关键约束

- `docs/plans/2026-03-09-openclaw-research-delegation.md`
  - 作用：
    - 记录“把研究任务委托给 OpenClaw”的实现规划与测试拆解
  - 价值：
    - 让最后一期演进路径和设计意图有文档沉淀，而不是只体现在代码里

### 踩坑记录

#### 1. `stdin / stdout` 一开始没有并发处理，导致 pipeline 阻塞

- 问题表现：
  - 子进程表面上已经启动
  - 但在输出较大内容时，整个命令链路会卡住
  - 最终表现为 OpenClaw 委托执行看起来像“没有返回”

- 根因：
  - 进程的标准输出、标准错误本质上都是独立缓冲区
  - 如果只串行读取、或者先等进程结束再读取，而不是并发消费这些输出流，就可能出现缓冲区写满
  - 一旦缓冲区写满，子进程会阻塞，整条 pipeline 也就卡住了

- 处理方式：
  - 在 `ShellOpenClawCommandRunner` 里把 `stdout` 和 `stderr` 改成异步并发读取
  - 通过 `CompletableFuture` 同时消费两个输出流
  - 再配合超时控制和统一结果汇总，避免命令执行阶段因为管线读取顺序不当而卡死

#### 2. 远程调研委托不应该在本地 todo 中被拆成伪内部步骤

- 问题表现：
  - 模型可能一边选择 `delegateResearchToOpenClaw`
  - 一边又在本地 todo 里写出“先搜索网页、再抓取页面、再整理价格”这类实际上已经属于远程代理内部完成的步骤

- 根因：
  - Agent 缺少“编排层任务”和“远程执行层任务”之间的边界约束

- 处理方式：
  - 在 `KeweiManus` 的 prompt 中明确规定：
    - OpenClaw 委托是一个原子级 todo 步骤
    - 本地 todo 只描述编排层动作，例如澄清目标、委托调研、汇总结果

### 本阶段结果

- 默认研究路径已从本地搜索/抓取切换为 OpenClaw 远程调研委托
- Manus 在研究类任务中已形成“TodoWrite 规划 -> OpenClaw 调研 -> 本地汇总”的分层执行方式
- 进程执行层已解决 `stdin/stdout/stderr` 处理不当导致的管线阻塞问题
- Prompt、工具注册、任务分域、命令执行和测试覆盖已经围绕最终架构完成统一

## 当前里程碑总结

到现在为止，这个项目已经把下面这些关键能力串起来了：

- 基础工程和运行环境已经稳定
- 模型接入覆盖阿里百炼与 Ollama
- `ChatClient` 已完成业务封装，并支持会话记忆
- 接口层已经统一响应格式和异常处理
- Advisor 链、自定义 RAG、PgVector、多数据源拆分都已经落地
- 工具调用、MCP、Agent、多模态、TodoWrite、两段式提问都已经打通
- OpenClaw 远程调研委托也已经接进主链路

换句话说，它已经不再是一个“聊天 demo”，而是一套能继续往下长的 Agent 骨架。

## 项目总结

这个项目是一步一步长出来的，不是先画了一张大蓝图再照着填空。

前期先把最基本的模型调用、`ChatClient`、记忆和统一响应做好；
中期开始补多模态、RAG、工具调用、MCP 和 Agent；
后期再把 TodoWrite、AskUserQuestion 两段式交互、OpenClaw 委托调研这些更贴近真实使用场景的能力补齐。

现在回头看，最有价值的不是“功能很多”，而是整个结构已经比较清楚：

- 对话与业务封装层：`LoveApp`、Controller、前端页面
- Agent 编排层：`KeweiManus`、任务分域、TodoWrite、AskUserQuestion、SSE 事件
- 外部能力扩展层：RAG、MCP、文件工具、PPT 生成、OpenClaw 远程研究

中间踩过的坑也比较典型：
- 多模型 Bean 冲突
- 多数据源冲突
- Spring AI 版本升级后的工具调用差异
- Web 场景下用户追问如何暂停和续跑
- 社区工具与 Spring AI 参数绑定不兼容
- 多进程 `stdin/stdout/stderr` 管线阻塞

但好处是，这些坑现在基本都踩明白了。

所以最终留下来的不是一堆零散功能，而是一套还算能打、也还能继续扩展的 AI Agent 实践工程，后续可以在这个基础上继续迭代更多能力，或者迁移到更正式的生产环境中去。
