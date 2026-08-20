# Android Emulator Project Docs

本文档目录用于让 Agent 继续维护和收口 Android 复古游戏大厅。阅读顺序很重要：先读项目边界，再读执行路线，最后按专项文档实现。

## 当前项目状态

当前仓库已经初始化 Android 工程，并进入结构收口和验收阶段。已有：

- 需求说明：`docs/requirements.md`
- 设计文档：`docs/design.md`
- UI 概念图：`docs/ui-concepts/`
- Agent 执行文档：`docs/agent/`
- 技术专项文档：`docs/technical/`
- UI 专项文档：`docs/ui/`
- 测试和发布文档：`docs/testing/`、`docs/release/`
- 实际代码：`app/`

## Agent 必读顺序

开始任何开发前，按顺序阅读：

1. `docs/agent/00-agent-brief.md`
2. `docs/requirements.md`
3. `docs/design.md`
4. `docs/agent/01-build-environment.md`
5. `docs/agent/02-implementation-roadmap.md`
6. `docs/agent/03-task-breakdown.md`
7. `docs/agent/04-acceptance-checklist.md`
8. `docs/agent/05-resource-repository-refactor.md`（资源仓库重构验收跟踪）

## 专项文档索引

工程结构：

- `docs/technical/project-structure.md`

数据模型：

- `docs/technical/data-model.md`

输入系统：

- `docs/technical/input-mapping.md`

模拟器集成：

- `docs/technical/libretro-integration.md`

存档系统：

- `docs/technical/save-system.md`

ROM 和资源边界：

- `docs/technical/assets-and-rom-policy.md`
- `docs/technical/private-resource-injection.md`
- `docs/technical/resource-repository-spec.md`

大厅 UI：

- `docs/ui/hall-ui-spec.md`

游戏详情页：

- `docs/ui/game-detail-spec.md`

游戏页：

- `docs/ui/game-screen-spec.md`

设置页：

- `docs/ui/settings-spec.md`

焦点导航：

- `docs/ui/focus-navigation-spec.md`

测试：

- `docs/testing/test-strategy.md`
- `docs/testing/instrumented-test-contract.md`
- `docs/testing/manual-qa.md`

打包和风险：

- `docs/release/build-and-package.md`
- `docs/release/known-risks.md`
- `docs/release/v0.1.0-apk-release.md`
- `docs/release/v1-local-validation-report.md`

## 执行原则

- 先保证代码、文档、测试和目录结构一致。
- 保留 `FakeEmulatorSession` 作为兜底；真实 ROM 只作为本地私有资源验收物，公开 Release APK 可以打包已审查许可证的 libretro core。
- 每完成一个任务，运行对应验证命令。
- 不提交 ROM、私有资源、签名文件或带 ROM 的 APK。
- 不添加第一版明确禁止的入口：本地 ROM 导入、游戏源、账号、云同步。
