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
| `030-item_sample.sql` | `tools/Gen-ItemMigration.ps1` | 零部件**样例** 139 条（每类 ≤15 条，非全量），`plm_object` + `item_master` 成对插入，单分类口径 |
| `040-item_bom_parts.sql` | `tools/Gen-BomMigration.ps1` | BOM 迁移的**补件** 113 个（050 引用的父子件中不在 030 的部分），id 基址与 030 不同 |
| `050-bom_line.sql` | `tools/Gen-BomMigration.ps1` | BOM 行 **100 条**（视图 DESIGN / 类型 QUOTE / `qty=1`），分布 16 个父件 |
| `060-code_kind_value.sql` | `tools/Gen-CodeKindMigration.ps1` | 编码种类 **4** + 码值 **292** + 分类编码路径 **258**（`Kind_xxx/1001/02/*`），对应蓝本 `CODEKIND`/`CODEVALUE`/`CLASSCODEPATH` |

**执行顺序**：`010`（分类树）→ `020`（属性）→ `030`（样例零部件，`class_id` 依赖 010）→ `040`（BOM 补件）→ `050`（BOM 行，依赖 030+040 的 id 映射）→ `060`（编码种类/码值/分类路径，`UPDATE doc_class.code_path`，依赖 010 与 `V81`）。顺序倒了会挂不上父分类/父子件/分类路径。

```bash
# 重新生成（需要蓝本库只读账号；脚本只发 SELECT）
pwsh tools/Gen-DocClassMigration.ps1 -User <user> -Password <pwd>
pwsh tools/Gen-DocClassAttrMigration.ps1 -User <user> -Password <pwd>
pwsh tools/Gen-ItemMigration.ps1 -User <user> -Password <pwd> -PerType 15
pwsh tools/Gen-BomMigration.ps1 -User <user> -Password <pwd> -Limit 100
pwsh tools/Gen-CodeKindMigration.ps1 -User <user> -Password <pwd>
```

> `040`/`050` 由同一个生成器一次产出（先补件、再 BOM 行）；执行顺序不能颠倒。

**重跑前清空样例数据**（可选；现场确认现有物料都是测试数据时才做）：

```sql
BEGIN;
DELETE FROM bom_line WHERE parent_object_id IN (SELECT id FROM plm_object WHERE object_type = 'PdmItem')
                        OR child_object_id  IN (SELECT id FROM plm_object WHERE object_type = 'PdmItem');
DELETE FROM item_view   WHERE item_object_id IN (SELECT id FROM plm_object WHERE object_type = 'PdmItem');
DELETE FROM item_master WHERE object_id IN (SELECT id FROM plm_object WHERE object_type = 'PdmItem');
DELETE FROM plm_object  WHERE object_type = 'PdmItem';
COMMIT;
```

> 会连带删除这些零部件的 BOM 行与视图定义；`doc_class` / `doc_class_attr` 不受影响（可重复跑 `010`/`020`）。

- 每条 INSERT 都带 `WHERE NOT EXISTS (同 entity_code + code)`，可重复执行；
- `id` 用固定基址 `9000000000001` 起递增（落在雪花 id 之外），`parent_id` 靠它稳定引用，`path` 按 `/父编码/子编码` 逐层拼；
- **执行前**必须确认目标库写权限与业务窗口（`docs/03` A3 迁移演练），并先备份（`deploy/backup.sh`）；
- 分类**属性**（蓝本 `CLASSIFYATTRIBUTE` 120 条）尚未生成，等分类树在目标库验证通过后再补 `020-doc_class_attr.sql`。
