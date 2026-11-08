# NexPLM

以华天软件 **InforCenter Platform Enterprise (ICP) 9.4 / 平台 8.11.2** 部署包（`../PLMSOFT`）为业务蓝本，
用 Java 技术栈重构的下一代 PLM 平台。目标不是逐行翻译旧系统，而是**保留其业务语义与功能覆盖，
重新设计内核**，得到更稳定、更灵活、更易扩展的产品级实现。

## 目录

| 目录 | 说明 |
|---|---|
| `docs/` | 现状分析、开发方案、数据模型、实施计划、工程规范、迁移方案、探查记录 |
| `backend/` | Java 17 + Spring Boot 3 后端（模块化单体，按领域分包） |
| `frontend/` | Vue 3 + TypeScript + Vite + Element Plus 前端 |
| `deploy/` | docker-compose、初始化脚本、运维手册 |

## 文档索引

1. [现状分析报告](docs/00-现状分析报告.md) —— 旧系统技术栈、元数据体系、功能域、机制语义、缺陷清单
2. [系统开发方案](docs/01-系统开发方案.md) —— 目标、原则、架构、模块、关键机制设计、非功能要求、风险
3. [数据模型设计](docs/02-数据模型设计.md) —— 通用对象模型、版本模型、关系模型、权限、工作流、文件、编码
4. [分阶段实施计划](docs/03-分阶段实施计划.md) —— 11 个阶段的任务拆解、里程碑、验收标准；P0~P10 已落地，**正文冻结为历史基线**，本文件同时承担**全项目唯一遗留台账**（A 环境阻塞 / B 功能遗留 / C 计划未开工 / D 待业务确认）
5. [工程规范与质量门禁](docs/04-工程规范与质量门禁.md)
6. [旧系统数据迁移方案](docs/05-旧系统数据迁移方案.md)
7. [系统探查记录](docs/06-探查记录.md) —— 每次代码/数据/环境探查的集中记录（结论、证据、决策、遗留）
8. [安全加固与等保自查](docs/07-安全加固与等保自查.md)
9. [运维手册](docs/08-运维手册.md)
10. [UI 风格分析与融合方案](docs/09-UI风格分析与融合方案.md) —— 旧系统视觉规范分析、SIPM 截图分析、设计令牌与页面级规范
11. [深度探查：零部件管理](docs/10-深度探查-零部件管理.md) —— 旧系统 PSEITEM 页面全量还原（列/条件/按钮/页签/字段/状态机）与新系统逐项差距、复刻分批方案
12. [全系统页面复刻深度开发计划](docs/11-全系统页面复刻深度开发计划.md) —— 旧系统 19 个一级域/113 菜单项/页面与 DLL 全景、逐域差距矩阵、批次 A~H 排期与验收口径；**§10 复刻台账（`REP-*`）是深度开发进度的唯一权威**（一行一个蓝本功能点的六维判等状态）
13. [PLM 复刻方法论](docs/12-PLM复刻方法论.md) —— **复刻类工作的执行规程**：R0~R8 流水线（立项→取证→上下游→差异决策→设计→实现→判等验证→留口→收口）、取证清单与 DLL 红线、六维判等清单、GAP 与遗留台账规则、反模式清单、任务卡/ADR/验收单模板。任务卡存放于 [`docs/tasks/`](docs/tasks/)（一任务一文件，如 `T-H-01.md`）
14. [TCPLM 数据库结构字典](docs/13-TCPLM数据库结构.md) —— PLM 蓝本库（`198.168.6.26:1433/TCPLM`）的**结构基线**：实例信息、行数 Top30、结构要点、业务域索引、483 表逐表列定义、62 视图、编码体系（`CODERULE`/`CODERES`）。由 [`tools/Dump-TcplmSchema.ps1`](tools/Dump-TcplmSchema.ps1) **只读**生成，结构变化时重跑即可
15. [TCPLM 分类数据清单](docs/14-TCPLM分类数据清单.md) —— 蓝本分类**数据基线**：264 分类 + 41 分组全量清单、一物多分类分布（16,276 零件中仅 66 件多分类）、属性/属性值规模、迁移口径建议与未解字段。由 [`tools/Dump-TcplmClassify.ps1`](tools/Dump-TcplmClassify.ps1) **只读**生成（`03` A5 数据判等的依据）

## 快速开始（开发环境）

```bash
# 1. 启动依赖（PostgreSQL / Redis / MinIO / RabbitMQ）
cd deploy && docker compose up -d

# 2. 后端
cd backend && mvn spring-boot:run

# 3. 前端
cd frontend && npm install && npm run dev
```

- 后端默认端口 `8080`，OpenAPI: `http://localhost:8080/swagger-ui/index.html`
- 前端默认端口 `5173`，已配置 `/api` 代理到后端
- 默认管理员：`admin / Admin@12345`（首次登录强制改密）

### 本机（Windows，无 Docker）实际启动方式

本机没有 Docker，数据库是 zonky 缓存的 PostgreSQL 二进制 `initdb` 到 `PLM_Clone/_pgdata`、端口 5432（详见
[`docs/03` 第 24 条](docs/03-分阶段实施计划.md) 与 [`docs/06` EXP-20261107-09](docs/06-探查记录.md)）。
**业务数据在 `postgres` 库、凭据 `postgres/postgres`**，与 `application.yml` 默认的 `nexplm/nexplm` 不同，
所以必须显式注入 `NEXPLM_DB_*`，否则后端会卡在 `HikariPool-1 - Starting...` 无限重试。

```powershell
# 0. 确认 PG 在跑（未跑则先 pg_ctl -D PLM_Clone/_pgdata -p 5432 start）
Get-NetTCPConnection -LocalPort 5432 -State Listen

# 1. 后端（工作目录必须是 backend：NEXPLM_FILE_ROOT=./data/files 是相对路径）
cd backend
$env:NEXPLM_DB_URL='jdbc:postgresql://localhost:5432/postgres'
$env:NEXPLM_DB_USER='postgres'
$env:NEXPLM_DB_PASSWORD='postgres'
java -jar plm-app\target\nexplm.jar     # 日志：logs\stdout.log、logs\stderr.log

# 2. 前端（另开终端）
cd frontend
npm install; npm run dev                # 日志：dev.log，端口 5173

# 3. 冒烟
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

- **改了后端代码**（尤其 `plm-platform`）不能只 `compile`：必须停进程 →
  `mvn -pl plm-app -am package -DskipTests` → 重启 jar（fat jar 打包时文件被占用会 rename 失败）。
- **只改前端**无需重新打包后端，Vite 热更新即可。
- 停服务：`Get-CimInstance Win32_Process -Filter "Name='java.exe' or Name='node.exe'"` 找到 PID 后
  `Stop-Process -Id <pid> -Force`。

## 状态

| 阶段 | 状态 | 说明 |
|---|---|---|
| P0 工程基线 | ✅ 完成 | Maven 三模块、统一响应/错误码/审计、JWT 鉴权、Flyway 基线（26 表）+ 种子、Docker 编排、Vue3 壳工程 |
| P1 平台内核 | ✅ 完成 | 组织架构、用户管理、角色与功能授权、元数据引擎（类型/字段/枚举/发布校验）、规则权限（嵌套条件树+试算）、对象 ACL（DENY 优先+属主白名单）、生命周期状态机与设计器、数据隔离（ALL/COMPANY/ORG/SELF） |
| P2 图文档 PDM | ✅ 完成 | 文件仓（分片直传/秒传/回收）、工作空间（三空间文件夹树/收藏/最近访问）、图文档（文档类型、文件角色、检入检出、版本回溯）、对象查看器（`meta_tab` 驱动七页签）、文档分类（分类树 + 必填/枚举/可继承属性 + 按分类检索）、全文检索（tsvector + 中文 ILIKE 兜底 + 高亮片段 + 保存查询条件）、批量操作（主文件打包 zip、批量授权按条返回结果）、预览转换（可插拔转换器、状态可见可重试）、批量导入（CSV 逐行结果、模板下载） |
| P3 零部件与 BOM | ✅ 完成 | 零部件（8 种类型、自动取号 `PN-000001`、规格/材料/单位/重量/供应方式/标准号、列表筛选与详情编辑）、BOM 树（三视图、增删改移、防环防重复、用量校验、被引用、结构校验）、展开与汇总（逐层累乘 + 损耗率、`leafOnly` 叶子口径）、替代件（全局/局部、双向、优先级）、基线（冻结 + 差异对比）、Excel 导出/增量导入（预演后提交）、批量改用量/批量替换（越权与冲突行跳过并给出原因）、序号上移下移、10,520 行性能门禁（树 351ms / 展开 375ms / 汇总 202ms） |
| P4 工作流 | ✅ 完成 | Flowable 7.1 嵌入式（`ACT_*` 引擎自管、BPMN 启动自动部署）、流程绑定 `wf_binding`（含 `condition_json` 条件选流程）、发起/我的待办/办理/驳回/撤回/实例详情、会签全票流程模型（发起 → 初审 → 会签 → 归档）、执行人解析 SPI（用户/角色/组织/组/组+角色/发起人/表单字段/项目角色）、加签、委托代办理（`wf_delegate`，生效区间 + 代办理痕迹）、我的已办 / 我发起的流程 / 批量审批（部分成功语义）、**三个业务插件**（`ChangeState` 驱动生命周期、`SendNotification` 节点/归档通知入通知通道、`ErpPublish` 归档后生成金蝶下发任务）、电子签名（`wf_signature`，含主文件摘要核验）、一致性巡检、超时催办（幂等）、bpmn-js 设计器 + 前端流程中心（待办/已办/我发起/委托/实例详情） |
| P5+ | ⏳ 未开始 | 见 `docs/03-分阶段实施计划.md` |

测试：`mvn test` 使用嵌入式 PostgreSQL 跑通「Flyway 迁移 → 登录 → 组织/用户/角色/元数据 → 文件仓 → 图文档 → 工作空间 → 零部件/BOM → 工作流会签」真实链路（39 个用例：12 单测 + 27 集成，含 10,520 行 BOM 性能门禁）。命令见 `docs/06-探查记录.md` 的 EXP-20261106-07。
