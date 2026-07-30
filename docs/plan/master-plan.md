# Android 复古游戏大厅完整开发计划书

## 目标

把当前文档阶段项目推进成可本地验证的 Android FC / NES 游戏大厅第一版。第一版面向个人研究和本地验证，不公开分发带 ROM 的 APK。

## 执行顺序

1. Phase 00：项目基线整理
2. Phase 01：Android 工程初始化
3. Phase 02：核心模型和分层
4. Phase 03：大厅、搜索、收藏、最近和详情页
5. Phase 04：统一输入系统
6. Phase 05：本地数据层
7. Phase 06：私有资源注入
8. Phase 07：FakeEmulatorSession 闭环
9. Phase 08：libretro 最小集成
10. Phase 09：存档和设置系统
11. Phase 10：最终 QA 和打包

## 阶段文档规则

每个阶段文档必须记录：

- 阶段目标
- 前置条件
- 主要文件
- 实现任务
- 禁止事项
- 自动验证命令
- 手动验收清单
- 阶段完成记录
- 未解决风险

## 第一版硬约束

- 只支持 FC / NES。
- 统一横屏。
- 不提供本地 ROM 导入口。
- 不提供游戏源入口。
- 不提供账号、云同步、在线多人。
- 不提交 ROM、私有封面、私有 libretro core、存档或签名文件。
- `FakeEmulatorSession` 只能作为阶段性替代，最终验收必须使用真实 libretro core 启动至少一个 NES 游戏。
