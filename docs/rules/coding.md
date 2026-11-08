# 规则卡：编码域（coding）

> 依据 `docs/17-复刻工作流v2.md` 的 P2。每条规则必须带证据；状态用 `待开发 / 已对齐 / 不改（待业务）`。
> 证据来源：`docs/16-PLMSOFT逆向-编码与物料新建.md`。

---

## CODE-01 取号（占号）

- **输入**：`CodeKindId`、`ValuePath`（码值路径）、`CodeValueId`（末级）、`GeneratorCount`、`ObjectType/ObjectInfoType`
- **规则**：按路径各段码值拼前缀，末段流水自增；一次可生成 `GeneratorCount` 个连续码（正则 `^[0-9]{1,5}$`）
- **证据**：`CodeMachine.js:1230-1273`；`CodingMaintenanceService.GeneratorMultiCodeWithCodeValuePath`
- **现状**：`CodeRuleService.nextByPath` 一次只生成一个
- **差异**：D1 缺批量
- **处置**：新增 `nextByPath(path, scope, count)`；`code_issue` 批量落流水；状态：**已对齐**（EXP-20261108-56，`POST /system/coding/generate`）

## CODE-02 只算路径（不占号）

- **规则**：`GetCodePath=true` 时不请求服务端生成，直接由码值路径拼 `GeneratorPath`
- **证据**：`CodeMachine.js:1240-1271`
- **现状**：`GET /items/code-preview` 会算流水序号，但**不占号**；路径用文本 `Kind/1001/02/*`
- **差异**：D2 路径格式不同
- **处置**：保留文本路径用于人读；另存/兼容 16hex 形式（或提供转换函数）；状态：**待开发**

## CODE-03 路径格式与占用校验

- **规则**：`GeneratorPath = 各段 split('_')[1].substring(0,16)`，用 `;` 连接；绑定前 `CheckRelatedCodePath(ClassCodePath, OldCodePath)`，被别的分类占用则报错
- **证据**：`CodeMachine.js:1247-1266`；`CLASSIFYCLASS.CLASSCODEPATH` 实测 4 段 16hex
- **现状**：`normalizeCodePath` 只校验种类/码值/`*`/段数，**没有占用校验**
- **差异**：D3
- **处置**：给 `normalizeCodePath` 加"同一 `code_path` 是否已被其他分类占用"检查；状态：**已对齐**（EXP-20261108-56，冲突 409）

## CODE-04 编码状态（换码失效）

- **规则**：换编码时把旧码置 `LoseEfficacy`，不删不改
- **证据**：`EditItem.js:148-150`（`SetCodeState({State:"LoseEfficacy"})`）
- **现状**：`code_issue` 无状态字段
- **差异**：D4
- **处置**：`V84` 给 `code_issue` 加 `state`（ACTIVE/LOSE_EFFICACY）+ 换码接口；状态：**已对齐**（EXP-20261108-56，`PUT /items/{id}/code`）

## CODE-05 编码重复检查

- **规则**：保存前 `CheckItemCodeRepeat({ItemCode})`，重复则弹框并退回第一步
- **证据**：`CreateItem.js:67-69`、`185-194`
- **现状**：仅 DB 唯一索引（`object_type + code`），无前端即时校验
- **差异**：D5
- **处置**：`GET /items/code-exists?code=`；表单失焦提示；状态：**已对齐**（接口已交付，表单失焦提示待接）

## CODE-06 流水初始码

- **规则**：编码器有 `InitialCode`（流水起始）与"批量设初始码"，默认不展示（性能考虑）
- **证据**：`CodeMachine.js:281-289`
- **现状**：`code_sequence` 只能从 1 开始
- **差异**：D6
- **处置**：加 `PUT /system/coding/sequences`（设起始值，仅当当前未发号）；状态：**已对齐**（EXP-20261108-59）

## CODE-07 编码 → 反查分类 → 属性默认值

- **规则**：保存物料时用 `GENERATORPATH` 反查分类，再取该分类属性默认值回填
- **证据**：`CreateItem.js:240-263`
- **现状**：只有"选分类 → 生成编码"，无反查
- **差异**：D7
- **处置**：`GET /items/code-resolve?code=` 返回分类与属性默认值；状态：**已对齐**（EXP-20261108-60；流水快照优先，其次 6+5 位前缀）

## CODE-08 编码长度/小类键

- **规则**：小类 = 编码前 6 位（与 ERP FNUMBER 一致），流水 5 位
- **证据**：`ItemSmartInput.js:161-164`
- **现状**：按 `doc_class.code` 匹配，未强制长度
- **差异**：D8
- **处置**：后端校验"小类码 6 位、总长 11 位"（可配置）；状态：**待开发**

---

## 汇总

| 编号 | 主题 | 差异 | 状态 |
|---|---|---|---|
| CODE-01 | 批量取号 | D1 | 已对齐（EXP-56） |
| CODE-02 | 只算路径 | D2 | 待开发 |
| CODE-03 | 路径格式/占用 | D3 | 已对齐（占用校验；16hex 格式仍待） |
| CODE-04 | 编码状态 | D4 | 已对齐（EXP-56） |
| CODE-05 | 重复检查 | D5 | 已对齐（接口） |
| CODE-06 | 初始码 | D6 | 已对齐（EXP-59） |
| CODE-07 | 反查分类 | D7 | 已对齐（EXP-60） |
| CODE-08 | 长度约束 | D8 | 待开发 |
