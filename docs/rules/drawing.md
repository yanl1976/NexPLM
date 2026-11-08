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

## 图纸字段清单（P0 取证，EXP-20261108-65）

### DOCDATASET（图纸对象）

| 蓝本字段 | 含义 | 新系统落点 |
|---|---|---|
| `ECODE` | 编号（图号） | `plm_object.code` |
| `ENAME` | 名称 | `plm_object.name` |
| `DESCRIPTION` | 详细描述 | `plm_object.ext_json.description` |
| `EVERSION` / `EMINORVERSION` | 大/小版本 | `plm_object.revision` / `iteration` |
| `STATEID` / `WORKFLOWSTATE` | 生命周期/流程状态 | `plm_object.lifecycle_state` / `workflow_state` |
| `DATASETCLASS` / `TYPEID` | 数据集分类/类型 | `doc_master.class_id` / `doc_type` |
| `FILEEXTENT` | 文件扩展名（dwg） | `doc_master.file_ext` |
| `MAINFILEID` | 主文件 | `doc_master.main_file_id` |
| `CHECKOUTSTATE` / `CHECKOUTUSER` / `CHECKOUTDATE` | 检出状态/人/时间 | `plm_checkout` |

### DOCBASEFORM（图档属性，`DOCDWG-CREATE.dic` + `DOCBASEFORM$.dic`）

| 蓝本字段 | 中文标签 | 新系统落点 |
|---|---|---|
| `DOCTYPE` | DocType（`DOCDWG`） | `doc_master.doc_type='DESIGN'` |
| `STANDARDCODE` | 标准号 | `ext_json.drawing.standardCode` |
| `NIANDAIHAO` | 年代号 | `ext_json.drawing.yearNo` |
| `JIUBIANHAO` | 旧编号 | `ext_json.drawing.oldCode` |
| `GUIGE` | 规格 | `ext_json.drawing.spec` |
| `ZHONGYAODU` | 重要度等级 | `ext_json.drawing.importance` |
| `JINGDUDJ` | 精度等级 | `ext_json.drawing.precisionGrade` |
| `JILIANGDANGWEI` | 计量单位 | `ext_json.drawing.measureUnit` |
| `ATTACHXH` / `ATTACHORDER` | 附图序号 | `ext_json.drawing.attachNo` |
| `ATTACHTYPE` | 附图分类 | `ext_json.drawing.attachType` |
| `XMDH` | 项目代号 | `ext_json.drawing.projectCode` |
| `WENJIANLEIXING` | 文件类型 | `ext_json.drawing.fileType` |

### DOCFILEMODEL（文件）

| 蓝本字段 | 新系统 |
|---|---|
| `FILENAME` / `FILEEXTENT` / `FILESIZE` / `ISMAINFILE` | `file_object` + `file_link(file_role=MAIN)` |
| `CHECKOUTUSER` / `CHECKOUTSTATE` | `plm_checkout` |

### 差异（P3）

| 项 | 蓝本 | 现状 | 处置 |
|---|---|---|---|
| 图档属性存储 | `DOCBASEFORM` 独立表 | `plm_object.ext_json.drawing` | 够用；若需按属性检索，再建 GIN 索引或独立表 |
| 图纸类型 | `TYPEID` / `DATASETCLASS` | `doc_master.doc_type` + `class_id` | 已覆盖 |
| 密级 | 未在 DOCDATASET 明列 | `plm_object.security_level` | 已保留 0~3 |
| 状态/检出 | `STATEID` / `CHECKOUT*` | 生命周期 + `plm_checkout` | 已复用 |
| 图幅 `TUFU1` | `PSEBASEFORM`（物料） | 未落 | 待加到物料设计属性 |

## DRW-05 图文档文件夹（已作废，EXP-20261108-68）

> 用户确认由「图纸库」统一管理，不再需要文档文件夹；`DocFolderController`、前端文件夹树与归档入口已回滚。以下仅保留取证记录。

**证据**：蓝本 `WSFOLDERCONTENT` 列 `LID/PEID/CEID/LQUALITY/LSEQUENCE/CETYPE`，
`InforCenter/PDM/CAD/CADService/Config/Model/Cad.cmdsql:185-193` 用
`WSFOLDERCONTENT.CEID = PSEITEM.EUID` 取文件夹内容；`PseItem.cmdsql:42` 明确
`CETYPE='DOCMASTER'`。说明**文件夹（整理）与分类（属性）是两套维度**。

**实现**：复用 `ws_folder`（`space_type='DOC'`，按 `owner_id` 隔离）+ `ws_folder_content`：

| 接口 | 用途 |
|---|---|
| `GET /doc-folders` | 当前用户的文件夹扁平列表 |
| `POST /doc-folders` | 新建（同级重名 409） |
| `PUT /doc-folders/{id}` | 重命名（递归同步子孙 path） |
| `DELETE /doc-folders/{id}` | 删除（有子文件夹拒绝） |
| `POST /doc-folders/{id}/documents?docId=` | 文档归档到文件夹 |
| `DELETE /doc-folders/{id}/documents/{docId}` | 移出 |

`GET /docs?folderId=` 按文件夹过滤；前端文档库左侧新增文件夹树，列表行新增「归档」。

**差异**：蓝本文件夹是带权限的公共树；当前按用户私有，后续若要项目/公共文件夹，需扩展 `space_type` 与 ACL。

**状态**：**已对齐（私有文件夹）**。

## DRW-06 图纸库（图文档菜单下，EXP-20261108-67）

**用户口径**：图纸库放在「图文档」菜单下，所有图纸在这里统一管理；不是独立一级菜单，也不是文档库里的文件夹。

**实现**

- 路由 `/doc/drawings`，菜单「图文档 → 图纸库」。
- 页面 `views/doc/DrawingLibraryView.vue`：图号/名称/分类/版本/状态/文件数/更新时间；筛选（关键词、分类、状态）；新建图纸（复用 `DrawingDialog`，18 项图档属性 + 上传）、打开文档详情、关联/解除物料。
- 后端 `GET /docs` 增加 `fileExt` 过滤，图纸库固定 `fileExt=dwg`。
- `POST /drawings` 的 `itemId` 改为可选：允许先入图纸库、后关联物料；建图纸时置 `doc_master.file_ext='dwg'` 作为识别标记（蓝本 `DOCDATASET.FILEEXTENT`）。

**状态**：**已对齐**。
