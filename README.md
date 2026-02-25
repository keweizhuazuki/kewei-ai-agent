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
- `src/main/java/com/kiwi/keweiaiagent/config`
  - `ChatMemoryConfig`：会话记忆实现装配与切换配置（file/mysql/redis）
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
- `src/main/java/com/kiwi/keweiaiagent/demo/invoke`
  - `SdkAiInvoke`：阿里百炼 SDK 调用示例
  - `SpringAiAiInvoke`：Spring AI + DashScope 调用示例
  - `OllamaAiInvoke`：Spring AI + Ollama 调用示例
  - `DebugBeans`：调试 `ChatModel` Bean 注册情况
  - `TestApiKey`：本地测试 API Key 占位（仅测试用）
- `src/main/resources`
  - `application.yml`：应用、端口、Profile、AI 模型、记忆方式切换与接口文档配置
- `src/test/java/com/kiwi/keweiaiagent`
  - 应用启动测试、`LoveApp` 对话能力测试（文本 / 结构化 / 多模态 / RAG）
- `src/test/java/com/kiwi/keweiaiagent/rag`
  - `LoveAppDocumentLoaderTest`：知识库 Markdown 文档加载测试

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

## 后续进度补充方式（约定）

后续你在这个对话里继续发我“第 X 步完成了什么”，我会按同样格式补充：

- 步骤目标
- 主要新增/修改文件
- 每个文件的类与方法职责
- 阶段结果与下一步衔接
