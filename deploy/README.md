# 部署与本地开发

## 1. 本地依赖

```bash
cd deploy
cp .env.example .env     # 按需修改
docker compose up -d
docker compose ps
```

| 组件 | 地址 | 默认凭据 |
|---|---|---|
| PostgreSQL | localhost:5432/nexplm | nexplm / nexplm |
| Redis | localhost:6379 | 无密码 |
| MinIO API | localhost:9000 | nexplm / nexplm12345 |
| MinIO 控制台 | http://localhost:9001 | 同上 |
| RabbitMQ | localhost:5672 | nexplm / nexplm12345 |
| RabbitMQ 控制台 | http://localhost:15672 | 同上 |

## 2. 启动后端

```bash
cd backend
mvn -q clean package -DskipTests
java -jar plm-app/target/nexplm.jar
# 或开发模式
mvn -pl plm-app -am spring-boot:run
```

- 健康检查：`GET http://localhost:8080/actuator/health`
- 接口文档：`http://localhost:8080/swagger-ui.html`
- 首次启动自动执行 Flyway 迁移并创建管理员（`NEXPLM_ADMIN_PASSWORD`，默认 `Admin@12345`）

## 3. 启动前端

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173 ，/api 已代理到 8080
```

## 4. 生产要点

1. 必须覆盖 `NEXPLM_JWT_SECRET`（≥32 字节随机值）与 `NEXPLM_ADMIN_PASSWORD`；
2. 数据库、MinIO、RabbitMQ 凭据通过环境变量或密钥管理注入，禁止写入配置文件；
3. 前置 Nginx 终止 TLS，开启 `X-Forwarded-For` 透传与限流；
4. MinIO 使用纠删码集群（≥4 节点），桶 `plm-vault` 需提前创建并设置版本化；
5. 备份：PostgreSQL `pg_basebackup` + WAL 归档；MinIO 站点复制；备份恢复每季度演练一次。
6. **中文字体（工艺卡片 PDF 导出必需）**：PDF 必须嵌入中文字体，否则在其它机器上显示为方块。
   镜像内安装字体（如 `fonts-noto-cjk`）或把 `.ttf` / `.ttc` 挂载进容器，然后用
   `NEXPLM_CAPP_PDF_FONT_PATH`（对应配置 `nexplm.capp.pdf.font-path`）指定路径；
   `.ttc` 需带索引，形如 `/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0`。
   未配置时后端会按 Windows/Linux 常见路径探测；都找不到则导出接口返回 400 并提示配置该项
   （不会生成乱码 PDF）。

## 依赖漏洞扫描（`docs/07` 待整改 ②）

`deploy/ci/security-scan.ps1` 封装 [trivy](https://github.com/aquasecurity/trivy)，CI 里作为构建门禁：

```bash
pwsh deploy/ci/security-scan.ps1                                    # 文件系统（依赖 + 配置）
pwsh deploy/ci/security-scan.ps1 -Target image -Image nexplm:<tag>  # 容器镜像
```

- 默认 `--severity HIGH,CRITICAL --ignore-unfixed --exit-code 1`：发现高危即失败；
- 报告落 `deploy/ci/reports/trivy-*.json`，建议作为 CI artifact 保留 30 天；
- **本机未安装 trivy 时返回 exit 2 并打印安装方式**，不会静默通过；
- CI 示例：GitLab `script: [ "pwsh deploy/ci/security-scan.ps1" ]`；Jenkins 加一个 `powershell` step；
- 脚本存在 **不等于**该项已验收：CI 未真正接入前，`docs/07` 里保持"待整改"。

## 旧系统数据迁移脚本（`deploy/migration/`）

脚本由 `tools/` 下的生成器从蓝本库**只读**导出，产物是 SQL 文本，**不会自动执行**：

| 文件 | 生成器 | 内容 |
|---|---|---|
| `010-doc_class.sql` | `tools/Gen-DocClassMigration.ps1` | 零部件分类树（305 节点 = 4 根 + 37 分组 + 264 分类），`entity_code=PdmItem`，幂等 INSERT |
| `020-doc_class_attr.sql` | `tools/Gen-DocClassAttrMigration.ps1` | 分类属性 1,287 条（由 1,287 条「属性→分类」关系展开），`value_type` 全 `STRING`，属性编码加 `ATTR` 前缀 |

**执行顺序**：`010`（分类树）→ `020`（属性）。`020` 的 `class_id` 从 `010` 解析而来，顺序倒了会挂不上父分类。

```bash
# 重新生成（需要蓝本库只读账号；脚本只发 SELECT）
pwsh tools/Gen-DocClassMigration.ps1 -User <user> -Password <pwd>
pwsh tools/Gen-DocClassAttrMigration.ps1 -User <user> -Password <pwd>
```

- 每条 INSERT 都带 `WHERE NOT EXISTS (同 entity_code + code)`，可重复执行；
- `id` 用固定基址 `9000000000001` 起递增（落在雪花 id 之外），`parent_id` 靠它稳定引用，`path` 按 `/父编码/子编码` 逐层拼；
- **执行前**必须确认目标库写权限与业务窗口（`docs/03` A3 迁移演练），并先备份（`deploy/backup.sh`）；
- 分类**属性**（蓝本 `CLASSIFYATTRIBUTE` 120 条）尚未生成，等分类树在目标库验证通过后再补 `020-doc_class_attr.sql`。
