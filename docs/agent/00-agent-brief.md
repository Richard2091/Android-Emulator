# Agent Brief

## 项目目标

本项目要开发一个自用型 Android 复古游戏大厅。第一版只支持 FC / NES，用户打开 App 后直接进入横屏游戏大厅，选中内置游戏即可游玩。

项目不是专业模拟器前端，也不是 ROM 管理器。第一版目标是做出一个可本地验证的完整闭环：大厅浏览、游戏启动、输入、暂停、存档、设置、退出。

## 必读文档

Agent 开始任何开发任务前必须先读：

- `docs/requirements.md`
- `docs/design.md`
- `docs/agent/00-agent-brief.md`
- `docs/agent/01-build-environment.md`
- `docs/agent/02-implementation-roadmap.md`
- `docs/agent/03-task-breakdown.md`
- `docs/agent/04-acceptance-checklist.md`

涉及专项实现时再读对应技术文档：

- 输入：`docs/technical/input-mapping.md`
- libretro：`docs/technical/libretro-integration.md`
- 存档：`docs/technical/save-system.md`
- ROM 和资源：`docs/technical/assets-and-rom-policy.md`
- UI：`docs/ui/*.md`

## 硬性约束

- 全程使用中文沟通。
- 第一版只支持 FC / NES。
- 第一版统一横屏。
- 焦点、手柄、电视遥控器是基础能力，触摸是补充能力。
- 不实现本地 ROM 导入。
- 不实现游戏源、游戏源浏览、游戏源下载。
- 不实现账号、云同步、在线多人。
- 不实现多机种模拟。
- 不公开提交商业 ROM。
- 不公开发布带 ROM 的 APK。
- ROM、私有测试资源、本地游戏资源必须放在 git 外部或私有环境。
- 第一版最终验收必须使用真实 libretro core 启动至少一个 NES 游戏；`FakeEmulatorSession` 只允许作为阶段性替身。
- UI 层不得直接访问 Room、DataStore、JNI 或 C++。
- libretro 细节必须封装在 emulator 层。

## 默认技术决策

- package 根命名：`com.richard.retrohall`
- 工程结构：单 module Android 工程，module 名为 `app`
- 语言：Kotlin
- UI：Jetpack Compose
- 本地数据库：Room
- 设置存储：DataStore Preferences
- 图片加载：Coil
- 模拟器桥接：Android NDK + JNI + libretro
- 第一阶段可使用 `FakeEmulatorSession` 打通非模拟器流程。

## Agent 执行规则

- 每个任务只做任务清单定义的范围，不擅自扩展功能。
- 每次修改后运行该任务要求的验证命令。
- 如果验证命令失败，先定位根因并修复，再继续下一个任务。
- 如果 libretro 或 NDK 阻塞，先保持 Kotlin 侧接口稳定，使用 `FakeEmulatorSession` 让大厅、输入、暂停、设置、存档流程继续推进。
- 任何涉及 ROM、core、私有资源的改动，都必须检查 `.gitignore` 和提交范围。
- 不要把失败隐藏成成功。无法验证时必须说明卡点、执行过的命令和失败输出摘要。

## 开发完成定义

一个阶段只有同时满足以下条件才算完成：

- 代码已实现。
- 对应自动化测试或手动验证已执行。
- 构建没有新增错误。
- 功能符合对应文档的完成标准。
- 没有引入第一版明确禁止的入口或能力。
