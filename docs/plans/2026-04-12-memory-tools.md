# Memory Tools Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add sandboxed long-term memory tools, wire them into LoveApp and Manus, and document the capability in `PROCEDURES.md`.

**Architecture:** Implement six local `Memory*` tools backed by a configured memories root directory and a companion memory system prompt. Register the tools in the default tool set, expose them to `LoveApp` and `KeweiManus`, and keep the implementation compatible with Spring AI `2.0.0-M2`.

**Tech Stack:** Java 21, Spring Boot 4, Spring AI 2.0.0-M2, JUnit 5

---

### Task 1: Add failing tests for sandboxed memory tools

**Files:**
- Create: `src/test/java/com/kiwi/keweiaiagent/tools/MemoryToolsTest.java`
- Reference: `src/main/java/com/kiwi/keweiaiagent/tools`

**Step 1: Write the failing test**

Add tests for:
- creating a memory file under the configured root
- viewing a file with line numbers
- inserting text after a line in `MEMORY.md`
- replacing a unique string
- renaming and deleting a file
- rejecting paths that escape the root directory

**Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=MemoryToolsTest test`
Expected: FAIL because the memory tool classes do not exist yet.

**Step 3: Write minimal implementation**

Create the supporting memory tool classes and shared path utility needed to satisfy the tests.

**Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=MemoryToolsTest test`
Expected: PASS

**Step 5: Commit**

Commit the new tools and their tests after verification.

### Task 2: Register memory tools in the default tool set

**Files:**
- Modify: `src/main/java/com/kiwi/keweiaiagent/tools/ToolRegistration.java`
- Modify: `src/test/java/com/kiwi/keweiaiagent/tools/ToolRegistrationTest.java`

**Step 1: Write the failing test**

Extend `ToolRegistrationTest` to assert the default tool set includes:
- `MemoryView`
- `MemoryCreate`
- `MemoryStrReplace`
- `MemoryInsert`
- `MemoryDelete`
- `MemoryRename`

**Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=ToolRegistrationTest test`
Expected: FAIL because the tools are not yet registered.

**Step 3: Write minimal implementation**

Update tool registration to create and expose the new memory tool callbacks.

**Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=ToolRegistrationTest test`
Expected: PASS

**Step 5: Commit**

Commit the registration changes after verification.

### Task 3: Add long-term memory prompt and app configuration

**Files:**
- Create: `src/main/resources/prompts/auto-memory-tools-system-prompt.md`
- Create: `src/main/java/com/kiwi/keweiaiagent/config/LongTermMemoryConfig.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/java/com/kiwi/keweiaiagent/app/LoveApp.java`

**Step 1: Write the failing test**

Add or extend app-focused tests to verify:
- memory prompt can be loaded
- `LoveApp` includes memory tools in tool-driven chat path

**Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=LoveAppTest test`
Expected: FAIL because memory prompt/configuration is missing.

**Step 3: Write minimal implementation**

Add configuration properties for the memories root directory and prompt resource, then update `LoveApp` to combine the prompt with the existing system behavior for tool-based conversations.

**Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=LoveAppTest test`
Expected: PASS for the relevant assertions.

**Step 5: Commit**

Commit the app wiring changes after verification.

### Task 4: Wire long-term memory into Manus

**Files:**
- Modify: `src/main/java/com/kiwi/keweiaiagent/agent/KeweiManus.java`
- Modify: `src/main/java/com/kiwi/keweiaiagent/agent/ManusSessionService.java`
- Modify: `src/test/java/com/kiwi/keweiaiagent/agent/ManusSessionServiceTest.java`

**Step 1: Write the failing test**

Add a test proving Manus tool selection keeps the memory tools available for general and research-oriented tasks.

**Step 2: Run test to verify it fails**

Run: `./mvnw -Dtest=ManusSessionServiceTest test`
Expected: FAIL because Manus does not yet include the memory tools.

**Step 3: Write minimal implementation**

Update Manus prompts and domain tool routing to include the new memory tools without removing the current task-specific tools.

**Step 4: Run test to verify it passes**

Run: `./mvnw -Dtest=ManusSessionServiceTest test`
Expected: PASS

**Step 5: Commit**

Commit the Manus integration changes after verification.

### Task 5: Document the capability

**Files:**
- Modify: `PROCEDURES.md`

**Step 1: Write the failing check**

Create a short checklist of required documentation updates:
- project goals mention long-term memory
- directory overview mentions memory tools/config
- feature section explains usage and behavior

**Step 2: Run manual verification to confirm missing content**

Run: `rg -n "长期记忆|MemoryView|MEMORY.md|memory tools" PROCEDURES.md`
Expected: existing doc is incomplete for this feature.

**Step 3: Write minimal implementation**

Update `PROCEDURES.md` to describe the new memory model, tools, configuration, and integration points.

**Step 4: Run verification**

Run: `rg -n "长期记忆|MemoryView|MEMORY.md|memory tools" PROCEDURES.md`
Expected: new sections are present.

**Step 5: Commit**

Commit the documentation update after verification.

### Task 6: Final verification and push

**Files:**
- Verify the full working tree

**Step 1: Run targeted tests**

Run:
- `./mvnw -Dtest=MemoryToolsTest,ToolRegistrationTest,ManusSessionServiceTest test`

Expected: PASS

**Step 2: Run a broader compile check**

Run: `./mvnw -q -DskipTests compile`
Expected: PASS

**Step 3: Review git diff**

Run: `git status --short && git diff --stat`
Expected: only memory-related source, tests, prompt, and docs changes.

**Step 4: Commit**

Commit with a message such as: `feat: add long-term memory tools`

**Step 5: Push**

Push the isolated branch and, if explicitly desired, fast-forward the remote `main` from this verified branch state.
