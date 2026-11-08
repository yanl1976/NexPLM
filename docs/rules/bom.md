# BOM 规则卡（P2）

> 依据 `docs/17-复刻工作流v2.md`。证据来自蓝本 `TCPLM` 存储过程导出
> `_plmsoft_probe/proc-*.sql`（仓库外只读副本）与 `backend/logs/proc-*.sql`，
> 以及 `Server/InforCenter/PSE/Bom/Js/OperateInBom.js`。
> 状态：`待开发 / 已对齐 / 不改（待业务）`。

## P1 模块盘点（BOM）

| 层 | 蓝本 | 新系统 |
|---|---|---|
| 表 | `PSEBOMLINE$`（版本快照）、`PSEITEM$`、`PSEITEMVIEW$` | `bom_line`、`plm_object` |
| 过程 | `PSEBOMLINEEDITCHECKNEW`（判环）、`PSEEDITBOM`（编辑）、`PSEVALIDITYITEM` / `PSEVALIDITYBOMLINE`（有效性）、`GETPSEBOMFORBASELINE`(37KB)、`GETBASELINEBOMSTRUCT`(22KB)、`PSEREVERSEBOMLINE*`（反查） | `BomService` |
| 前端 | `OperateInBom.js`、`ProductBOM.js` | `ItemDetailView.vue` BOM 页签 |
| 视图类型 | `@ITEMVIEWTYPE` + `@ITEMDEFAULTVIEWTYPE` 双视图 | `view_type`（DESIGN/QUOTE/MANUFACTURE） |

## BOM-01 加子件前的判环

- **证据**：`proc-PSEBOMLINEEDITCHECKNEW.sql:23-69`
  - `t` = 从**待加子件**向下展开（`LEVELID-1`，`where C.LEVELID*-1 < @MAXLEVEL`）
  - `rt` = 从**当前父件**向上展开（`LEVELID+1`，`where rt.LEVELID < @MAXLEVEL`）
  - `union all` 后 `group by CITEMUID having count(CITEMUID)>1` —— 即**两个方向都出现过的节点**才判成环
  - `@ISCONTAIMSELF=1` 时 `SET @ADDITEMMASTERID_TEMP='XXX'`，正向集合不再排除自身，**允许自包含/同一物料多行**
- **现状（改动前）**：`BomService.ensureNoCycle` 只做一次 `ancestorsOf(parentId)` 向上找祖先；递归 CTE 无层数上限；`add()` 里一律禁止 `parentId == childId`。
- **差异 D-B1**：方向（单向 vs 双向）、层数上限、自包含。
- **处置**：`ensureNoCycle` 改为「正向后代 ∩ 反向祖先」，两者都带 `MAX_DEPTH` 上限；`@ISCONTAIMSELF` 暂不启用（默认禁止自引用），登记为待业务。
- **状态**：**已对齐（双向 + 上限）**；自包含开关 **不改（待业务）**。

## BOM-02 版本快照表 `$`

- **证据**：`PSEBOMLINEEDITCHECKNEW` 全程读 `PSEBOMLINE$` / `PSEITEM$` / `PSEITEMVIEW$`，且 `B.ELASTINSERT=B.EID` 取最新插入版本。
- **现状**：`bom_line` 无版本快照列；`plm_object` 有 `revision/iteration/is_latest`。
- **差异 D-B2**：蓝本判环是在**某个版本快照**上做，我们是在当前行上做。
- **处置**：**不改（待业务）**——需要现场确认多版本并存时 BOM 是否按版本隔离；当前按"最新有效行"处理。

## BOM-03 有效性（validity）

- **证据**：`PSEVALIDITYITEM`(12.7KB)、`PSEVALIDITYBOMLINE`、`PSEBOMLINE$.LID` 上的有效期字段。
- **现状**：`bom_line.effect_from/effect_to` + `BomService.validateEffectRange`；`summary/view` 支持 `effectAt`。
- **差异 D-B3**：蓝本有效性过程还处理「按日期选择版本/视图」，我们只做区间校验。
- **状态**：**待开发**（需先逐条逆向 `PSEVALIDITYITEM`）。

## BOM-04 反查（where-used）

- **证据**：`PSEREVERSEBOMLINEVALIDITY`(17.9KB)、`PSEREVERSEBOMLINE*`。
- **现状**：`BomService.whereUsed` / `GET /items/{id}/where-used`。
- **差异 D-B4**：蓝本反查带有效性与版本条件。
- **状态**：**待开发**。

## BOM-05 基线快照

- **证据**：`GETPSEBOMFORBASELINE`(37KB)、`GETBASELINEBOMSTRUCT`(22KB)。
- **现状**：`bom_baseline` / `bom_baseline_line` + `bomApi.baselines()`。
- **差异 D-B5**：未逐条核对蓝本的基线展开口径。
- **状态**：**待开发**（下一片优先读这两个过程）。

## 差异表（P3）

| 编号 | 规则 | 蓝本 | 现状 | 影响 | 处置 |
|---|---|---|---|---|---|
| B1 | 判环 | 双向展开交集 + `@MAXLEVEL` + 自包含开关 | 单向上找祖先、无上限 | 深 BOM 递归无上限；规则口径不一致 | 已对齐双向+上限；自包含待业务 |
| B2 | 版本快照 | `$` 表 + `ELASTINSERT` | 当前行 | 多版本场景语义不同 | 待业务 |
| B3 | 有效性 | `PSEVALIDITY*` 按日期选版本 | 区间校验 + effectAt | 部分场景 | 待开发 |
| B4 | 反查 | 带有效性/版本 | 基础反查 | 部分场景 | 待开发 |
| B5 | 基线 | 37KB/22KB 两个过程 | 简化实现 | 展开口径可能不同 | 待开发 |

## 开发顺序（P4）

1. B1 双向判环 + 层数上限（本片）
2. B5 基线展开口径逆向 → 差异修正
3. B3/B4 有效性与反查
4. B2 版本快照（需业务确认）
