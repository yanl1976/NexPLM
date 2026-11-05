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
