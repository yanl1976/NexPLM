# 图纸管理规则卡（P2）

> 依据 `docs/17-复刻工作流v2.md`。证据来自 `PLMSOFT` 部署产物（只读）与蓝本 `TCPLM` 库。
> 状态：`待开发 / 已对齐 / 不改（待业务）`。

## P0/P1 取证与盘点

### 蓝本：非标自制件与图纸到底怎么关联

| 项 | 证据 | 结论 |
|---|---|---|
| 非标自制件类型 | `PSEITEM.ITEMTYPE` 分布：`PSEITEMGAUGEFORM=51467`（最多）、`PSEITEMPARTFORM=3480`（通用自制件）；`AutoGenerator/Config/Pages/PSEITEMGAUGEFORM-CREATE.dic:161`「新建非标自制件」 | **非标自制件 = `PSEITEMGAUGEFORM`**；新系统对应 `item_type=GAUGE` |
| 关联表 | `TSDC/PSE/Item/Config/DBConfig/PseItem.cmdsql:3-8`，命令 `GetUpdateDWGDocInfo`：`PSEITEMRELATION.PEID = 物料`，`CEID = DOCDATASET.EID`，`RELATIONTYPE='Specification'`，`DOCDATASET.FILEEXTENT='dwg'` | 图纸是**文档数据集**，通过通用关系表的 `Specification` 关系挂到物料上 |
| 关系统计 | `PSEITEMRELATION.RELATIONTYPE`：`Specification=43824`、`Reference=3`、空=2753；其中 `FILEEXTENT='dwg'` 的 **43800** 条 | 该关系几乎全部是 DWG 图纸，不是普通附件 |
| 图纸字段 | `PSEBASEFORM`：`DWGNO`（图号）、`TUFU1`（图幅，样本值 3/5）、`PSEITEM.GGXH`（规格型号/图号，如 `5S719-110134L`）；`PSEITEM.FILEORNOT='是'` 111333 条 | 图号/图幅/是否有文件是物料侧的设计属性 |
| 物理文件 | `DOCFILEMODEL`：`DOCID`、`FILENAME`、`FILEEXTENT`、`ISMAINFILE`、`CHECKOUTUSER/CHECKOUTSTATE`；`DOCMASTER.MAINFILEID` | 文档与文件分离，主文件标记 + 检出状态 |
| BOM 图纸统计 | `DRAWINGSBOMSTATISTICVALIDITY`：先 `Exec GETPSEBOMFORBASELINE` 展开 BOM，再 join `PSEITEM + PSEBASEFORM`，输出 `TUFU1`、`LPATH`、`TOTALQTY`，`@DRAWINGSFITER` 过滤 | 图纸统计是**按 BOM 展开**的，且带有效性/版本参数 |
| 前端 | `TSDC/PSE/Statistics/Js/BomDrawingsStatistics.js:23` → `Hoteam.InforCenter.TSDCPSEService.GetDrawingsStatisticsData`；`InforCenter/Ctrls/eDrawing/*` | 有独立的 BOM 图纸统计页 |

### 新系统现状

| 层 | 现状 |
|---|---|
| 对象 | `plm_object`（`PdmItem` 260 / `PdmDoc` 3）；`doc_master`（`doc_type/file_ext/main_file_id/file_count`） |
| 文件 | `file_object`（sha256/size/ext）+ `file_link`（`owner_object_id/file_role/is_latest`） |
| 关系 | `plm_relation`：`relation_type/src_id/dst_id/attrs_json`，已有 `DocViewService`/`ItemViewService` 读它 |
| 物料 | `item_master.design_attr` 含 `dwgNo`；`item_type` 有 `GAUGE` |
| 页面 | **无图纸管理页**；无 `Specification` 关系；无 BOM 图纸统计 |

## P2 规则卡 / P3 差异表

| 编号 | 规则 | 蓝本 | 现状 | 处置 |
|---|---|---|---|---|
| DRW-01 | 图纸与物料关联 | `PSEITEMRELATION(Specification)` → `DOCDATASET(dwg)` | 无 | 用 `plm_relation(relation_type='Specification')`，`src_id=物料`、`dst_id=图纸文档`、`attrs_json` 存图号/图幅 | **已对齐（EXP-20261108-63）** |
| DRW-02 | 图纸文档 | `DOCDATASET` + `DOCFILEMODEL`，`FILEEXTENT='dwg'` | `PdmDoc` + `doc_master` + `file_link` | 建图纸文档时 `doc_type='DESIGN'`、`source='CAD'`、`doc_master.file_ext='dwg'` | **已对齐（EXP-63）** |
| DRW-03 | 非标自制件 | `PSEITEMGAUGEFORM` | `item_type=GAUGE` | 图纸管理页可只筛非标自制件；不新增类型 | **已对齐（EXP-63）** |
| DRW-04 | BOM 图纸统计 | `GETPSEBOMFORBASELINE` + `PSEBASEFORM.TUFU1` + 有效性/版本 | `bom_line` 展开 + `summary` | 待开发（先复用现有 BOM 展开，暂不做版本/有效性） |
| DRW-05 | 图幅 | `TUFU1`（3=A3、5=A5 待确认） | 无 | 作为 `attrs_json.sheetSize` 保存，显示原值，不做换算 |

## P4 开发顺序

1. DRW-01/02/03：图纸关联接口 + 独立「图纸管理」页（本片）
2. DRW-04：BOM 图纸统计（下一片）
3. 文件上传/预览/检出：复用现有 `DocController` 的 files/checkout 端点

## 边界

- 蓝本的 `PSEITEMRELATION` 还承载其它关系（`Reference`、空类型），我们只落 `Specification`。
- `TUFU1` 数值含义（图幅代码）尚未从字典确认，按原值保存展示。
- `$` 版本快照与有效性不在本片范围，见 `docs/rules/bom.md` B2/B3。

## 修正（EXP-20261108-64）

第一版把图纸做成了「物料—图纸关联清单 + 独立页面」，是错的：

| 错误 | 修正 |
|---|---|
| `POST /drawings` 直接建空 `PdmDoc` 并硬改 `file_ext='dwg'` | 删除该路径；图纸文档由 `DocController` 创建，文件走 `/docs/{id}/files` |
| 独立一级菜单 + 独立页面 | 删除；改为**独立组件 `DrawingDialog`**，挂在工作空间「新建」下拉里 |
| 图幅写进 `plm_relation.attrs_json` | 移除；`TUFU1` 属物料设计属性，不属图纸关联 |
| 只有关联/解除 | 组件内完成：新建图纸文档 → 上传文件 → 关联物料；并列出该物料已有图纸可解除 |

DRW-01 状态：**已对齐**；DRW-02 状态：**已对齐（复用文档域）**；DRW-04（BOM 图纸目录汇总）仍待开发。
