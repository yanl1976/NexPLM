#!/usr/bin/env node
/**
 * NexPLM 端到端压测脚本（P10-4）——零依赖，只用 Node 内置 fetch。
 *
 * 为什么不用 k6/JMeter：开发机与常见预发环境不一定装了这些工具，装一次的成本比写 300 行脚本高；
 * 而 Node 18+ 自带 fetch，任何能跑前端的机器都能复跑同一套脚本，验收时双方看到的是同一份代码。
 *
 * 用法（先在目标环境准备好账号与测试数据）：
 *   node deploy/perf/load-test.mjs --base-url http://staging:8080 --username admin --password 'Admin@12345'
 *   node deploy/perf/load-test.mjs --base-url http://staging:8080 --token "$TOKEN" --scenario bom --bom-item-id 123
 *
 * 场景：
 *   list    文档/零部件列表分页（读）
 *   bom     BOM 树 + 汇总 + 反查（读，需要 --bom-item-id）
 *   todo    我的待办列表（读）
 *   approve 审批通过（写，会真实推进流程，需要 --task-id；只在专用测试单据上跑）
 *   qty     批量改用量（写，需要 --bom-item-id 与 --line-ids，改的是同一个值所以幂等）
 *
 * 退出码：P95 超过 --p95 预算或错误率超过 --max-error（默认 1%）时为 1，可直接用于流水线门禁。
 */
import { writeFileSync } from 'node:fs'
import { performance } from 'node:perf_hooks'

const argv = process.argv.slice(2)

function arg(name, fallback) {
  const index = argv.indexOf(`--${name}`)
  if (index < 0) {
    return fallback
  }
  const value = argv[index + 1]
  return value === undefined || value.startsWith('--') ? true : value
}

const baseUrl = String(arg('base-url', 'http://localhost:8080')).replace(/\/$/, '')
const username = arg('username', 'admin')
const password = arg('password', 'Admin@12345')
const fixedToken = arg('token', '')
const scenarioName = String(arg('scenario', 'all'))
const concurrency = Number(arg('concurrency', 200))
const durationSec = Number(arg('duration', 30))
const p95Budget = Number(arg('p95', 1000))
const maxErrorRate = Number(arg('max-error', 1))
const bomItemId = arg('bom-item-id', '')
const taskId = arg('task-id', '')
const lineIds = arg('line-ids', '')
const reportPath = arg('report', '')
const noThreshold = Boolean(arg('no-threshold', false))

if (!Number.isFinite(concurrency) || concurrency < 1) {
  fail('--concurrency 必须是正整数')
}
if (!Number.isFinite(durationSec) || durationSec < 1) {
  fail('--duration 必须是正整数（秒）')
}

function fail(message) {
  console.error(`[load-test] ${message}`)
  process.exit(2)
}

async function login() {
  if (fixedToken) {
    return String(fixedToken)
  }
  const response = await fetch(`${baseUrl}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })
  const text = await response.text()
  if (!response.ok) {
    fail(`登录失败 HTTP ${response.status}: ${text.slice(0, 300)}`)
  }
  const body = JSON.parse(text)
  const token = body?.data?.accessToken
  if (!token) {
    fail(`登录响应里没有 accessToken：${text.slice(0, 300)}`)
  }
  return token
}

/** 每个场景是「请求列表」，worker 在列表上轮转，避免只压一个接口把缓存压成假象。 */
function buildScenario(name) {
  if (name === 'list') {
    return [
      { name: 'doc-list', method: 'GET', path: '/api/v1/docs?page=1&size=20' },
      { name: 'item-list', method: 'GET', path: '/api/v1/items?page=1&size=20' }
    ]
  }
  if (name === 'bom') {
    if (!bomItemId) {
      fail('bom 场景需要 --bom-item-id <零部件 id>')
    }
    return [
      { name: 'bom-tree', method: 'GET', path: `/api/v1/items/${bomItemId}/bom?depth=5` },
      { name: 'bom-summary', method: 'GET', path: `/api/v1/items/${bomItemId}/bom/summary` },
      { name: 'bom-where-used', method: 'GET', path: `/api/v1/items/${bomItemId}/bom/where-used` }
    ]
  }
  if (name === 'todo') {
    return [{ name: 'my-tasks', method: 'GET', path: '/api/v1/workflow/tasks?page=1&size=20' }]
  }
  if (name === 'approve') {
    if (!taskId) {
      fail('approve 场景需要 --task-id <待办任务 id>（建议用专用测试单据，会真实推进流程）')
    }
    return [
      {
        name: 'complete-task',
        method: 'POST',
        path: `/api/v1/workflow/tasks/${taskId}/complete`,
        body: { comment: '压测审批' },
        // 第一次之后任务已完成，后续请求返回 409/400 属预期：不把业务拒绝算成系统错误
        acceptStatus: [200, 400, 404, 409]
      }
    ]
  }
  if (name === 'qty') {
    if (!bomItemId || !lineIds) {
      fail('qty 场景需要 --bom-item-id 与 --line-ids a,b,c（改同一用量，幂等）')
    }
    const ids = String(lineIds).split(',').map((item) => item.trim()).filter(Boolean)
    return [
      {
        name: 'batch-update-qty',
        method: 'POST',
        path: `/api/v1/items/${bomItemId}/bom/batch/update-qty`,
        body: { lineIds: ids, qty: 1 },
        acceptStatus: [200, 400, 404, 409]
      }
    ]
  }
  if (name === 'all') {
    const merged = [
      { name: 'doc-list', method: 'GET', path: '/api/v1/docs?page=1&size=20' },
      { name: 'item-list', method: 'GET', path: '/api/v1/items?page=1&size=20' },
      { name: 'my-tasks', method: 'GET', path: '/api/v1/workflow/tasks?page=1&size=20' }
    ]
    if (bomItemId) {
      merged.push(
        { name: 'bom-tree', method: 'GET', path: `/api/v1/items/${bomItemId}/bom?depth=5` },
        { name: 'bom-summary', method: 'GET', path: `/api/v1/items/${bomItemId}/bom/summary` }
      )
    }
    return merged
  }
  fail(`未知场景：${name}（可选 list|bom|todo|approve|qty|all）`)
}

function percentile(sorted, ratio) {
  if (!sorted.length) {
    return 0
  }
  const index = Math.min(sorted.length - 1, Math.max(0, Math.ceil(sorted.length * ratio) - 1))
  return sorted[index]
}

async function runOne(request, token) {
  const started = performance.now()
  let status = 0
  let ok = false
  try {
    const response = await fetch(`${baseUrl}${request.path}`, {
      method: request.method,
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token}`,
        ...(request.body ? { 'Content-Type': 'application/json' } : {})
      },
      body: request.body ? JSON.stringify(request.body) : undefined
    })
    status = response.status
    // 必须把响应体读完再计时，否则只测了首字节
    await response.arrayBuffer()
    const accepted = request.acceptStatus ?? []
    ok = response.ok || accepted.includes(status)
  } catch (error) {
    status = -1
    ok = false
  }
  return { ms: performance.now() - started, status, ok }
}

async function main() {
  const requests = buildScenario(scenarioName)
  const token = await login()

  console.info(`[load-test] target=${baseUrl} scenario=${scenarioName} concurrency=${concurrency} duration=${durationSec}s`)
  console.info(`[load-test] 请求清单：${requests.map((item) => `${item.name}(${item.method})`).join(', ')}`)

  const samples = new Map()
  for (const request of requests) {
    samples.set(request.name, { durations: [], ok: 0, failed: 0, statuses: new Map() })
  }

  const endAt = performance.now() + durationSec * 1000
  let counter = 0

  async function worker() {
    while (performance.now() < endAt) {
      const request = requests[counter++ % requests.length]
      const result = await runOne(request, token)
      const bucket = samples.get(request.name)
      bucket.durations.push(result.ms)
      bucket.statuses.set(result.status, (bucket.statuses.get(result.status) ?? 0) + 1)
      if (result.ok) {
        bucket.ok += 1
      } else {
        bucket.failed += 1
      }
    }
  }

  const startedAt = new Date()
  await Promise.all(Array.from({ length: concurrency }, () => worker()))
  const elapsedSec = (performance.now() - (endAt - durationSec * 1000)) / 1000

  const allDurations = []
  const rows = []
  for (const [name, bucket] of samples) {
    const sorted = [...bucket.durations].sort((a, b) => a - b)
    allDurations.push(...bucket.durations)
    rows.push({
      name,
      count: sorted.length,
      ok: bucket.ok,
      failed: bucket.failed,
      p50: Math.round(percentile(sorted, 0.5)),
      p95: Math.round(percentile(sorted, 0.95)),
      p99: Math.round(percentile(sorted, 0.99)),
      max: Math.round(sorted[sorted.length - 1] ?? 0),
      statuses: Object.fromEntries([...bucket.statuses.entries()].sort((a, b) => a[0] - b[0]))
    })
  }

  const totalCount = allDurations.length
  const totalFailed = rows.reduce((acc, row) => acc + row.failed, 0)
  const errorRate = totalCount ? (totalFailed / totalCount) * 100 : 0
  const allSorted = allDurations.sort((a, b) => a - b)
  const summary = {
    target: baseUrl,
    scenario: scenarioName,
    concurrency,
    durationSec: Number(elapsedSec.toFixed(1)),
    startedAt: startedAt.toISOString(),
    total: totalCount,
    failed: totalFailed,
    errorRate: Number(errorRate.toFixed(2)),
    qps: Number((totalCount / elapsedSec).toFixed(1)),
    p50: Math.round(percentile(allSorted, 0.5)),
    p95: Math.round(percentile(allSorted, 0.95)),
    p99: Math.round(percentile(allSorted, 0.99)),
    budgetP95: p95Budget,
    rows
  }

  console.table(rows.map(({ name, count, failed, p50, p95, p99, max }) => ({
    endpoint: name, count, failed, p50, p95, p99, max
  })))
  console.info(
    `[load-test] 合计 ${summary.total} 次 / ${summary.durationSec}s（QPS ${summary.qps}），` +
    `错误 ${summary.failed}（${summary.errorRate}%），P50 ${summary.p50}ms / P95 ${summary.p95}ms / P99 ${summary.p99}ms`
  )
  console.info(`[load-test] DoD 预算：P95 ≤ ${p95Budget}ms、错误率 ≤ ${maxErrorRate}%`)

  if (reportPath) {
    writeFileSync(String(reportPath), `${JSON.stringify(summary, null, 2)}\n`, 'utf8')
    console.info(`[load-test] 报告已写入 ${reportPath}`)
  }

  if (noThreshold) {
    return
  }
  const p95Ok = summary.p95 <= p95Budget
  const errorOk = summary.errorRate <= maxErrorRate
  if (!p95Ok || !errorOk) {
    console.error(
      `[load-test] 未达标：${p95Ok ? '' : `P95 ${summary.p95}ms > ${p95Budget}ms；`}` +
      `${errorOk ? '' : `错误率 ${summary.errorRate}% > ${maxErrorRate}%`}`
    )
    process.exit(1)
  }
  console.info('[load-test] 达标')
}

main().catch((error) => {
  console.error('[load-test] 执行失败：', error)
  process.exit(2)
})
