# P10-4 性能压测

DoD 原文：**压测达标（200 并发 P95 < 1s）**。

本目录提供一套**零依赖**的端到端压测脚本（`load-test.mjs`，只用 Node 18+ 内置 `fetch`），
用于在**目标环境**（预发/联调/生产，需审批）跑正式验收；本地单元测试里的并发基线
（`ApiConcurrencyBaselineTest`）只作回归门禁，不能替代验收——它不走网络与 Tomcat 线程池。

## 为什么不用 k6 / JMeter

开发机与多数预发机器未必装了这些工具，装一次的成本高于写这 300 行；而 Node 自带 fetch，
任何能跑前端的机器都能复跑**同一份脚本**，避免"我这边压出来 800ms、你那边 3000ms 但脚本不一样"。

## 前置条件

1. 目标环境已启动，数据库是**要验收的那套**（不是 H2/Embedded PG）；
2. 一个能登录的账号（注意首次登录强制改密的账号要先改密，或用 `--token` 直接给令牌）；
3. **测试数据**：
   - 列表场景：文档/零部件各 ≥ 1 万条更接近真实分布；
   - BOM 场景：需要一个 1 万行量级的根件（id 通过零部件列表接口查编码得到）；
   - 待办场景：需要有真实待办（发起几条流程）；
   - 审批/改用量场景：会**真实写库**，只在专用测试单据上跑。

## 用法

```bash
# 1) 只读场景（安全，可反复跑）：文档/零部件列表 + 待办
node deploy/perf/load-test.mjs --base-url http://staging:8080 --username admin --password 'Admin@12345'

# 2) BOM 场景（1 万行根件）
node deploy/perf/load-test.mjs --base-url http://staging:8080 --token "$TOKEN" \
  --scenario bom --bom-item-id 1234567890123456789

# 3) 审批（写，只在测试单据上）
node deploy/perf/load-test.mjs --base-url http://staging:8080 --token "$TOKEN" \
  --scenario approve --task-id 9876543210987654321

# 4) 批量改用量（写，幂等：同一用量重复写）
node deploy/perf/load-test.mjs --base-url http://staging:8080 --token "$TOKEN" \
  --scenario qty --bom-item-id 123 --line-ids 111,222,333

# 5) 正式验收（200 并发 30s，输出 JSON 报告）
node deploy/perf/load-test.mjs --base-url http://staging:8080 --token "$TOKEN" \
  --scenario all --bom-item-id 123 --concurrency 200 --duration 30 \
  --report deploy/perf/load-$(date +%Y%m%d-%H%M).json
```

## 参数

| 参数 | 默认 | 说明 |
|---|---|---|
| `--base-url` | `http://localhost:8080` | 目标地址（不要带尾斜杠） |
| `--username` / `--password` | `admin` / `Admin@12345` | 登录取令牌 |
| `--token` | 空 | 直接给令牌，优先于账号密码 |
| `--scenario` | `all` | `list` / `bom` / `todo` / `approve` / `qty` / `all` |
| `--concurrency` | `200` | 并发 worker 数 |
| `--duration` | `30` | 压测时长（秒） |
| `--p95` | `1000` | P95 预算（毫秒），超了退出码 1 |
| `--max-error` | `1` | 允许错误率（%），超了退出码 1 |
| `--bom-item-id` | 空 | `bom` / `qty` 场景必填 |
| `--task-id` | 空 | `approve` 场景必填 |
| `--line-ids` | 空 | `qty` 场景必填，逗号分隔 |
| `--report` | 空 | 结果 JSON 落盘路径 |
| `--no-threshold` | 关 | 只测不判（用于观察基线） |

## 结果解读

脚本输出每个端点的 `count / failed / p50 / p95 / p99 / max`，以及整体 QPS 与错误率。

- **达标**：整体 P95 ≤ 1000ms 且错误率 ≤ 1%；
- **失败的口径**：非 2xx 且不在该场景的 `acceptStatus` 白名单内（例如审批任务已完成后的 409 属预期，
  不算系统错误——它证明接口语义对，不是性能问题）；
- 退出码即门禁结果，可直接串到流水线。

## 压测时的注意事项

1. **先冷后热**：JVM 预热会显著影响首轮数字，正式结论取第二轮（第一轮当 warmup）；
2. **别在生产压写场景**：`approve` / `qty` 会改业务数据；生产只做只读场景并事先报备；
3. **看数据库**：跨网络访问 PG 时，`p95` 里很可能一半是网络往返。若 DB 与应用不在同一台机器，
   结论要注明拓扑；
4. **连接池**：后端 `spring.datasource.hikari.maximum-pool-size` 决定并发上限，200 并发下若池只有 10，
   排队时间会全部计入 `p95`——先确认池大小，再判断是不是代码慢；
5. **记录环境**：报告里补上机器配置、DB 是否同机、数据规模（本文下方模板）。

## 验收记录模板

```markdown
- 日期 / 执行人：
- 环境：<主机配置>，DB 是否同机：<是/否>，数据规模：文档 x 条 / 零部件 y 条 / BOM 根件 z 行
- 命令：node deploy/perf/load-test.mjs --scenario all --concurrency 200 --duration 30 ...
- 结果：QPS ___，P50 ___ms，P95 ___ms，P99 ___ms，错误率 ___%
- 结论：达标 / 未达标（未达标时列瓶颈与优化项）
```

## 本地基线（不是验收）

`ApiConcurrencyBaselineTest` 在嵌入式 PG 上用多线程打 MockMvc，验证**应用层无共享状态瓶颈**
（例如误加的全局锁、非线程安全缓存）。它不走 Tomcat 线程池与网络，数字只能横向对比自己，
不能用来证明"200 并发 P95 < 1s"。
