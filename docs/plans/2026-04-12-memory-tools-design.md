# Memory Tools Design

**Date:** 2026-04-12

## Goal

在当前 `Spring AI 2.0.0-M2` + `spring-ai-agent-utils 0.4.2` 的基础上，为项目补充一层“跨会话长期记忆”能力，参考 Spring 官方 2026-04-07 文章中的 `AutoMemoryTools` 设计，但不引入高风险版本升级。

## Why This Design

官方文章里的 `AutoMemoryToolsAdvisor` 需要 `spring-ai-agent-utils 0.7.0` 且依赖 Spring AI `2.0.0-M4+`。当前仓库仍停留在 `2.0.0-M2`，并且已经接入了 `TodoWriteTool`、`AskUserQuestionTool`、`SkillsTool`、MCP、Manus 分域执行与自定义工具注册。如果直接升级，兼容性风险会扩散到整个 Agent 栈。

因此本次采用“能力对齐、接口本地实现”的思路：

- 保留文章里的核心记忆模型：
  - `MEMORY.md` 作为索引
  - typed memory files（`user` / `feedback` / `project` / `reference`）
  - 六个 purpose-named memory tools
  - 模型通过工具自行读写长期记忆
- 不直接升级到新版 `AutoMemoryToolsAdvisor`
- 在本项目内实现受限目录沙箱，避免暴露全文件系统写权限

## Scope

本次功能覆盖：

- 新增长期记忆目录配置
- 新增 `MemoryView` / `MemoryCreate` / `MemoryStrReplace` / `MemoryInsert` / `MemoryDelete` / `MemoryRename`
- 新增长期记忆 system prompt
- 将长期记忆工具接入 `LoveApp` 的技能 / tools 对话链路
- 将长期记忆工具接入 `KeweiManus`
- 在 `PROCEDURES.md` 中补充本次能力和关键类说明

本次不做：

- Spring AI 主版本升级
- 官方 `AutoMemoryToolsAdvisor` 的一比一 API 接入
- 自动 consolidation trigger
- 基于向量检索的长期记忆召回

## Architecture

### 1. Memory Storage Model

长期记忆目录通过配置项指定，默认落在 `tmp/agent-memory`。目录结构遵循文章中的模式：

- `MEMORY.md`
  - 作为索引文件，保存各个 memory 文件的单行摘要链接
- `*.md`
  - 具体记忆文件，带 YAML frontmatter

单个 memory 文件示例：

```md
---
name: user profile
description: 用户偏好简洁回答
type: user
---

用户偏好简洁回答。
```

### 2. Sandbox Model

所有 memory 工具仅允许访问配置的 `memoriesRootDirectory`。实现层统一做路径归一化和越界校验：

- 输入路径统一按 root 下的相对路径处理
- 禁止 `..` 跳出根目录
- 禁止读写 root 外部路径

### 3. Tool Semantics

六个工具语义与文章保持一致：

- `MemoryView`
  - 读取文件并带行号
  - 或列出目录内容
- `MemoryCreate`
  - 新建 memory 文件
- `MemoryStrReplace`
  - 替换文件中唯一且精确的字符串
- `MemoryInsert`
  - 在指定行后插入文本
- `MemoryDelete`
  - 删除文件或目录
- `MemoryRename`
  - 重命名或移动文件

### 4. Prompting Strategy

新增 memory system prompt，提示模型：

- 在会话开始时先查看 `MEMORY.md`
- 只把值得长期保留的事实写入 memory
- 新增记忆时走两步：
  - 先 `MemoryCreate`
  - 再 `MemoryInsert` 更新 `MEMORY.md`
- 修改或删除 memory 时保持 `MEMORY.md` 与文件系统一致

### 5. Integration Points

- `LoveApp`
  - 在技能 / tools 对话中把 memory prompt 拼入 system prompt
  - 注册 memory tool callbacks
- `KeweiManus`
  - 在主系统提示词中加入长期记忆使用约束
  - 将 memory tools 放入 Manus 默认工具集

## Testing Strategy

按 TDD 实施：

- 工具单测：
  - 路径沙箱
  - 文件创建 / 查看 / 插入 / 替换 / 删除 / 重命名
  - 唯一字符串替换校验
- 注册测试：
  - 默认工具集包含全部 `Memory*` 工具
- Agent / App 测试：
  - 验证 memory prompt 和 memory tool 可被装配

## Risk Notes

- 当前仓库主工作区存在大量未提交文件，因此本次开发在独立 worktree 中完成
- 推送时仅提交本次 memory / docs 相关改动，不混入现有主工作区的脏变更
