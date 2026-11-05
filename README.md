# NexPLM

以华天软件 **InforCenter Platform Enterprise (ICP) 9.4 / 平台 8.11.2** 部署包（`../PLMSOFT`）为业务蓝本，
用 Java 技术栈重构的下一代 PLM 平台。目标不是逐行翻译旧系统，而是**保留其业务语义与功能覆盖，
重新设计内核**，得到更稳定、更灵活、更易扩展的产品级实现。

## 目录

| 目录 | 说明 |
|---|---|
| `docs/` | 现状分析、开发方案、数据模型、实施计划、工程规范、迁移方案 |
| `backend/` | Java 17 + Spring Boot 3 后端（模块化单体，按领域分包） |
| `frontend/` | Vue 3 + TypeScript + Vite + Element Plus 前端 |
| `deploy/` | docker-compose、初始化脚本、运维手册 |

## 文档索引

1. [现状分析报告](docs/00-现状分析报告.md) —— 旧系统技术栈、元数据体系、功能域、机制语义、缺陷清单
2. [系统开发方案](docs/01-系统开发方案.md) —— 目标、原则、架构、模块、关键机制设计、非功能要求、风险
3. [数据模型设计](docs/02-数据模型设计.md) —— 通用对象模型、版本模型、关系模型、权限、工作流、文件、编码
4. [分阶段实施计划](docs/03-分阶段实施计划.md) —— 11 个阶段的任务拆解、里程碑、验收标准
5. [工程规范与质量门禁](docs/04-工程规范与质量门禁.md)
6. [旧系统数据迁移方案](docs/05-旧系统数据迁移方案.md)

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

## 状态

阶段 0（工程基线）已落地：工程骨架、统一响应与异常、JWT 鉴权骨架、Flyway 基线迁移、
Docker 依赖编排、前端壳工程。详细进度见 `docs/03-分阶段实施计划.md`。
