# PLMSOFT 逆向笔记（一）：编码与物料新建

> 目标：不再靠"表结构 + 数据分布"猜业务，而是从**部署产物**（JS / `.page` / DLL）反推真实调用链与规则。
> 素材：`E:\tedri_project\dev\Plm_Code\PLMSOFT`（与 `PLM_Clone\PLMSOFT` 同一份），**3.9 GB / 32,128 文件**。
> 本笔记只记录**已确认**的事实；未确认的一律标注"待反编译/待验证"。

---

## 一、资产地图（已确认存在）

| 目录 | 内容 | 规模 |
|---|---|---|
| `Server\Bin` | 平台 DLL | 2,145 个 `.dll`（全站） |
| `Server\InforCenter\Coding` | **编码模块**（`Bin` / `Config` / `Js`） | `CodeMachine.js` 74 KB、`CodeValueManagement.js` 31 KB、`CodeKindManagement.js` 36 KB、`CodeRuleManagement.js` 20 KB |
| `Server\InforCenter\PSE\Item` | 物料（`Js` + `Bin` + `Config`） | `CreateItem.js` 26 KB、`EditItem.js` 19 KB、`ItemView.js` 27 KB |
| `Server\InforCenter\PSE\Bom` | BOM（`Js`） | `OperateInBom.js` 33 KB、`ProductBOM.js` 45 KB、`ProcessAddByBom.js` 105 KB |
| `Server\InforCenter\PSE\BomValidity` | BOM 有效性 | `BomValidity.js` 24 KB、`UnitSetting.js` 36 KB |
| `Server\SecondaryDev\ItemSmartInput` | **客户二开**：物料智能输入 | `ItemSmartInput.js` 35 KB + `ItemSmartInput.fileref` |
| 全站 | 低代码元数据 | `.page` 2,295、`.model` 940、`.schema` 941、`.view` 872、`.generator` 1,668 |

> `.page/.model/.schema/.generator` 是**可读文本**，比反编译 DLL 更适合还原"页面字段、校验、按钮行为"。

---

## 二、已确认的调用链（来自 JS 中的 `DataService.Call`）

### 2.1 新建物料保存（`PSE\Item\Js\CreateItem.js`）

```
1) CheckItemCodeRepeat({ ItemCode: ECODE })            → 编码重复则弹框并退回第 0 步
2) GetPathByCode({ CodeText: ECODE, ObjectType: '' })   → 返回 { GENERATORPATH, ... }
3) GetClassByPathName({ ClassCodePath: GENERATORPATH, ClassObjectType: ITEMTYPE })
                                                        → 由编码路径反查"分类 EID"
4) GetAttributeDefaultData({ PEID: 分类EID })           → 取该分类的属性默认值（必填/默认）
5) 用 4) 的结果回填表单属性
```

**关键结论（与现有实现相反）**：蓝本是 **先有编码 → 用编码里的 `GENERATORPATH` 反查分类 → 带出分类属性默认值**；
我们实现的是"先选分类 → 生成编码"。两者都能得到同一套数据，但蓝本的**分类是编码的结果，而不是输入**。

### 2.2 编码生成（`InforCenter\Coding\Js\CodeMachine.js`）

- 打开编码器：

```js
UIManager.Popup("CodeMachine", {
  Value, ObjectType, ObjectInfoType, TreeItemName,
  ShowTreeQuery, FromRoot, CheckResult,
  GetCodePath: true, FLPath
}, callback, ...)
```

- 申请编码：

```js
var coderes = InforCenter_Coding_CodeGenerator_GenerateCode(ctrlEvent);
pagePara.ReturnValueType = "CODERES";
```

- 页面支持"路径预览"模式：`GetCodePath: true` 时把「申请编码」按钮文案改成「确定」、并显示"路径预览"；
- 有 `InitialCode`（初始码/流水起始）与 `SetAllInitial`（批量设初始码）两个概念，且注释明确：
  **"默认不显示应用所有流水码，可能存在严重性能问题"**——说明流水码是逐条落库的（与 `CODERES` 一致）。

- 服务端入口（类名，来自 DLL 类型表）：
  `Hoteam.InforCenter.Coding.Maintenance.Bussiness.CodeMachineCache`、
  `CodeKindBussiness`，以及 `CodingMaintenanceService`（`GetPathByCode`/`SetCodeState`/`GetPermissionCodeKindData`…）。

### 2.3 编辑物料（`EditItem.js`）

```
GetCodeValueColumn({ ItemType, ActionType })   // 哪些列是"编码列"
SetCodeState({ State: "LoseEfficacy", ObjectID: "CODERES_xxx;" })  // 换编码时把旧码置为失效
```

**结论**：蓝本对"换编码"的处理是**把旧编码状态置为 `LoseEfficacy`（失效）**，不是删除，也不是改历史数据。
新系统目前没有编码状态机，这是实打实的缺口。

### 2.4 二开 `ItemSmartInput`（不改生成逻辑）

- 文件头写明：`PSEITEM.GGXH nvarchar(500) ← 规格型号/图号`；
- 功能是**名称/规格联想 + 同小类查重 + 把 GGXH 同步到 ZYCS/FLSXHB**；
- 明确一句：**"不改变 PLM 生成逻辑"**；
- 但里面有一条重要假设：`// 当前小类：编码前 6 位（PLM 编码规则与 ERP FNUMBER 一致）`，
  代码 `code.length >= 6 ? code.slice(0, 6) : ''` —— 印证 **6 位小类前缀 + 5 位流水**。

---

## 三、与当前实现的差异（需排期修）

| # | 蓝本行为 | 新系统现状 | 影响 | 建议 |
|---|---|---|---|---|
| 1 | 编码 → `GENERATORPATH` → 反查分类 → 带出分类属性默认值 | 选分类 → 生成编码，**没有反查分类与属性默认值** | 现场若先有编码（导入、沿用旧码），无法自动归类/带属性 | 加 `GET /items/code-resolve?code=`，按 `code_path` 反查分类与属性默认值 |
| 2 | 编码有状态机：`LoseEfficacy`（换码时旧码失效） | `code_issue` 只记发放，没有状态 | 换码后旧码仍"有效"，追溯会歧义 | 给 `code_issue` 加 `state`（ACTIVE/LOSE_EFFICACY）+ 换码接口 |
| 3 | 保存前 `CheckItemCodeRepeat` 全局查重 | 只有 DB 唯一索引（按 object_type+code） | 跨类型重码只在插入时报错，体验差 | 提供查重接口，表单失焦即提示 |
| 4 | 编码器支持 `InitialCode`（流水起始）与批量设置 | 无 | 迁移/补号场景做不到 | `code_sequence` 加初始值设置接口 |
| 5 | 二开用"编码前 6 位"作小类键 | 我们用 `doc_class.code` 直接匹配 | 语义一致，但**依赖编码长度=11** | 明确约束或改成查 `code_path` |

---

## 四、未确认项（必须靠反编译或样本）

1. `CodeMachineCache` 里 **码段拼接、`LENGTH=0` 的处理、`DropDown` vs 自动流水的分支**——需要 IL 级反编译；
2. `GenerateCode` 的并发控制（是否用行锁/序列表）——决定我们 `code_sequence` 的写法是否等价；
3. `GetPathByCode` 对"人工编码（无路径）"的返回——决定手工码能否反查分类；
4. `SetCodeState` 的状态集合（除 `LoseEfficacy` 还有哪些）。

**工具缺口**：本机没有 `dotnet` / `ilspycmd` / `ildasm`，`ReflectionOnlyLoadFrom` 只能枚举到 167 个类型（含大量编译器闭包），拿不到方法体。
需要其一：① 允许安装 ILSpy（联网）；② 现场提供反编译后的 `.cs`；③ 接受"只按 JS + `.page` + 数据样本"推断。

---

## 五、编码生成协议（反编译 + JS 双向确认，已闭环）

### 5.1 工具链（已装好，可复现）

```
winget install Microsoft.DotNet.SDK.8          -> 8.0.427
dotnet tool install -g ilspycmd --version 8.2.0.7535
$env:DOTNET_ROLL_FORWARD='Major'               -> 让 net6 版 ilspycmd 跑在 net8 上
ilspycmd -p -o <out> Hoteam.InforCenter.Coding.Maintenance.Bussiness.dll
```

反编译产物 22 个 `.cs`：`CodeMachineCache.cs`(86KB)、`CodeValueBussiness.cs`(116KB)、`CodeResBussiness.cs`(100KB)、`CodeKindBussiness.cs`(92KB)、`CodeRuleBussiness.cs`(28KB)…

### 5.2 取号（占号）

`CodeMachine.js:1230-1273`：

```js
para = {
  GeneratorCount: applyCount,   // 批量取号，正则 ^[0-9]{1,5}$
  CodeValueId:   codeValueId,   // 末级码值 ID
  ValuePath:     path,          // 码值路径，形如 "…;…"（分号分隔的 类型_ID）
  CodeKindId:    codeKindValue, // 编码种类
  ObjectInfoType, ObjectTypeValue
}
DataService.Call("InforCenter.Coding.CodingMaintenanceService.GeneratorMultiCodeWithCodeValuePath", { para })
```

**结论**：蓝本支持**一次生成 N 个连续码**（`GeneratorCount`），不是一次一个；我们现在的
`code_sequence` 单条自增只能逐个取，需补批量接口。

### 5.3 只算路径（不占号，用于分类绑定）

`CodeMachine.js:1240-1271`，`GetCodePath: true` 时前端**不发生成请求**，自己拼路径：

```js
PathArr = path.split(';')
GeneratorPath = PathArr.map(p => p.split('_')[1].substring(0,16)).join(';')
CheckRelatedCodePath({ ClassCodePath: GeneratorPath, OldCodePath })   // 该路径是否已被别的分类占用
return { PathName: `${codeKindName}/${allCode}`, GeneratorPath }
```

**这解释了 `CLASSCODEPATH` 为什么是 16 位十六进制段**——它是把码值对象 ID
（`CODEVALUE_<32hex>`）截断到 16 位后的拼接；`PathName` 就是界面上显示的
「元器件编码规则/1001/02」。

### 5.4 与现有实现的差异（新增 3 条）

| # | 蓝本 | 新系统 | 影响 |
|---|---|---|---|
| 6 | 批量取号 `GeneratorCount`（最多 5 位） | 只能一次一个 | 补号/批量建物料场景做不了 |
| 7 | 路径由**码值对象 ID 前 16 位**拼接，且绑定前用 `CheckRelatedCodePath` 查占用 | 路径直接用码值文本（`1001/02/*`） | 我们的路径可读但**不是蓝本格式**，迁移对照需转换；且缺"路径已被占用"校验 |
| 8 | `PathName`（人读）+ `GeneratorPath`（机器）双表示 | 只有一种 | UI 展示与存储同源，改码值文本会影响已存路径 |

## 六、下一步（按此顺序）

1. **读 `.page/.model/.schema`**（纯文本，无需工具）：`Coding` 与 `PSEITEM-CREATE` 的字段、校验、按钮绑定——补全 §2 的页面级规则；
2. **BOM 逆向**：`OperateInBom.js` / `ProductBOM.js` + `PSEBOMLINEEDITCHECKNEW` 过程，产出「BOM 编辑规则清单」；
3. 拿到反编译能力后，再补 §四 的 4 个未确认项。
