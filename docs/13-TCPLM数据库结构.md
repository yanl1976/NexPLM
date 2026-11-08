# TCPLM（PLM 蓝本库）数据库结构字典

> 由 `tools/Dump-TcplmSchema.ps1` **只读**生成（仅 SELECT / sys.* / INFORMATION_SCHEMA），生成时间：2026-11-08 10:38:21。
> 用途：蓝本取证与迁移对照的唯一结构基线；**任何写入操作都不在本脚本能力范围内**。

## 一、实例与库

| 项 | 值 |
|---|---|
| 服务器 | 198.168.6.26,1433 |
| 数据库 | TCPLM |
| 版本 | Microsoft SQL Server 2016 (RTM) - 13.0.1601.5 (X64) |
| 大小 | 59,690 MB |
| 兼容级别 | 130 |
| 排序规则 | Chinese_PRC_CI_AS |
| 创建时间 | 10/26/2026 21:24:51 |
| 表 / 视图 | 483 / 62 |
| 列总数 | 9252 |

## 二、行数 Top 30

| 表 | 行数 | 列数 | 主键 |
|---|---|---|---|
| QUERYMACHINESHOWVIEW | 1,708,803 | 11 | EID |
| OPERATELOG | 867,105 | 17 | EID |
| WSFOLDERCONTENT | 376,564 | 15 | LID |
| PSEBOMLINE$ | 253,829 | 38 | LID |
| PSEBASEFORM$ | 196,842 | 52 | EID |
| PSEITEM$ | 196,520 | 51 | EID |
| PSEITEMVIEW$ | 137,746 | 30 | EID |
| INTERNALMAIL | 125,276 | 28 | EID |
| PSEITEMVIEW | 116,038 | 29 | EID |
| PSEBASEFORM | 116,037 | 51 | EID |
| PSEITEM | 116,035 | 50 | EID |
| PSEBOMLINE | 79,105 | 37 | LID |
| CODERES | 60,300 | 18 | EID |
| DOCFILEMODEL$ | 60,233 | 31 | EID |
| DOCBASEFORM$ | 60,130 | 20 | EID |
| DOCDATASET$ | 59,823 | 56 | EID |
| CLASSIFYATTRIBUTEDATA | 57,235 | 9 | EID |
| WORKFLOWTASKINFO | 55,897 | 30 | EID |
| WORKFLOWACTORINFO | 52,509 | 8 | EID |
| PSEITEMRELATION | 46,580 | 19 | LID |
| DOCFILEMODEL | 45,848 | 30 | EID |
| DOCBASEFORM | 45,560 | 19 | EID |
| DOCDATASET | 45,548 | 55 | EID |
| WORKFLOWATTACHINFO | 41,520 | 18 | EID |
| OPOBJECTID | 28,838 | 8 | EID |
| PROJECTWORKDAY | 28,798 | 2 | ENAME, PROJECTPLANID |
| SENDQGDDATAVERSION | 28,728 | 17 | EID |
| CLASSIFYRELATION | 17,648 | 17 | LID |
| QUERYROLEHISTORY | 17,226 | 19 | EID |
| WORKFLOWINFORM | 14,002 | 17 | EID |

## 二·补、结构要点与业务域索引

### 结构要点（先看这五条，少踩坑）

1. **全库 0 个外键**（`sys.foreign_keys` 为空）：对象引用靠 `EID`/`LID` 字符串 + 应用层维护，迁移校验不能指望数据库约束，必须按业务语义自校验。
2. **主键统一是 `nvarchar` 的 `EID`**（关系表是 `LID`），形如 `PSEITEM_<32hex>`；**不是自增数字**，跨表关联一律用这个字符串。
3. **带 `$` 后缀的表是"快照/历史"伴生表**：`PSEITEM$` 196,520 vs `PSEITEM` 116,035、`PSEBASEFORM$` 196,842 vs 116,037、`PSEBOMLINE$` 253,829 vs 79,105、`DOCBASEFORM$` 60,130 vs 45,560 —— 迁移取"当前"数据应读不带 `$` 的表（**待验证**：`$` 表是否即版本快照）。
4. **业务属性不在主表**：`PSEITEM`/`PSEBASEFORM` 是两套（50 vs 51 列），零件业务属性落 `PSEBASEFORM`；文档同理 `DOCBASEFORM`/`DOCDATASET`/`DOCFILEMODEL` 三张表分工（主档 / 数据集 / 文件模型）。
5. **中文列注释（`MS_Description`）基本缺失**：本文档"说明"列多为空，字段语义要靠蓝本 `.model/.dic` 与代码取证补，不要凭列名猜。

### 业务域索引（按表名前缀）

| 前缀 | 表数 | 业务域 | 典型表 | NexPLM 对应 |
|---|---|---|---|---|
| `PROJEC` | 51 | 项目管理（计划/工作项/工作日报/风险） | `PROJECTPLAN`、`PROJECTWORKDAY`、`PROJECTTASK*` | `prj_project` / `project_task` / `project_timesheet` |
| `MOBILE` | 17 | 移动端/待办/消息 | `MOBILE*` | `/m/*` 页面 + `sys_notify_inbox` |
| `WORKFL` | 13 | 工作流（任务/会签/通知/模板） | `WORKFLOWTASKINFO`(55,897)、`WORKFLOWINFO`、`WORKFLOWTEMPLATE` | Flowable `ACT_*` + `wf_binding`/`wf_comment` |
| `CLASSI` | 11 | 零部件分类（分组/分类/关系/属性） | `CLASSIFYCLASS`(264)、`CLASSIFYGROUP`(41)、`CLASSIFYRELATION`(17,648) | `doc_class`（entity_code=PdmItem）+ `item_master.class_id`（**单分类差异**，见 `EXP-20261108-17`） |
| `CAPP*` | 9+ | 工艺（资源/路线/卡片） | `CAPPRE*` | `capp_routing` / `capp_resource` / `capp_card` |
| `PSEITE` | 8 | 零件（主档/视图/关系/关联对象） | `PSEITEM`(116,035)、`PSEITEMVIEW`(116,038)、`PSEITEMRELATION`(46,580) | `plm_object` + `item_master` |
| `PSEBASE` | 2 | 零件业务属性（含 `$` 快照） | `PSEBASEFORM`(116,037) | `item_master` + `plm_object.ext_json` |
| `PSEBOM*` | — | BOM 结构（含 `$`） | `PSEBOMLINE`(79,105)、`PSEBOMLINE$`(253,829) | `bom_line`（三视图） |
| `DOC*` | 6+ | 图文档（主档/数据集/文件模型） | `DOCBASEFORM`(45,560)、`DOCDATASET`(45,548)、`DOCFILEMODEL`(45,848) | `doc_master` + `file_object`/`file_link` |
| `CODER*` `CODEVA*` | 8 | 编码规则段、编码发放记录、编码值 | `CODERULE`(58 段)、`CODERES`(60,300)、`CODERULETOVALUE` | `code_rule` + `code_rule_audit`（**B19 切换的对照源**） |
| `QUERYM*` `QUERYR*` `MYQUER*` | 14 | 高级查询（机器/角色级历史） | `QUERYMACHINESHOWVIEW`(1,708,803) | `doc_saved_query` |
| `QPROBL*` | 8 | 质量问题（问题/方案/模板/关注） | `QPROBLEM`、`QPROBLEMSOLUTION`、`QPROBLEMTEMPLATE` | `qlt_issue` + 8D 模板 |
| `COSTB*` `COSTC*` `COSTD*` `COSTS*` `COSTA*` | 19 | 成本基线/成本变更/成本科目 | `COSTBASELINE*` | 未复刻（新系统成本走金蝶） |
| `WSFOLD*` | — | 工作空间（文件夹/内容） | `WSFOLDERCONTENT`(376,564) | `ws_folder` / `ws_content` |
| `OPERATELOG` | 1 | 操作日志（86.7 万） | `OPERATELOG`(867,105) | `sys_audit_log` |
| `INTERNALMAIL` | 1 | 站内消息 | `INTERNALMAIL`(125,276) | `sys_notify_inbox` |
| `SUP*` / `QLT*` | — | 供应商 / 质量体系（按需在表清单检索） | — | `sup_supplier` / `qlt_*` |

> 前缀统计原始数据见本节生成时的 `sys.tables` 聚合（`PROJEC=51, MOBILE=17, WORKFL=13, CLASSI=11, CAPPRE=9, PSEITE=8, QPROBL=8, PMTASK=7, QUERYM=7, COSTBA=6, DOCFIL=6, …`）。

### 编码体系（B19 直接相关）

- `CODERULE`（58 行）是**"码段"级记录**，不是整条规则：列 `ENAME/STATEID/LENGTH/MARKCHAR/ORDERNUM/THICKRULE/THINRULE/PARENTCODERULE/CODEDROPMODE`。
  `THICKRULE/THINRULE` 与 `Config/CodeRule/Coding.coderule` 的段类型一一对应（`01`=Number、`02`=Char、`03`=NumberOrChar、`04`=OrderCode、`05`=Date）；`PARENTCODERULE` + `ORDERNUM` 表达段层级与顺序（实测 53 个根段 + 5 个子段）。
- `CODERES`（60,300 行）是**已发放编码记录**（对象 → 编码），可作为新系统 `code_rule.current_no` 初始化的对照源。
- `CODERULETOVALUE` 为规则与编码值的关联表（结构见下方明细）。

## 三、表清单（按表名排序）

| # | 表 | 行数 | 列数 | 主键 | 说明 |
|---|---|---|---|---|---|
| 1 | AB | 6,096 | 2 |  |  |
| 2 | ABC | 0 | 5 |  |  |
| 3 | ABCD | 4,482 | 1 |  |  |
| 4 | ACTIONRULE | 30 | 10 | EID |  |
| 5 | ACTIONRULECONDITION | 46 | 8 | EID |  |
| 6 | ACTIONRULEMESSAGE | 0 | 8 | EID |  |
| 7 | ACTIONRULEPERMISSION | 48 | 8 | EID |  |
| 8 | ALTERNATESRELATION | 0 | 19 | LID |  |
| 9 | APPLYRELATION | 0 | 14 | LID |  |
| 10 | ATTENTIONISSUELINK | 0 | 14 | LID |  |
| 11 | ATTENTIONRISKLINK | 0 | 12 | LID |  |
| 12 | AUTHORIZEPERMISSIONLOG | 4,801 | 7 | EID |  |
| 13 | AUTOSAVEBASELINETARGETINFO | 5 | 12 | EID |  |
| 14 | BACKPMTASK | 0 | 97 | EID |  |
| 15 | BACKPMTASKOUTPUTRELATION | 0 | 22 | LID |  |
| 16 | BACKPROJECT | 0 | 52 | EID |  |
| 17 | BACKPROJECTTASKTORESOURCE | 0 | 22 | LID |  |
| 18 | BASELINEBOM | 1,176 | 19 | LID |  |
| 19 | BASELINEDOCATTACH | 0 | 11 | LID |  |
| 20 | BASELINEDOCRELATION | 0 | 16 | LID |  |
| 21 | BASELINEOBJECT | 78 | 35 | EID |  |
| 22 | BASELINERELATION | 248 | 16 | LID |  |
| 23 | BOMCODELINK | 0 | 20 | EID |  |
| 24 | BROADCAST | 0 | 10 | EID |  |
| 25 | BROADCASTLINK | 0 | 8 | LID |  |
| 26 | CACHEINFO | 17 | 8 | EID |  |
| 27 | CACHETEMPINFO | 0 | 6 | EID |  |
| 28 | CAPPPROCESSCONFIGLINK | 1 | 15 | LID |  |
| 29 | CAPPRESOURCEATTRIBUTE | 30 | 20 | EID |  |
| 30 | CAPPRESOURCEATTRIBUTEVAL | 640 | 9 | EID |  |
| 31 | CAPPRESOURCEATTRIBUTEVAL2 | 0 | 10 | EID |  |
| 32 | CAPPRESOURCECLASS | 7 | 18 | EID |  |
| 33 | CAPPRESOURCELINK | 172 | 17 | LID |  |
| 34 | CAPPRESOURCEQUERYCOND | 0 | 18 | EID |  |
| 35 | CAPPRESOURCEQUERYHISTORY | 0 | 14 | EID |  |
| 36 | CAPPRESOURCERECORD | 135 | 14 | EID |  |
| 37 | CAPPRESOURCERELATION | 0 | 15 | LID |  |
| 38 | CIS | 0 | 27 | EID |  |
| 39 | CLASSIFYATTRIBUTE | 120 | 23 | EID |  |
| 40 | CLASSIFYATTRIBUTEDATA | 57,235 | 9 | EID |  |
| 41 | CLASSIFYCLASS | 264 | 19 | EID |  |
| 42 | CLASSIFYENUMERATIONITEM | 0 | 14 | EID |  |
| 43 | CLASSIFYGROUP | 41 | 15 | EID |  |
| 44 | CLASSIFYLINK | 305 | 14 | LID |  |
| 45 | CLASSIFYQUERYHISTORY | 2 | 6 | EID |  |
| 46 | CLASSIFYQUERYHISTORYCONDITION | 2 | 9 | EID |  |
| 47 | CLASSIFYRELATION | 17,648 | 17 | LID |  |
| 48 | CLASSIFYSERIALNUM | 3 | 9 | EID |  |
| 49 | CLASSIFYVIEW | 0 | 15 | EID |  |
| 50 | CMAPPLY | 3 | 28 | EID |  |
| 51 | CMAPPLYBASEFORM | 8 | 6 | EID |  |
| 52 | CMNOTICE | 11 | 25 | EID |  |
| 53 | CMNOTICEBASEFORM | 30 | 6 | EID |  |
| 54 | CMPROBLEM | 0 | 23 | EID |  |
| 55 | CMPROBLEMVERIFY | 0 | 16 | EID |  |
| 56 | CMRELATION | 82 | 15 | LID |  |
| 57 | CNCPARA | 0 | 20 | EID |  |
| 58 | CODEKIND | 17 | 13 | EID |  |
| 59 | CODEKINDTORULE | 58 | 8 | LID |  |
| 60 | CODEPERMISSION | 78 | 9 | EID |  |
| 61 | CODERES | 60,300 | 18 | EID |  |
| 62 | CODERULE | 58 | 17 | EID |  |
| 63 | CODERULETOVALUE | 1,170 | 8 | LID |  |
| 64 | CODEVALUE | 1,170 | 16 | EID |  |
| 65 | CODEVALUEPERMISSION | 0 | 8 | EID |  |
| 66 | CODEVALUETORES | 0 | 8 | LID |  |
| 67 | CODEVALUETOVALUE | 260 | 8 | LID |  |
| 68 | COMMANDCLUSTER | 1 | 6 | EID |  |
| 69 | COMMANDSERVICELOCK | 0 | 6 | EID |  |
| 70 | COMMANDSERVICESETTING | 1 | 7 | EID |  |
| 71 | COMMONAPPLY | 0 | 21 | EID |  |
| 72 | COMMONAPPLYBASEFORM | 0 | 9 | EID |  |
| 73 | COMPANY | 1 | 12 | EID |  |
| 74 | COMPANYRULE | 1 | 10 | EID |  |
| 75 | COMPANYRULECONDITION | 1 | 8 | EID |  |
| 76 | COMPANYRULEOBJECT | 1 | 7 | EID |  |
| 77 | COMPANYRULEPERMISSION | 1 | 8 | EID |  |
| 78 | CONFIGCONDITION | 0 | 16 | EID |  |
| 79 | CONVERSIONCOMMAND | 4 | 8 | EID |  |
| 80 | CONVERSIONENDPOINT | 1 | 16 | EID |  |
| 81 | COSTACT | 0 | 28 | EID |  |
| 82 | COSTACTBASELINE | 0 | 29 | EID |  |
| 83 | COSTACTUSERLINK | 0 | 14 | LID |  |
| 84 | COSTAUDIT | 0 | 9 | EID |  |
| 85 | COSTBASELINE | 0 | 21 | EID |  |
| 86 | COSTBASELINEDETAIL | 0 | 29 | EID |  |
| 87 | COSTBASELINEINFO | 0 | 36 | EID |  |
| 88 | COSTBASELINESPACE | 0 | 20 | EID |  |
| 89 | COSTBASELINESUBJECT | 0 | 31 | EID |  |
| 90 | COSTBASELINESUBJECTLINK | 0 | 16 | LID |  |
| 91 | COSTCHANGE | 0 | 26 | EID |  |
| 92 | COSTCHANGEAPPLY | 0 | 26 | EID |  |
| 93 | COSTCHANGEAPPLYFORM | 0 | 22 | EID |  |
| 94 | COSTCHANGEFORM | 0 | 22 | EID |  |
| 95 | COSTDETAIL | 0 | 29 | EID |  |
| 96 | COSTDIC | 1 | 20 | EID |  |
| 97 | COSTDICI | 0 | 14 | EID |  |
| 98 | COSTDICLINK | 0 | 14 | LID |  |
| 99 | COSTINFO | 0 | 35 | EID |  |
| 100 | COSTSPACE | 0 | 20 | EID |  |
| 101 | COSTSPACETEMPLATE | 0 | 20 | EID |  |
| 102 | COSTSUBJECT | 0 | 31 | EID |  |
| 103 | COSTSUBJECTSLINK | 0 | 16 | LID |  |
| 104 | COSTSUBJECTTEMPLATELINK | 0 | 17 | LID |  |
| 105 | COSTTEMPLATEINFO | 0 | 29 | EID |  |
| 106 | CREATESTEPTHREE | 0 | 10 | EID |  |
| 107 | CREPORTLINKRESOURCE | 33 | 19 | EID |  |
| 108 | CREPORTOBJECT | 2,880 | 44 | EID |  |
| 109 | CREPORTOBJECT$ | 5,879 | 45 | EID |  |
| 110 | CRESOURCESERNUM | 0 | 15 | EID |  |
| 111 | CSOBJ | 0 | 30 | EID |  |
| 112 | CUSTOMBOARDFOLDER | 0 | 10 | EID |  |
| 113 | CUSTOMBOARDITEM | 0 | 10 | EID |  |
| 114 | CUSTOMBOARDLINK | 0 | 12 | LID |  |
| 115 | DASHBOARDLAYOUT | 0 | 17 | EID |  |
| 116 | DASHBOARDLAYOUTTMP | 0 | 18 | EID |  |
| 117 | DASHBOARDRELATION | 0 | 10 | LID |  |
| 118 | DASHBOARDWIDGET | 0 | 16 | EID |  |
| 119 | DATASETTYPE | 39 | 15 | EID |  |
| 120 | DATESETTING | 0 | 16 | EID |  |
| 121 | DATEVALIDITY | 0 | 21 | EID |  |
| 122 | DEPLOYCODE | 0 | 17 | EID |  |
| 123 | DEPLOYCODEVALUE | 0 | 17 | EID |  |
| 124 | DEPLOYVARIANTRELATION | 0 | 14 | LID |  |
| 125 | DOCBASEFORM | 45,560 | 19 | EID |  |
| 126 | DOCBASEFORM$ | 60,130 | 20 | EID |  |
| 127 | DOCDATASET | 45,548 | 55 | EID |  |
| 128 | DOCDATASET$ | 59,823 | 56 | EID |  |
| 129 | DOCFILEMODEL | 45,848 | 30 | EID |  |
| 130 | DOCFILEMODEL$ | 60,233 | 31 | EID |  |
| 131 | DOCFILETEMPLATE | 49 | 18 | EID |  |
| 132 | DOCFILETEMPLATECLASSIFY | 5 | 15 | EID |  |
| 133 | DOCFILETEMPLATELINK | 54 | 14 | LID |  |
| 134 | DOCFILETEMPLATETOFILE | 51 | 14 | LID |  |
| 135 | DOCJHORPC | 0 | 18 | EID |  |
| 136 | DOCRELATION | 5 | 15 | LID |  |
| 137 | DOCTEMPLATE | 7 | 23 | EID |  |
| 138 | DOCTEMPLATETODOCFILETEMPLATE | 50 | 8 | LID |  |
| 139 | EAISETTING | 0 | 20 | EID |  |
| 140 | ELECMATERIAL | 0 | 14 | EID |  |
| 141 | ENUMERATION | 38 | 14 | EID |  |
| 142 | ENUMERATIONCATEGORY | 9 | 10 | EID |  |
| 143 | ENUMERATIONITEM | 254 | 15 | EID |  |
| 144 | EPSLINK | 1,037 | 15 | LID |  |
| 145 | FAVORITEFOLDER | 0 | 9 | EID |  |
| 146 | FAVORITELINK | 24 | 14 | LID |  |
| 147 | FLOWBINDINFO | 0 | 7 | EID |  |
| 148 | FORMBUILDERDATA | 0 | 17 | EID |  |
| 149 | FORMCENTOR | 0 | 5 | EID |  |
| 150 | FORMCLASSIFY | 0 | 7 | EID |  |
| 151 | FORMCLASSIFYLINK | 0 | 8 | LID |  |
| 152 | FORMCLASSIFYTOOBJTYPE | 0 | 8 | EID |  |
| 153 | FORMDATA | 0 | 9 | EID |  |
| 154 | FORMFLOWTASKPERMISSION | 0 | 9 | EID |  |
| 155 | FORMULAPARA | 2,597 | 22 | EID |  |
| 156 | FORMULAPARAVALUE | 31 | 18 | EID |  |
| 157 | FORPLATFORM | 0 | 8 | EID |  |
| 158 | FSMANAGEROOTLINK | 0 | 10 | LID |  |
| 159 | FUNCBUTTON | 30 | 19 | EID |  |
| 160 | GROUPINFO | 68 | 20 | EID |  |
| 161 | GROUPISOLATEINFO | 0 | 10 | EID |  |
| 162 | GROUPSHAREMAPPING | 1 | 9 | EID |  |
| 163 | GROUPSPACEDISPLAY | 0 | 7 | EID |  |
| 164 | INDIVIDUATIONSETTING | 2,065 | 22 | EID |  |
| 165 | INSTANTMESSAGE | 0 | 12 | EID |  |
| 166 | INTERNALMAIL | 125,276 | 28 | EID |  |
| 167 | INTERNALMAILTOPLUGIN | 0 | 8 | LID |  |
| 168 | ISSUE | 0 | 43 | EID |  |
| 169 | ISSUEPRINCIPAL | 0 | 7 | EID |  |
| 170 | ISSUEPROMOTE | 0 | 10 | EID |  |
| 171 | ISSUETASK | 0 | 15 | EID |  |
| 172 | ISSUETASKLINK | 0 | 9 | LID |  |
| 173 | ISSUETEMPLATE | 0 | 26 | EID |  |
| 174 | ISSUETODOC | 0 | 9 | LID |  |
| 175 | ITEMATTTOFORMULAPARA | 12 | 18 | EID |  |
| 176 | LIFECYCLE | 0 | 12 | EID |  |
| 177 | LOOPPLAYIMAGE | 14 | 11 | EID |  |
| 178 | MAZYTABLE | 0 | 14 | EID |  |
| 179 | MCOBJ | 0 | 14 | EID |  |
| 180 | MCOBJLINK | 0 | 14 | LID |  |
| 181 | MCOBJVALUE | 0 | 15 | EID |  |
| 182 | MLDETAIL | 0 | 8 | EID |  |
| 183 | MOBILEADVANCEDQUERY | 3 | 17 | EID |  |
| 184 | MOBILEMODULEPERMISSION | 0 | 7 | EID |  |
| 185 | MOBILEPAGEBUTTON | 62 | 15 | EID |  |
| 186 | MOBILEPAGEDEV | 27 | 9 | EID |  |
| 187 | MOBILEPAGEFILTER | 6 | 16 | EID |  |
| 188 | MOBILEPAGELISTITEM | 28 | 15 | EID |  |
| 189 | MOBILEPAGENAV | 9 | 20 | EID |  |
| 190 | MOBILEPAGEORDER | 4 | 13 | EID |  |
| 191 | MOBILEPAGEPROPERTYTAB | 34 | 24 | EID |  |
| 192 | MOBILEPAGEPROPERTYTABCONFIG | 0 | 8 | EID |  |
| 193 | MOBILEPAGEPROPERTYTABCUSTOM | 0 | 10 | EID |  |
| 194 | MOBILEPAGEPROPERTYTABWEB | 0 | 8 | EID |  |
| 195 | MOBILEPAGEROOTTODEV | 27 | 8 | LID |  |
| 196 | MOBILEPAGETAB | 21 | 35 | EID |  |
| 197 | MOBILEPAGETABCONDITION | 0 | 17 | EID |  |
| 198 | MOBILEPAGETABGROUP | 0 | 18 | EID |  |
| 199 | MOBILEPAGETABSTANDARD | 0 | 14 | EID |  |
| 200 | MODELDATA | 0 | 23 | EID |  |
| 201 | MODULEPERMISSION | 50 | 7 | EID |  |
| 202 | MODULEPERMISSIONDATA | 177 | 8 | EID |  |
| 203 | MSEACTPARA | 0 | 20 | EID |  |
| 204 | MSEBOOK | 0 | 13 | EID |  |
| 205 | MSEBOOKITEM | 0 | 17 | EID |  |
| 206 | MSECL | 0 | 24 | EID |  |
| 207 | MSECLASS | 0 | 14 | EID |  |
| 208 | MSECLASSATT | 0 | 18 | EID |  |
| 209 | MSECLASSLINK | 0 | 14 | LID |  |
| 210 | MSECLASSRELA | 0 | 14 | LID |  |
| 211 | MSECODEPARA | 0 | 20 | EID |  |
| 212 | MSECONFIG | 1 | 29 | EID |  |
| 213 | MSECRELA | 0 | 15 | LID |  |
| 214 | MSEDCPARA | 0 | 20 | EID |  |
| 215 | MSEFILDATA | 0 | 17 | EID |  |
| 216 | MSEJYX | 0 | 34 | EID |  |
| 217 | MSELABEL | 0 | 10 | EID |  |
| 218 | MSELEVELCONFIG | 0 | 9 | EID |  |
| 219 | MSELINK | 0 | 15 | LID |  |
| 220 | MSELINK$ | 0 | 16 | LID |  |
| 221 | MSEMATERIAL | 0 | 27 | EID |  |
| 222 | MSENL | 0 | 10 | EID |  |
| 223 | MSENODESTYLE | 3 | 9 | EID |  |
| 224 | MSEOBJ | 0 | 52 | EID |  |
| 225 | MSEOBJ$ | 0 | 53 | EID |  |
| 226 | MSEOP | 0 | 22 | EID |  |
| 227 | MSEOPINION | 0 | 23 | EID |  |
| 228 | MSEPARA | 0 | 27 | EID |  |
| 229 | MSEPARALIB | 0 | 20 | EID |  |
| 230 | MSEPARAPLANLIB | 0 | 21 | EID |  |
| 231 | MSEPARAPRO | 0 | 16 | EID |  |
| 232 | MSEPARAPROLINK | 0 | 14 | LID |  |
| 233 | MSEPATHPARA | 0 | 17 | EID |  |
| 234 | MSEPER | 0 | 19 | EID |  |
| 235 | MSERELA | 0 | 16 | LID |  |
| 236 | MSERELATYPE | 0 | 19 | EID |  |
| 237 | MSEREPATT | 0 | 20 | EID |  |
| 238 | MSEREPORT | 0 | 18 | EID |  |
| 239 | MSESORT | 0 | 18 | EID |  |
| 240 | MSESTRDRI | 0 | 21 | EID |  |
| 241 | MSESTRDRIVAR | 0 | 18 | EID |  |
| 242 | MSETIME | 0 | 24 | EID |  |
| 243 | MSETYPPARALIB | 0 | 20 | EID |  |
| 244 | MSEZLATT | 0 | 20 | EID |  |
| 245 | MTGROUP | 0 | 12 | EID |  |
| 246 | MTGROUPLINK | 0 | 15 | LID |  |
| 247 | MTLINK | 0 | 15 | LID |  |
| 248 | MTPARA | 0 | 16 | EID |  |
| 249 | MTVALUE | 0 | 21 | EID |  |
| 250 | MYQUERYMACHINEBASEINFO | 1 | 18 | EID |  |
| 251 | MYQUERYMACHINEPOM | 1 | 7 | EID |  |
| 252 | MYQUERYPOMLINKDATA | 1 | 10 | EID |  |
| 253 | MYQUERYROLE | 14 | 18 | EID |  |
| 254 | NEEDREFRESHFLAG | 0 | 6 | EID |  |
| 255 | OBJECTFORM | 1 | 14 | EID |  |
| 256 | OBJECTMESSAGE | 0 | 9 | EID |  |
| 257 | OBJECTPERMISSION | 3,160 | 12 | EID |  |
| 258 | OBJGRPER | 0 | 6 | EID |  |
| 259 | OLBCISSCHEMATIC | 0 | 10 | EID |  |
| 260 | OLBFILE | 0 | 14 | EID |  |
| 261 | OPACTIONVALUE | 5,400 | 10 | EID |  |
| 262 | OPACTORVALUE | 5,400 | 10 | EID |  |
| 263 | OPERATELOG | 867,105 | 17 | EID |  |
| 264 | OPERATIONSPECIFICATION | 0 | 16 | EID |  |
| 265 | OPOBJECTID | 28,838 | 8 | EID |  |
| 266 | OPTIONRESTRAIN | 0 | 19 | EID |  |
| 267 | ORGANIZE | 403 | 11 | LID |  |
| 268 | PAGEMAINCONFIG | 0 | 13 | EID |  |
| 269 | PANELCHART | 3 | 32 | EID |  |
| 270 | PARTIALCACHEINFO | 0 | 7 | EID |  |
| 271 | PATCH | 0 | 25 | EID |  |
| 272 | PATCHDEPENDENCELINK | 0 | 8 | LID |  |
| 273 | PATCHDEPENDENCELINKINSTALL | 0 | 10 | LID |  |
| 274 | PATCHINSTALL | 0 | 29 | EID |  |
| 275 | PCHAR | 0 | 10 | EID |  |
| 276 | PERSON | 248 | 18 | EID |  |
| 277 | PLMENTITY | 0 | 14 | EID |  |
| 278 | PLMENTITYITERATION | 0 | 26 | EID |  |
| 279 | PLMFORM | 0 | 6 | EID |  |
| 280 | PLMLINK | 0 | 14 | LID |  |
| 281 | PLMSTATISTICS | 0 | 14 | EID |  |
| 282 | PLMSTATISTICSLINK | 0 | 8 | LID |  |
| 283 | PLMSTATISTICSQUERYROLE | 0 | 15 | EID |  |
| 284 | PLMUNIT | 28 | 10 | EID |  |
| 285 | PMTASK | 11,203 | 93 | EID |  |
| 286 | PMTASKBASEFORM | 0 | 15 | EID |  |
| 287 | PMTASKCOPY | 12,978 | 94 | EID, COPYBATCHNO |  |
| 288 | PMTASKLINE | 679 | 94 | EID |  |
| 289 | PMTASKOUTPUTRELATION | 501 | 18 | LID |  |
| 290 | PMTASKRELATION | 0 | 16 | LID |  |
| 291 | PMTASKTEMPLATE | 0 | 17 | EID |  |
| 292 | POSITIONLINK | 0 | 15 | LID |  |
| 293 | POSITIONRELATION | 0 | 15 | LID |  |
| 294 | POSITIONUNIT | 0 | 16 | EID |  |
| 295 | PRLINK | 0 | 14 | LID |  |
| 296 | PROCESSATTACHDOC | 0 | 19 | EID |  |
| 297 | PROCESSROUTE | 0 | 19 | EID |  |
| 298 | PRODUCTCONFIGBILL | 0 | 14 | LID |  |
| 299 | PRODUCTMATCH | 0 | 17 | EID |  |
| 300 | PROJECT | 512 | 48 | EID |  |
| 301 | PROJECTATTENTIONLINK | 7 | 15 | LID |  |
| 302 | PROJECTBASELINE | 16 | 19 | EID |  |
| 303 | PROJECTBASELINETOPLAN | 16 | 15 | LID |  |
| 304 | PROJECTCALENDAR | 4,899 | 24 | EID |  |
| 305 | PROJECTCALENDARTYPE | 1 | 9 | EID |  |
| 306 | PROJECTCHANGERECORD | 0 | 11 | EID |  |
| 307 | PROJECTENTITY | 0 | 14 | EID |  |
| 308 | PROJECTEPS | 13 | 16 | EID |  |
| 309 | PROJECTFOLDER | 5,026 | 18 | EID |  |
| 310 | PROJECTFOLDERLINK | 8,561 | 14 | LID |  |
| 311 | PROJECTFOLDERTEMPLATE | 71 | 14 | EID |  |
| 312 | PROJECTFOLDERTEMPLATECLASSIFY | 1 | 14 | EID |  |
| 313 | PROJECTFOLDERTEMPLATELINK | 55 | 14 | LID |  |
| 314 | PROJECTFORM | 523 | 19 | EID |  |
| 315 | PROJECTGLOBLECALENDAR | 5,223 | 1 | ENAME |  |
| 316 | PROJECTINFOCHANGE | 0 | 22 | EID |  |
| 317 | PROJECTINFOCHANGEAPPLY | 0 | 22 | EID |  |
| 318 | PROJECTINFOCHANGEAPPLYFORM | 0 | 17 | EID |  |
| 319 | PROJECTINFOCHANGEFORM | 0 | 17 | EID |  |
| 320 | PROJECTLINK | 0 | 15 | LID |  |
| 321 | PROJECTPLAN | 512 | 24 | EID |  |
| 322 | PROJECTPLANCHANGE | 1 | 22 | EID |  |
| 323 | PROJECTPLANCHANGEAPPLY | 0 | 22 | EID |  |
| 324 | PROJECTPLANCHANGEAPPLYFORM | 1 | 17 | EID |  |
| 325 | PROJECTPLANCHANGEFORM | 6 | 17 | EID |  |
| 326 | PROJECTPLANFLOW | 1 | 16 | EID |  |
| 327 | PROJECTPLANLINE | 16 | 25 | EID |  |
| 328 | PROJECTPLANTOPLAN | 512 | 14 | LID |  |
| 329 | PROJECTRESOUSCELINK | 755 | 16 | LID |  |
| 330 | PROJECTROLEINFO | 14 | 15 | EID |  |
| 331 | PROJECTROLELINK | 2,098 | 15 | LID |  |
| 332 | PROJECTSUSPEND | 0 | 17 | EID |  |
| 333 | PROJECTTASKADDWORKTIME | 73 | 18 | LID |  |
| 334 | PROJECTTASKLINK | 3 | 18 | LID |  |
| 335 | PROJECTTASKLINKCOPY | 88 | 19 | LID, COPYBATCHNO |  |
| 336 | PROJECTTASKLINKLINE | 3 | 19 | LID |  |
| 337 | PROJECTTASKLOG | 607 | 11 | EID |  |
| 338 | PROJECTTASKTEMPLATE | 514 | 39 | EID |  |
| 339 | PROJECTTASKTEMPLATECOPY | 508 | 40 | EID, COPYBATCHNO |  |
| 340 | PROJECTTASKTEMPLATEFOLDER | 1 | 14 | EID |  |
| 341 | PROJECTTASKTEMPLATELINK | 2 | 17 | LID |  |
| 342 | PROJECTTASKTORESLINE | 951 | 19 | LID |  |
| 343 | PROJECTTASKTORESOURCE | 2,702 | 18 | LID |  |
| 344 | PROJECTTASKTORESOURCECOPY | 1,891 | 19 | LID, COPYBATCHNO |  |
| 345 | PROJECTTEMPLATE | 5 | 20 | EID |  |
| 346 | PROJECTTEMPLATELINK | 0 | 15 | LID |  |
| 347 | PROJECTTOBASELINE | 16 | 15 | LID |  |
| 348 | PROJECTTOPROJECTLINK | 2 | 15 | LID |  |
| 349 | PROJECTWBSCOLUMNSET | 18 | 9 | EID |  |
| 350 | PROJECTWORKDAY | 28,798 | 2 | ENAME, PROJECTPLANID |  |
| 351 | PROUTE | 0 | 36 | EID |  |
| 352 | PSEBASEFORM | 116,037 | 51 | EID |  |
| 353 | PSEBASEFORM$ | 196,842 | 52 | EID |  |
| 354 | PSEBOMLINE | 79,105 | 37 | LID |  |
| 355 | PSEBOMLINE$ | 253,829 | 38 | LID |  |
| 356 | PSECLASSIFYRELATION | 1,302 | 16 | LID |  |
| 357 | PSEITEM | 116,035 | 50 | EID |  |
| 358 | PSEITEM$ | 196,520 | 51 | EID |  |
| 359 | PSEITEMASSOCIATEDOBJECT | 0 | 14 | LID |  |
| 360 | PSEITEMREFERENCEOBJECT | 0 | 14 | LID |  |
| 361 | PSEITEMRELATION | 46,580 | 19 | LID |  |
| 362 | PSEITEMRELATION$ | 0 | 20 | LID |  |
| 363 | PSEITEMVIEW | 116,038 | 29 | EID |  |
| 364 | PSEITEMVIEW$ | 137,746 | 30 | EID |  |
| 365 | PSEVERSIONRULE | 2 | 16 | EID |  |
| 366 | PSEVERSIONRULEDATA | 2 | 10 | EID |  |
| 367 | PTTTOPTTLINK | 2 | 18 | LID |  |
| 368 | PTTTOPTTLINKCOPY | 1 | 19 | LID, COPYBATCHNO |  |
| 369 | QPROBLEM | 0 | 46 | EID |  |
| 370 | QPROBLEMATTENTION | 0 | 9 | LID |  |
| 371 | QPROBLEMDOCFILE | 0 | 13 | EID |  |
| 372 | QPROBLEMPROMOTE | 0 | 10 | EID |  |
| 373 | QPROBLEMRELATION | 0 | 10 | LID |  |
| 374 | QPROBLEMSOLUTION | 0 | 12 | EID |  |
| 375 | QPROBLEMTEMPLATE | 0 | 26 | EID |  |
| 376 | QPROBLEMTODOC | 0 | 8 | LID |  |
| 377 | QUALITYCHECK | 0 | 19 | EID |  |
| 378 | QUERYMACHINEBASEINFO | 14 | 17 | EID |  |
| 379 | QUERYMACHINEHISTORY | 1,256 | 18 | EID |  |
| 380 | QUERYMACHINEPOM | 30 | 7 | EID |  |
| 381 | QUERYMACHINEPOMHISTORY | 1,176 | 7 | EID |  |
| 382 | QUERYMACHINEPOMLINKDATA | 10 | 10 | EID |  |
| 383 | QUERYMACHINESHOWVIEW | 1,708,803 | 11 | EID |  |
| 384 | QUERYMACHINESTRUCTURE | 0 | 8 | LID |  |
| 385 | QUERYPOMLINKHISTORY | 1,051 | 10 | EID |  |
| 386 | QUERYROLEHISTORY | 17,226 | 19 | EID |  |
| 387 | QUERYRULEBASEINFO | 118 | 18 | EID |  |
| 388 | QUERYRULEBUILDININFO | 0 | 17 | EID |  |
| 389 | QUEUECACHE | 0 | 8 | EID |  |
| 390 | QUICKNAVIGATION | 45 | 14 | EID |  |
| 391 | RESITEMATTRELATEDCONFIG | 0 | 9 | EID |  |
| 392 | RESOURCEBASE | 247 | 27 | EID |  |
| 393 | RESOURCECLASSIFY | 1 | 13 | EID |  |
| 394 | RESOURCELINK | 248 | 15 | LID |  |
| 395 | RISK | 0 | 43 | EID |  |
| 396 | RISKAIMLINK | 0 | 9 | LID |  |
| 397 | RISKAIMS | 0 | 9 | EID |  |
| 398 | RISKAPPRAISERLINK | 0 | 12 | LID |  |
| 399 | RISKASSESSAIMSTITLE | 0 | 11 | EID |  |
| 400 | RISKASSESSAMISDATA | 0 | 11 | EID |  |
| 401 | RISKASSESSINFO | 0 | 8 | EID |  |
| 402 | RISKDOCLINK | 0 | 11 | LID |  |
| 403 | RISKDOCMODEL | 0 | 14 | EID |  |
| 404 | RISKTEMPLATE | 0 | 21 | EID |  |
| 405 | ROLEINFO | 78 | 10 | EID |  |
| 406 | RP_CELL | 947 | 34 | TEMPLATEID, CCELLPOS, RCELLPOS |  |
| 407 | RP_COL | 51 | 10 | TEMPLATEID, COLID |  |
| 408 | RP_FITEM | 0 | 11 | REPORTID, PAGEID, ITEMID |  |
| 409 | RP_IMAGE | 5,346 | 7 | REPORTID, PAGEID, DBLABEL |  |
| 410 | RP_LOG | 0 | 4 | ID |  |
| 411 | RP_NOTE | 0 | 6 | REPORTID, NOTEID |  |
| 412 | RP_PAGE | 5,874 | 5 | REPORTID, PAGEID |  |
| 413 | RP_PAGEPOSITION | 0 | 6 | REPORTID, PAGEID, TYPE, POS |  |
| 414 | RP_POSITION | 212 | 4 | TEMPLATEID, TYPE, POS |  |
| 415 | RP_PROPERTY | 8,190 | 4 | REPORTID, PAGEID, DBLABEL |  |
| 416 | RP_REPORT | 5,879 | 39 | REPORTID |  |
| 417 | RP_SPECIALCHAR | 0 | 9 | ID |  |
| 418 | RP_TABLE | 5 | 4 | TEMPLATEID, AREA |  |
| 419 | RP_TEMPLATE | 6 | 14 | ID |  |
| 420 | RP_TEMPLATETYPE | 2 | 2 | TYPEID |  |
| 421 | RPD_LJJGK | 4,992 | 12 | REPORTID, PAGEID, ROWNO |  |
| 422 | RPD_LJJGMX | 13,935 | 17 | REPORTID, PAGEID, ROWNO |  |
| 423 | RPD_ZPGYK | 885 | 12 | REPORTID, PAGEID, ROWNO |  |
| 424 | RPD_ZPGYKMX | 5,843 | 14 | REPORTID, PAGEID, ROWNO |  |
| 425 | SELECTOBJSHAREINFO | 0 | 11 | EID |  |
| 426 | SENDQGDDATAVERSION | 28,728 | 17 | EID |  |
| 427 | SHAREREFRESHLOCK | 0 | 6 | EID |  |
| 428 | SHOWVIEWCOLUMN | 0 | 10 | EID |  |
| 429 | SHOWVIEWINFO | 0 | 9 | EID |  |
| 430 | SLAVETABLEPOSITIONINFO | 0 | 8 | EID |  |
| 431 | TASKLOGTRANCE | 0 | 13 | EID |  |
| 432 | TEMPTOOLING | 0 | 42 | EID |  |
| 433 | TFOBJECT | 0 | 18 | EID |  |
| 434 | TFOBJECTFOLDER | 0 | 13 | EID |  |
| 435 | TFOBJECTLINK | 0 | 15 | LID |  |
| 436 | TFTEMPLATE | 0 | 12 | EID |  |
| 437 | TFTEMPLATEFOLDER | 0 | 13 | EID |  |
| 438 | TFTEMPLATELINK | 0 | 19 | LID |  |
| 439 | THEMDESIGNDATA | 0 | 9 | EID |  |
| 440 | THIRDEAISETTING | 0 | 10 | EID |  |
| 441 | TIMEQUOTA | 0 | 29 | EID |  |
| 442 | TIMEQUOTALINK | 0 | 14 | LID |  |
| 443 | TIMEQUOTAPARA | 0 | 17 | EID |  |
| 444 | TIMEREXECUTERECORD | 1 | 8 | EID |  |
| 445 | TSCLASS | 3 | 18 | EID |  |
| 446 | TSCLASSATTRIBUTE | 0 | 19 | EID |  |
| 447 | TSCLASSTOGROUP | 0 | 20 | EID |  |
| 448 | TSCONFIGOBJ | 84 | 22 | EID |  |
| 449 | TSGROUP | 0 | 19 | EID |  |
| 450 | TSGROUPATTRIBUTE | 0 | 17 | EID |  |
| 451 | TSGROUPTOGROUP | 0 | 17 | EID |  |
| 452 | TSMANAGER | 0 | 55 | EID |  |
| 453 | TTQGROUP | 0 | 15 | EID |  |
| 454 | TTQGROUPLINK | 0 | 15 | LID |  |
| 455 | TTQOBJ | 0 | 18 | EID |  |
| 456 | TTQPARA | 0 | 19 | EID |  |
| 457 | TTQPARAVALUE | 0 | 16 | EID |  |
| 458 | TYPICALROUTE | 0 | 14 | EID |  |
| 459 | UNITVALIDITY | 0 | 20 | EID |  |
| 460 | USERGETHISTORY | 711 | 13 | EID |  |
| 461 | USERGLOBALINFO | 0 | 7 | EID |  |
| 462 | USERINFO | 247 | 33 | EID |  |
| 463 | WBSUSERINFORMATION | 26 | 8 | EID |  |
| 464 | WEBMODELVIEWDATA | 0 | 10 | EID |  |
| 465 | WORKFLOWACTORINFO | 52,509 | 8 | EID |  |
| 466 | WORKFLOWATTACHINFO | 41,520 | 18 | EID |  |
| 467 | WORKFLOWCHANGESTATE | 11,931 | 7 | EID |  |
| 468 | WORKFLOWCLASSIFY | 5 | 10 | EID |  |
| 469 | WORKFLOWCODE | 12,258 | 8 | EID |  |
| 470 | WORKFLOWENTERAUDITINFO | 328 | 8 | EID |  |
| 471 | WORKFLOWINFO | 11,930 | 17 | EID |  |
| 472 | WORKFLOWINFORM | 14,002 | 17 | EID |  |
| 473 | WORKFLOWPLACEONFILE | 3,933 | 8 | EID |  |
| 474 | WORKFLOWQUOTEATTACH | 0 | 7 | EID |  |
| 475 | WORKFLOWTASKDEALWITH | 0 | 17 | LID |  |
| 476 | WORKFLOWTASKINFO | 55,897 | 30 | EID |  |
| 477 | WORKFLOWTEMPLATE | 370 | 18 | EID |  |
| 478 | WORKLOG | 4 | 34 | EID |  |
| 479 | WORKLOGREMARK | 0 | 9 | EID |  |
| 480 | WSFOLDER | 2,677 | 21 | EID |  |
| 481 | WSFOLDERCONTENT | 376,564 | 15 | LID |  |
| 482 | WSPOSITION | 87 | 15 | LID |  |
| 483 | XMLPERSISTENCE | 0 | 5 | XMLID |  |

## 四、表结构明细

### AB

**行数** 6,096

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | ECODE | nvarchar(100) | Y |  |  |  |
| 2 | GGXH | nvarchar(300) | Y |  |  |  |

### ABC

**行数** 0

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | MD$NUMBER | nvarchar(100) | Y |  |  |  |
| 2 | MD$DESC | nvarchar(100) | Y |  |  |  |
| 3 | VF$OUID | nvarchar(100) | Y |  |  |  |
| 4 | MD$VERSION | nvarchar(100) | Y |  |  |  |
| 5 | MD$PATH | nvarchar(500) | Y |  |  |  |

### ABCD

**行数** 4,482

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | ECODE | nvarchar(100) | Y |  |  |  |

### ACTIONRULE

**行数** 30 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y | (N'0') |  |  |
| 8 | PEID | nvarchar(100) | Y |  |  |  |
| 9 | USEBY | nvarchar(100) | Y |  |  |  |
| 10 | CREATEPERMISSIONOBJECTTYPE | nvarchar(100) | Y |  |  |  |

### ACTIONRULECONDITION

**行数** 46 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ACTIONRULEID | nvarchar(100) |  |  |  |  |
| 7 | OPERATOR | nvarchar(100) |  |  |  |  |
| 8 | CONDITIONVALUE | nvarchar(500) |  |  |  |  |

### ACTIONRULEMESSAGE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ACTIONRULEID | nvarchar(100) |  |  |  |  |
| 7 | MESSAGEACTORVALUE | ntext |  |  |  |  |
| 8 | MESSAGEACTIONVALUE | ntext |  |  |  |  |

### ACTIONRULEPERMISSION

**行数** 48 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ACTIONRULEID | nvarchar(100) |  |  |  |  |
| 7 | PERMISSIONACTORVALUE | ntext |  |  |  |  |
| 8 | PERMISSIONACTIONVALUE | ntext |  |  |  |  |

### ALTERNATESRELATION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | PVIEWID | nvarchar(100) | Y |  |  |  |
| 16 | ISGLOBAL | bit | Y |  |  |  |
| 17 | ISTWOWAY | bit | Y |  |  |  |
| 18 | PRIORITYLEVEL | nvarchar(100) | Y |  |  |  |
| 19 | LDESCRIPTION | nvarchar(500) | Y |  |  |  |

### APPLYRELATION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### ATTENTIONISSUELINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | LCREATETIME | bigint |  |  |  |  |
| 10 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 11 | PEUID | nvarchar(100) | Y |  |  |  |
| 12 | CEUID | nvarchar(100) | Y |  |  |  |
| 13 | VERSIONEID | nvarchar(100) | Y |  |  |  |
| 14 | ISVIEW | nvarchar(100) | Y | (N'false') |  |  |

### ATTENTIONRISKLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | LCREATETIME | bigint |  |  |  |  |
| 8 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | PEUID | nvarchar(100) | Y |  |  |  |
| 10 | CEUID | nvarchar(100) | Y |  |  |  |
| 11 | VERSIONEID | nvarchar(100) | Y |  |  |  |
| 12 | ISVIEW | nvarchar(100) | Y | (N'false') |  |  |

### AUTHORIZEPERMISSIONLOG

**行数** 4,801 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | LOGCONTENT | nvarchar(1000) | Y |  |  |  |
| 7 | OBJECTID | nvarchar(100) | Y |  |  |  |

### AUTOSAVEBASELINETARGETINFO

**行数** 5 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ITEMTYPETEXT | nvarchar(500) | Y |  |  |  |
| 7 | ITEMTYPEVALUE | nvarchar(500) | Y |  |  |  |
| 8 | TARGETATTACHMENTVALUE | nvarchar(500) | Y |  |  |  |
| 9 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 10 | DOCRELATIONTYPE | nvarchar(200) | Y |  |  |  |
| 11 | DOCTYPEID | nvarchar(1000) | Y |  |  |  |
| 12 | DOCSTATE | nvarchar(200) | Y |  |  |  |

### BACKPMTASK

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ICON | nvarchar(200) | Y |  |  |  |
| 17 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 18 | SERIALNUMBER | int | Y |  |  |  |
| 19 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 20 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 21 | TASKTYPE | nvarchar(100) |  | (N'PMNORMALTASKFORM') |  |  |
| 22 | PRINCIPAL | nvarchar(2000) | Y |  |  |  |
| 23 | PLANSTARTDATE | bigint | Y |  |  |  |
| 24 | PLANENDDATE | bigint | Y |  |  |  |
| 25 | ACTSTARTDATE | bigint | Y |  |  |  |
| 26 | ACTENDDATE | bigint | Y |  |  |  |
| 27 | PLANPERIOD | nvarchar(20) | Y | (N'1') |  |  |
| 28 | ACTPERIOD | float | Y |  |  |  |
| 29 | PLANTASKTIME | float | Y |  |  |  |
| 30 | ACTWORKTIME | float | Y |  |  |  |
| 31 | FORMID | nvarchar(100) | Y |  |  |  |
| 32 | TASKPERCENT | float |  | (N'0') |  |  |
| 33 | TASKCHECKER | nvarchar(2000) | Y |  |  |  |
| 34 | TASKSTATE | nvarchar(100) |  | (N'New') |  |  |
| 35 | PRIORITY | nvarchar(20) | Y |  |  |  |
| 36 | EXAMINE | nvarchar(1000) | Y |  |  |  |
| 37 | ISMILESTONE | bit | Y | (N'0') |  |  |
| 38 | ISCYCLETASK | bit | Y | (N'0') |  |  |
| 39 | ISLEAFTASK | nvarchar(100) | Y | (N'1') |  |  |
| 40 | HASSEND | bit | Y | (N'0') |  |  |
| 41 | ISAUTHORITYRESOLVE | bit | Y | (N'0') |  |  |
| 42 | LIMITTYPE | nvarchar(20) | Y |  |  |  |
| 43 | LIMITDATETIME | bigint | Y |  |  |  |
| 44 | VIEWUSERIDS | nvarchar(2000) | Y |  |  |  |
| 45 | ISTASKPUBLISH | int | Y | (N'0') |  |  |
| 46 | TASKPUBLISHDATE | bigint | Y |  |  |  |
| 47 | RECEIVEDATETIME | bigint | Y |  |  |  |
| 48 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 49 | PRETASKSTATE | nvarchar(100) | Y |  |  |  |
| 50 | PARENTTASKID | nvarchar(100) | Y |  |  |  |
| 51 | OUTLINENUM | nvarchar(2000) | Y |  |  |  |
| 52 | TASKLEVEL | int | Y |  |  |  |
| 53 | ISKEY | bit | Y | (N'0') |  |  |
| 54 | TASKPCTCOMPPLAN | float | Y | (N'0') |  |  |
| 55 | EARLIESTSTARTTIME | nvarchar(20) | Y |  |  |  |
| 56 | EARLISTENDTIME | nvarchar(20) | Y |  |  |  |
| 57 | LATESTARTDATE | nvarchar(20) | Y |  |  |  |
| 58 | LATEFINISHDATE | nvarchar(20) | Y |  |  |  |
| 59 | ISLOCK | bit | Y | (N'0') |  |  |
| 60 | CUREDITUSER | nvarchar(100) | Y |  |  |  |
| 61 | CHECKOUTDATE | bigint | Y |  |  |  |
| 62 | LASTCHECKOUTTIME | bigint | Y |  |  |  |
| 63 | FINISHPERCENT | nvarchar(100) | Y |  |  |  |
| 64 | WBSSTARTTIME | nvarchar(100) | Y |  |  |  |
| 65 | WBSENDTIME | nvarchar(100) | Y |  |  |  |
| 66 | PLANPERIODDAYS | float | Y | (N'1') |  |  |
| 67 | PRINCIPALATTACH | nvarchar(200) | Y |  |  |  |
| 68 | TASKMODE | nvarchar(100) | Y | (N'AUTO_SCHEDULED') |  |  |
| 69 | TASKREMARK | nvarchar(1000) | Y |  |  |  |
| 70 | WORKINGHOUR | nvarchar(100) | Y |  |  |  |
| 71 | BASEPLANSTARTDATE | bigint | Y |  |  |  |
| 72 | BASEPLANENDDATE | bigint | Y |  |  |  |
| 73 | BREAKTASKSTATUS | nvarchar(100) | Y |  |  |  |
| 74 | BREAKCHILDTASKSTATUS | nvarchar(100) | Y |  |  |  |
| 75 | PARENTPATH | nvarchar(max) | Y |  |  |  |
| 76 | PROJECTROLE | nvarchar(100) | Y |  |  |  |
| 77 | PLANOFFSET | float | Y |  |  |  |
| 78 | WORKHOURSPERDAY | nvarchar(100) | Y |  |  |  |
| 79 | TASKDOC | nvarchar(2000) | Y |  |  |  |
| 80 | MUSTHASDOC | bit | Y | (N'0') |  |  |
| 81 | TASKDOCIMAGE | nvarchar(100) | Y |  |  |  |
| 82 | ISMULTIRES | int | Y |  |  |  |
| 83 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 84 | IMPOWERUSER | nvarchar(2000) | Y |  |  |  |
| 85 | IMPOWERUSERNAME | nvarchar(100) | Y |  |  |  |
| 86 | IMPOWERTOUSER | nvarchar(100) | Y |  |  |  |
| 87 | IMPOWERTOUSERNAME | nvarchar(100) | Y |  |  |  |
| 88 | IMPOWERDATE | nvarchar(100) | Y |  |  |  |
| 89 | OLDTASKEID | nvarchar(100) | Y |  |  |  |
| 90 | OLDPARENTTASKID | nvarchar(100) | Y |  |  |  |
| 91 | INPUTDOC | nvarchar(2000) | Y |  |  |  |
| 92 | MSETASKTYPE | nvarchar(100) | Y |  |  |  |
| 93 | OEID | nvarchar(100) | Y |  |  |  |
| 94 | OCREATETIME | nvarchar(100) | Y |  |  |  |
| 95 | OUPDATETIME | nvarchar(100) | Y |  |  |  |
| 96 | CHANGECOL | nvarchar(max) | Y |  |  |  |
| 97 | ERPTASKID | nvarchar(100) | Y |  |  |  |

### BACKPMTASKOUTPUTRELATION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | WEIGHT | nvarchar(20) | Y | (N'0') |  |  |
| 16 | LINKTYPE | nvarchar(100) | Y |  |  |  |
| 17 | REMARK2 | nvarchar(2000) | Y |  |  |  |
| 18 | REMARK | nvarchar(100) | Y |  |  |  |
| 19 | LOGCREATETIME | bigint | Y |  |  |  |
| 20 | OEID | nvarchar(100) | Y |  |  |  |
| 21 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 22 | CHANGECOL | nvarchar(max) | Y |  |  |  |

### BACKPROJECT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 22 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 23 | RELEASEDATE | bigint | Y |  |  |  |
| 24 | PROJECTTYPE | nvarchar(100) |  |  |  |  |
| 25 | PROJECTMANAGER | nvarchar(100) |  |  |  |  |
| 26 | PROJECTLEVEL | nvarchar(100) |  |  |  |  |
| 27 | PLANSTARTDATE | nvarchar(100) |  |  |  |  |
| 28 | PLANENDDATE | nvarchar(100) |  |  |  |  |
| 29 | PLANSTATE | nvarchar(100) | Y | (N'Normal') |  |  |
| 30 | FORMID | nvarchar(100) | Y |  |  |  |
| 31 | EPS | nvarchar(100) |  |  |  |  |
| 32 | DEPARTMENT | nvarchar(100) |  |  |  |  |
| 33 | PLANSCHEDULE | int | Y |  |  |  |
| 34 | PERCENTCOMPLETE | int | Y |  |  |  |
| 35 | PROGRESSICON | nvarchar(200) | Y |  |  |  |
| 36 | PLANDEVIATIONRATE | int | Y |  |  |  |
| 37 | PLANPUBLISHSTATE | nvarchar(100) | Y | (N'No') |  |  |
| 38 | PUBLISHDATE | nvarchar(100) | Y |  |  |  |
| 39 | ACTSTARTDATE | nvarchar(100) | Y |  |  |  |
| 40 | ACTENDDATE | nvarchar(100) | Y |  |  |  |
| 41 | DELIVERYGENERATETYPE | nvarchar(100) |  | (N'WbsMode') |  |  |
| 42 | PROJECTTEMPLATE | nvarchar(100) | Y |  |  |  |
| 43 | PRESTAGESTATE | nvarchar(100) | Y |  |  |  |
| 44 | HASSAVEBYWBS | nvarchar(100) | Y |  |  |  |
| 45 | OEID | nvarchar(100) | Y |  |  |  |
| 46 | CHANGECOL | nvarchar(max) | Y |  |  |  |
| 47 | OCREATETIME | nvarchar(100) | Y |  |  |  |
| 48 | OUPDATETIME | nvarchar(100) | Y |  |  |  |
| 49 | ZUZHIERP | nvarchar(100) | Y | (N'31') |  |  |
| 50 | SHIYONGZUZHI | nvarchar(100) | Y |  |  |  |
| 51 | CHUANGJIANZUZHI | nvarchar(100) | Y |  |  |  |
| 52 | SQZZ | nvarchar(100) | Y |  |  |  |

### BACKPROJECTTASKTORESOURCE

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | RATE | nvarchar(100) | Y |  |  |  |
| 17 | ISMAIN | nvarchar(20) | Y |  |  |  |
| 18 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 19 | LOGCREATETIME | bigint | Y |  |  |  |
| 20 | OEID | nvarchar(100) | Y |  |  |  |
| 21 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 22 | CHANGECOL | nvarchar(max) | Y |  |  |  |

### BASELINEBOM

**行数** 1,176 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | LCREATETIME | bigint |  |  |  |  |
| 8 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 9 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 10 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 11 | PVICEID | nvarchar(100) | Y |  |  |  |
| 12 | CVICEID | nvarchar(100) | Y |  |  |  |
| 13 | VICELID | nvarchar(100) | Y |  |  |  |
| 14 | BASELINEEID | nvarchar(100) | Y |  |  |  |
| 15 | LPATH | nvarchar(1000) | Y |  |  |  |
| 16 | CVIEWID | nvarchar(100) | Y |  |  |  |
| 17 | VIEWTYPE | nvarchar(100) | Y |  |  |  |
| 18 | STARTVALIDITY | bigint | Y |  |  |  |
| 19 | ENDVALIDITY | nvarchar(50) | Y |  |  |  |

### BASELINEDOCATTACH

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |
| 9 | PVICEID | nvarchar(50) | Y |  |  |  |
| 10 | CVICEID | nvarchar(50) | Y |  |  |  |
| 11 | BASELINEID | nvarchar(50) | Y |  |  |  |

### BASELINEDOCRELATION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | DOCLINKTYPE | nvarchar(100) | Y |  |  |  |
| 16 | BASELINEEID | nvarchar(100) | Y |  |  |  |

### BASELINEOBJECT

**行数** 78 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 11 | SERIALNUMBER | int |  | (N'1') |  |  |
| 12 | ITEMID | nvarchar(100) | Y |  |  |  |
| 13 | ITEMEUID | nvarchar(100) | Y |  |  |  |
| 14 | ITEMTYPE | nvarchar(50) | Y |  |  |  |
| 15 | VALIDITYTYPE | nvarchar(100) | Y |  |  |  |
| 16 | VERSIONRULE | nvarchar(100) | Y | (N'Lastest') |  |  |
| 17 | VALIDITYITEMECODE | nvarchar(100) | Y |  |  |  |
| 18 | QUERY | nvarchar(100) | Y |  |  |  |
| 19 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 20 | ISFROMWORKFLOW | nvarchar(20) | Y | (N'False') |  |  |
| 21 | VIEWTYPE | nvarchar(20) | Y |  |  |  |
| 22 | VALIDITYCALCULATIONRULE | nvarchar(100) | Y |  |  |  |
| 23 | VERSIONNAME | nvarchar(100) | Y |  |  |  |
| 24 | ITEMNAME | nvarchar(100) | Y |  |  |  |
| 25 | ITEMCODE | nvarchar(100) | Y |  |  |  |
| 26 | DOCRELATIONTYPE | nvarchar(100) | Y |  |  |  |
| 27 | DOCTYPEID | nvarchar(1000) | Y |  |  |  |
| 28 | DOCSTATE | nvarchar(100) | Y |  |  |  |
| 29 | VICEEID | nvarchar(100) | Y |  |  |  |
| 30 | ITERATION | nvarchar(100) | Y |  |  |  |
| 31 | VALIDITYRANGE | nvarchar(100) | Y |  |  |  |
| 32 | WORKFLOWINFOID | nvarchar(50) | Y |  |  |  |
| 33 | STARTVALIDITY | bigint | Y |  |  |  |
| 34 | ENDVALIDITY | nvarchar(50) | Y |  |  |  |
| 35 | ROOTITEMVIEWTYPE | nvarchar(20) | Y |  |  |  |

### BASELINERELATION

**行数** 248 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | LCREATETIME | bigint |  |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | CVICEID | nvarchar(100) | Y |  |  |  |
| 14 | BASELINEEID | nvarchar(100) | Y |  |  |  |
| 15 | RELATIONTYPE | nvarchar(100) | Y |  |  |  |
| 16 | MAINREFERTYPE | nvarchar(100) | Y |  |  |  |

### BOMCODELINK

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | PEID | nvarchar(100) | Y |  |  |  |
| 16 | DEPLOYCODEEID | nvarchar(100) | Y |  |  |  |
| 17 | CEID | nvarchar(100) | Y |  |  |  |
| 18 | CODETYPE | nvarchar(100) | Y |  |  |  |
| 19 | VALUETYPE | nvarchar(100) | Y |  |  |  |
| 20 | SORTNO | int | Y |  |  |  |

### BROADCAST

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | RELEASESTATUS | nvarchar(100) | Y |  |  |  |
| 9 | RELEASETIME | bigint | Y |  |  |  |
| 10 | BROADCASTCONTENT | nvarchar(1000) | Y |  |  |  |

### BROADCASTLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### CACHEINFO

**行数** 17 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | EVERSION | int |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | PARTIALREFERSH | bit | Y | (N'0') |  |  |

### CACHETEMPINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | IDDATA | ntext | Y |  |  |  |

### CAPPPROCESSCONFIGLINK

**行数** 1 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | VERSIONEID | nvarchar(100) | Y |  |  |  |

### CAPPRESOURCEATTRIBUTE

**行数** 30 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ICON | nvarchar(200) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 16 | DATATYPE | nvarchar(100) |  | (N'String200') |  |  |
| 17 | UNIT | nvarchar(100) | Y |  |  |  |
| 18 | ISSYSATTR | nvarchar(100) |  | (N'0') |  |  |
| 19 | UNIQUENO | int | Y | (N'0') |  |  |
| 20 | LOV | nvarchar(100) | Y |  |  |  |

### CAPPRESOURCEATTRIBUTEVAL

**行数** 640 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | CLASSID | nvarchar(100) | Y |  |  |  |
| 7 | RECORDID | nvarchar(100) | Y |  |  |  |
| 8 | ATTRIBUTEID | nvarchar(100) | Y |  |  |  |
| 9 | ATTRIBUTEVAL | nvarchar(1000) | Y |  |  |  |

### CAPPRESOURCEATTRIBUTEVAL2

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | CLASSID | nvarchar(100) | Y |  |  |  |
| 7 | RECORDID | nvarchar(100) | Y |  |  |  |
| 8 | ATTRIBUTEID | nvarchar(100) | Y |  |  |  |
| 9 | ATTRIBUTEVAL | nvarchar(1000) | Y |  |  |  |
| 10 | CAPPPROCESSOBJECTID | nvarchar(100) | Y |  |  |  |

### CAPPRESOURCECLASS

**行数** 7 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ICON | nvarchar(200) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 16 | CLASSTYPE | nvarchar(100) | Y | (N'StoredClass') |  |  |
| 17 | ISTOOLING | nvarchar(100) | Y | (N'0') |  |  |
| 18 | PERGROUP | nvarchar(100) | Y |  |  |  |

### CAPPRESOURCELINK

**行数** 172 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | ISREQUIRE | bit | Y | (N'0') |  |  |
| 16 | ISKEY | bit | Y | (N'0') |  |  |
| 17 | VIEWSTATE | bit | Y | (N'1') |  |  |

### CAPPRESOURCEQUERYCOND

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | QUERYID | nvarchar(100) | Y |  |  |  |
| 16 | QUERYKEY | nvarchar(200) | Y |  |  |  |
| 17 | QUERYVALUE | nvarchar(200) | Y |  |  |  |
| 18 | QUERYSORT | int | Y |  |  |  |

### CAPPRESOURCEQUERYHISTORY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |

### CAPPRESOURCERECORD

**行数** 135 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | OWNUID | nvarchar(50) | Y |  |  |  |
| 7 | OWNGID | nvarchar(50) | Y |  |  |  |
| 8 | ESTATION | nvarchar(100) | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 12 | ENAME | nvarchar(2000) | Y |  |  |  |
| 13 | ECODE | nvarchar(2000) | Y |  |  |  |
| 14 | RESOURCEID | nvarchar(100) | Y |  |  |  |

### CAPPRESOURCERELATION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |

### CIS

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | CISTYPE | nvarchar(100) |  |  |  |  |
| 7 | TYPE | nvarchar(100) | Y |  |  |  |
| 8 | ITEMCODE | nvarchar(100) | Y |  |  |  |
| 9 | ITEMNAME | nvarchar(200) | Y |  |  |  |
| 10 | INSTALLSTYLE | nvarchar(100) | Y |  |  |  |
| 11 | MANUFACTURER | nvarchar(100) | Y |  |  |  |
| 12 | MANUFACTURERPARTNUMBER | nvarchar(100) | Y |  |  |  |
| 13 | SPEC | nvarchar(100) |  |  |  |  |
| 14 | VALUE | nvarchar(100) | Y |  |  |  |
| 15 | DOMESTIC | nvarchar(100) | Y |  |  |  |
| 16 | QUALITYLEVEL | nvarchar(100) | Y |  |  |  |
| 17 | UNITS | nvarchar(100) | Y |  |  |  |
| 18 | WORKRANGE | nvarchar(100) | Y |  |  |  |
| 19 | TEMPERATURE | nvarchar(100) | Y |  |  |  |
| 20 | ESDS | nvarchar(100) | Y |  |  |  |
| 21 | SCHEMATIC | nvarchar(100) | Y |  |  |  |
| 22 | PCB | nvarchar(100) |  |  |  |  |
| 23 | DATASHEET | nvarchar(100) | Y |  |  |  |
| 24 | OPTIMIZATION | nvarchar(100) | Y |  |  |  |
| 25 | PERFORMANCE | nvarchar(200) | Y |  |  |  |
| 26 | STATUS | nvarchar(100) | Y | (N'New') |  |  |
| 27 | ITEMID | nvarchar(100) | Y |  |  |  |

### CLASSIFYATTRIBUTE

**行数** 120 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 16 | DATATYPE | nvarchar(50) |  | (N'String200') |  |  |
| 17 | UNIT | nvarchar(100) | Y |  |  |  |
| 18 | DEFAULTVALUE | nvarchar(200) | Y |  |  |  |
| 19 | CLASSATTRMAXVALUE | nvarchar(200) | Y |  |  |  |
| 20 | MINVALUE | nvarchar(200) | Y |  |  |  |
| 21 | LOV | nvarchar(100) | Y |  |  |  |
| 22 | ISEMPTY | bit | Y | (N'0') |  |  |
| 23 | CLASSATTRNO | int | Y |  |  |  |

### CLASSIFYATTRIBUTEDATA

**行数** 57,235 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ATTRIBUTEID | nvarchar(100) | Y |  |  |  |
| 7 | CLASSIFYID | nvarchar(100) | Y |  |  |  |
| 8 | OBJECTID | nvarchar(100) | Y |  |  |  |
| 9 | ATTRIBUTEVALUE | nvarchar(max) | Y |  |  |  |

### CLASSIFYCLASS

**行数** 264 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ICON | nvarchar(200) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | CLASSTYPE | nvarchar(100) | Y | (N'StoredClass') |  |  |
| 16 | CLASSOBJECTTYPE | nvarchar(100) | Y | (N'PSEITEM') |  |  |
| 17 | PERGROUP | nvarchar(100) | Y |  |  |  |
| 18 | OBJECTFORMTYPE | nvarchar(200) | Y |  |  |  |
| 19 | CLASSCODEPATH | nvarchar(2000) | Y |  |  |  |

### CLASSIFYENUMERATIONITEM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SEQUENCE | int |  |  |  |  |
| 10 | ATTRIBUTEEID | nvarchar(100) |  |  |  |  |
| 11 | CLASSIFYEID | nvarchar(100) |  |  |  |  |
| 12 | DESCRIPTION | nvarchar(100) | Y |  |  |  |
| 13 | SUITGROUP | varchar(4000) | Y |  |  |  |
| 14 | PEID | nvarchar(100) |  |  |  |  |

### CLASSIFYGROUP

**行数** 41 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ICON | nvarchar(200) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | REFERENCECODE | nvarchar(100) | Y |  |  |  |

### CLASSIFYLINK

**行数** 305 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### CLASSIFYQUERYHISTORY

**行数** 2 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |

### CLASSIFYQUERYHISTORYCONDITION

**行数** 2 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | QUERYID | nvarchar(100) | Y |  |  |  |
| 7 | COLUMNNAME | nvarchar(200) | Y |  |  |  |
| 8 | COLUMNVALUE | nvarchar(200) | Y |  |  |  |
| 9 | SORT | int | Y |  |  |  |

### CLASSIFYRELATION

**行数** 17,648 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | HASOWNLOV | int | Y |  |  |  |
| 17 | OWNLOV | nvarchar(100) | Y |  |  |  |

### CLASSIFYSERIALNUM

**行数** 3 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ECODE | nvarchar(100) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ESTATION | nvarchar(100) | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | LEVELID | int | Y |  |  |  |
| 9 | SERIALNUM | int | Y |  |  |  |

### CLASSIFYVIEW

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ICON | nvarchar(200) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | THUMBNAIL | nvarchar(500) | Y |  |  |  |

### CMAPPLY

**行数** 3 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 15 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 16 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 17 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 18 | POSITION | nvarchar(100) |  |  |  |  |
| 19 | RECEIVEUSER | nvarchar(2000) | Y |  |  |  |
| 20 | REASON | nvarchar(500) | Y |  |  |  |
| 21 | FEASIBILITY | nvarchar(500) | Y |  |  |  |
| 22 | TYPE | nvarchar(100) |  |  |  |  |
| 23 | FORMID | nvarchar(100) | Y |  |  |  |
| 24 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 25 | CLASSTYPE | nvarchar(100) |  |  |  |  |
| 26 | PRODUCT | nvarchar(100) | Y |  |  |  |
| 27 | XMDH | nvarchar(100) | Y |  |  |  |
| 28 | GGCS | nvarchar(100) | Y |  |  |  |

### CMAPPLYBASEFORM

**行数** 8 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OBJECTID | nvarchar(100) | Y |  |  |  |

### CMNOTICE

**行数** 11 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 15 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 16 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 17 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 18 | POSITION | nvarchar(100) |  |  |  |  |
| 19 | NOTICEUSER | nvarchar(2000) | Y |  |  |  |
| 20 | REASON | nvarchar(500) | Y |  |  |  |
| 21 | CONTENT | nvarchar(500) | Y |  |  |  |
| 22 | TYPE | nvarchar(100) |  |  |  |  |
| 23 | FORMID | nvarchar(100) | Y |  |  |  |
| 24 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 25 | CLASSTYPE | nvarchar(100) |  |  |  |  |

### CMNOTICEBASEFORM

**行数** 30 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OBJECTID | nvarchar(100) | Y |  |  |  |

### CMPROBLEM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 15 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 16 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 17 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 18 | POSITION | nvarchar(100) |  |  |  |  |
| 19 | RECEIVEUSER | nvarchar(2000) | Y |  |  |  |
| 20 | TYPE | nvarchar(100) | Y | (N'CMPROBLEM') |  |  |
| 21 | CLASSTYPE | nvarchar(100) |  |  |  |  |
| 22 | FORMID | nvarchar(100) | Y |  |  |  |
| 23 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |

### CMPROBLEMVERIFY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | STATECHANGE | bit | Y |  |  |  |
| 8 | REASON1 | nvarchar(100) | Y |  |  |  |
| 9 | ISMATCHRELATION | bit | Y |  |  |  |
| 10 | REASON2 | nvarchar(100) | Y |  |  |  |
| 11 | ISEFFECTSKIN | bit | Y |  |  |  |
| 12 | REASON3 | nvarchar(100) | Y |  |  |  |
| 13 | ISEFFECTASSIGN | bit | Y |  |  |  |
| 14 | REASON4 | nvarchar(100) | Y |  |  |  |
| 15 | ISOTSACCEPT | bit | Y |  |  |  |
| 16 | REASON5 | nvarchar(100) | Y |  |  |  |

### CMRELATION

**行数** 82 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | LINKTYPE | nvarchar(100) | Y |  |  |  |

### CNCPARA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | XZXC | nvarchar(100) | Y |  |  |  |
| 16 | GZTCC | nvarchar(100) | Y |  |  |  |
| 17 | YZXC | nvarchar(100) | Y |  |  |  |
| 18 | ZXKCC | nvarchar(100) | Y |  |  |  |
| 19 | ZZXC | nvarchar(100) | Y |  |  |  |
| 20 | TXCSL | nvarchar(100) | Y |  |  |  |

### CODEKIND

**行数** 17 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y | (N'New') |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 12 | SERIALNUMBER | int | Y |  |  |  |
| 13 | DESCRIPTION | nvarchar(500) | Y |  |  |  |

### CODEKINDTORULE

**行数** 58 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### CODEPERMISSION

**行数** 78 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OBJECTID | nvarchar(100) | Y |  |  |  |
| 7 | PERMISSIONACTORTYPE | nvarchar(100) | Y |  |  |  |
| 8 | PERMISSIONACTORVALUE | ntext | Y |  |  |  |
| 9 | PERMISSIONEXPIREVALUE | bigint | Y |  |  |  |

### CODERES

**行数** 60,300 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | STATEID | nvarchar(20) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |
| 10 | CODES | nvarchar(500) | Y |  |  |  |
| 11 | ORDINAL | nvarchar(100) | Y |  |  |  |
| 12 | APPLYER | nvarchar(100) | Y |  |  |  |
| 13 | APPLYDATE | nvarchar(100) | Y |  |  |  |
| 14 | OBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 15 | OBJECTID | nvarchar(100) | Y |  |  |  |
| 16 | CODEKINDID | nvarchar(100) | Y |  |  |  |
| 17 | GENERATORPATH | nvarchar(500) | Y |  |  |  |
| 18 | PROPERTY | nvarchar(100) | Y |  |  |  |

### CODERULE

**行数** 58 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | STATEID | nvarchar(20) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |
| 10 | DESCRIPTION | nvarchar(200) | Y |  |  |  |
| 11 | LENGTH | nvarchar(100) |  |  |  |  |
| 12 | MARKCHAR | nvarchar(100) | Y |  |  |  |
| 13 | ORDERNUM | nvarchar(100) | Y |  |  |  |
| 14 | THICKRULE | nvarchar(100) |  |  |  |  |
| 15 | THINRULE | nvarchar(100) |  |  |  |  |
| 16 | PARENTCODERULE | nvarchar(100) | Y |  |  |  |
| 17 | CODEDROPMODE | nvarchar(100) | Y | (N'DropDown') |  |  |

### CODERULETOVALUE

**行数** 1,170 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### CODEVALUE

**行数** 1,170 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | CODEVALUE | nvarchar(100) | Y |  |  |  |
| 10 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 11 | KINDID | nvarchar(100) | Y |  |  |  |
| 12 | RULEID | nvarchar(100) | Y |  |  |  |
| 13 | SEGMENTID | nvarchar(100) | Y |  |  |  |
| 14 | PARENTVALUE | nvarchar(100) | Y |  |  |  |
| 15 | PVALUEID | nvarchar(100) | Y |  |  |  |
| 16 | INITIALCODE | nvarchar(100) | Y |  |  |  |

### CODEVALUEPERMISSION

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | CODEVALUEPERMISSION | nvarchar(1000) | Y |  |  |  |
| 7 | CODEVALUEID | nvarchar(100) | Y |  |  |  |
| 8 | CODEVALUEPERMISSIONTEXT | nvarchar(1000) | Y |  |  |  |

### CODEVALUETORES

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### CODEVALUETOVALUE

**行数** 260 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### COMMANDCLUSTER

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | CLUSTERNAME | nvarchar(100) |  |  |  |  |

### COMMANDSERVICELOCK

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ISLOCK | bit | Y |  |  |  |

### COMMANDSERVICESETTING

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | CONFIGINFO | ntext | Y |  |  |  |
| 7 | ISSYNC | bit | Y |  |  |  |

### COMMONAPPLY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 16 | POSITION | nvarchar(100) |  |  |  |  |
| 17 | APPLYTYPE | nvarchar(100) |  | (N'TOOLINGAPPLYFORM') |  |  |
| 18 | APPLYREASON | nvarchar(500) | Y |  |  |  |
| 19 | FEASIBILITY | nvarchar(500) | Y |  |  |  |
| 20 | RECEIVEUSER | nvarchar(100) | Y |  |  |  |
| 21 | FORMID | nvarchar(100) | Y |  |  |  |

### COMMONAPPLYBASEFORM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | EXTENDATT1 | nvarchar(100) | Y |  |  |  |
| 8 | EXTENDATT2 | nvarchar(100) | Y |  |  |  |
| 9 | OBJECTID | nvarchar(100) | Y |  |  |  |

### COMPANY

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |
| 10 | ADDRESS | nvarchar(500) | Y |  |  |  |
| 11 | DESCRIPT | nvarchar(500) | Y |  |  |  |
| 12 | SYSTEMUSERID | nvarchar(100) | Y |  |  |  |

### COMPANYRULE

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int |  | (N'0') |  |  |
| 8 | PEID | nvarchar(100) | Y |  |  |  |
| 9 | USEBY | nvarchar(100) | Y |  |  |  |
| 10 | CREATEPERMISSIONOBJECTTYPE | nvarchar(100) | Y |  |  |  |

### COMPANYRULECONDITION

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int |  |  |  |  |
| 6 | ACTIONRULEID | nvarchar(100) |  |  |  |  |
| 7 | OPERATOR | nvarchar(100) |  |  |  |  |
| 8 | CONDITIONVALUE | nvarchar(500) |  |  |  |  |

### COMPANYRULEOBJECT

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 7 | COMPANYRULEID | nvarchar(100) | Y |  |  |  |

### COMPANYRULEPERMISSION

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int |  |  |  |  |
| 6 | ACTIONRULEID | nvarchar(100) |  |  |  |  |
| 7 | PERMISSIONACTORVALUE | ntext |  |  |  |  |
| 8 | PERMISSIONACTIONVALUE | ntext |  |  |  |  |

### CONFIGCONDITION

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | LEFTBRACKET | nvarchar(20) | Y |  |  |  |
| 8 | DEPLOYCODEEID | nvarchar(450) |  |  |  |  |
| 9 | OPERATOR | nvarchar(20) | Y | (N'=') |  |  |
| 10 | VALUE | nvarchar(50) |  |  |  |  |
| 11 | LOGICOPERATOR | nvarchar(20) | Y | (N'Or') |  |  |
| 12 | BOMLINEID | nvarchar(100) | Y |  |  |  |
| 13 | SORTNO | int | Y |  |  |  |
| 14 | RIGHTBRACKET | nvarchar(20) | Y |  |  |  |
| 15 | VALUESTATE | nvarchar(20) | Y |  |  |  |
| 16 | VALUETYPE | nvarchar(20) | Y |  |  |  |

### CONVERSIONCOMMAND

**行数** 4 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | COMMAND | nvarchar(100) | Y |  |  |  |
| 7 | OWNCONVERSIONENDPOINT | nvarchar(100) | Y |  |  |  |
| 8 | OWNCLUSTER | nvarchar(100) | Y |  |  |  |

### CONVERSIONENDPOINT

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | NAME | nvarchar(100) |  |  |  |  |
| 7 | DISPLAYNAME | nvarchar(100) |  |  |  |  |
| 8 | CONVERSIONIP | nvarchar(100) |  |  |  |  |
| 9 | CONVERSIONPORT | int |  |  |  |  |
| 10 | BINPATH | nvarchar(200) |  | (N'%CONVERSIONSERVER_HOME%/../FileConversionService/Bin') |  |  |
| 11 | COMMANDWAITSECOND | int |  | (N'600') |  |  |
| 12 | COMMANDFAILCOUNT | int |  | (N'3') |  |  |
| 13 | ISRESERVED | bit | Y |  |  |  |
| 14 | OWNCLUSTER | nvarchar(100) | Y |  |  |  |
| 15 | EXTFILTERGROUP | nvarchar(100) | Y |  |  |  |
| 16 | FILESERVERIP | nvarchar(100) | Y |  |  |  |

### COSTACT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 16 | PROJECTEUID | nvarchar(200) | Y |  |  |  |
| 17 | COSTEUID | nvarchar(100) | Y |  |  |  |
| 18 | COSTDATE | bigint |  |  |  |  |
| 19 | SPACECODE | nvarchar(100) | Y |  |  |  |
| 20 | COSTUSER | nvarchar(100) | Y |  |  |  |
| 21 | COSTVALUE | nvarchar(100) | Y |  |  |  |
| 22 | APPROVEREECORD | nvarchar(500) | Y |  |  |  |
| 23 | COSTEXPLAIN | nvarchar(100) | Y |  |  |  |
| 24 | AUDITUSER | nvarchar(max) | Y |  |  |  |
| 25 | AUDITTIME | bigint | Y |  |  |  |
| 26 | PHASEID | nvarchar(200) | Y |  |  |  |
| 27 | TASKEUID | nvarchar(100) | Y |  |  |  |
| 28 | SUBJECTEUID | nvarchar(100) | Y |  |  |  |

### COSTACTBASELINE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 16 | PROJECTEUID | nvarchar(200) | Y |  |  |  |
| 17 | COSTEUID | nvarchar(100) | Y |  |  |  |
| 18 | COSTDATE | bigint |  |  |  |  |
| 19 | SPACECODE | nvarchar(100) | Y |  |  |  |
| 20 | COSTUSER | nvarchar(100) | Y |  |  |  |
| 21 | COSTVALUE | nvarchar(100) | Y |  |  |  |
| 22 | APPROVEREECORD | nvarchar(500) | Y |  |  |  |
| 23 | COSTEXPLAIN | nvarchar(100) | Y |  |  |  |
| 24 | AUDITUSER | nvarchar(max) | Y |  |  |  |
| 25 | AUDITTIME | bigint | Y |  |  |  |
| 26 | PHASEID | nvarchar(200) | Y |  |  |  |
| 27 | TASKEUID | nvarchar(100) | Y |  |  |  |
| 28 | SUBJECTEUID | nvarchar(100) | Y |  |  |  |
| 29 | COSTBASELINEINFOID | nvarchar(100) | Y |  |  |  |

### COSTACTUSERLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### COSTAUDIT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OPERATION | nvarchar(100) | Y |  |  |  |
| 7 | RESULT | nvarchar(100) | Y |  |  |  |
| 8 | OPINION | nvarchar(500) | Y |  |  |  |
| 9 | OWNCOSTACT | nvarchar(100) | Y |  |  |  |

### COSTBASELINE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | CREATEWAY | nvarchar(100) | Y | (N'Auto') |  |  |
| 16 | COSTBASELINETYPE | nvarchar(100) | Y | (N'CostMake') |  |  |
| 17 | OWNCOSTEUID | nvarchar(100) | Y |  |  |  |
| 18 | OWNCOSTEID | nvarchar(100) | Y |  |  |  |
| 19 | COSTBASE | nvarchar(100) | Y |  |  |  |
| 20 | COSTEXTENT | nvarchar(100) | Y |  |  |  |
| 21 | OWNPROJECTEUID | nvarchar(100) | Y |  |  |  |

### COSTBASELINEDETAIL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EUID | nvarchar(100) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 16 | SUBJECTID | nvarchar(100) | Y |  |  |  |
| 17 | COLTYPE | nvarchar(100) | Y |  |  |  |
| 18 | COLPHASEAME | nvarchar(100) | Y |  |  |  |
| 19 | COLORDERNO | nvarchar(100) | Y |  |  |  |
| 20 | SPACEID | nvarchar(100) | Y |  |  |  |
| 21 | SPACECODE | nvarchar(100) | Y |  |  |  |
| 22 | SPACENAME | nvarchar(250) | Y |  |  |  |
| 23 | COSTAMOUNT | nvarchar(100) | Y |  |  |  |
| 24 | ADJUSTVALUE | nvarchar(100) | Y |  |  |  |
| 25 | ADJUSTEDAMOUNT | nvarchar(100) | Y |  |  |  |
| 26 | SPACESTARTDATE | bigint | Y |  |  |  |
| 27 | SPACEENDDATE | bigint | Y |  |  |  |
| 28 | COSTID | nvarchar(100) | Y |  |  |  |
| 29 | PROJECTEUID | nvarchar(100) | Y |  |  |  |

### COSTBASELINEINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y | (N'New') |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 19 | SERIALNUMBER | int | Y |  |  |  |
| 20 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 21 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 22 | RELEASEDATE | bigint | Y |  |  |  |
| 23 | COSTSTARTDATE | bigint | Y |  |  |  |
| 24 | COSTENDDATE | bigint | Y |  |  |  |
| 25 | COSTMODE | nvarchar(100) | Y | (N'Total') |  |  |
| 26 | PERIODTYPE | nvarchar(100) | Y | (N'Month') |  |  |
| 27 | PROJECTEUID | nvarchar(100) | Y |  |  |  |
| 28 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 29 | CHECKOUTSTATE | nvarchar(100) | Y |  |  |  |
| 30 | CHECKOUTDATE | bigint | Y |  |  |  |
| 31 | COSTSTATE | nvarchar(100) | Y | (N'New') |  |  |
| 32 | PROJECTCODE | nvarchar(100) | Y |  |  |  |
| 33 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 34 | OWNPROJECTEUID | nvarchar(100) | Y |  |  |  |
| 35 | FLOWAUDITRESULT | nvarchar(20) | Y | (N'1') |  |  |
| 36 | COSTBASELINEID | nvarchar(100) | Y |  |  |  |

### COSTBASELINESPACE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | SEQUENCENO | int | Y |  |  |  |
| 16 | COSTID | nvarchar(100) | Y |  |  |  |
| 17 | COSTMODE | nvarchar(100) | Y |  |  |  |
| 18 | ISPUBLISHED | bit | Y |  |  |  |
| 19 | PROJECTEUID | nvarchar(100) | Y |  |  |  |
| 20 | PROJECTID | nvarchar(100) | Y |  |  |  |

### COSTBASELINESUBJECT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 19 | SERIALNUMBER | int | Y |  |  |  |
| 20 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 21 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 22 | RELEASEDATE | bigint | Y |  |  |  |
| 23 | SUBJECTID | nvarchar(100) | Y |  |  |  |
| 24 | ISPUBLISHED | bit | Y |  |  |  |
| 25 | COSTID | nvarchar(100) | Y |  |  |  |
| 26 | SUBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 27 | PARENTEID | nvarchar(100) | Y |  |  |  |
| 28 | OWNROOTEID | nvarchar(100) | Y |  |  |  |
| 29 | SUBJECTLEVEL | int | Y |  |  |  |
| 30 | ISLEAF | bit | Y |  |  |  |
| 31 | PROJECTEUID | nvarchar(100) | Y |  |  |  |

### COSTBASELINESUBJECTLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | COSTID | nvarchar(100) | Y |  |  |  |
| 16 | PROJECTEUID | nvarchar(100) | Y |  |  |  |

### COSTCHANGE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 16 | RELEASEDATE | bigint | Y |  |  |  |
| 17 | CHANGEREASON | nvarchar(100) | Y |  |  |  |
| 18 | CHANGECONTENT | nvarchar(500) | Y |  |  |  |
| 19 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 20 | PROJECTEUID | nvarchar(100) | Y |  |  |  |
| 21 | COSTID | nvarchar(100) | Y |  |  |  |
| 22 | COSTEUID | nvarchar(100) | Y |  |  |  |
| 23 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 24 | CHANGETYPE | nvarchar(100) | Y |  |  |  |
| 25 | CHANGEFORMID | nvarchar(100) | Y |  |  |  |
| 26 | WORKFLOWRESULT | nvarchar(100) | Y | (N'2') |  |  |

### COSTCHANGEAPPLY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 16 | RELEASEDATE | bigint | Y |  |  |  |
| 17 | CHANGEREASON | nvarchar(100) | Y |  |  |  |
| 18 | CHANGECONTENT | nvarchar(500) | Y |  |  |  |
| 19 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 20 | PROJECTEUID | nvarchar(100) | Y |  |  |  |
| 21 | COSTID | nvarchar(100) | Y |  |  |  |
| 22 | COSTEUID | nvarchar(100) | Y |  |  |  |
| 23 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 24 | CHANGETYPE | nvarchar(100) | Y |  |  |  |
| 25 | CHANGEFORMID | nvarchar(100) | Y |  |  |  |
| 26 | WORKFLOWRESULT | nvarchar(100) | Y | (N'2') |  |  |

### COSTCHANGEAPPLYFORM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | FORMTYPE | nvarchar(100) | Y |  |  |  |
| 16 | CHANGEID | nvarchar(100) | Y |  |  |  |
| 17 | ADJUSTEDAMOUNT | nvarchar(100) | Y |  |  |  |
| 18 | EXTENDPROPERTYA | nvarchar(100) | Y |  |  |  |
| 19 | PROJECTEUID | nvarchar(100) | Y |  |  |  |
| 20 | COSTEUID | nvarchar(100) | Y |  |  |  |
| 21 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 22 | COSTID | nvarchar(100) | Y |  |  |  |

### COSTCHANGEFORM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | FORMTYPE | nvarchar(100) | Y |  |  |  |
| 16 | CHANGEID | nvarchar(100) | Y |  |  |  |
| 17 | ADJUSTEDAMOUNT | nvarchar(100) | Y |  |  |  |
| 18 | EXTENDPROPERTYA | nvarchar(100) | Y |  |  |  |
| 19 | PROJECTEUID | nvarchar(100) | Y |  |  |  |
| 20 | COSTEUID | nvarchar(100) | Y |  |  |  |
| 21 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 22 | COSTID | nvarchar(100) | Y |  |  |  |

### COSTDETAIL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EUID | nvarchar(100) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 16 | SUBJECTID | nvarchar(100) | Y |  |  |  |
| 17 | COLTYPE | nvarchar(100) | Y |  |  |  |
| 18 | COLPHASEAME | nvarchar(100) | Y |  |  |  |
| 19 | COLORDERNO | nvarchar(100) | Y |  |  |  |
| 20 | SPACEID | nvarchar(100) | Y |  |  |  |
| 21 | SPACECODE | nvarchar(100) | Y |  |  |  |
| 22 | SPACENAME | nvarchar(250) | Y |  |  |  |
| 23 | COSTAMOUNT | nvarchar(100) | Y |  |  |  |
| 24 | ADJUSTVALUE | nvarchar(100) | Y |  |  |  |
| 25 | ADJUSTEDAMOUNT | nvarchar(100) | Y |  |  |  |
| 26 | SPACESTARTDATE | bigint | Y |  |  |  |
| 27 | SPACEENDDATE | bigint | Y |  |  |  |
| 28 | COSTID | nvarchar(100) | Y |  |  |  |
| 29 | PROJECTEUID | nvarchar(100) | Y |  |  |  |

### COSTDIC

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | SUBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 16 | PARENTEID | nvarchar(100) | Y |  |  |  |
| 17 | OWNROOTEID | nvarchar(100) | Y |  |  |  |
| 18 | SUBJECTLEVEL | int | Y |  |  |  |
| 19 | ISLEAF | bit | Y |  |  |  |
| 20 | HAVEOPENCHILD | nvarchar(100) | Y |  |  |  |

### COSTDICI

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |

### COSTDICLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### COSTINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y | (N'New') |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 19 | SERIALNUMBER | int | Y |  |  |  |
| 20 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 21 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 22 | RELEASEDATE | bigint | Y |  |  |  |
| 23 | COSTSTARTDATE | bigint | Y |  |  |  |
| 24 | COSTENDDATE | bigint | Y |  |  |  |
| 25 | COSTMODE | nvarchar(100) | Y | (N'Total') |  |  |
| 26 | PERIODTYPE | nvarchar(100) | Y | (N'Month') |  |  |
| 27 | PROJECTEUID | nvarchar(100) | Y |  |  |  |
| 28 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 29 | CHECKOUTSTATE | nvarchar(100) | Y |  |  |  |
| 30 | CHECKOUTDATE | bigint | Y |  |  |  |
| 31 | COSTSTATE | nvarchar(100) | Y | (N'New') |  |  |
| 32 | PROJECTCODE | nvarchar(100) | Y |  |  |  |
| 33 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 34 | OWNPROJECTEUID | nvarchar(100) | Y |  |  |  |
| 35 | FLOWAUDITRESULT | nvarchar(20) | Y | (N'1') |  |  |

### COSTSPACE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | SEQUENCENO | int | Y |  |  |  |
| 16 | COSTID | nvarchar(100) | Y |  |  |  |
| 17 | COSTMODE | nvarchar(100) | Y |  |  |  |
| 18 | ISPUBLISHED | bit | Y |  |  |  |
| 19 | PROJECTEUID | nvarchar(100) | Y |  |  |  |
| 20 | PROJECTID | nvarchar(100) | Y |  |  |  |

### COSTSPACETEMPLATE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | SEQUENCENO | int | Y |  |  |  |
| 16 | COSTID | nvarchar(100) | Y |  |  |  |
| 17 | COSTMODE | nvarchar(100) | Y |  |  |  |
| 18 | ISPUBLISHED | bit | Y |  |  |  |
| 19 | PROJECTEUID | nvarchar(100) | Y |  |  |  |
| 20 | PROJECTID | nvarchar(100) | Y |  |  |  |

### COSTSUBJECT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y | (N'New') |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 19 | SERIALNUMBER | int | Y |  |  |  |
| 20 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 21 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 22 | RELEASEDATE | bigint | Y |  |  |  |
| 23 | SUBJECTID | nvarchar(100) | Y |  |  |  |
| 24 | ISPUBLISHED | bit | Y |  |  |  |
| 25 | COSTID | nvarchar(100) | Y |  |  |  |
| 26 | SUBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 27 | PARENTEID | nvarchar(100) | Y |  |  |  |
| 28 | OWNROOTEID | nvarchar(100) | Y |  |  |  |
| 29 | SUBJECTLEVEL | int | Y |  |  |  |
| 30 | ISLEAF | bit | Y |  |  |  |
| 31 | PROJECTEUID | nvarchar(100) | Y |  |  |  |

### COSTSUBJECTSLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | COSTID | nvarchar(100) | Y |  |  |  |
| 16 | PROJECTEUID | nvarchar(100) | Y |  |  |  |

### COSTSUBJECTTEMPLATELINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | COSTTEMPLATEID | nvarchar(100) | Y |  |  |  |
| 16 | SERIALNUMBER | int | Y |  |  |  |
| 17 | PROJECTTEMPLATEID | nvarchar(100) | Y |  |  |  |

### COSTTEMPLATEINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y | (N'New') |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 16 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 17 | COSTSTARTDATE | bigint | Y |  |  |  |
| 18 | COSTENDDATE | bigint | Y |  |  |  |
| 19 | COSTMODE | nvarchar(100) | Y | (N'Total') |  |  |
| 20 | PERIODTYPE | nvarchar(100) | Y | (N'Month') |  |  |
| 21 | PROJECTEUID | nvarchar(100) | Y |  |  |  |
| 22 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 23 | CHECKOUTSTATE | nvarchar(100) | Y |  |  |  |
| 24 | CHECKOUTDATE | bigint | Y |  |  |  |
| 25 | COSTSTATE | nvarchar(100) | Y | (N'New') |  |  |
| 26 | PROJECTCODE | nvarchar(100) | Y |  |  |  |
| 27 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 28 | OWNPROJECTEUID | nvarchar(100) | Y |  |  |  |
| 29 | FLOWAUDITRESULT | nvarchar(20) | Y | (N'1') |  |  |

### CREATESTEPTHREE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | VALUE | nvarchar(100) | Y |  |  |  |
| 8 | DESCRIPTION | nvarchar(100) | Y |  |  |  |
| 9 | VALUETYPE | nvarchar(100) | Y |  |  |  |
| 10 | DEFAULTVALUE | nvarchar(100) | Y |  |  |  |

### CREPORTLINKRESOURCE

**行数** 33 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | OWNUID | nvarchar(50) | Y |  |  |  |
| 7 | OWNGID | nvarchar(50) | Y |  |  |  |
| 8 | ESTATION | nvarchar(100) | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 12 | RPTEMPLATEID | nvarchar(100) | Y |  |  |  |
| 13 | RPDBLABEL | nvarchar(100) | Y |  |  |  |
| 14 | CAPPRESOURCEID | nvarchar(100) | Y |  |  |  |
| 15 | CAPPATTRIBUTEID | nvarchar(100) | Y |  |  |  |
| 16 | ISFILTER | bit | Y |  |  |  |
| 17 | LINKTYPE | nvarchar(20) | Y |  |  |  |
| 18 | CONNECTSIGN | nvarchar(100) | Y |  |  |  |
| 19 | SEQUENCE | int | Y | (N'1') |  |  |

### CREPORTOBJECT

**行数** 2,880 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 22 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 23 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 24 | WORKFLOWSTATE | nvarchar(20) |  | (N'New') |  |  |
| 25 | RELEASEDATE | bigint | Y |  |  |  |
| 26 | POSITION | nvarchar(100) | Y |  |  |  |
| 27 | CURITERATION | bigint | Y | (N'1') |  |  |
| 28 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 29 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 30 | CHECKOUTDATE | bigint | Y |  |  |  |
| 31 | TEMPLATETYPE | nvarchar(50) | Y | (N'-1') |  |  |
| 32 | TEMPLATEID | nvarchar(50) |  |  |  |  |
| 33 | PRODUCTCODE | nvarchar(100) | Y |  |  |  |
| 34 | ITEMCODE | nvarchar(100) | Y |  |  |  |
| 35 | BOMVIEW | nvarchar(50) | Y |  |  |  |
| 36 | ISFROMMSE | int | Y | (N'0') |  |  |
| 37 | ISTYPICALREPORT | bit | Y | (N'0') |  |  |
| 38 | PERGROUP | nvarchar(100) | Y |  |  |  |
| 39 | ISSEND | nvarchar(100) | Y |  |  |  |
| 40 | PROCESSID | nvarchar(100) | Y |  |  |  |
| 41 | XMDH | nvarchar(100) | Y |  |  |  |
| 42 | ZUZHIERP | nvarchar(100) | Y | (N'31') |  |  |
| 43 | SHIYONGZUZHI | nvarchar(100) | Y |  |  |  |
| 44 | SCGS | nvarchar(100) | Y | (N'31') |  |  |

### CREPORTOBJECT$

**行数** 5,879 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 22 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 23 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 24 | WORKFLOWSTATE | nvarchar(20) |  | (N'New') |  |  |
| 25 | RELEASEDATE | bigint | Y |  |  |  |
| 26 | POSITION | nvarchar(100) | Y |  |  |  |
| 27 | CURITERATION | bigint | Y | (N'1') |  |  |
| 28 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 29 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 30 | CHECKOUTDATE | bigint | Y |  |  |  |
| 31 | TEMPLATETYPE | nvarchar(50) | Y | (N'-1') |  |  |
| 32 | TEMPLATEID | nvarchar(50) |  |  |  |  |
| 33 | PRODUCTCODE | nvarchar(100) | Y |  |  |  |
| 34 | ITEMCODE | nvarchar(100) | Y |  |  |  |
| 35 | BOMVIEW | nvarchar(50) | Y |  |  |  |
| 36 | ISFROMMSE | int | Y | (N'0') |  |  |
| 37 | ISTYPICALREPORT | bit | Y | (N'0') |  |  |
| 38 | PERGROUP | nvarchar(100) | Y |  |  |  |
| 39 | MASTERID$ | nvarchar(100) | Y |  |  |  |
| 40 | ISSEND | nvarchar(100) | Y |  |  |  |
| 41 | PROCESSID | nvarchar(100) | Y |  |  |  |
| 42 | XMDH | nvarchar(100) | Y |  |  |  |
| 43 | ZUZHIERP | nvarchar(100) | Y | (N'31') |  |  |
| 44 | SHIYONGZUZHI | nvarchar(100) | Y |  |  |  |
| 45 | SCGS | nvarchar(100) | Y | (N'31') |  |  |

### CRESOURCESERNUM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | LEVELID | int | Y |  |  |  |
| 15 | SERIALNUM | int | Y |  |  |  |

### CSOBJ

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | WORKFLOWSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 16 | ITEMEUID | nvarchar(100) | Y |  |  |  |
| 17 | ITEMECODE | nvarchar(100) | Y |  |  |  |
| 18 | ITEMENAME | nvarchar(100) | Y |  |  |  |
| 19 | NUM1 | nvarchar(100) | Y |  |  |  |
| 20 | ISREFITEM | nvarchar(100) | Y |  |  |  |
| 21 | FSEID | nvarchar(100) | Y |  |  |  |
| 22 | FSCODE | nvarchar(100) | Y |  |  |  |
| 23 | FSNAME | nvarchar(100) | Y |  |  |  |
| 24 | SUITABILITY | nvarchar(100) | Y |  |  |  |
| 25 | VIEWIDSTR | nvarchar(100) | Y |  |  |  |
| 26 | PARENTCODE | nvarchar(100) | Y |  |  |  |
| 27 | FITPRODUCT | nvarchar(100) | Y |  |  |  |
| 28 | ALLROUTE | nvarchar(2000) | Y |  |  |  |
| 29 | PRIMARYPRODUCT | nvarchar(100) | Y |  |  |  |
| 30 | PRIMARYPLANTSTRUCTURE | nvarchar(100) | Y |  |  |  |

### CUSTOMBOARDFOLDER

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) |  | (N'SHOW') |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | DESCRIPTION | nvarchar(100) | Y |  |  |  |
| 10 | IMAGEDATA | ntext |  |  |  |  |

### CUSTOMBOARDITEM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) |  | (N'SHOW') |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | IMAGEDATA | ntext |  |  |  |  |
| 10 | DESCRIPTION | nvarchar(100) | Y |  |  |  |

### CUSTOMBOARDLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | LCREATETIME | bigint |  |  |  |  |
| 10 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 11 | PEUID | nvarchar(100) | Y |  |  |  |
| 12 | CEUID | nvarchar(100) | Y |  |  |  |

### DASHBOARDLAYOUT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | DASHBOARDWIDGETID | nvarchar(100) | Y |  |  |  |
| 7 | USERID | nvarchar(100) | Y |  |  |  |
| 8 | LAYOUTX | int | Y |  |  |  |
| 9 | LAYOUTY | int | Y |  |  |  |
| 10 | LAYOUTWIDTH | nvarchar(20) | Y |  |  |  |
| 11 | LAYOUTHEIGHT | nvarchar(20) | Y |  |  |  |
| 12 | TITLE | nvarchar(100) | Y |  |  |  |
| 13 | TAG | nvarchar(100) | Y |  |  |  |
| 14 | DASHBOARDGUID | nvarchar(100) | Y |  |  |  |
| 15 | DASHBOARDPARAM | nvarchar(2000) | Y |  |  |  |
| 16 | COLUMNNUM | int | Y |  |  |  |
| 17 | CLASSIFYEID | nvarchar(100) | Y |  |  |  |

### DASHBOARDLAYOUTTMP

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | DASHBOARDWIDGETID | nvarchar(100) | Y |  |  |  |
| 7 | LAYOUTX | int | Y |  |  |  |
| 8 | LAYOUTY | int | Y |  |  |  |
| 9 | LAYOUTWIDTH | nvarchar(20) | Y |  |  |  |
| 10 | LAYOUTHEIGHT | nvarchar(20) | Y |  |  |  |
| 11 | TITLE | nvarchar(100) | Y |  |  |  |
| 12 | TAG | nvarchar(100) | Y |  |  |  |
| 13 | DASHBOARDGUID | nvarchar(100) | Y |  |  |  |
| 14 | DASHBOARDPARAM | nvarchar(2000) | Y |  |  |  |
| 15 | COLUMNNUM | int | Y |  |  |  |
| 16 | CLASSIFYEID | nvarchar(100) | Y |  |  |  |
| 17 | TEMPLATENAME | nvarchar(100) | Y |  |  |  |
| 18 | FROMTYPE | nvarchar(100) | Y |  |  |  |

### DASHBOARDRELATION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | PEUID | nvarchar(100) | Y |  |  |  |
| 10 | CEUID | nvarchar(100) | Y |  |  |  |

### DASHBOARDWIDGET

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | IMG | nvarchar(100) | Y |  |  |  |
| 8 | TAG | nvarchar(200) | Y |  |  |  |
| 9 | CREATESCRIPT | ntext | Y |  |  |  |
| 10 | LOADSCRIPT | ntext | Y |  |  |  |
| 11 | REFRESHSCRIPT | ntext | Y |  |  |  |
| 12 | DESTORYSCRIPT | ntext | Y |  |  |  |
| 13 | RESIZESCRIPT | ntext | Y |  |  |  |
| 14 | ORDERNUMBER | int | Y |  |  |  |
| 15 | WIDTH | int | Y |  |  |  |
| 16 | HEIGHT | int | Y |  |  |  |

### DATASETTYPE

**行数** 39 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ICON | nvarchar(200) | Y |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |
| 10 | FILEEXTENT | nvarchar(20) |  |  |  |  |
| 11 | TYPEID | nvarchar(2000) |  | (N'DOCNORMAL') |  |  |
| 12 | VIEWTOOL | nvarchar(100) | Y |  |  |  |
| 13 | DOWNFILTER | nvarchar(100) | Y |  |  |  |
| 14 | ISINTEGRATECAD | nvarchar(20) | Y |  |  |  |
| 15 | UPLOADTRANS | bit | Y |  |  |  |

### DATESETTING

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | EDATE | bigint | Y |  |  |  |
| 16 | ID | nvarchar(100) | Y |  |  |  |

### DATEVALIDITY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | STARTEID | nvarchar(100) | Y |  |  |  |
| 16 | STARTDATE | bigint | Y |  |  |  |
| 17 | ENDEID | nvarchar(100) | Y |  |  |  |
| 18 | ENDDATE | bigint | Y |  |  |  |
| 19 | RELATIONID | nvarchar(100) | Y |  |  |  |
| 20 | RELATIONEUID | nvarchar(100) | Y |  |  |  |
| 21 | ISSYSTEM | nvarchar(100) | Y |  |  |  |

### DEPLOYCODE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 12 | SERIALNUMBER | int | Y |  |  |  |
| 13 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 14 | SOURCEGROUP | nvarchar(100) |  |  |  |  |
| 15 | SOURCELEVE | nvarchar(100) | Y |  |  |  |
| 16 | DEFAULTVALUE | nvarchar(100) | Y |  |  |  |
| 17 | VALUETYPE | nvarchar(20) |  | (N'String') |  |  |

### DEPLOYCODEVALUE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | OWNUID | nvarchar(50) | Y |  |  |  |
| 7 | OWNGID | nvarchar(50) | Y |  |  |  |
| 8 | ESTATION | nvarchar(100) | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 12 | NUMORDER | nvarchar(100) | Y |  |  |  |
| 13 | CODEVALUE | nvarchar(50) |  |  |  |  |
| 14 | DEPLOYCODEEID | nvarchar(100) | Y |  |  |  |
| 15 | VALUESTATE | nvarchar(20) |  |  |  |  |
| 16 | OPERATOR | nvarchar(20) | Y |  |  |  |
| 17 | VALUETYPE | nvarchar(20) | Y |  |  |  |

### DEPLOYVARIANTRELATION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### DOCBASEFORM

**行数** 45,560 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | DOCID | nvarchar(100) | Y |  |  |  |
| 8 | DOCTYPE | nvarchar(100) | Y |  |  |  |
| 9 | STANDARDCODE | nvarchar(100) | Y |  |  |  |
| 10 | NIANDAIHAO | nvarchar(100) | Y |  |  |  |
| 11 | JIUBIANHAO | nvarchar(100) | Y |  |  |  |
| 12 | GUIGE | nvarchar(100) | Y |  |  |  |
| 13 | ZHONGYAODU | nvarchar(100) | Y |  |  |  |
| 14 | JINGDUDJ | nvarchar(100) | Y |  |  |  |
| 15 | JILIANGDANGWEI | nvarchar(100) | Y |  |  |  |
| 16 | ATTACHXH | int | Y |  |  |  |
| 17 | ATTACHTYPE | nvarchar(100) | Y |  |  |  |
| 18 | XMDH | nvarchar(100) | Y |  |  |  |
| 19 | WENJIANLEIXING | nvarchar(100) | Y |  |  |  |

### DOCBASEFORM$

**行数** 60,130 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | DOCID | nvarchar(100) | Y |  |  |  |
| 8 | DOCTYPE | nvarchar(100) | Y |  |  |  |
| 9 | STANDARDCODE | nvarchar(100) | Y |  |  |  |
| 10 | NIANDAIHAO | nvarchar(100) | Y |  |  |  |
| 11 | JIUBIANHAO | nvarchar(100) | Y |  |  |  |
| 12 | GUIGE | nvarchar(100) | Y |  |  |  |
| 13 | ZHONGYAODU | nvarchar(100) | Y |  |  |  |
| 14 | JINGDUDJ | nvarchar(100) | Y |  |  |  |
| 15 | JILIANGDANGWEI | nvarchar(100) | Y |  |  |  |
| 16 | ATTACHXH | int | Y |  |  |  |
| 17 | MASTERID$ | nvarchar(100) | Y |  |  |  |
| 18 | ATTACHTYPE | nvarchar(100) | Y |  |  |  |
| 19 | XMDH | nvarchar(100) | Y |  |  |  |
| 20 | WENJIANLEIXING | nvarchar(100) | Y |  |  |  |

### DOCDATASET

**行数** 45,548 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 22 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 23 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 24 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 25 | RELEASEDATE | bigint | Y |  |  |  |
| 26 | POSITION | nvarchar(100) |  |  |  |  |
| 27 | CURITERATION | bigint | Y | (N'1') |  |  |
| 28 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 29 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 30 | CHECKOUTDATE | bigint | Y |  |  |  |
| 31 | TYPEID | nvarchar(100) |  |  |  |  |
| 32 | FILENAME | nvarchar(250) | Y |  |  |  |
| 33 | FILEEXTENT | nvarchar(100) | Y |  |  |  |
| 34 | CURVOLUMEID | nvarchar(100) | Y |  |  |  |
| 35 | CHECKOUTPATH | nvarchar(500) | Y |  |  |  |
| 36 | EXTENTINFO | nvarchar(2000) |  |  |  |  |
| 37 | DATASETCLASS | nvarchar(100) | Y |  |  |  |
| 38 | PAGETYPE | nvarchar(100) | Y |  |  |  |
| 39 | FORMID | nvarchar(100) | Y |  |  |  |
| 40 | ISMAINMODEL | bit | Y |  |  |  |
| 41 | MAINFILEID | nvarchar(100) | Y |  |  |  |
| 42 | ISDERIVEDOC | bit | Y | (N'0') |  |  |
| 43 | CREOITERATION | nvarchar(100) | Y |  |  |  |
| 44 | CHECKOUTIP | nvarchar(100) | Y |  |  |  |
| 45 | MAINFILETEMPLATEID | nvarchar(100) | Y |  |  |  |
| 46 | CHECKOUTMODE | nvarchar(20) | Y |  |  |  |
| 47 | MSEPOSITION | nvarchar(100) | Y |  |  |  |
| 48 | CADTYPE | nvarchar(100) | Y |  |  |  |
| 49 | CADVERSION | nvarchar(100) | Y |  |  |  |
| 50 | MD5 | nvarchar(100) | Y |  |  |  |
| 51 | DOCSHEETSTYLE | nvarchar(100) | Y |  |  |  |
| 52 | ITEMDOCEXTENTINFO | nvarchar(100) | Y |  |  |  |
| 53 | XMDHDOC | nvarchar(100) | Y |  |  |  |
| 54 | ISSENDDOC | nvarchar(100) | Y |  |  |  |
| 55 | ERPFORMID | nvarchar(100) | Y |  |  |  |

### DOCDATASET$

**行数** 59,823 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 22 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 23 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 24 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 25 | RELEASEDATE | bigint | Y |  |  |  |
| 26 | POSITION | nvarchar(100) |  |  |  |  |
| 27 | CURITERATION | bigint | Y | (N'1') |  |  |
| 28 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 29 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 30 | CHECKOUTDATE | bigint | Y |  |  |  |
| 31 | TYPEID | nvarchar(100) |  |  |  |  |
| 32 | FILENAME | nvarchar(250) | Y |  |  |  |
| 33 | FILEEXTENT | nvarchar(100) | Y |  |  |  |
| 34 | CURVOLUMEID | nvarchar(100) | Y |  |  |  |
| 35 | CHECKOUTPATH | nvarchar(500) | Y |  |  |  |
| 36 | EXTENTINFO | nvarchar(2000) |  |  |  |  |
| 37 | DATASETCLASS | nvarchar(100) | Y |  |  |  |
| 38 | PAGETYPE | nvarchar(100) | Y |  |  |  |
| 39 | FORMID | nvarchar(100) | Y |  |  |  |
| 40 | ISMAINMODEL | bit | Y |  |  |  |
| 41 | MAINFILEID | nvarchar(100) | Y |  |  |  |
| 42 | ISDERIVEDOC | bit | Y | (N'0') |  |  |
| 43 | CREOITERATION | nvarchar(100) | Y |  |  |  |
| 44 | CHECKOUTIP | nvarchar(100) | Y |  |  |  |
| 45 | MAINFILETEMPLATEID | nvarchar(100) | Y |  |  |  |
| 46 | CHECKOUTMODE | nvarchar(20) | Y |  |  |  |
| 47 | MSEPOSITION | nvarchar(100) | Y |  |  |  |
| 48 | CADTYPE | nvarchar(100) | Y |  |  |  |
| 49 | CADVERSION | nvarchar(100) | Y |  |  |  |
| 50 | MD5 | nvarchar(100) | Y |  |  |  |
| 51 | DOCSHEETSTYLE | nvarchar(100) | Y |  |  |  |
| 52 | ITEMDOCEXTENTINFO | nvarchar(100) | Y |  |  |  |
| 53 | MASTERID$ | nvarchar(100) | Y |  |  |  |
| 54 | XMDHDOC | nvarchar(100) | Y |  |  |  |
| 55 | ISSENDDOC | nvarchar(100) | Y |  |  |  |
| 56 | ERPFORMID | nvarchar(100) | Y |  |  |  |

### DOCFILEMODEL

**行数** 45,848 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EUID | nvarchar(100) | Y |  |  |  |
| 6 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 7 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 8 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | CREATETIME | bigint | Y |  |  |  |
| 10 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 11 | UPDATETIME | bigint | Y |  |  |  |
| 12 | OWNUID | nvarchar(50) | Y |  |  |  |
| 13 | OWNGID | nvarchar(50) | Y |  |  |  |
| 14 | ESTATION | nvarchar(100) | Y |  |  |  |
| 15 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 16 | SERIALNUMBER | int | Y |  |  |  |
| 17 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 18 | ITERATION | nvarchar(100) | Y |  |  |  |
| 19 | FILEPATH | nvarchar(500) |  |  |  |  |
| 20 | FILESIZE | bigint | Y |  |  |  |
| 21 | CURVOLUMEID | nvarchar(100) | Y |  |  |  |
| 22 | TYPEID | nvarchar(100) | Y |  |  |  |
| 23 | FILEEXTENT | nvarchar(100) | Y |  |  |  |
| 24 | CHECKOUTPATH | nvarchar(500) | Y |  |  |  |
| 25 | DOCID | nvarchar(100) | Y |  |  |  |
| 26 | FILENAME | nvarchar(250) |  |  |  |  |
| 27 | ISMAINFILE | bit | Y |  |  |  |
| 28 | CHECKOUTUSER | nvarchar(500) | Y |  |  |  |
| 29 | CHECKOUTDATE | bigint | Y |  |  |  |
| 30 | CHECKOUTSTATE | bit | Y |  |  |  |

### DOCFILEMODEL$

**行数** 60,233 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EUID | nvarchar(100) | Y |  |  |  |
| 6 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 7 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 8 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | CREATETIME | bigint | Y |  |  |  |
| 10 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 11 | UPDATETIME | bigint | Y |  |  |  |
| 12 | OWNUID | nvarchar(50) | Y |  |  |  |
| 13 | OWNGID | nvarchar(50) | Y |  |  |  |
| 14 | ESTATION | nvarchar(100) | Y |  |  |  |
| 15 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 16 | SERIALNUMBER | int | Y |  |  |  |
| 17 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 18 | ITERATION | nvarchar(100) | Y |  |  |  |
| 19 | FILEPATH | nvarchar(500) |  |  |  |  |
| 20 | FILESIZE | bigint | Y |  |  |  |
| 21 | CURVOLUMEID | nvarchar(100) | Y |  |  |  |
| 22 | TYPEID | nvarchar(100) | Y |  |  |  |
| 23 | FILEEXTENT | nvarchar(100) | Y |  |  |  |
| 24 | CHECKOUTPATH | nvarchar(500) | Y |  |  |  |
| 25 | DOCID | nvarchar(100) | Y |  |  |  |
| 26 | FILENAME | nvarchar(250) |  |  |  |  |
| 27 | ISMAINFILE | bit | Y |  |  |  |
| 28 | CHECKOUTUSER | nvarchar(500) | Y |  |  |  |
| 29 | CHECKOUTDATE | bigint | Y |  |  |  |
| 30 | CHECKOUTSTATE | bit | Y |  |  |  |
| 31 | MASTERID$ | nvarchar(100) | Y |  |  |  |

### DOCFILETEMPLATE

**行数** 49 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | POSITION | nvarchar(100) |  |  |  |  |
| 16 | FILEINFORMATION | ntext |  |  |  |  |
| 17 | EXTEND | nvarchar(100) | Y |  |  |  |
| 18 | WORKFLOWTEMPLATE | nvarchar(100) | Y |  |  |  |

### DOCFILETEMPLATECLASSIFY

**行数** 5 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | ORDERNUMBER | nvarchar(100) |  |  |  |  |

### DOCFILETEMPLATELINK

**行数** 54 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### DOCFILETEMPLATETOFILE

**行数** 51 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### DOCJHORPC

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | DOCID | nvarchar(100) | Y |  |  |  |
| 8 | DOCTYPE | nvarchar(100) | Y | (N'NormalDoc') |  |  |
| 9 | STANDARDCODE | nvarchar(100) | Y |  |  |  |
| 10 | NIANDAIHAO | nvarchar(100) | Y |  |  |  |
| 11 | JIUBIANHAO | nvarchar(100) | Y |  |  |  |
| 12 | GUIGE | nvarchar(100) | Y |  |  |  |
| 13 | ZHONGYAODU | nvarchar(100) | Y |  |  |  |
| 14 | JINGDUDJ | nvarchar(100) | Y |  |  |  |
| 15 | JILIANGDANGWEI | nvarchar(100) | Y |  |  |  |
| 16 | ATTACHXH | int | Y |  |  |  |
| 17 | ATTACHTYPE | nvarchar(100) | Y |  |  |  |
| 18 | XMDH | nvarchar(100) | Y |  |  |  |

### DOCRELATION

**行数** 5 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | DOCLINKTYPE | nvarchar(100) | Y |  |  |  |

### DOCTEMPLATE

**行数** 7 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | TYPEID | nvarchar(100) |  |  |  |  |
| 16 | FILENAME | nvarchar(100) | Y |  |  |  |
| 17 | FILEEXTENT | nvarchar(100) | Y |  |  |  |
| 18 | CURVOLUMEID | nvarchar(100) | Y |  |  |  |
| 19 | DATASETCLASS | nvarchar(100) | Y |  |  |  |
| 20 | PAGETYPE | nvarchar(100) | Y |  |  |  |
| 21 | RESTRAIN | nvarchar(100) | Y |  |  |  |
| 22 | FORMID | nvarchar(100) | Y |  |  |  |
| 23 | PERGROUP | nvarchar(100) | Y |  |  |  |

### DOCTEMPLATETODOCFILETEMPLATE

**行数** 50 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### EAISETTING

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |
| 10 | SETTINGTYPE | nvarchar(100) | Y |  |  |  |
| 11 | OBJECTTEXT | nvarchar(200) | Y |  |  |  |
| 12 | NAMEVALUE | nvarchar(500) | Y |  |  |  |
| 13 | EAIOBJECT | nvarchar(200) | Y |  |  |  |
| 14 | EAICOLNAME | nvarchar(100) | Y |  |  |  |
| 15 | UNIQUEFRILD | nvarchar(100) | Y | (N'false') |  |  |
| 16 | ISSHOW | nvarchar(100) | Y | (N'true') |  |  |
| 17 | ISDISPLAYVALUE | nvarchar(100) | Y | (N'true') |  |  |
| 18 | ISNOTNULL | nvarchar(100) | Y | (N'false') |  |  |
| 19 | DATACHANGECONFIG | nvarchar(500) | Y |  |  |  |
| 20 | DEFAULTVALUE | nvarchar(100) | Y |  |  |  |

### ELECMATERIAL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | NO | nvarchar(2000) | Y |  |  |  |
| 7 | COUNT | nvarchar(100) | Y |  |  |  |
| 8 | ITEMTYPE | nvarchar(100) | Y |  |  |  |
| 9 | PARAMETER | nvarchar(100) | Y |  |  |  |
| 10 | DESCRIBE | nvarchar(100) | Y |  |  |  |
| 11 | SPEC | nvarchar(100) | Y |  |  |  |
| 12 | SEQUENCE | nvarchar(100) | Y |  |  |  |
| 13 | CISID | nvarchar(100) | Y |  |  |  |
| 14 | ITEMID | nvarchar(100) | Y |  |  |  |

### ENUMERATION

**行数** 38 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ICON | nvarchar(200) | Y |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |
| 10 | PEID | nvarchar(100) |  |  |  |  |
| 11 | DESCRIPTION | nvarchar(200) | Y |  |  |  |
| 12 | LOVTYPE | nvarchar(20) | Y | (N'Custom') |  |  |
| 13 | SHOWMODE | nvarchar(20) | Y | (N'Exhaustion') |  |  |
| 14 | SEQUENCE | int | Y |  |  |  |

### ENUMERATIONCATEGORY

**行数** 9 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | DESCRIPTION | nvarchar(200) | Y |  |  |  |
| 10 | SEQUENCE | int | Y |  |  |  |

### ENUMERATIONITEM

**行数** 254 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | ICON | nvarchar(200) | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | PEID | nvarchar(100) |  |  |  |  |
| 12 | SEQUENCE | int |  |  |  |  |
| 13 | LOVTYPE | nvarchar(20) | Y | (N'Custom') |  |  |
| 14 | DESCRIPTION | nvarchar(100) | Y |  |  |  |
| 15 | SUITGROUP | varchar(4000) | Y |  |  |  |

### EPSLINK

**行数** 1,037 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |

### FAVORITEFOLDER

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | OWNUID | nvarchar(50) | Y |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |

### FAVORITELINK

**行数** 24 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | PEUID | nvarchar(100) | Y |  |  |  |
| 11 | CEUID | nvarchar(100) | Y |  |  |  |
| 12 | FAVORITETIME | bigint | Y |  |  |  |
| 13 | IMPORTANT | bit | Y | (N'0') |  |  |
| 14 | IMPORTANTICON | nvarchar(100) | Y |  |  |  |

### FLOWBINDINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OBJTYPENAME | nvarchar(100) | Y |  |  |  |
| 7 | FLOWTEMPLATEUID | nvarchar(100) | Y |  |  |  |

### FORMBUILDERDATA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | EVERSION | int | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | MASTERTYPE | nvarchar(100) |  |  |  |  |
| 10 | SLAVETYPE | nvarchar(500) | Y |  |  |  |
| 11 | DESIGNERDATA | ntext |  |  |  |  |
| 12 | CONTROLDATA | ntext | Y |  |  |  |
| 13 | TEMPORARYDATA | ntext | Y |  |  |  |
| 14 | PERMISSIONDATA | ntext | Y |  |  |  |
| 15 | MODELDATAID | nvarchar(100) | Y |  |  |  |
| 16 | TYPEID | nvarchar(100) | Y |  |  |  |
| 17 | ASSISTANTGID | nvarchar(2000) | Y |  |  |  |

### FORMCENTOR

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |

### FORMCLASSIFY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | DESCRIPTION | nvarchar(500) | Y |  |  |  |

### FORMCLASSIFYLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### FORMCLASSIFYTOOBJTYPE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ICON | nvarchar(200) | Y | (N'/UploadFile/Image/FormCentor/FormImg.png') |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | CLASSIFYID | nvarchar(100) | Y |  |  |  |
| 8 | FORMOBJECTTYPE | nvarchar(100) | Y |  |  |  |

### FORMDATA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | TYPE | nvarchar(100) |  |  |  |  |
| 9 | MODELTYPE | nvarchar(100) |  |  |  |  |

### FORMFLOWTASKPERMISSION

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | FORMTYPE | nvarchar(100) | Y |  |  |  |
| 7 | CONTROLID | nvarchar(100) | Y |  |  |  |
| 8 | TASKNODEID | nvarchar(100) | Y |  |  |  |
| 9 | PERMISSIONCONTENT | nvarchar(2000) | Y |  |  |  |

### FORMULAPARA

**行数** 2,597 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | PARASIGN | nvarchar(200) |  |  |  |  |
| 16 | PARANAME | nvarchar(200) |  |  |  |  |
| 17 | CALCULATEFORMULA | nvarchar(1000) | Y |  |  |  |
| 18 | PARATYPE | nvarchar(200) |  |  |  |  |
| 19 | UNIT | nvarchar(100) | Y |  |  |  |
| 20 | PARAVALUE | nvarchar(100) | Y |  |  |  |
| 21 | DECIMALDIGITS | nvarchar(20) | Y | (N'2') |  |  |
| 22 | MATERIALTYPE | nvarchar(200) | Y |  |  |  |

### FORMULAPARAVALUE

**行数** 31 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | OBJECTID | nvarchar(200) | Y |  |  |  |
| 16 | FORMULAPARAID | nvarchar(200) | Y |  |  |  |
| 17 | PARASIGN | nvarchar(200) | Y |  |  |  |
| 18 | PARAVALUE | nvarchar(200) | Y |  |  |  |

### FORPLATFORM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | FTYPE | nvarchar(100) | Y |  |  |  |
| 8 | FDESCRIBE | nvarchar(100) | Y |  |  |  |

### FSMANAGEROOTLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | PEUID | nvarchar(100) | Y |  |  |  |
| 10 | CEUID | nvarchar(100) | Y |  |  |  |

### FUNCBUTTON

**行数** 30 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ICON | nvarchar(200) | Y |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |
| 10 | BUTTONTEXT | nvarchar(500) | Y |  |  |  |
| 11 | CALLPAGE | nvarchar(100) | Y |  |  |  |
| 12 | GROUPNAME | nvarchar(100) | Y |  |  |  |
| 13 | WEBVIEWURL | nvarchar(100) | Y |  |  |  |
| 14 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 15 | SEQUENCE | int | Y |  |  |  |
| 16 | ENABLE | bit | Y | (N'1') |  |  |
| 17 | PAGETYPE | nvarchar(100) | Y |  |  |  |
| 18 | ISPAD | bit | Y | (N'1') |  |  |
| 19 | ISPHONE | bit | Y | (N'1') |  |  |

### GROUPINFO

**行数** 68 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) |  |  |  |  |
| 7 | UPDATETIME | bigint |  |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |
| 10 | PARENTGROUPEID | nvarchar(100) | Y |  |  |  |
| 11 | VAULTINFO | nvarchar(100) |  |  |  |  |
| 12 | DESCRIPT | nvarchar(500) | Y |  |  |  |
| 13 | MANAGER | ntext | Y |  |  |  |
| 14 | ISGROUPSPACE | bit | Y |  |  |  |
| 15 | ISDATAGROUP | bit | Y |  |  |  |
| 16 | GROUPCODE | nvarchar(20) | Y |  |  |  |
| 17 | GROUPSPACEICON | nvarchar(100) | Y |  |  |  |
| 18 | MAXGROUPCODE | bigint | Y |  |  |  |
| 19 | DATAGROUPICON | nvarchar(100) | Y |  |  |  |
| 20 | ERPDEPCODE | nvarchar(100) | Y |  |  |  |

### GROUPISOLATEINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | GROUPID | nvarchar(100) | Y |  |  |  |
| 7 | GROUPCODE | nvarchar(20) | Y |  |  |  |
| 8 | SETGROUPID | nvarchar(100) | Y |  |  |  |
| 9 | SETGROUPCODE | nvarchar(20) | Y |  |  |  |
| 10 | SETMODE | nvarchar(20) | Y |  |  |  |

### GROUPSHAREMAPPING

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | GROUPID | nvarchar(100) | Y |  |  |  |
| 7 | GROUPCODE | nvarchar(20) | Y |  |  |  |
| 8 | SHOWGROUPID | nvarchar(100) | Y |  |  |  |
| 9 | SHOWGROUPCODE | nvarchar(20) | Y |  |  |  |

### GROUPSPACEDISPLAY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | GROUPID | nvarchar(100) | Y |  |  |  |
| 7 | SHOWGROUPID | nvarchar(100) | Y |  |  |  |

### INDIVIDUATIONSETTING

**行数** 2,065 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | SETTINGKEY | nvarchar(1000) | Y |  |  |  |
| 10 | PARENTKEY | nvarchar(100) | Y |  |  |  |
| 11 | SETTINGTYPE | nvarchar(50) | Y |  |  |  |
| 12 | SETTINGVALUE | ntext | Y |  |  |  |
| 13 | REMARK | nvarchar(2000) | Y |  |  |  |
| 14 | MODULEID | nvarchar(100) | Y |  |  |  |
| 15 | OWNER | nvarchar(100) | Y |  |  |  |
| 16 | USINGUSERSET | bit | Y | (N'0') |  |  |
| 17 | USETYPE | nvarchar(50) | Y |  |  |  |
| 18 | TOCLIENT | bit | Y |  |  |  |
| 19 | ISCOMPANY | bit | Y | (N'1') |  |  |
| 20 | USINGGROUPSET | bit | Y | (N'0') |  |  |
| 21 | GROUPID | nvarchar(100) | Y |  |  |  |
| 22 | SETTINGDATATYPE | nvarchar(50) | Y |  |  |  |

### INSTANTMESSAGE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | MFROM | nvarchar(100) | Y |  |  |  |
| 7 | MTO | nvarchar(100) | Y |  |  |  |
| 8 | MSTATE | nvarchar(100) | Y |  |  |  |
| 9 | MLINK | nvarchar(500) | Y |  |  |  |
| 10 | MTITLE | nvarchar(100) | Y |  |  |  |
| 11 | MCONTENT | ntext | Y |  |  |  |
| 12 | MSTATEICON | nvarchar(100) | Y |  |  |  |

### INTERNALMAIL

**行数** 125,276 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | RECIVETIME | bigint | Y |  |  |  |
| 10 | RECIVER | ntext | Y |  |  |  |
| 11 | SENDTIME | bigint | Y |  |  |  |
| 12 | SENDUSER | nvarchar(100) | Y |  |  |  |
| 13 | COPYTOUSER | ntext | Y |  |  |  |
| 14 | MTITLE | nvarchar(500) | Y |  |  |  |
| 15 | MCONTENT | ntext | Y |  |  |  |
| 16 | MLINK | nvarchar(500) | Y |  |  |  |
| 17 | IMPORTANT | nvarchar(100) | Y | (N'Normal') |  |  |
| 18 | MAILSIZE | nvarchar(100) | Y |  |  |  |
| 19 | SECRETUSER | ntext | Y |  |  |  |
| 20 | MTYPE | nvarchar(100) | Y |  |  |  |
| 21 | MSTATE | nvarchar(100) | Y |  |  |  |
| 22 | ISSEND | bit | Y | (N'0') |  |  |
| 23 | MSTATEICON | nvarchar(100) | Y |  |  |  |
| 24 | ACTUALRECIVER | nvarchar(100) | Y |  |  |  |
| 25 | PLUGINID | ntext | Y |  |  |  |
| 26 | SENDUSERTEXT | nvarchar(100) | Y |  |  |  |
| 27 | RECIVERTEXT | ntext | Y |  |  |  |
| 28 | SOURCE | nvarchar(100) | Y |  |  |  |

### INTERNALMAILTOPLUGIN

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### ISSUE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y | (N'new') |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 16 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 17 | TYPE | nvarchar(100) | Y |  |  |  |
| 18 | PROJECT | nvarchar(100) |  |  |  |  |
| 19 | ISSUELEVEL | nvarchar(100) |  | (N'high') |  |  |
| 20 | YZCD | nvarchar(100) |  | (N'yiban') |  |  |
| 21 | ISSUESOURCE | nvarchar(100) | Y |  |  |  |
| 22 | WTGY | nvarchar(2000) | Y |  |  |  |
| 23 | WTFL | nvarchar(100) |  | (N'cpsj') |  |  |
| 24 | JSFL | nvarchar(100) | Y |  |  |  |
| 25 | YXJ | nvarchar(100) |  | (N'high') |  |  |
| 26 | FSYY | nvarchar(2000) | Y |  |  |  |
| 27 | FINDUSER | nvarchar(100) |  |  |  |  |
| 28 | FINDDATE | nvarchar(100) |  |  |  |  |
| 29 | SUBMITUSER | nvarchar(100) | Y |  |  |  |
| 30 | SUBMITDATE | nvarchar(100) | Y |  |  |  |
| 31 | ISQUESTION | nvarchar(100) | Y |  |  |  |
| 32 | ZPR | nvarchar(100) | Y |  |  |  |
| 33 | RESPONSER | nvarchar(100) | Y |  |  |  |
| 34 | GZR | nvarchar(100) | Y |  |  |  |
| 35 | PLANENDDATE | nvarchar(100) | Y |  |  |  |
| 36 | PLANSOLUTION | nvarchar(2000) | Y |  |  |  |
| 37 | ACTUREENDDATE | nvarchar(100) | Y |  |  |  |
| 38 | ACTURESOLUTION | nvarchar(2000) | Y |  |  |  |
| 39 | SCHDULE | nvarchar(100) | Y | (N'0') |  |  |
| 40 | FAVORITE | bit | Y |  |  |  |
| 41 | ZPRQ | nvarchar(100) | Y |  |  |  |
| 42 | ISSUEPROJECT | nvarchar(100) | Y |  |  |  |
| 43 | ISSUEPROJECTEUID | nvarchar(100) | Y |  |  |  |

### ISSUEPRINCIPAL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ISSUETYPE | nvarchar(100) |  |  |  |  |
| 7 | PRINCIPAL | nvarchar(100) |  |  |  |  |

### ISSUEPROMOTE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ISSUEID | nvarchar(100) | Y |  |  |  |
| 7 | TYPE | nvarchar(100) | Y |  |  |  |
| 8 | BEFOREVALUE | nvarchar(100) | Y |  |  |  |
| 9 | AFTERVALUE | nvarchar(100) | Y |  |  |  |
| 10 | AUTOTYPE | nvarchar(100) | Y |  |  |  |

### ISSUETASK

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | TASKTYPE | nvarchar(100) | Y |  |  |  |
| 8 | TASKID | nvarchar(100) | Y |  |  |  |
| 9 | PROPERTY1 | nvarchar(100) | Y |  |  |  |
| 10 | ISMILESTONE | bit | Y | (N'0') |  |  |
| 11 | ISCYCLETASK | bit | Y | (N'0') |  |  |
| 12 | ISLEAFTASK | nvarchar(100) | Y |  |  |  |
| 13 | ISAUTHORITYRESOLVE | bit | Y |  |  |  |
| 14 | LIMITTYPE | nvarchar(20) | Y |  |  |  |
| 15 | LIMITDATETIME | bigint | Y |  |  |  |

### ISSUETASKLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |
| 9 | VERSIONEID | nvarchar(100) | Y |  |  |  |

### ISSUETEMPLATE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 16 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 17 | ISSUELEVEL | nvarchar(100) | Y | (N'high') |  |  |
| 18 | YZCD | nvarchar(100) |  | (N'yiban') |  |  |
| 19 | ISSUESOURCE | nvarchar(100) | Y |  |  |  |
| 20 | WTGY | nvarchar(2000) | Y |  |  |  |
| 21 | WTFL | nvarchar(100) |  | (N'cpsj') |  |  |
| 22 | JSFL | nvarchar(100) | Y |  |  |  |
| 23 | YXJ | nvarchar(100) |  | (N'high') |  |  |
| 24 | FSYY | nvarchar(100) | Y |  |  |  |
| 25 | PLANSOLUTION | nvarchar(100) | Y |  |  |  |
| 26 | INVALID | nvarchar(100) | Y | (N'no') |  |  |

### ISSUETODOC

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |
| 9 | VERSIONEID | nvarchar(100) | Y |  |  |  |

### ITEMATTTOFORMULAPARA

**行数** 12 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | OBJECTTYPE | nvarchar(200) | Y |  |  |  |
| 16 | OBJECTATTNAME | nvarchar(200) | Y |  |  |  |
| 17 | PARAID | nvarchar(200) | Y |  |  |  |
| 18 | PARAGROUP | nvarchar(200) | Y |  |  |  |

### LIFECYCLE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | LIFECYCLEINFO | ntext | Y |  |  |  |
| 8 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 9 | XMLNAME | nvarchar(100) | Y |  |  |  |
| 10 | INITSTATE | nvarchar(100) | Y |  |  |  |
| 11 | TYPE | nvarchar(100) |  | (N'Workflow') |  |  |
| 12 | ENABLE | nvarchar(100) | Y | (N'0') |  |  |

### LOOPPLAYIMAGE

**行数** 14 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 10 | IMAGEPATH | nvarchar(max) | Y |  |  |  |
| 11 | ENABLE | bit | Y | (N'1') |  |  |

### MAZYTABLE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |

### MCOBJ

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |

### MCOBJLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### MCOBJVALUE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | COEFFICIENT | nvarchar(100) |  |  |  |  |

### MLDETAIL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | LABELID | nvarchar(100) | Y |  |  |  |
| 7 | DETAILTYPE | nvarchar(100) | Y |  |  |  |
| 8 | JSONSTR | nvarchar(2000) | Y |  |  |  |

### MOBILEADVANCEDQUERY

**行数** 3 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | OWNUID | nvarchar(50) | Y |  |  |  |
| 8 | OWNGID | nvarchar(50) | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | CONDITIONTITLE | nvarchar(100) | Y |  |  |  |
| 12 | CONTROLTYPE | nvarchar(100) |  |  |  |  |
| 13 | FILTERSTRING | nvarchar(100) |  |  |  |  |
| 14 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 15 | ENABLE | bit | Y | (N'1') |  |  |
| 16 | OBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 17 | INFONAME | nvarchar(100) | Y |  |  |  |

### MOBILEMODULEPERMISSION

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | PERMISSIONTYPEVALUE | nvarchar(200) | Y |  |  |  |
| 7 | PERMISSIONVALUE | ntext | Y |  |  |  |

### MOBILEPAGEBUTTON

**行数** 62 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ICON | nvarchar(200) | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | TITLE | nvarchar(200) | Y |  |  |  |
| 9 | BUTTONITEMTYPE | nvarchar(100) | Y |  |  |  |
| 10 | BACKGROUNDCOLOR | nvarchar(20) | Y |  |  |  |
| 11 | URL | nvarchar(200) | Y |  |  |  |
| 12 | BUTTONITEMTYPEEXTEND | nvarchar(max) | Y |  |  |  |
| 13 | ENABLE | bit | Y | (N'1') |  |  |
| 14 | ISQUERY | bit | Y |  |  |  |
| 15 | ISMULTIPLESELECTION | bit | Y |  |  |  |

### MOBILEPAGEDEV

**行数** 27 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | PAGENAME | nvarchar(100) | Y |  |  |  |

### MOBILEPAGEFILTER

**行数** 6 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | OBJECTTYPE | nvarchar(50) | Y |  |  |  |
| 10 | FILTER | nvarchar(100) | Y |  |  |  |
| 11 | FILTERGROUP | nvarchar(100) | Y |  |  |  |
| 12 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 13 | TITLE | nvarchar(500) | Y |  |  |  |
| 14 | ASSEMBLY | nvarchar(100) | Y |  |  |  |
| 15 | CLASSNAME | nvarchar(100) | Y |  |  |  |
| 16 | ENABLE | bit | Y | (N'1') |  |  |

### MOBILEPAGELISTITEM

**行数** 28 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | OBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 10 | OBJECTSTATE | nvarchar(20) | Y |  |  |  |
| 11 | SINGLEBUTTON | nvarchar(100) | Y |  |  |  |
| 12 | SLIDEBUTTON | nvarchar(1000) | Y |  |  |  |
| 13 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 14 | LISTCONTENT | nvarchar(100) | Y |  |  |  |
| 15 | ENABLE | bit | Y | (N'1') |  |  |

### MOBILEPAGENAV

**行数** 9 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | TITLE | nvarchar(200) | Y |  |  |  |
| 10 | PAGENAME | nvarchar(100) | Y |  |  |  |
| 11 | WEBVIEWURL | nvarchar(100) | Y |  |  |  |
| 12 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 13 | SEQUENCE | int | Y |  |  |  |
| 14 | NORMALICON | nvarchar(100) | Y |  |  |  |
| 15 | HIGHLIGHTICON | nvarchar(100) | Y |  |  |  |
| 16 | WHITEICON | nvarchar(100) | Y |  |  |  |
| 17 | ENABLE | bit | Y | (N'1') |  |  |
| 18 | ISPAD | bit | Y | (N'1') |  |  |
| 19 | ISPHONE | bit | Y | (N'1') |  |  |
| 20 | SELECTWHITEICON | nvarchar(100) | Y |  |  |  |

### MOBILEPAGEORDER

**行数** 4 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | ORDERITEM | nvarchar(100) | Y |  |  |  |
| 10 | ORDERTYPE | nvarchar(100) | Y |  |  |  |
| 11 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 12 | TITLE | nvarchar(200) | Y |  |  |  |
| 13 | ENABLE | bit | Y |  |  |  |

### MOBILEPAGEPROPERTYTAB

**行数** 34 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | TITLE | nvarchar(100) | Y |  |  |  |
| 10 | OBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 11 | CONTROLTYPE | nvarchar(100) | Y |  |  |  |
| 12 | PAGENAME | nvarchar(100) | Y |  |  |  |
| 13 | URL | nvarchar(100) | Y |  |  |  |
| 14 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 15 | ASSEMBLYNAME | nvarchar(100) | Y |  |  |  |
| 16 | CLASSNAME | nvarchar(100) | Y |  |  |  |
| 17 | DATASOURCETYPE | nvarchar(100) | Y |  |  |  |
| 18 | SEQUENCE | int | Y |  |  |  |
| 19 | URLPARA | nvarchar(100) | Y |  |  |  |
| 20 | INFOTYPES | nvarchar(500) | Y |  |  |  |
| 21 | STRINGFILTER | nvarchar(500) | Y |  |  |  |
| 22 | RESULTOBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 23 | CONTENT | nvarchar(max) | Y |  |  |  |
| 24 | ENABLE | bit | Y | (N'1') |  |  |

### MOBILEPAGEPROPERTYTABCONFIG

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ASSEMBLYNAME | nvarchar(100) | Y |  |  |  |
| 7 | CLASSNAME | nvarchar(100) | Y |  |  |  |
| 8 | INFOTYPES | nvarchar(500) | Y |  |  |  |

### MOBILEPAGEPROPERTYTABCUSTOM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | ASSEMBLYNAME | nvarchar(100) | Y |  |  |  |
| 9 | CLASSNAME | nvarchar(100) | Y |  |  |  |
| 10 | INFOTYPES | nvarchar(500) | Y |  |  |  |

### MOBILEPAGEPROPERTYTABWEB

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | CONTENT | nvarchar(max) | Y |  |  |  |

### MOBILEPAGEROOTTODEV

**行数** 27 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### MOBILEPAGETAB

**行数** 21 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | TAB | nvarchar(100) | Y |  |  |  |
| 10 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 11 | TITLE | nvarchar(100) | Y |  |  |  |
| 12 | FILTERSTRING | nvarchar(500) | Y |  |  |  |
| 13 | LISTPAGECOUNT | nvarchar(100) | Y | (N'20') |  |  |
| 14 | ISSELECTED | bit | Y |  |  |  |
| 15 | ISALLSELECT | bit | Y |  |  |  |
| 16 | TABASSEMBLY | nvarchar(100) | Y |  |  |  |
| 17 | TABCLASSNAME | nvarchar(100) | Y |  |  |  |
| 18 | ASSEMBLY | nvarchar(100) | Y |  |  |  |
| 19 | CLASSNAME | nvarchar(100) | Y |  |  |  |
| 20 | SQLCOMMANDNAME | nvarchar(100) | Y |  |  |  |
| 21 | DATASOURCETYPE | nvarchar(100) |  |  |  |  |
| 22 | RESULTOBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 23 | RESULTORDERBY | nvarchar(100) | Y | (N'CreateTime desc') |  |  |
| 24 | SEQUENCE | int |  |  |  |  |
| 25 | ITEMTITLE | nvarchar(100) | Y |  |  |  |
| 26 | ITEMSUBTITLE | nvarchar(100) | Y |  |  |  |
| 27 | LISTITEMSTATE | nvarchar(100) | Y |  |  |  |
| 28 | LISTITEMSTATEICON | nvarchar(100) | Y |  |  |  |
| 29 | ITEMSUBTITLETWO | nvarchar(100) | Y |  |  |  |
| 30 | LISTITEMOBJECTMANAGER | nvarchar(100) | Y |  |  |  |
| 31 | PROPERTYCOLUMN | nvarchar(100) | Y |  |  |  |
| 32 | PROPERTYASSEMBLY | nvarchar(100) | Y |  |  |  |
| 33 | PROPERTYCLASSNAME | nvarchar(100) | Y |  |  |  |
| 34 | ISROOTQUERY | bit | Y |  |  |  |
| 35 | ENABLE | bit | Y | (N'1') |  |  |

### MOBILEPAGETABCONDITION

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | ITEMTITLE | nvarchar(100) | Y |  |  |  |
| 10 | ITEMSUBTITLE | nvarchar(100) | Y |  |  |  |
| 11 | LISTITEMSTATE | nvarchar(100) | Y |  |  |  |
| 12 | LISTITEMSTATEICON | nvarchar(100) | Y |  |  |  |
| 13 | ITEMSUBTITLETWO | nvarchar(100) | Y |  |  |  |
| 14 | LISTITEMOBJECTMANAGER | nvarchar(100) | Y |  |  |  |
| 15 | PROPERTYCOLUMN | nvarchar(100) | Y |  |  |  |
| 16 | PROPERTYASSEMBLY | nvarchar(100) | Y |  |  |  |
| 17 | PROPERTYCLASSNAME | nvarchar(100) | Y |  |  |  |

### MOBILEPAGETABGROUP

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | ITEMTITLE | nvarchar(100) | Y |  |  |  |
| 10 | ITEMSUBTITLE | nvarchar(100) | Y |  |  |  |
| 11 | LISTITEMSTATE | nvarchar(100) | Y |  |  |  |
| 12 | LISTITEMSTATEICON | nvarchar(100) | Y |  |  |  |
| 13 | ITEMSUBTITLETWO | nvarchar(100) | Y |  |  |  |
| 14 | LISTITEMOBJECTMANAGER | nvarchar(100) | Y |  |  |  |
| 15 | PROPERTYCOLUMN | nvarchar(100) | Y |  |  |  |
| 16 | PROPERTYASSEMBLY | nvarchar(100) | Y |  |  |  |
| 17 | PROPERTYCLASSNAME | nvarchar(100) | Y |  |  |  |
| 18 | ISROOTQUERY | bit | Y |  |  |  |

### MOBILEPAGETABSTANDARD

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | ITEMTITLE | nvarchar(100) | Y |  |  |  |
| 10 | ITEMSUBTITLE | nvarchar(100) | Y |  |  |  |
| 11 | LISTITEMSTATE | nvarchar(100) | Y |  |  |  |
| 12 | PROPERTYCOLUMN | nvarchar(100) | Y |  |  |  |
| 13 | PROPERTYASSEMBLY | nvarchar(100) | Y |  |  |  |
| 14 | PROPERTYCLASSNAME | nvarchar(100) | Y |  |  |  |

### MODELDATA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | STATEID | nvarchar(20) |  | (N'Temporary') |  |  |
| 4 | EVERSION | int |  |  |  |  |
| 5 | EUID | nvarchar(100) | Y |  |  |  |
| 6 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 7 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 8 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | CREATETIME | bigint | Y |  |  |  |
| 10 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 11 | UPDATETIME | bigint | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | MODEL | ntext | Y |  |  |  |
| 15 | SCHEMADATA | ntext | Y |  |  |  |
| 16 | DICDATA | ntext | Y |  |  |  |
| 17 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 18 | CHECKOUTDATE | nvarchar(100) | Y |  |  |  |
| 19 | FOREIGNKEYDATA | nvarchar(2000) | Y |  |  |  |
| 20 | MULTILANG | nvarchar(100) | Y |  |  |  |
| 21 | COMPANYEXTENDTYPE | nvarchar(20) | Y |  |  |  |
| 22 | FROMTYPE | nvarchar(100) | Y |  |  |  |
| 23 | ABOUTMODEL | nvarchar(2000) | Y |  |  |  |

### MODULEPERMISSION

**行数** 50 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | PERMISSIONTYPEVALUE | nvarchar(200) | Y |  |  |  |
| 7 | PERMISSIONVALUE | ntext | Y |  |  |  |

### MODULEPERMISSIONDATA

**行数** 177 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | TYPE | nvarchar(100) | Y |  |  |  |
| 7 | PERMISSIONVALUE | nvarchar(500) | Y |  |  |  |
| 8 | MPID | nvarchar(100) | Y |  |  |  |

### MSEACTPARA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | XZXC | nvarchar(100) | Y |  |  |  |
| 16 | GZTCC | nvarchar(100) | Y |  |  |  |
| 17 | YZXC | nvarchar(100) | Y |  |  |  |
| 18 | ZXKCC | nvarchar(100) | Y |  |  |  |
| 19 | ZZXC | nvarchar(100) | Y |  |  |  |
| 20 | TXCSL | nvarchar(100) | Y |  |  |  |

### MSEBOOK

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | STATEID | nvarchar(20) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 12 | SERIALNUMBER | int | Y |  |  |  |
| 13 | PERMISSION | nvarchar(20) | Y |  |  |  |

### MSEBOOKITEM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | CONFIGOBJECTID | nvarchar(100) | Y |  |  |  |
| 16 | REPORTTEMID | nvarchar(100) | Y |  |  |  |
| 17 | MSEREPORTID | nvarchar(100) | Y |  |  |  |

### MSECL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 17 | SERIALNUMBER | int | Y |  |  |  |
| 18 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 19 | CELIANGDIANCL | nvarchar(100) | Y |  |  |  |
| 20 | YAOQIUCL | nvarchar(200) | Y |  |  |  |
| 21 | METHODCL | nvarchar(200) | Y |  |  |  |
| 22 | TOOLCL | nvarchar(200) | Y |  |  |  |
| 23 | RANGECL | nvarchar(200) | Y |  |  |  |
| 24 | CISHUCL | int | Y |  |  |  |

### MSECLASS

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |

### MSECLASSATT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | DATABIND | nvarchar(200) | Y |  |  |  |
| 16 | EDITINFO | nvarchar(150) | Y | (N'Create Edit Copy') |  |  |
| 17 | SHOWINFO | nvarchar(150) | Y | (N'Create Edit View Copy') |  |  |
| 18 | WIDTHINFO | nvarchar(20) | Y | (N'100') |  |  |

### MSECLASSLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### MSECLASSRELA

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### MSECODEPARA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | XZXC | nvarchar(100) | Y |  |  |  |
| 16 | GZTCC | nvarchar(100) | Y |  |  |  |
| 17 | YZXC | nvarchar(100) | Y |  |  |  |
| 18 | ZXKCC | nvarchar(100) | Y |  |  |  |
| 19 | ZZXC | nvarchar(100) | Y |  |  |  |
| 20 | TXCSL | nvarchar(100) | Y |  |  |  |

### MSECONFIG

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ICON | nvarchar(200) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | SHOWITEMINTREE | int | Y | (N'0') |  |  |
| 16 | LEVELTYPE | nvarchar(100) |  |  |  |  |
| 17 | SHOWINPUTITEM | nvarchar(20) | Y | (N'Show') |  |  |
| 18 | SHOWOUTPUTITEM | nvarchar(20) | Y | (N'Show') |  |  |
| 19 | SHOWTFOBJECT | nvarchar(20) | Y | (N'Show') |  |  |
| 20 | SHOWQUALITYCHECK | nvarchar(20) | Y | (N'NotShow') |  |  |
| 21 | SHOWOPERATIONSPECIFICATION | nvarchar(20) | Y | (N'NotShow') |  |  |
| 22 | SHOWRESOURCE | nvarchar(max) | Y |  |  |  |
| 23 | INDVERSION | int | Y | (N'1') |  |  |
| 24 | HASCOPRO | int | Y | (N'0') |  |  |
| 25 | MSETEMCLASSID | nvarchar(100) | Y |  |  |  |
| 26 | PERMISSIONTOUSER | nvarchar(100) | Y |  |  |  |
| 27 | PREFIX | nvarchar(50) | Y |  |  |  |
| 28 | INITIALVALUE | nvarchar(20) | Y |  |  |  |
| 29 | INCREASEVALUE | int | Y |  |  |  |

### MSECRELA

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | ATTCORRESPONDS | nvarchar(max) | Y |  |  |  |

### MSEDCPARA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | XZXC | nvarchar(100) | Y |  |  |  |
| 16 | GZTCC | nvarchar(100) | Y |  |  |  |
| 17 | YZXC | nvarchar(100) | Y |  |  |  |
| 18 | ZXKCC | nvarchar(100) | Y |  |  |  |
| 19 | ZZXC | nvarchar(100) | Y |  |  |  |
| 20 | TXCSL | nvarchar(100) | Y |  |  |  |

### MSEFILDATA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | MSEOBJ | nvarchar(100) | Y |  |  |  |
| 16 | MSECONFIGE | nvarchar(100) | Y |  |  |  |
| 17 | CLASS | nvarchar(100) | Y |  |  |  |

### MSEJYX

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 17 | SERIALNUMBER | int | Y |  |  |  |
| 18 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 19 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 20 | RELEASEDATE | bigint | Y |  |  |  |
| 21 | JYXNAME | nvarchar(200) | Y |  |  |  |
| 22 | TYPE | nvarchar(100) | Y |  |  |  |
| 23 | ZHIBIAO | nvarchar(200) | Y |  |  |  |
| 24 | SGC | float | Y |  |  |  |
| 25 | XGC | float | Y |  |  |  |
| 26 | JYORDER | int | Y |  |  |  |
| 27 | IMPORTANT | nvarchar(20) | Y |  |  |  |
| 28 | METHOD | nvarchar(200) | Y |  |  |  |
| 29 | TOOL | nvarchar(200) | Y |  |  |  |
| 30 | JYTYPE | nvarchar(20) | Y |  |  |  |
| 31 | LINKDOC | nvarchar(20) | Y |  |  |  |
| 32 | PANDING | nvarchar(20) | Y |  |  |  |
| 33 | OBJECTID | nvarchar(100) | Y |  |  |  |
| 34 | MSEPOSITION | nvarchar(100) | Y |  |  |  |

### MSELABEL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | PROCESSID | nvarchar(100) | Y |  |  |  |
| 7 | CONFIGJSON | nvarchar(100) | Y |  |  |  |
| 8 | PROCESSJSON | nvarchar(100) | Y |  |  |  |
| 9 | LINKJSON | nvarchar(100) | Y |  |  |  |
| 10 | RELAJSON | nvarchar(100) | Y |  |  |  |

### MSELEVELCONFIG

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | PROCESS | nvarchar(100) | Y |  |  |  |
| 7 | POSITION | nvarchar(100) | Y |  |  |  |
| 8 | ROUTEUNIT | nvarchar(100) | Y |  |  |  |
| 9 | LSEQUENCE | int | Y |  |  |  |

### MSELINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | PROCESSORDER | nvarchar(100) | Y |  |  |  |

### MSELINK$

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | PROCESSORDER | nvarchar(100) | Y |  |  |  |
| 16 | MASTERID$ | nvarchar(100) | Y |  |  |  |

### MSEMATERIAL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 17 | SERIALNUMBER | int | Y |  |  |  |
| 18 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 19 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 20 | RELEASEDATE | bigint | Y |  |  |  |
| 21 | GUIGE | nvarchar(100) | Y |  |  |  |
| 22 | LENGTH | float | Y | (N'0') |  |  |
| 23 | CHICUN | float | Y | (N'0') |  |  |
| 24 | JIANSHU | int | Y | (N'1') |  |  |
| 25 | MATERIALTYPE | nvarchar(100) | Y |  |  |  |
| 26 | MATERIALWEIGHT | float | Y |  |  |  |
| 27 | MSEPOSITION | nvarchar(100) | Y |  |  |  |

### MSENL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | PPEID | nvarchar(100) | Y |  |  |  |
| 7 | CCEID | nvarchar(100) | Y |  |  |  |
| 8 | XX | nvarchar(100) | Y |  |  |  |
| 9 | YY | nvarchar(100) | Y |  |  |  |
| 10 | SCALE | nvarchar(100) | Y |  |  |  |

### MSENODESTYLE

**行数** 3 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | TYPE1 | nvarchar(100) | Y |  |  |  |
| 7 | FIELD | nvarchar(100) | Y |  |  |  |
| 8 | VALUE | nvarchar(100) | Y |  |  |  |
| 9 | FLOWCHARTNODESTYLE | nvarchar(100) | Y |  |  |  |

### MSEOBJ

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 22 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 23 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 24 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 25 | RELEASEDATE | bigint | Y |  |  |  |
| 26 | CURITERATION | bigint | Y | (N'1') |  |  |
| 27 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 28 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 29 | CHECKOUTDATE | bigint | Y |  |  |  |
| 30 | ITEMID | nvarchar(500) | Y |  |  |  |
| 31 | BOMVIEWID | nvarchar(100) | Y |  |  |  |
| 32 | PROPOSITION | nvarchar(100) | Y |  |  |  |
| 33 | PROCESSTYPE | nvarchar(100) | Y |  |  |  |
| 34 | ISOUTPUTVIEW | nvarchar(100) | Y | (N'false') |  |  |
| 35 | ISTYPICAL | nvarchar(100) | Y | (N'False') |  |  |
| 36 | PROCESSVIEWID | nvarchar(100) |  |  |  |  |
| 37 | WORKHOUR | float | Y |  |  |  |
| 38 | ZHUJICHANG | nvarchar(100) | Y |  |  |  |
| 39 | PROCESSEDITDATE | bigint | Y |  |  |  |
| 40 | CARDEDITDATE | bigint | Y |  |  |  |
| 41 | PROCESSPLANID | nvarchar(200) | Y |  |  |  |
| 42 | PRODUCTTYPE | nvarchar(200) | Y |  |  |  |
| 43 | PRODUCTNAME | nvarchar(200) | Y |  |  |  |
| 44 | WORKPLACE | nvarchar(200) | Y |  |  |  |
| 45 | MSESECTION | nvarchar(200) | Y |  |  |  |
| 46 | ZHUNZHONG | nvarchar(100) | Y |  |  |  |
| 47 | DANJIAN | nvarchar(100) | Y |  |  |  |
| 48 | PERGROUP | nvarchar(100) | Y |  |  |  |
| 49 | FROMPROCESSID | nvarchar(100) | Y |  |  |  |
| 50 | PROCESSCODE | nvarchar(100) | Y |  |  |  |
| 51 | PREPROCESS | nvarchar(100) | Y |  |  |  |
| 52 | OPERATIONTYPE | nvarchar(100) | Y |  |  |  |

### MSEOBJ$

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 22 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 23 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 24 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 25 | RELEASEDATE | bigint | Y |  |  |  |
| 26 | CURITERATION | bigint | Y | (N'1') |  |  |
| 27 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 28 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 29 | CHECKOUTDATE | bigint | Y |  |  |  |
| 30 | ITEMID | nvarchar(500) | Y |  |  |  |
| 31 | BOMVIEWID | nvarchar(100) | Y |  |  |  |
| 32 | PROPOSITION | nvarchar(100) | Y |  |  |  |
| 33 | PROCESSTYPE | nvarchar(100) | Y |  |  |  |
| 34 | ISOUTPUTVIEW | nvarchar(100) | Y | (N'false') |  |  |
| 35 | ISTYPICAL | nvarchar(100) | Y | (N'False') |  |  |
| 36 | PROCESSVIEWID | nvarchar(100) |  |  |  |  |
| 37 | WORKHOUR | float | Y |  |  |  |
| 38 | ZHUJICHANG | nvarchar(100) | Y |  |  |  |
| 39 | PROCESSEDITDATE | bigint | Y |  |  |  |
| 40 | CARDEDITDATE | bigint | Y |  |  |  |
| 41 | PROCESSPLANID | nvarchar(200) | Y |  |  |  |
| 42 | PRODUCTTYPE | nvarchar(200) | Y |  |  |  |
| 43 | PRODUCTNAME | nvarchar(200) | Y |  |  |  |
| 44 | WORKPLACE | nvarchar(200) | Y |  |  |  |
| 45 | MSESECTION | nvarchar(200) | Y |  |  |  |
| 46 | ZHUNZHONG | nvarchar(100) | Y |  |  |  |
| 47 | DANJIAN | nvarchar(100) | Y |  |  |  |
| 48 | PERGROUP | nvarchar(100) | Y |  |  |  |
| 49 | FROMPROCESSID | nvarchar(100) | Y |  |  |  |
| 50 | PROCESSCODE | nvarchar(100) | Y |  |  |  |
| 51 | PREPROCESS | nvarchar(100) | Y |  |  |  |
| 52 | OPERATIONTYPE | nvarchar(100) | Y |  |  |  |
| 53 | MASTERID$ | nvarchar(100) | Y |  |  |  |

### MSEOP

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 17 | SERIALNUMBER | int | Y |  |  |  |
| 18 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 19 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 20 | RELEASEDATE | bigint | Y |  |  |  |
| 21 | OPDES | nvarchar(200) | Y |  |  |  |
| 22 | OPREQUIRMENT | nvarchar(200) | Y |  |  |  |

### MSEOPINION

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | WORKFLOWID | nvarchar(100) |  |  |  |  |
| 16 | MSEID | nvarchar(100) |  |  |  |  |
| 17 | ITEMTYPE | nvarchar(100) | Y |  |  |  |
| 18 | ITEMID | nvarchar(100) | Y |  |  |  |
| 19 | OPINION | nvarchar(1000) | Y |  |  |  |
| 20 | MEASURE | nvarchar(1000) | Y |  |  |  |
| 21 | OPINIONSTATE | nvarchar(100) |  |  |  |  |
| 22 | ROOTMSEID | nvarchar(100) |  |  |  |  |
| 23 | COMMENTID | nvarchar(100) | Y |  |  |  |

### MSEPARA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 17 | SERIALNUMBER | int | Y |  |  |  |
| 18 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 19 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 20 | RELEASEDATE | bigint | Y |  |  |  |
| 21 | PARAOTHERNAME1 | nvarchar(100) | Y |  |  |  |
| 22 | CNAME1 | nvarchar(100) | Y |  |  |  |
| 23 | PARAVALUE1 | nvarchar(100) | Y |  |  |  |
| 24 | NJ | nvarchar(200) | Y |  |  |  |
| 25 | GD | nvarchar(200) | Y |  |  |  |
| 26 | SD | nvarchar(200) | Y |  |  |  |
| 27 | CD | nvarchar(200) | Y |  |  |  |

### MSEPARALIB

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | PARAGROUP | nvarchar(100) | Y |  |  |  |
| 16 | PARANAME | nvarchar(100) | Y |  |  |  |
| 17 | PARATYPE | nvarchar(100) | Y |  |  |  |
| 18 | DEFAULTVALUE | nvarchar(100) | Y |  |  |  |
| 19 | DATASOURCE | nvarchar(100) | Y |  |  |  |
| 20 | DATASOURCENAME | nvarchar(100) | Y |  |  |  |

### MSEPARAPLANLIB

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | PARAOTHERNAME | nvarchar(100) | Y |  |  |  |
| 16 | PARANAME | nvarchar(100) | Y |  |  |  |
| 17 | CALCULATIONTYPE | nvarchar(100) | Y | (N'FormulaConstant') |  |  |
| 18 | CALCULATIONFORMULA | nvarchar(100) | Y |  |  |  |
| 19 | MSECONFIGID | nvarchar(100) | Y |  |  |  |
| 20 | ATTRIBUTESCOLUMN | nvarchar(100) | Y |  |  |  |
| 21 | PARAID | nvarchar(100) | Y |  |  |  |

### MSEPARAPRO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | PROCESSVIEWID | nvarchar(100) | Y |  |  |  |
| 16 | RELATEDOBJ | nvarchar(2000) | Y |  |  |  |

### MSEPARAPROLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### MSEPATHPARA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | DGPARA1 | nvarchar(100) | Y |  |  |  |
| 16 | DGPARA2 | nvarchar(100) | Y |  |  |  |
| 17 | DGPARA3 | nvarchar(100) | Y |  |  |  |

### MSEPER

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | OBJECTID | nvarchar(100) | Y |  |  |  |
| 16 | RELATIONTYPE | nvarchar(100) | Y |  |  |  |
| 17 | REPORTID | nvarchar(100) | Y |  |  |  |
| 18 | PERMISSIONACTORTYPE | nvarchar(100) | Y |  |  |  |
| 19 | PERMISSIONACTORVALUE | nvarchar(2000) | Y |  |  |  |

### MSERELA

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | LINKTYPE | nvarchar(20) | Y |  |  |  |
| 16 | ITEMROUTE | nvarchar(2000) | Y |  |  |  |

### MSERELATYPE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | RELATYPE | nvarchar(100) | Y |  |  |  |
| 16 | EXTENDATT | nvarchar(500) | Y |  |  |  |
| 17 | CONTROLTYPE | nvarchar(50) | Y | (N'UnControl') |  |  |
| 18 | RELARESOURCE | nvarchar(max) | Y |  |  |  |
| 19 | PERMISSION | nvarchar(20) | Y |  |  |  |

### MSEREPATT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | CELLTYPE | nvarchar(20) | Y |  |  |  |
| 16 | DBLABEL | nvarchar(200) | Y |  |  |  |
| 17 | PROCESSATT | nvarchar(500) | Y |  |  |  |
| 18 | ISKEY | int | Y | (N'0') |  |  |
| 19 | ISSPLIT | int | Y | (N'0') |  |  |
| 20 | OTHERINFO | nvarchar(500) | Y |  |  |  |

### MSEREPORT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | REPORTTEMID | nvarchar(100) | Y |  |  |  |
| 15 | REPORTCLASS | nvarchar(100) | Y | (N'Common') |  |  |
| 16 | PERMISSION | nvarchar(20) | Y |  |  |  |
| 17 | CONTEMID | nvarchar(100) | Y |  |  |  |
| 18 | ATTATEMID | nvarchar(100) | Y |  |  |  |

### MSESORT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | PPROCESS | nvarchar(100) | Y |  |  |  |
| 16 | CURPROCESS | nvarchar(100) | Y |  |  |  |
| 17 | PREPROCESS | nvarchar(1000) | Y |  |  |  |
| 18 | MARK | nvarchar(100) | Y |  |  |  |

### MSESTRDRI

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | TYPE | nvarchar(100) | Y |  |  |  |
| 16 | DRIVINGRULES | nvarchar(100) | Y |  |  |  |
| 17 | DRIVERDATASOURCE | nvarchar(max) | Y |  |  |  |
| 18 | APPLICATIONCONDITIONS | nvarchar(max) | Y |  |  |  |
| 19 | ASSOCIATEDDATASOURCE | nvarchar(max) | Y |  |  |  |
| 20 | ACQUISITIONCONDITIONS | nvarchar(max) | Y |  |  |  |
| 21 | DRIVERVARIABLE | nvarchar(100) | Y |  |  |  |

### MSESTRDRIVAR

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | SOURCEGROUP | nvarchar(100) | Y |  |  |  |
| 16 | VALUETYPE | nvarchar(100) | Y |  |  |  |
| 17 | CALCULATIONTYPE | nvarchar(100) | Y | (N'FormulaConstant') |  |  |
| 18 | CALCULATIONFORMULA | nvarchar(100) | Y |  |  |  |

### MSETIME

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 17 | SERIALNUMBER | int | Y |  |  |  |
| 18 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 19 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 20 | RELEASEDATE | bigint | Y |  |  |  |
| 21 | SHUQIE | float | Y | (N'1') |  |  |
| 22 | JIANBAN | float | Y | (N'1') |  |  |
| 23 | DENGGAO | float | Y | (N'1') |  |  |
| 24 | JUQIE | float | Y | (N'1') |  |  |

### MSETYPPARALIB

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | PARANAME | nvarchar(100) | Y |  |  |  |
| 16 | PARAOTHERNAME | nvarchar(100) | Y |  |  |  |
| 17 | CALCULATIONTYPE | nvarchar(100) | Y | (N'FormulaConstant') |  |  |
| 18 | CALCULATIONFORMULA | nvarchar(100) | Y |  |  |  |
| 19 | ATTRIBUTESCOLUMN | nvarchar(100) | Y |  |  |  |
| 20 | PARAID | nvarchar(100) | Y |  |  |  |

### MSEZLATT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y | (N'MSEZLATT') |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | TYPE | nvarchar(100) | Y |  |  |  |
| 16 | OBJTYPE | nvarchar(20) | Y |  |  |  |
| 17 | EXTENDPROPERTY2 | nvarchar(200) | Y |  |  |  |
| 18 | EXTENDPROPERTY3 | nvarchar(200) | Y |  |  |  |
| 19 | COLWIDTH | nvarchar(100) | Y | (N'100') |  |  |
| 20 | COLWRAP | nvarchar(100) | Y |  |  |  |

### MTGROUP

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | STATEID | nvarchar(20) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 12 | SERIALNUMBER | int | Y |  |  |  |

### MTGROUPLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | LINKTYPE | nvarchar(100) | Y |  |  |  |

### MTLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | LINKTYPE | nvarchar(100) | Y |  |  |  |

### MTPARA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | COORDINATETYPE | nvarchar(100) | Y |  |  |  |
| 16 | COORDINATEVALUE | nvarchar(100) | Y |  |  |  |

### MTVALUE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | PARA1 | nvarchar(100) | Y |  |  |  |
| 16 | PARA2 | nvarchar(100) | Y |  |  |  |
| 17 | PARA3 | nvarchar(100) | Y |  |  |  |
| 18 | PARA4 | nvarchar(100) | Y |  |  |  |
| 19 | PARA5 | nvarchar(100) | Y |  |  |  |
| 20 | PARA6 | nvarchar(100) | Y |  |  |  |
| 21 | VALUEDATA | nvarchar(100) |  |  |  |  |

### MYQUERYMACHINEBASEINFO

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | DESCRIPTION | nvarchar(200) | Y |  |  |  |
| 10 | ROOTPOM | nvarchar(100) |  |  |  |  |
| 11 | SHOWVIEW | ntext | Y |  |  |  |
| 12 | DATALINKMODEL | nvarchar(1000) | Y |  |  |  |
| 13 | FIELDKEY | nvarchar(100) | Y |  |  |  |
| 14 | ORGANIZATIONROLE | ntext | Y |  |  |  |
| 15 | CUSTOMQUERYNAME | nvarchar(100) | Y |  |  |  |
| 16 | CUSTOMQUERYSQLCOMMAND | ntext | Y |  |  |  |
| 17 | SEQUENCE | int | Y |  |  |  |
| 18 | BASEQUERYID | nvarchar(100) | Y |  |  |  |

### MYQUERYMACHINEPOM

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |
| 7 | POMOBJECTNAME | nvarchar(100) | Y |  |  |  |

### MYQUERYPOMLINKDATA

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ACOLNAME | nvarchar(100) | Y |  |  |  |
| 7 | ATYPENAME | nvarchar(100) | Y |  |  |  |
| 8 | BCOLNAME | nvarchar(100) | Y |  |  |  |
| 9 | BTYPENAME | nvarchar(100) | Y |  |  |  |
| 10 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |

### MYQUERYROLE

**行数** 14 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |
| 9 | OPERATETYPE | nvarchar(100) | Y |  |  |  |
| 10 | COLNAME | nvarchar(100) | Y |  |  |  |
| 11 | COLTEXT | nvarchar(100) | Y |  |  |  |
| 12 | OBJECTTYPENAME | nvarchar(100) | Y |  |  |  |
| 13 | COMPARISONCONDITION | nvarchar(100) | Y |  |  |  |
| 14 | RESULTVALUE | nvarchar(150) | Y |  |  |  |
| 15 | COLTYPE | nvarchar(100) | Y |  |  |  |
| 16 | ODERNUMBER | bigint | Y |  |  |  |
| 17 | CUSTOMQUERYROLEID | nvarchar(100) | Y |  |  |  |
| 18 | CUSTOMQUERYREPLACEPARA | nvarchar(100) | Y |  |  |  |

### NEEDREFRESHFLAG

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ISREFRESHED | bit | Y |  |  |  |

### OBJECTFORM

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | OBJTYPE | nvarchar(100) |  |  |  |  |
| 8 | INFOTYPE | nvarchar(max) | Y |  |  |  |
| 9 | ACTIONBUTTON | nvarchar(1000) | Y |  |  |  |
| 10 | ENABLE | bit | Y |  |  |  |
| 11 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 12 | CONTENTMODE | nvarchar(100) | Y |  |  |  |
| 13 | TITLESERVICE | nvarchar(100) | Y |  |  |  |
| 14 | GRIDDATASERVICE | nvarchar(100) | Y |  |  |  |

### OBJECTMESSAGE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OBJECTID | nvarchar(100) |  |  |  |  |
| 7 | MESSAGEACTORVALUE | ntext | Y |  |  |  |
| 8 | MESSAGEACTIONVALUE | ntext | Y |  |  |  |
| 9 | OBJECTEUID | nvarchar(100) | Y |  |  |  |

### OBJECTPERMISSION

**行数** 3,160 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OBJECTID | nvarchar(100) | Y |  |  |  |
| 7 | PERMISSIONACTORVALUE | ntext | Y |  |  |  |
| 8 | PERMISSIONACTIONVALUE | ntext | Y |  |  |  |
| 9 | PERMISSIONEXPIREVALUE | bigint | Y |  |  |  |
| 10 | USEOBJECTID | nvarchar(100) | Y |  |  |  |
| 11 | OBJECTPERMISSIONMODE | nvarchar(50) | Y | (N'Manual') |  |  |
| 12 | OBJECTIDS | ntext | Y |  |  |  |

### OBJGRPER

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | OBJID | nvarchar(100) | Y |  |  |  |
| 6 | GRID | nvarchar(100) | Y |  |  |  |

### OLBCISSCHEMATIC

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OLBFILENAME | nvarchar(100) | Y |  |  |  |
| 7 | SCHEMATIC | nvarchar(100) | Y |  |  |  |
| 8 | SCHEMATICPART | nvarchar(100) | Y |  |  |  |
| 9 | CISTYPE | nvarchar(50) | Y |  |  |  |
| 10 | ITERATION | int | Y | (N'1') |  |  |

### OLBFILE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | ENAME | nvarchar(100) | Y |  |  |  |
| 9 | FILEPATH | nvarchar(200) | Y |  |  |  |
| 10 | FILESIZE | nvarchar(100) | Y |  |  |  |
| 11 | VOLUMN | nvarchar(100) | Y |  |  |  |
| 12 | ITERATION | float | Y |  |  |  |
| 13 | CISTYPE | nvarchar(100) | Y |  |  |  |
| 14 | CHECKOUTTIME | nvarchar(100) | Y |  |  |  |

### OPACTIONVALUE

**行数** 5,400 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ALLOWACTIONS | nvarchar(200) | Y |  |  |  |
| 7 | DENYACTIONS | nvarchar(200) | Y |  |  |  |
| 8 | OBJECTPERMISSIONID | nvarchar(100) | Y |  |  |  |
| 9 | USEOBJECTID | nvarchar(100) | Y |  |  |  |
| 10 | OBJECTID | nvarchar(100) | Y |  |  |  |

### OPACTORVALUE

**行数** 5,400 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OBJECTPERMISSIONID | nvarchar(100) | Y |  |  |  |
| 7 | ACTORVALUE | nvarchar(100) | Y |  |  |  |
| 8 | ACTORTYPE | nvarchar(100) | Y |  |  |  |
| 9 | USEOBJECTID | nvarchar(100) | Y |  |  |  |
| 10 | OBJECTID | nvarchar(100) | Y |  |  |  |

### OPERATELOG

**行数** 867,105 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | USERID | nvarchar(100) | Y |  |  |  |
| 7 | OBJNAME | nvarchar(2000) | Y |  |  |  |
| 8 | OBJCODE | nvarchar(2000) | Y |  |  |  |
| 9 | OBJTYPE | nvarchar(100) | Y |  |  |  |
| 10 | OBJID | nvarchar(100) | Y |  |  |  |
| 11 | OPERATE | nvarchar(100) | Y |  |  |  |
| 12 | DESCRIPTION | ntext | Y |  |  |  |
| 13 | IP | nvarchar(100) | Y |  |  |  |
| 14 | ENABLE | bit | Y | (N'1') |  |  |
| 15 | SUBJECTSECURITYLEVEL | nvarchar(20) | Y |  |  |  |
| 16 | OBJECTSECURITYLEVEL | nvarchar(20) | Y |  |  |  |
| 17 | OPERATELOGCLASSIFY | nvarchar(20) | Y | (N'Normal') |  |  |

### OPERATIONSPECIFICATION

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | OPERATIONITEM | nvarchar(2000) | Y |  |  |  |
| 16 | OPERATIONCONTENT | nvarchar(2000) | Y |  |  |  |

### OPOBJECTID

**行数** 28,838 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OBJECTPERMISSIONID | nvarchar(100) | Y |  |  |  |
| 7 | OBJECTID | nvarchar(100) | Y |  |  |  |
| 8 | USEOBJECTID | nvarchar(100) | Y |  |  |  |

### OPTIONRESTRAIN

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | OWNUID | nvarchar(50) | Y |  |  |  |
| 7 | OWNGID | nvarchar(50) | Y |  |  |  |
| 8 | ESTATION | nvarchar(100) | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | CODEEIDONE | nvarchar(450) |  |  |  |  |
| 12 | VALUEEIDONE | nvarchar(50) |  |  |  |  |
| 13 | CODEEIDTWO | nvarchar(450) |  |  |  |  |
| 14 | VALUEEIDTWO | nvarchar(50) |  |  |  |  |
| 15 | RESTRAINTY | nvarchar(100) |  |  |  |  |
| 16 | PROMPT | nvarchar(2000) | Y |  |  |  |
| 17 | ITEMID | nvarchar(50) | Y |  |  |  |
| 18 | VARTYPEONE | nvarchar(20) | Y |  |  |  |
| 19 | VARTYPETWO | nvarchar(20) | Y |  |  |  |

### ORGANIZE

**行数** 403 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | PEUID | nvarchar(100) | Y |  |  |  |
| 10 | CEUID | nvarchar(100) | Y |  |  |  |
| 11 | GROUPID | nvarchar(100) | Y |  |  |  |

### PAGEMAINCONFIG

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | PAGENAME | nvarchar(100) | Y |  |  |  |
| 10 | DATASOURCEASSEMBLY | nvarchar(100) | Y |  |  |  |
| 11 | DATASOURCECLASS | nvarchar(200) | Y |  |  |  |
| 12 | MAINCONFIGPARA | nvarchar(max) | Y |  |  |  |
| 13 | ENABLE | bit | Y | (N'1') |  |  |

### PANELCHART

**行数** 3 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | TAB | nvarchar(100) | Y |  |  |  |
| 10 | MOBILEPAGEDEVID | nvarchar(100) | Y |  |  |  |
| 11 | TITLE | nvarchar(100) | Y |  |  |  |
| 12 | FILTERSTRING | nvarchar(500) | Y |  |  |  |
| 13 | TABASSEMBLY | nvarchar(100) | Y |  |  |  |
| 14 | TABCLASSNAME | nvarchar(100) | Y |  |  |  |
| 15 | ASSEMBLY | nvarchar(100) | Y |  |  |  |
| 16 | CLASSNAME | nvarchar(100) | Y |  |  |  |
| 17 | SQLCOMMANDNAME | nvarchar(100) | Y |  |  |  |
| 18 | RESULTOBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 19 | SEQUENCE | int |  |  |  |  |
| 20 | ITEMTITLE | nvarchar(100) | Y |  |  |  |
| 21 | ITEMSUBTITLE | nvarchar(100) | Y |  |  |  |
| 22 | LISTITEMSTATE | nvarchar(100) | Y |  |  |  |
| 23 | LISTITEMSTATEICON | nvarchar(100) | Y |  |  |  |
| 24 | ITEMSUBTITLETWO | nvarchar(100) | Y |  |  |  |
| 25 | LISTITEMOBJECTMANAGER | nvarchar(100) | Y |  |  |  |
| 26 | PROPERTYCOLUMN | nvarchar(100) | Y |  |  |  |
| 27 | PROPERTYASSEMBLY | nvarchar(100) | Y |  |  |  |
| 28 | PROPERTYCLASSNAME | nvarchar(100) | Y |  |  |  |
| 29 | ISROOTQUERY | bit | Y |  |  |  |
| 30 | ENABLE | bit | Y | (N'1') |  |  |
| 31 | CLASSIFYNAME | nvarchar(100) | Y |  |  |  |
| 32 | CHARTTYPE | nvarchar(100) | Y |  |  |  |

### PARTIALCACHEINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | CACHENAME | nvarchar(100) | Y |  |  |  |
| 7 | CACHEVERSION | int | Y |  |  |  |

### PATCH

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | PATCHID | nvarchar(100) | Y |  |  |  |
| 12 | PATCHNUM | nvarchar(100) | Y |  |  |  |
| 13 | PATCHHASHCODE | nvarchar(100) | Y |  |  |  |
| 14 | FAILUREREASON | nvarchar(20) | Y |  |  |  |
| 15 | ISRECOVERYPOINT | nvarchar(20) | Y |  |  |  |
| 16 | PATCHPATH | ntext | Y |  |  |  |
| 17 | APPNAME | ntext | Y |  |  |  |
| 18 | APPCODE | ntext | Y |  |  |  |
| 19 | APPVERSION | nvarchar(100) | Y |  |  |  |
| 20 | PATCHTYPE | nvarchar(20) | Y |  |  |  |
| 21 | PATCHCATEGORY | nvarchar(20) | Y |  |  |  |
| 22 | PUBLISHTIME | nvarchar(20) | Y |  |  |  |
| 23 | IMPORTANCE | nvarchar(20) | Y |  |  |  |
| 24 | BUGCODE | nvarchar(100) | Y |  |  |  |
| 25 | ISINITPATCH | nvarchar(20) | Y |  |  |  |

### PATCHDEPENDENCELINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### PATCHDEPENDENCELINKINSTALL

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |
| 9 | ECODE | nvarchar(100) | Y |  |  |  |
| 10 | ENAME | nvarchar(100) | Y |  |  |  |

### PATCHINSTALL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | PATCHID | nvarchar(100) | Y |  |  |  |
| 12 | PATCHNUM | nvarchar(100) | Y |  |  |  |
| 13 | PATCHHASHCODE | nvarchar(100) | Y |  |  |  |
| 14 | FAILUREREASON | nvarchar(20) | Y |  |  |  |
| 15 | ISRECOVERYPOINT | nvarchar(20) | Y |  |  |  |
| 16 | PATCHPATH | ntext | Y |  |  |  |
| 17 | APPNAME | ntext | Y |  |  |  |
| 18 | APPCODE | ntext | Y |  |  |  |
| 19 | APPVERSION | nvarchar(100) | Y |  |  |  |
| 20 | PATCHTYPE | nvarchar(20) | Y |  |  |  |
| 21 | PATCHCATEGORY | nvarchar(20) | Y |  |  |  |
| 22 | PUBLISHTIME | nvarchar(20) | Y |  |  |  |
| 23 | IMPORTANCE | nvarchar(20) | Y |  |  |  |
| 24 | BUGCODE | nvarchar(100) | Y |  |  |  |
| 25 | ISINITPATCH | nvarchar(20) | Y |  |  |  |
| 26 | PATCHVERSION | nvarchar(100) | Y |  |  |  |
| 27 | UPLOADTIME | nvarchar(100) | Y |  |  |  |
| 28 | INSTALLTIME | nvarchar(100) | Y |  |  |  |
| 29 | OBJECTJSONSTR | ntext | Y |  |  |  |

### PCHAR

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | OBJECTID | nvarchar(100) |  |  |  |  |
| 7 | ATTNAME | nvarchar(50) | Y |  |  |  |
| 8 | VALUE | nvarchar(2000) | Y |  |  |  |
| 9 | XH | int | Y |  |  |  |
| 10 | PTYPE | nvarchar(100) | Y |  |  |  |

### PERSON

**行数** 248 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ICON | nvarchar(200) | Y | (N'p_userobject') |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |
| 10 | GENDER | nvarchar(20) | Y |  |  |  |
| 11 | BIRTHDAY | bigint | Y |  |  |  |
| 12 | ACADEMICRECORD | nvarchar(20) | Y |  |  |  |
| 13 | PHONE | nvarchar(20) | Y |  |  |  |
| 14 | OFFICENUMBER | nvarchar(20) | Y |  |  |  |
| 15 | FAXNUMBER | nvarchar(20) | Y |  |  |  |
| 16 | EMAIL | nvarchar(100) | Y |  |  |  |
| 17 | ZIPCODE | nvarchar(20) | Y |  |  |  |
| 18 | ADDRESS | nvarchar(500) | Y |  |  |  |

### PLMENTITY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |

### PLMENTITYITERATION

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 11 | CREATETIME | bigint | Y |  |  |  |
| 12 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 13 | UPDATETIME | bigint | Y |  |  |  |
| 14 | OWNUID | nvarchar(50) | Y |  |  |  |
| 15 | OWNGID | nvarchar(50) | Y |  |  |  |
| 16 | ESTATION | nvarchar(100) | Y |  |  |  |
| 17 | ICON | nvarchar(200) | Y |  |  |  |
| 18 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 19 | SERIALNUMBER | int | Y |  |  |  |
| 20 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 21 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 22 | RELEASEDATE | bigint | Y |  |  |  |
| 23 | CURITERATION | bigint | Y | (N'1') |  |  |
| 24 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 25 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 26 | CHECKOUTDATE | bigint | Y |  |  |  |

### PLMFORM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |

### PLMLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### PLMSTATISTICS

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | SHOWVIEW | nvarchar(100) | Y |  |  |  |
| 10 | STATISTICSOBJECT | ntext | Y |  |  |  |
| 11 | ORGANIZATIONROLE | ntext | Y |  |  |  |
| 12 | ROOTEID | nvarchar(100) | Y |  |  |  |
| 13 | XBAR | nvarchar(100) | Y |  |  |  |
| 14 | XGROUP | nvarchar(100) | Y |  |  |  |

### PLMSTATISTICSLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### PLMSTATISTICSQUERYROLE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | OBJECTTYPENAME | nvarchar(100) | Y |  |  |  |
| 9 | COLTYPE | nvarchar(100) | Y |  |  |  |
| 10 | OPERATETYPE | nvarchar(100) | Y |  |  |  |
| 11 | COLNAME | nvarchar(100) | Y |  |  |  |
| 12 | CONDITIONS | nvarchar(100) | Y |  |  |  |
| 13 | RESULTVALUE | nvarchar(100) | Y |  |  |  |
| 14 | STATISTICSID | nvarchar(100) | Y |  |  |  |
| 15 | SORTNO | int |  |  |  |  |

### PLMUNIT

**行数** 28 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | INUSE | int | Y | (N'1') |  |  |
| 9 | UNITTYPE | nvarchar(100) | Y |  |  |  |
| 10 | CONVERSION | float | Y |  |  |  |

### PMTASK

**行数** 11,203 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ICON | nvarchar(200) | Y |  |  |  |
| 17 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 18 | SERIALNUMBER | int | Y |  |  |  |
| 19 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 20 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 21 | TASKTYPE | nvarchar(100) |  | (N'PMNORMALTASKFORM') |  |  |
| 22 | PRINCIPAL | nvarchar(2000) | Y |  |  |  |
| 23 | PLANSTARTDATE | bigint | Y |  |  |  |
| 24 | PLANENDDATE | bigint | Y |  |  |  |
| 25 | ACTSTARTDATE | bigint | Y |  |  |  |
| 26 | ACTENDDATE | bigint | Y |  |  |  |
| 27 | PLANPERIOD | nvarchar(20) | Y | (N'1') |  |  |
| 28 | ACTPERIOD | float | Y |  |  |  |
| 29 | PLANTASKTIME | float | Y |  |  |  |
| 30 | ACTWORKTIME | float | Y |  |  |  |
| 31 | FORMID | nvarchar(100) | Y |  |  |  |
| 32 | TASKPERCENT | float |  | (N'0') |  |  |
| 33 | TASKCHECKER | nvarchar(2000) | Y |  |  |  |
| 34 | TASKSTATE | nvarchar(100) |  | (N'New') |  |  |
| 35 | PRIORITY | nvarchar(20) | Y |  |  |  |
| 36 | EXAMINE | nvarchar(1000) | Y |  |  |  |
| 37 | ISMILESTONE | bit | Y | (N'0') |  |  |
| 38 | ISCYCLETASK | bit | Y | (N'0') |  |  |
| 39 | ISLEAFTASK | nvarchar(100) | Y | (N'1') |  |  |
| 40 | HASSEND | bit | Y | (N'0') |  |  |
| 41 | ISAUTHORITYRESOLVE | bit | Y | (N'0') |  |  |
| 42 | LIMITTYPE | nvarchar(20) | Y |  |  |  |
| 43 | LIMITDATETIME | bigint | Y |  |  |  |
| 44 | VIEWUSERIDS | nvarchar(2000) | Y |  |  |  |
| 45 | ISTASKPUBLISH | int | Y | (N'0') |  |  |
| 46 | TASKPUBLISHDATE | bigint | Y |  |  |  |
| 47 | RECEIVEDATETIME | bigint | Y |  |  |  |
| 48 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 49 | PRETASKSTATE | nvarchar(100) | Y |  |  |  |
| 50 | PARENTTASKID | nvarchar(100) | Y |  |  |  |
| 51 | OUTLINENUM | nvarchar(2000) | Y |  |  |  |
| 52 | TASKLEVEL | int | Y |  |  |  |
| 53 | ISKEY | bit | Y | (N'0') |  |  |
| 54 | TASKPCTCOMPPLAN | float | Y | (N'0') |  |  |
| 55 | EARLIESTSTARTTIME | nvarchar(20) | Y |  |  |  |
| 56 | EARLISTENDTIME | nvarchar(20) | Y |  |  |  |
| 57 | LATESTARTDATE | nvarchar(20) | Y |  |  |  |
| 58 | LATEFINISHDATE | nvarchar(20) | Y |  |  |  |
| 59 | ISLOCK | bit | Y | (N'0') |  |  |
| 60 | CUREDITUSER | nvarchar(100) | Y |  |  |  |
| 61 | CHECKOUTDATE | bigint | Y |  |  |  |
| 62 | LASTCHECKOUTTIME | bigint | Y |  |  |  |
| 63 | FINISHPERCENT | nvarchar(100) | Y |  |  |  |
| 64 | WBSSTARTTIME | nvarchar(100) | Y |  |  |  |
| 65 | WBSENDTIME | nvarchar(100) | Y |  |  |  |
| 66 | PLANPERIODDAYS | float | Y | (N'1') |  |  |
| 67 | PRINCIPALATTACH | nvarchar(200) | Y |  |  |  |
| 68 | TASKMODE | nvarchar(100) | Y | (N'AUTO_SCHEDULED') |  |  |
| 69 | TASKREMARK | nvarchar(1000) | Y |  |  |  |
| 70 | WORKINGHOUR | nvarchar(100) | Y |  |  |  |
| 71 | BASEPLANSTARTDATE | bigint | Y |  |  |  |
| 72 | BASEPLANENDDATE | bigint | Y |  |  |  |
| 73 | BREAKTASKSTATUS | nvarchar(100) | Y |  |  |  |
| 74 | BREAKCHILDTASKSTATUS | nvarchar(100) | Y |  |  |  |
| 75 | PARENTPATH | nvarchar(max) | Y |  |  |  |
| 76 | PROJECTROLE | nvarchar(100) | Y |  |  |  |
| 77 | PLANOFFSET | float | Y |  |  |  |
| 78 | WORKHOURSPERDAY | nvarchar(100) | Y |  |  |  |
| 79 | TASKDOC | nvarchar(2000) | Y |  |  |  |
| 80 | MUSTHASDOC | bit | Y | (N'0') |  |  |
| 81 | TASKDOCIMAGE | nvarchar(100) | Y |  |  |  |
| 82 | ISMULTIRES | int | Y |  |  |  |
| 83 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 84 | IMPOWERUSER | nvarchar(2000) | Y |  |  |  |
| 85 | IMPOWERUSERNAME | nvarchar(100) | Y |  |  |  |
| 86 | IMPOWERTOUSER | nvarchar(100) | Y |  |  |  |
| 87 | IMPOWERTOUSERNAME | nvarchar(100) | Y |  |  |  |
| 88 | IMPOWERDATE | nvarchar(100) | Y |  |  |  |
| 89 | OLDTASKEID | nvarchar(100) | Y |  |  |  |
| 90 | OLDPARENTTASKID | nvarchar(100) | Y |  |  |  |
| 91 | INPUTDOC | nvarchar(2000) | Y |  |  |  |
| 92 | MSETASKTYPE | nvarchar(100) | Y |  |  |  |
| 93 | ERPTASKID | nvarchar(100) | Y |  |  |  |

### PMTASKBASEFORM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | TASKTYPE | nvarchar(100) | Y |  |  |  |
| 8 | TASKID | nvarchar(100) | Y |  |  |  |
| 9 | PROPERTY1 | nvarchar(100) | Y |  |  |  |
| 10 | ISMILESTONE | bit | Y | (N'0') |  |  |
| 11 | ISCYCLETASK | bit | Y | (N'0') |  |  |
| 12 | ISLEAFTASK | nvarchar(100) | Y |  |  |  |
| 13 | ISAUTHORITYRESOLVE | bit | Y |  |  |  |
| 14 | LIMITTYPE | nvarchar(20) | Y |  |  |  |
| 15 | LIMITDATETIME | bigint | Y |  |  |  |

### PMTASKCOPY

**行数** 12,978 ｜ **主键** EID, COPYBATCHNO

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ICON | nvarchar(200) | Y |  |  |  |
| 17 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 18 | SERIALNUMBER | int | Y |  |  |  |
| 19 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 20 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 21 | TASKTYPE | nvarchar(100) |  | (N'PMNORMALTASKFORM') |  |  |
| 22 | PRINCIPAL | nvarchar(2000) | Y |  |  |  |
| 23 | PLANSTARTDATE | bigint | Y |  |  |  |
| 24 | PLANENDDATE | bigint | Y |  |  |  |
| 25 | ACTSTARTDATE | bigint | Y |  |  |  |
| 26 | ACTENDDATE | bigint | Y |  |  |  |
| 27 | PLANPERIOD | nvarchar(20) | Y | (N'1') |  |  |
| 28 | ACTPERIOD | float | Y |  |  |  |
| 29 | PLANTASKTIME | float | Y |  |  |  |
| 30 | ACTWORKTIME | float | Y |  |  |  |
| 31 | FORMID | nvarchar(100) | Y |  |  |  |
| 32 | TASKPERCENT | float |  | (N'0') |  |  |
| 33 | TASKCHECKER | nvarchar(2000) | Y |  |  |  |
| 34 | TASKSTATE | nvarchar(100) |  | (N'New') |  |  |
| 35 | PRIORITY | nvarchar(20) | Y |  |  |  |
| 36 | EXAMINE | nvarchar(1000) | Y |  |  |  |
| 37 | ISMILESTONE | bit | Y | (N'0') |  |  |
| 38 | ISCYCLETASK | bit | Y | (N'0') |  |  |
| 39 | ISLEAFTASK | nvarchar(100) | Y | (N'1') |  |  |
| 40 | HASSEND | bit | Y | (N'0') |  |  |
| 41 | ISAUTHORITYRESOLVE | bit | Y | (N'0') |  |  |
| 42 | LIMITTYPE | nvarchar(20) | Y |  |  |  |
| 43 | LIMITDATETIME | bigint | Y |  |  |  |
| 44 | VIEWUSERIDS | nvarchar(2000) | Y |  |  |  |
| 45 | ISTASKPUBLISH | int | Y | (N'0') |  |  |
| 46 | TASKPUBLISHDATE | bigint | Y |  |  |  |
| 47 | RECEIVEDATETIME | bigint | Y |  |  |  |
| 48 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 49 | PRETASKSTATE | nvarchar(100) | Y |  |  |  |
| 50 | PARENTTASKID | nvarchar(100) | Y |  |  |  |
| 51 | OUTLINENUM | nvarchar(2000) | Y |  |  |  |
| 52 | TASKLEVEL | int | Y |  |  |  |
| 53 | ISKEY | bit | Y | (N'0') |  |  |
| 54 | TASKPCTCOMPPLAN | float | Y | (N'0') |  |  |
| 55 | EARLIESTSTARTTIME | nvarchar(20) | Y |  |  |  |
| 56 | EARLISTENDTIME | nvarchar(20) | Y |  |  |  |
| 57 | LATESTARTDATE | nvarchar(20) | Y |  |  |  |
| 58 | LATEFINISHDATE | nvarchar(20) | Y |  |  |  |
| 59 | ISLOCK | bit | Y | (N'0') |  |  |
| 60 | CUREDITUSER | nvarchar(100) | Y |  |  |  |
| 61 | CHECKOUTDATE | bigint | Y |  |  |  |
| 62 | LASTCHECKOUTTIME | bigint | Y |  |  |  |
| 63 | FINISHPERCENT | nvarchar(100) | Y |  |  |  |
| 64 | WBSSTARTTIME | nvarchar(100) | Y |  |  |  |
| 65 | WBSENDTIME | nvarchar(100) | Y |  |  |  |
| 66 | PLANPERIODDAYS | float | Y | (N'1') |  |  |
| 67 | PRINCIPALATTACH | nvarchar(200) | Y |  |  |  |
| 68 | TASKMODE | nvarchar(100) | Y | (N'AUTO_SCHEDULED') |  |  |
| 69 | TASKREMARK | nvarchar(1000) | Y |  |  |  |
| 70 | WORKINGHOUR | nvarchar(100) | Y |  |  |  |
| 71 | BASEPLANSTARTDATE | bigint | Y |  |  |  |
| 72 | BASEPLANENDDATE | bigint | Y |  |  |  |
| 73 | BREAKTASKSTATUS | nvarchar(100) | Y |  |  |  |
| 74 | BREAKCHILDTASKSTATUS | nvarchar(100) | Y |  |  |  |
| 75 | PARENTPATH | nvarchar(max) | Y |  |  |  |
| 76 | PROJECTROLE | nvarchar(100) | Y |  |  |  |
| 77 | PLANOFFSET | float | Y |  |  |  |
| 78 | WORKHOURSPERDAY | nvarchar(100) | Y |  |  |  |
| 79 | TASKDOC | nvarchar(2000) | Y |  |  |  |
| 80 | MUSTHASDOC | bit | Y | (N'0') |  |  |
| 81 | TASKDOCIMAGE | nvarchar(100) | Y |  |  |  |
| 82 | ISMULTIRES | int | Y |  |  |  |
| 83 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 84 | IMPOWERUSER | nvarchar(2000) | Y |  |  |  |
| 85 | IMPOWERUSERNAME | nvarchar(100) | Y |  |  |  |
| 86 | IMPOWERTOUSER | nvarchar(100) | Y |  |  |  |
| 87 | IMPOWERTOUSERNAME | nvarchar(100) | Y |  |  |  |
| 88 | IMPOWERDATE | nvarchar(100) | Y |  |  |  |
| 89 | OLDTASKEID | nvarchar(100) | Y |  |  |  |
| 90 | OLDPARENTTASKID | nvarchar(100) | Y |  |  |  |
| 91 | INPUTDOC | nvarchar(2000) | Y |  |  |  |
| 92 | MSETASKTYPE | nvarchar(100) | Y |  |  |  |
| 93 | COPYBATCHNO | nvarchar(100) |  |  | PK |  |
| 94 | ERPTASKID | nvarchar(100) | Y |  |  |  |

### PMTASKLINE

**行数** 679 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ICON | nvarchar(200) | Y |  |  |  |
| 17 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 18 | SERIALNUMBER | int | Y |  |  |  |
| 19 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 20 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 21 | TASKTYPE | nvarchar(100) |  | (N'PMNORMALTASKFORM') |  |  |
| 22 | PRINCIPAL | nvarchar(2000) | Y |  |  |  |
| 23 | PLANSTARTDATE | bigint | Y |  |  |  |
| 24 | PLANENDDATE | bigint | Y |  |  |  |
| 25 | ACTSTARTDATE | bigint | Y |  |  |  |
| 26 | ACTENDDATE | bigint | Y |  |  |  |
| 27 | PLANPERIOD | nvarchar(20) | Y | (N'1') |  |  |
| 28 | ACTPERIOD | float | Y |  |  |  |
| 29 | PLANTASKTIME | float | Y |  |  |  |
| 30 | ACTWORKTIME | float | Y |  |  |  |
| 31 | FORMID | nvarchar(100) | Y |  |  |  |
| 32 | TASKPERCENT | float |  | (N'0') |  |  |
| 33 | TASKCHECKER | nvarchar(2000) | Y |  |  |  |
| 34 | TASKSTATE | nvarchar(100) |  | (N'New') |  |  |
| 35 | PRIORITY | nvarchar(20) | Y |  |  |  |
| 36 | EXAMINE | nvarchar(1000) | Y |  |  |  |
| 37 | ISMILESTONE | bit | Y | (N'0') |  |  |
| 38 | ISCYCLETASK | bit | Y | (N'0') |  |  |
| 39 | ISLEAFTASK | nvarchar(100) | Y | (N'1') |  |  |
| 40 | HASSEND | bit | Y | (N'0') |  |  |
| 41 | ISAUTHORITYRESOLVE | bit | Y | (N'0') |  |  |
| 42 | LIMITTYPE | nvarchar(20) | Y |  |  |  |
| 43 | LIMITDATETIME | bigint | Y |  |  |  |
| 44 | VIEWUSERIDS | nvarchar(2000) | Y |  |  |  |
| 45 | ISTASKPUBLISH | int | Y | (N'0') |  |  |
| 46 | TASKPUBLISHDATE | bigint | Y |  |  |  |
| 47 | RECEIVEDATETIME | bigint | Y |  |  |  |
| 48 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 49 | PRETASKSTATE | nvarchar(100) | Y |  |  |  |
| 50 | PARENTTASKID | nvarchar(100) | Y |  |  |  |
| 51 | OUTLINENUM | nvarchar(2000) | Y |  |  |  |
| 52 | TASKLEVEL | int | Y |  |  |  |
| 53 | ISKEY | bit | Y | (N'0') |  |  |
| 54 | TASKPCTCOMPPLAN | float | Y | (N'0') |  |  |
| 55 | EARLIESTSTARTTIME | nvarchar(20) | Y |  |  |  |
| 56 | EARLISTENDTIME | nvarchar(20) | Y |  |  |  |
| 57 | LATESTARTDATE | nvarchar(20) | Y |  |  |  |
| 58 | LATEFINISHDATE | nvarchar(20) | Y |  |  |  |
| 59 | ISLOCK | bit | Y | (N'0') |  |  |
| 60 | CUREDITUSER | nvarchar(100) | Y |  |  |  |
| 61 | CHECKOUTDATE | bigint | Y |  |  |  |
| 62 | LASTCHECKOUTTIME | bigint | Y |  |  |  |
| 63 | FINISHPERCENT | nvarchar(100) | Y |  |  |  |
| 64 | WBSSTARTTIME | nvarchar(100) | Y |  |  |  |
| 65 | WBSENDTIME | nvarchar(100) | Y |  |  |  |
| 66 | PLANPERIODDAYS | float | Y | (N'1') |  |  |
| 67 | PRINCIPALATTACH | nvarchar(200) | Y |  |  |  |
| 68 | TASKMODE | nvarchar(100) | Y | (N'AUTO_SCHEDULED') |  |  |
| 69 | TASKREMARK | nvarchar(1000) | Y |  |  |  |
| 70 | WORKINGHOUR | nvarchar(100) | Y |  |  |  |
| 71 | BASEPLANSTARTDATE | bigint | Y |  |  |  |
| 72 | BASEPLANENDDATE | bigint | Y |  |  |  |
| 73 | BREAKTASKSTATUS | nvarchar(100) | Y |  |  |  |
| 74 | BREAKCHILDTASKSTATUS | nvarchar(100) | Y |  |  |  |
| 75 | PARENTPATH | nvarchar(max) | Y |  |  |  |
| 76 | PROJECTROLE | nvarchar(100) | Y |  |  |  |
| 77 | PLANOFFSET | float | Y |  |  |  |
| 78 | WORKHOURSPERDAY | nvarchar(100) | Y |  |  |  |
| 79 | TASKDOC | nvarchar(2000) | Y |  |  |  |
| 80 | MUSTHASDOC | bit | Y | (N'0') |  |  |
| 81 | TASKDOCIMAGE | nvarchar(100) | Y |  |  |  |
| 82 | ISMULTIRES | int | Y |  |  |  |
| 83 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 84 | IMPOWERUSER | nvarchar(2000) | Y |  |  |  |
| 85 | IMPOWERUSERNAME | nvarchar(100) | Y |  |  |  |
| 86 | IMPOWERTOUSER | nvarchar(100) | Y |  |  |  |
| 87 | IMPOWERTOUSERNAME | nvarchar(100) | Y |  |  |  |
| 88 | IMPOWERDATE | nvarchar(100) | Y |  |  |  |
| 89 | OLDTASKEID | nvarchar(100) | Y |  |  |  |
| 90 | OLDPARENTTASKID | nvarchar(100) | Y |  |  |  |
| 91 | INPUTDOC | nvarchar(2000) | Y |  |  |  |
| 92 | MSETASKTYPE | nvarchar(100) | Y |  |  |  |
| 93 | PMTASKEID | nvarchar(100) | Y |  |  |  |
| 94 | ERPTASKID | nvarchar(100) | Y |  |  |  |

### PMTASKOUTPUTRELATION

**行数** 501 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | WEIGHT | nvarchar(20) | Y | (N'0') |  |  |
| 16 | LINKTYPE | nvarchar(100) | Y |  |  |  |
| 17 | REMARK2 | nvarchar(2000) | Y |  |  |  |
| 18 | REMARK | nvarchar(100) | Y |  |  |  |

### PMTASKRELATION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | VERSIONEID | nvarchar(100) | Y |  |  |  |
| 16 | LINKTYPE | nvarchar(100) | Y |  |  |  |

### PMTASKTEMPLATE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ESTATION | nvarchar(100) | Y |  |  |  |
| 8 | ICON | nvarchar(200) | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 12 | TASKTYPE | nvarchar(100) | Y | (N'PMNORMALTASKFORM') |  |  |
| 13 | PRINCIPAL | nvarchar(250) | Y |  |  |  |
| 14 | TASKCHECKER | nvarchar(2000) | Y |  |  |  |
| 15 | PRIORITY | nvarchar(20) | Y |  |  |  |
| 16 | PLANPERIOD | nvarchar(20) | Y | (N'1') |  |  |
| 17 | PLANTASKTIME | float | Y |  |  |  |

### POSITIONLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | VERSIONEID | nvarchar(100) | Y |  |  |  |

### POSITIONRELATION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | RESOURCECLASSID | nvarchar(100) | Y |  |  |  |

### POSITIONUNIT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | LOGISTICSUNIT | nvarchar(100) |  | (N'N') |  |  |
| 16 | POSITIONLEVEL | nvarchar(100) |  |  |  |  |

### PRLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### PROCESSATTACHDOC

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 8 | CREATETIME | bigint | Y |  |  |  |
| 9 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 10 | UPDATETIME | bigint | Y |  |  |  |
| 11 | OWNUID | nvarchar(50) | Y |  |  |  |
| 12 | OWNGID | nvarchar(50) | Y |  |  |  |
| 13 | ESTATION | nvarchar(100) | Y |  |  |  |
| 14 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 15 | SERIALNUMBER | int | Y |  |  |  |
| 16 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 17 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 18 | FILEINFO | nvarchar(max) |  |  |  |  |
| 19 | KEYWORDS | nvarchar(200) | Y |  |  |  |

### PROCESSROUTE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | OWNUID | nvarchar(50) | Y |  |  |  |
| 7 | OWNGID | nvarchar(50) | Y |  |  |  |
| 8 | ESTATION | nvarchar(100) | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | PARENTCODE | nvarchar(100) | Y | (N'##') |  |  |
| 12 | FITPRODUCT | nvarchar(100) | Y | (N'##') |  |  |
| 13 | ALLROUTE | nvarchar(500) | Y | (N'##') |  |  |
| 14 | MASTERITEMID | nvarchar(100) | Y |  |  |  |
| 15 | ENAME | nvarchar(2000) | Y |  |  |  |
| 16 | ECODE | nvarchar(2000) | Y |  |  |  |
| 17 | LEVERNUM | nvarchar(100) | Y |  |  |  |
| 18 | ITEMNAME | nvarchar(200) | Y |  |  |  |
| 19 | ITEMCODE | nvarchar(100) | Y |  |  |  |

### PRODUCTCONFIGBILL

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | VALUE | nvarchar(100) | Y |  |  |  |
| 14 | VARTYPE | nvarchar(20) | Y |  |  |  |

### PRODUCTMATCH

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 12 | SERIALNUMBER | int | Y |  |  |  |
| 13 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 14 | BOMVIEW | nvarchar(100) |  |  |  |  |
| 15 | ITEMID | nvarchar(100) | Y |  |  |  |
| 16 | OPTIONCODE | nvarchar(200) | Y |  |  |  |
| 17 | CONFIGITEMID | nvarchar(100) | Y |  |  |  |

### PROJECT

**行数** 512 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 22 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 23 | RELEASEDATE | bigint | Y |  |  |  |
| 24 | PROJECTTYPE | nvarchar(100) |  |  |  |  |
| 25 | PROJECTMANAGER | nvarchar(100) |  |  |  |  |
| 26 | PROJECTLEVEL | nvarchar(100) |  |  |  |  |
| 27 | PLANSTARTDATE | nvarchar(100) |  |  |  |  |
| 28 | PLANENDDATE | nvarchar(100) |  |  |  |  |
| 29 | PLANSTATE | nvarchar(100) | Y | (N'Normal') |  |  |
| 30 | FORMID | nvarchar(100) | Y |  |  |  |
| 31 | EPS | nvarchar(100) |  |  |  |  |
| 32 | DEPARTMENT | nvarchar(100) |  |  |  |  |
| 33 | PLANSCHEDULE | int | Y |  |  |  |
| 34 | PERCENTCOMPLETE | int | Y |  |  |  |
| 35 | PROGRESSICON | nvarchar(200) | Y |  |  |  |
| 36 | PLANDEVIATIONRATE | int | Y |  |  |  |
| 37 | PLANPUBLISHSTATE | nvarchar(100) | Y | (N'No') |  |  |
| 38 | PUBLISHDATE | nvarchar(100) | Y |  |  |  |
| 39 | ACTSTARTDATE | nvarchar(100) | Y |  |  |  |
| 40 | ACTENDDATE | nvarchar(100) | Y |  |  |  |
| 41 | DELIVERYGENERATETYPE | nvarchar(100) |  | (N'WbsMode') |  |  |
| 42 | PROJECTTEMPLATE | nvarchar(100) | Y |  |  |  |
| 43 | PRESTAGESTATE | nvarchar(100) | Y |  |  |  |
| 44 | HASSAVEBYWBS | nvarchar(100) | Y |  |  |  |
| 45 | ZUZHIERP | nvarchar(100) | Y | (N'31') |  |  |
| 46 | SHIYONGZUZHI | nvarchar(100) | Y |  |  |  |
| 47 | CHUANGJIANZUZHI | nvarchar(100) | Y |  |  |  |
| 48 | SQZZ | nvarchar(100) | Y |  |  |  |

### PROJECTATTENTIONLINK

**行数** 7 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |

### PROJECTBASELINE

**行数** 16 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 16 | OWNPROJECTEUID | nvarchar(100) | Y |  |  |  |
| 17 | PLANVERSION | int |  |  |  |  |
| 18 | CREATETYPE | nvarchar(20) |  |  |  |  |
| 19 | PLANEID | nvarchar(100) | Y |  |  |  |

### PROJECTBASELINETOPLAN

**行数** 16 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | VERSIONEID | nvarchar(100) | Y |  |  |  |

### PROJECTCALENDAR

**行数** 4,899 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | PROJECTEID | nvarchar(100) | Y |  |  |  |
| 10 | TIMENAME | nvarchar(20) | Y |  |  |  |
| 11 | CALENDARTYPE | nvarchar(20) |  |  |  |  |
| 12 | SHIFT1START | nvarchar(50) | Y |  |  |  |
| 13 | SHIFT1FINISH | nvarchar(50) | Y |  |  |  |
| 14 | SHIFT2START | nvarchar(50) | Y |  |  |  |
| 15 | SHIFT2FINISH | nvarchar(50) | Y |  |  |  |
| 16 | SHIFT3START | nvarchar(50) | Y |  |  |  |
| 17 | SHIFT3FINISH | nvarchar(50) | Y |  |  |  |
| 18 | SHIFT4START | nvarchar(50) | Y |  |  |  |
| 19 | SHIFT4FINISH | nvarchar(50) | Y |  |  |  |
| 20 | SHIFT5START | nvarchar(50) | Y |  |  |  |
| 21 | SHIFT5FINISH | nvarchar(50) | Y |  |  |  |
| 22 | STARTDATE | nvarchar(100) | Y |  |  |  |
| 23 | ENDDATE | nvarchar(100) | Y |  |  |  |
| 24 | MINSPERDAY | nvarchar(100) | Y |  |  |  |

### PROJECTCALENDARTYPE

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | ISDEFAUTL | bit | Y |  |  |  |

### PROJECTCHANGERECORD

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 9 | EMENDTYPE | nvarchar(100) | Y |  |  |  |
| 10 | APPLYID | nvarchar(100) | Y |  |  |  |
| 11 | ISVALIDATE | bit | Y |  |  |  |

### PROJECTENTITY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |

### PROJECTEPS

**行数** 13 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | MANAGER | ntext |  |  |  |  |
| 16 | SEEUSER | ntext | Y |  |  |  |

### PROJECTFOLDER

**行数** 5,026 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | OWNPROJECTEUID | nvarchar(100) | Y |  |  |  |
| 16 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 17 | FOLDERTYPE | nvarchar(100) | Y |  |  |  |
| 18 | TYPEICON | nvarchar(200) | Y |  |  |  |

### PROJECTFOLDERLINK

**行数** 8,561 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### PROJECTFOLDERTEMPLATE

**行数** 71 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | STATEID | nvarchar(20) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 12 | SERIALNUMBER | int | Y |  |  |  |
| 13 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 14 | FOLDERTYPE | nvarchar(100) | Y |  |  |  |

### PROJECTFOLDERTEMPLATECLASSIFY

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |

### PROJECTFOLDERTEMPLATELINK

**行数** 55 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | PEUID | nvarchar(100) | Y |  |  |  |
| 10 | CEUID | nvarchar(100) | Y |  |  |  |
| 11 | VERSIONEID | nvarchar(100) | Y |  |  |  |
| 12 | REMARK | nvarchar(100) | Y |  |  |  |
| 13 | LINKTYPE | nvarchar(100) | Y |  |  |  |
| 14 | REMARK2 | nvarchar(100) | Y |  |  |  |

### PROJECTFORM

**行数** 523 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | PROJECTTYPE | nvarchar(100) | Y |  |  |  |
| 16 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 17 | EXTENDPROPERTYA | nvarchar(100) | Y |  |  |  |
| 18 | EXTENDPROPERTYB | nvarchar(100) | Y |  |  |  |
| 19 | EXTENDPROPERTYC | nvarchar(100) | Y |  |  |  |

### PROJECTGLOBLECALENDAR

**行数** 5,223 ｜ **主键** ENAME

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | ENAME | varchar(50) |  |  | PK |  |

### PROJECTINFOCHANGE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 16 | RELEASEDATE | bigint | Y |  |  |  |
| 17 | CHANGEREASON | nvarchar(100) | Y |  |  |  |
| 18 | CHANGECONTENT | nvarchar(500) | Y |  |  |  |
| 19 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 20 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 21 | CHANGETYPE | nvarchar(100) | Y |  |  |  |
| 22 | CHANGEFORMID | nvarchar(100) | Y |  |  |  |

### PROJECTINFOCHANGEAPPLY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 16 | RELEASEDATE | bigint | Y |  |  |  |
| 17 | CHANGEREASON | nvarchar(100) | Y |  |  |  |
| 18 | CHANGECONTENT | nvarchar(500) | Y |  |  |  |
| 19 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 20 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 21 | CHANGETYPE | nvarchar(100) | Y |  |  |  |
| 22 | CHANGEFORMID | nvarchar(100) | Y |  |  |  |

### PROJECTINFOCHANGEAPPLYFORM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | FORMTYPE | nvarchar(100) | Y |  |  |  |
| 16 | CHANGEID | nvarchar(100) | Y |  |  |  |
| 17 | EXTENDPROPERTYA | nvarchar(100) | Y |  |  |  |

### PROJECTINFOCHANGEFORM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | FORMTYPE | nvarchar(100) | Y |  |  |  |
| 16 | CHANGEID | nvarchar(100) | Y |  |  |  |
| 17 | EXTENDPROPERTYA | nvarchar(100) | Y |  |  |  |

### PROJECTLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |

### PROJECTPLAN

**行数** 512 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EUID | nvarchar(100) | Y |  |  |  |
| 8 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 9 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 10 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 11 | CREATETIME | bigint | Y |  |  |  |
| 12 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 13 | UPDATETIME | bigint | Y |  |  |  |
| 14 | OWNUID | nvarchar(50) | Y |  |  |  |
| 15 | OWNGID | nvarchar(50) | Y |  |  |  |
| 16 | ESTATION | nvarchar(100) | Y |  |  |  |
| 17 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 18 | SERIALNUMBER | int | Y |  |  |  |
| 19 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 20 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 21 | PLANPROCESS | int | Y |  |  |  |
| 22 | ACTUALPROCESS | int | Y |  |  |  |
| 23 | PROCESSDEVIATION | int | Y |  |  |  |
| 24 | PLANNUMBER | int | Y |  |  |  |

### PROJECTPLANCHANGE

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 16 | RELEASEDATE | bigint | Y |  |  |  |
| 17 | CHANGEREASON | nvarchar(100) | Y |  |  |  |
| 18 | CHANGECONTENT | nvarchar(500) | Y |  |  |  |
| 19 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 20 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 21 | CHANGETYPE | nvarchar(100) | Y |  |  |  |
| 22 | CHANGEFORMID | nvarchar(100) | Y |  |  |  |

### PROJECTPLANCHANGEAPPLY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 16 | RELEASEDATE | bigint | Y |  |  |  |
| 17 | CHANGEREASON | nvarchar(100) | Y |  |  |  |
| 18 | CHANGECONTENT | nvarchar(500) | Y |  |  |  |
| 19 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 20 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 21 | CHANGETYPE | nvarchar(100) | Y |  |  |  |
| 22 | CHANGEFORMID | nvarchar(100) | Y |  |  |  |

### PROJECTPLANCHANGEAPPLYFORM

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | FORMTYPE | nvarchar(100) | Y |  |  |  |
| 16 | CHANGEID | nvarchar(100) | Y |  |  |  |
| 17 | EXTENDPROPERTYA | nvarchar(100) | Y |  |  |  |

### PROJECTPLANCHANGEFORM

**行数** 6 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | FORMTYPE | nvarchar(100) | Y |  |  |  |
| 16 | CHANGEID | nvarchar(100) | Y |  |  |  |
| 17 | EXTENDPROPERTYA | nvarchar(100) | Y |  |  |  |

### PROJECTPLANFLOW

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | FLOWID | nvarchar(100) | Y |  |  |  |
| 10 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 11 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 12 | TASKID | nvarchar(100) | Y |  |  |  |
| 13 | ISPUBLISHALL | bit | Y |  |  |  |
| 14 | TASKLEVEL | nvarchar(100) | Y |  |  |  |
| 15 | ISVALID | nvarchar(100) | Y |  |  |  |
| 16 | ISPROJECT | bit | Y |  |  |  |

### PROJECTPLANLINE

**行数** 16 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EUID | nvarchar(100) | Y |  |  |  |
| 8 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 9 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 10 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 11 | CREATETIME | bigint | Y |  |  |  |
| 12 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 13 | UPDATETIME | bigint | Y |  |  |  |
| 14 | OWNUID | nvarchar(50) | Y |  |  |  |
| 15 | OWNGID | nvarchar(50) | Y |  |  |  |
| 16 | ESTATION | nvarchar(100) | Y |  |  |  |
| 17 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 18 | SERIALNUMBER | int | Y |  |  |  |
| 19 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 20 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 21 | PLANPROCESS | int | Y |  |  |  |
| 22 | ACTUALPROCESS | int | Y |  |  |  |
| 23 | PROCESSDEVIATION | int | Y |  |  |  |
| 24 | PLANNUMBER | int | Y |  |  |  |
| 25 | PLANEID | nvarchar(100) | Y |  |  |  |

### PROJECTPLANTOPLAN

**行数** 512 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### PROJECTRESOUSCELINK

**行数** 755 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | PROJECTROLE | nvarchar(50) | Y | (N'PROJECTROLEINFO_MEMBER') |  |  |

### PROJECTROLEINFO

**行数** 14 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | ISINNATE | bit | Y |  |  |  |

### PROJECTROLELINK

**行数** 2,098 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |

### PROJECTSUSPEND

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | SUSPENDREASON | nvarchar(100) | Y |  |  |  |
| 16 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 17 | WORKFLOWID | nvarchar(100) | Y |  |  |  |

### PROJECTTASKADDWORKTIME

**行数** 73 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | WORKTIME | nvarchar(100) | Y |  |  |  |
| 17 | USERID | nvarchar(100) | Y |  |  |  |
| 18 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |

### PROJECTTASKLINK

**行数** 3 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | LINKTYPE | nvarchar(100) | Y |  |  |  |
| 17 | DELAYTIME | nvarchar(100) | Y |  |  |  |
| 18 | LPROJECTPLANID | nvarchar(100) | Y |  |  |  |

### PROJECTTASKLINKCOPY

**行数** 88 ｜ **主键** LID, COPYBATCHNO

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | LINKTYPE | nvarchar(100) | Y |  |  |  |
| 17 | DELAYTIME | nvarchar(100) | Y |  |  |  |
| 18 | LPROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 19 | COPYBATCHNO | nvarchar(100) |  |  | PK |  |

### PROJECTTASKLINKLINE

**行数** 3 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | LINKTYPE | nvarchar(100) | Y |  |  |  |
| 17 | DELAYTIME | nvarchar(100) | Y |  |  |  |
| 18 | LPROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 19 | PLANLINEEID | nvarchar(100) | Y |  |  |  |

### PROJECTTASKLOG

**行数** 607 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | TASKID | nvarchar(100) | Y |  |  |  |
| 7 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 8 | TASKPERCENT | nvarchar(20) |  | (N'0') |  |  |
| 9 | FINISHPERCENT | nvarchar(100) | Y |  |  |  |
| 10 | HASUPDATEPROJECT | int | Y | (N'0') |  |  |
| 11 | UPDATEPROJECTTIME | nvarchar(100) | Y |  |  |  |

### PROJECTTASKTEMPLATE

**行数** 514 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | PLANPERIODDAYS | nvarchar(20) |  |  |  |  |
| 16 | PLANTASKTIME | nvarchar(100) | Y |  |  |  |
| 17 | ISMILESTONE | nvarchar(100) | Y |  |  |  |
| 18 | ISCYCLETASK | nvarchar(100) | Y |  |  |  |
| 19 | ISLEAFTASK | nvarchar(100) | Y |  |  |  |
| 20 | ISAUTHORITYRESOLVE | nvarchar(100) | Y |  |  |  |
| 21 | LIMITTYPE | nvarchar(100) | Y |  |  |  |
| 22 | LIMITDATETIME | nvarchar(100) | Y |  |  |  |
| 23 | EXCUTOR | nvarchar(1000) | Y |  |  |  |
| 24 | ATTENTIONUSER | nvarchar(1000) | Y |  |  |  |
| 25 | PARENTTASKID | nvarchar(100) | Y |  |  |  |
| 26 | OUTLINENUM | nvarchar(2000) | Y |  |  |  |
| 27 | TASKLEVEL | int | Y |  |  |  |
| 28 | ISKEY | bit | Y |  |  |  |
| 29 | ROOTTASKID | nvarchar(100) | Y |  |  |  |
| 30 | PLANSTARTDATE | bigint | Y |  |  |  |
| 31 | PLANENDDATE | bigint | Y |  |  |  |
| 32 | PROJECTTEMPLATEID | nvarchar(100) | Y |  |  |  |
| 33 | PLANPERIOD | nvarchar(100) | Y |  |  |  |
| 34 | TASKMODE | nvarchar(100) | Y | (N'AUTO_SCHEDULED') |  |  |
| 35 | TASKREMARK | nvarchar(2000) | Y |  |  |  |
| 36 | PROJECTROLE | nvarchar(100) | Y |  |  |  |
| 37 | TASKDOC | nvarchar(2000) | Y |  |  |  |
| 38 | MUSTHASDOC | bit | Y | (N'0') |  |  |
| 39 | TASKDOCIMAGE | nvarchar(100) | Y |  |  |  |

### PROJECTTASKTEMPLATECOPY

**行数** 508 ｜ **主键** EID, COPYBATCHNO

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | PLANPERIODDAYS | nvarchar(20) |  |  |  |  |
| 16 | PLANTASKTIME | nvarchar(100) | Y |  |  |  |
| 17 | ISMILESTONE | nvarchar(100) | Y |  |  |  |
| 18 | ISCYCLETASK | nvarchar(100) | Y |  |  |  |
| 19 | ISLEAFTASK | nvarchar(100) | Y |  |  |  |
| 20 | ISAUTHORITYRESOLVE | nvarchar(100) | Y |  |  |  |
| 21 | LIMITTYPE | nvarchar(100) | Y |  |  |  |
| 22 | LIMITDATETIME | nvarchar(100) | Y |  |  |  |
| 23 | EXCUTOR | nvarchar(1000) | Y |  |  |  |
| 24 | ATTENTIONUSER | nvarchar(1000) | Y |  |  |  |
| 25 | PARENTTASKID | nvarchar(100) | Y |  |  |  |
| 26 | OUTLINENUM | nvarchar(2000) | Y |  |  |  |
| 27 | TASKLEVEL | int | Y |  |  |  |
| 28 | ISKEY | bit | Y |  |  |  |
| 29 | ROOTTASKID | nvarchar(100) | Y |  |  |  |
| 30 | PLANSTARTDATE | bigint | Y |  |  |  |
| 31 | PLANENDDATE | bigint | Y |  |  |  |
| 32 | PROJECTTEMPLATEID | nvarchar(100) | Y |  |  |  |
| 33 | PLANPERIOD | nvarchar(100) | Y |  |  |  |
| 34 | TASKMODE | nvarchar(100) | Y | (N'AUTO_SCHEDULED') |  |  |
| 35 | TASKREMARK | nvarchar(2000) | Y |  |  |  |
| 36 | PROJECTROLE | nvarchar(100) | Y |  |  |  |
| 37 | TASKDOC | nvarchar(2000) | Y |  |  |  |
| 38 | MUSTHASDOC | bit | Y | (N'0') |  |  |
| 39 | TASKDOCIMAGE | nvarchar(100) | Y |  |  |  |
| 40 | COPYBATCHNO | nvarchar(100) |  |  | PK |  |

### PROJECTTASKTEMPLATEFOLDER

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |

### PROJECTTASKTEMPLATELINK

**行数** 2 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | REMARK | nvarchar(100) | Y |  |  |  |
| 16 | LINKTYPE | nvarchar(100) | Y |  |  |  |
| 17 | REMARK2 | nvarchar(100) | Y |  |  |  |

### PROJECTTASKTORESLINE

**行数** 951 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | RATE | nvarchar(100) | Y |  |  |  |
| 17 | ISMAIN | nvarchar(20) | Y |  |  |  |
| 18 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 19 | PLANLINEEID | nvarchar(100) | Y |  |  |  |

### PROJECTTASKTORESOURCE

**行数** 2,702 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | RATE | nvarchar(100) | Y |  |  |  |
| 17 | ISMAIN | nvarchar(20) | Y |  |  |  |
| 18 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |

### PROJECTTASKTORESOURCECOPY

**行数** 1,891 ｜ **主键** LID, COPYBATCHNO

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | RATE | nvarchar(100) | Y |  |  |  |
| 17 | ISMAIN | nvarchar(20) | Y |  |  |  |
| 18 | PROJECTPLANID | nvarchar(100) | Y |  |  |  |
| 19 | COPYBATCHNO | nvarchar(100) |  |  | PK |  |

### PROJECTTEMPLATE

**行数** 5 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | PROJECTTYPE | nvarchar(100) | Y |  |  |  |
| 16 | PROJECTLEVEL | nvarchar(100) |  | (N'Province') |  |  |
| 17 | FOLDERTEMPLATE | nvarchar(100) | Y |  |  |  |
| 18 | FORMID | nvarchar(100) | Y |  |  |  |
| 19 | DEPARMENT | nvarchar(100) | Y |  |  |  |
| 20 | ISTOLERANT | nvarchar(100) | Y | (N'0') |  |  |

### PROJECTTEMPLATELINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |

### PROJECTTOBASELINE

**行数** 16 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |

### PROJECTTOPROJECTLINK

**行数** 2 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |

### PROJECTWBSCOLUMNSET

**行数** 18 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | WIDTH | nvarchar(100) | Y |  |  |  |
| 8 | BSHOW | nvarchar(100) | Y |  |  |  |
| 9 | WBSTYPE | nvarchar(100) | Y |  |  |  |

### PROJECTWORKDAY

**行数** 28,798 ｜ **主键** ENAME, PROJECTPLANID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | ENAME | varchar(50) |  |  | PK |  |
| 2 | PROJECTPLANID | varchar(200) |  |  | PK |  |

### PROUTE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 22 | RELEASEDATE | bigint | Y |  |  |  |
| 23 | CURITERATION | bigint | Y | (N'1') |  |  |
| 24 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 25 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 26 | CHECKOUTDATE | bigint | Y |  |  |  |
| 27 | PARENTCODE | nvarchar(100) | Y | (N'##') |  |  |
| 28 | FITPRODUCT | nvarchar(100) | Y | (N'##') |  |  |
| 29 | ALLROUTE | nvarchar(500) | Y | (N'##') |  |  |
| 30 | MASTERITEMID | nvarchar(100) | Y |  |  |  |
| 31 | LEVERNUM | nvarchar(100) | Y |  |  |  |
| 32 | ITEMNAME | nvarchar(200) | Y |  |  |  |
| 33 | ITEMCODE | nvarchar(100) | Y |  |  |  |
| 34 | VIEWIDSTR | nvarchar(100) | Y |  |  |  |
| 35 | MAINUNIT | nvarchar(2000) | Y |  |  |  |
| 36 | ROUTESUITABILITY | nvarchar(100) | Y |  |  |  |

### PSEBASEFORM

**行数** 116,037 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | ITEMTYPE | nvarchar(100) | Y |  |  |  |
| 8 | FORMTYPE | nvarchar(20) | Y | (N'PSE') |  |  |
| 9 | ITEMID | nvarchar(50) | Y |  |  |  |
| 10 | MATERIAL | nvarchar(100) | Y |  |  |  |
| 11 | STANDARD | nvarchar(100) | Y |  |  |  |
| 12 | PROVIDER | nvarchar(100) | Y |  |  |  |
| 13 | MONTAGE | nvarchar(100) | Y |  |  |  |
| 14 | REMARKS | nvarchar(100) | Y |  |  |  |
| 15 | PRODUCTMODEL | nvarchar(100) | Y |  |  |  |
| 16 | DWGNO | nvarchar(100) | Y |  |  |  |
| 17 | WEIGHT | nvarchar(100) | Y |  |  |  |
| 18 | STANDARDNO | nvarchar(100) | Y |  |  |  |
| 19 | TYPE | nvarchar(500) | Y |  |  |  |
| 20 | SURFACETREATMENT | nvarchar(100) | Y |  |  |  |
| 21 | ISTOOLING | nvarchar(20) | Y |  |  |  |
| 22 | MATERIALTYPE2 | nvarchar(100) | Y |  |  |  |
| 23 | WEIGHTUNIT | nvarchar(100) | Y |  |  |  |
| 24 | MATERIALNAME | nvarchar(100) | Y |  |  |  |
| 25 | MATERIALGRADE | nvarchar(100) | Y |  |  |  |
| 26 | EXTERNALDIMENSIONS | nvarchar(100) | Y |  |  |  |
| 27 | CHARACTER | nvarchar(100) | Y |  |  |  |
| 28 | JSYQ | nvarchar(100) | Y |  |  |  |
| 29 | AZCC | nvarchar(100) | Y |  |  |  |
| 30 | KUBIE | nvarchar(100) | Y |  |  |  |
| 31 | ZLDAH | nvarchar(100) | Y |  |  |  |
| 32 | XMDH | nvarchar(100) | Y |  |  |  |
| 33 | ZUZHIERP | nvarchar(100) | Y |  |  |  |
| 34 | BILI | nvarchar(100) | Y |  |  |  |
| 35 | LSBZ | nvarchar(500) | Y |  |  |  |
| 36 | FLSXHB | nvarchar(100) | Y |  |  |  |
| 37 | WULIAOSHUXING | nvarchar(100) | Y |  |  |  |
| 38 | CUNHUOLEIBIE | nvarchar(100) | Y |  |  |  |
| 39 | JIHUACELUE | nvarchar(100) | Y |  |  |  |
| 40 | YUNXUSHENGCHAN | nvarchar(100) | Y |  |  |  |
| 41 | SHIYONGZUZHI | nvarchar(100) | Y |  |  |  |
| 42 | CHUANGJIANZUZHI | nvarchar(100) | Y |  |  |  |
| 43 | SQZZ | nvarchar(100) | Y |  |  |  |
| 44 | SFZFXC | nvarchar(100) | Y |  |  |  |
| 45 | YUNXUXIAOSHOU | nvarchar(100) | Y |  |  |  |
| 46 | YUNXUCAIGOU | nvarchar(100) | Y |  |  |  |
| 47 | YUNXUKUCUN | nvarchar(100) | Y |  |  |  |
| 48 | YUNXUWEIWAI | nvarchar(100) | Y |  |  |  |
| 49 | ERPCANGKU | nvarchar(100) | Y |  |  |  |
| 50 | TUFU1 | nvarchar(100) | Y |  |  |  |
| 51 | CGLX | nvarchar(100) | Y |  |  |  |

### PSEBASEFORM$

**行数** 196,842 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ESTATION | nvarchar(100) | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | ITEMTYPE | nvarchar(100) | Y |  |  |  |
| 8 | FORMTYPE | nvarchar(20) | Y | (N'PSE') |  |  |
| 9 | ITEMID | nvarchar(50) | Y |  |  |  |
| 10 | MATERIAL | nvarchar(100) | Y |  |  |  |
| 11 | STANDARD | nvarchar(100) | Y |  |  |  |
| 12 | PROVIDER | nvarchar(100) | Y |  |  |  |
| 13 | MONTAGE | nvarchar(100) | Y |  |  |  |
| 14 | REMARKS | nvarchar(100) | Y |  |  |  |
| 15 | PRODUCTMODEL | nvarchar(100) | Y |  |  |  |
| 16 | DWGNO | nvarchar(100) | Y |  |  |  |
| 17 | WEIGHT | nvarchar(100) | Y |  |  |  |
| 18 | STANDARDNO | nvarchar(100) | Y |  |  |  |
| 19 | TYPE | nvarchar(500) | Y |  |  |  |
| 20 | SURFACETREATMENT | nvarchar(100) | Y |  |  |  |
| 21 | ISTOOLING | nvarchar(20) | Y |  |  |  |
| 22 | MATERIALTYPE2 | nvarchar(100) | Y |  |  |  |
| 23 | WEIGHTUNIT | nvarchar(100) | Y |  |  |  |
| 24 | MASTERID$ | nvarchar(100) | Y |  |  |  |
| 25 | MATERIALNAME | nvarchar(100) | Y |  |  |  |
| 26 | MATERIALGRADE | nvarchar(100) | Y |  |  |  |
| 27 | EXTERNALDIMENSIONS | nvarchar(100) | Y |  |  |  |
| 28 | CHARACTER | nvarchar(100) | Y |  |  |  |
| 29 | JSYQ | nvarchar(100) | Y |  |  |  |
| 30 | AZCC | nvarchar(100) | Y |  |  |  |
| 31 | KUBIE | nvarchar(100) | Y |  |  |  |
| 32 | ZLDAH | nvarchar(100) | Y |  |  |  |
| 33 | XMDH | nvarchar(100) | Y |  |  |  |
| 34 | ZUZHIERP | nvarchar(100) | Y |  |  |  |
| 35 | BILI | nvarchar(100) | Y |  |  |  |
| 36 | LSBZ | nvarchar(500) | Y |  |  |  |
| 37 | FLSXHB | nvarchar(100) | Y |  |  |  |
| 38 | WULIAOSHUXING | nvarchar(100) | Y |  |  |  |
| 39 | CUNHUOLEIBIE | nvarchar(100) | Y |  |  |  |
| 40 | JIHUACELUE | nvarchar(100) | Y |  |  |  |
| 41 | YUNXUSHENGCHAN | nvarchar(100) | Y |  |  |  |
| 42 | SHIYONGZUZHI | nvarchar(100) | Y |  |  |  |
| 43 | CHUANGJIANZUZHI | nvarchar(100) | Y |  |  |  |
| 44 | SQZZ | nvarchar(100) | Y |  |  |  |
| 45 | SFZFXC | nvarchar(100) | Y |  |  |  |
| 46 | YUNXUXIAOSHOU | nvarchar(100) | Y |  |  |  |
| 47 | YUNXUCAIGOU | nvarchar(100) | Y |  |  |  |
| 48 | YUNXUKUCUN | nvarchar(100) | Y |  |  |  |
| 49 | YUNXUWEIWAI | nvarchar(100) | Y |  |  |  |
| 50 | ERPCANGKU | nvarchar(100) | Y |  |  |  |
| 51 | TUFU1 | nvarchar(100) | Y |  |  |  |
| 52 | CGLX | nvarchar(100) | Y |  |  |  |

### PSEBOMLINE

**行数** 79,105 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | LBOMTYPE | nvarchar(50) |  | (N'Quote') |  |  |
| 17 | LDESCRIPTION | nvarchar(200) | Y |  |  |  |
| 18 | LVALIDITYID | nvarchar(100) | Y |  |  |  |
| 19 | PVIEWTYPE | nvarchar(100) | Y | (N'Design') |  |  |
| 20 | PVIEWID | nvarchar(100) | Y |  |  |  |
| 21 | TOTALQTY | float | Y |  |  |  |
| 22 | ITEMROUTE | nvarchar(2000) | Y |  |  |  |
| 23 | USEQTY | float | Y |  |  |  |
| 24 | SURPLUSQTY | float | Y |  |  |  |
| 25 | SUBUSEQTY | float | Y |  |  |  |
| 26 | POSNO | nvarchar(2000) | Y |  |  |  |
| 27 | DATASOURCE | nvarchar(20) | Y | (N'MPDM') |  |  |
| 28 | CGJHBZ1 | nvarchar(100) | Y |  |  |  |
| 29 | CGJHDBBZ | nvarchar(100) | Y |  |  |  |
| 30 | F_ZXCV_KFYJ | nvarchar(250) | Y |  |  |  |
| 31 | F_ZXCV_XKSL | nvarchar(250) | Y |  |  |  |
| 32 | F_ZXCV_DBSL | nvarchar(250) | Y |  |  |  |
| 33 | F_ZXCV_XKR | nvarchar(100) | Y |  |  |  |
| 34 | FAPPROVEQTY | nvarchar(100) | Y | (N'0') |  |  |
| 35 | DBXM | nvarchar(200) | Y |  |  |  |
| 36 | XKRXM | nvarchar(100) | Y |  |  |  |
| 37 | ERPBH | nvarchar(100) | Y |  |  |  |

### PSEBOMLINE$

**行数** 253,829 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | LBOMTYPE | nvarchar(50) |  | (N'Quote') |  |  |
| 17 | LDESCRIPTION | nvarchar(200) | Y |  |  |  |
| 18 | LVALIDITYID | nvarchar(100) | Y |  |  |  |
| 19 | PVIEWTYPE | nvarchar(100) | Y | (N'Design') |  |  |
| 20 | PVIEWID | nvarchar(100) | Y |  |  |  |
| 21 | TOTALQTY | float | Y |  |  |  |
| 22 | ITEMROUTE | nvarchar(2000) | Y |  |  |  |
| 23 | USEQTY | float | Y |  |  |  |
| 24 | SURPLUSQTY | float | Y |  |  |  |
| 25 | SUBUSEQTY | float | Y |  |  |  |
| 26 | POSNO | nvarchar(2000) | Y |  |  |  |
| 27 | DATASOURCE | nvarchar(20) | Y | (N'MPDM') |  |  |
| 28 | MASTERID$ | nvarchar(100) | Y |  |  |  |
| 29 | CGJHBZ1 | nvarchar(100) | Y |  |  |  |
| 30 | CGJHDBBZ | nvarchar(100) | Y |  |  |  |
| 31 | F_ZXCV_KFYJ | nvarchar(250) | Y |  |  |  |
| 32 | F_ZXCV_XKSL | nvarchar(250) | Y |  |  |  |
| 33 | F_ZXCV_DBSL | nvarchar(250) | Y |  |  |  |
| 34 | F_ZXCV_XKR | nvarchar(100) | Y |  |  |  |
| 35 | FAPPROVEQTY | nvarchar(100) | Y | (N'0') |  |  |
| 36 | DBXM | nvarchar(200) | Y |  |  |  |
| 37 | XKRXM | nvarchar(100) | Y |  |  |  |
| 38 | ERPBH | nvarchar(100) | Y |  |  |  |

### PSECLASSIFYRELATION

**行数** 1,302 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | FORMCOLUMN | nvarchar(50) | Y |  |  |  |
| 14 | PREFIX | nvarchar(20) | Y |  |  |  |
| 15 | SUFFIX | nvarchar(20) | Y |  |  |  |
| 16 | ISUSING | bit | Y |  |  |  |

### PSEITEM

**行数** 116,035 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y | (N'Using') |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y | (N'1') |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 22 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 23 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 24 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 25 | RELEASEDATE | bigint | Y |  |  |  |
| 26 | POSITION | nvarchar(100) |  |  |  |  |
| 27 | CURITERATION | bigint | Y | (N'1') |  |  |
| 28 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 29 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 30 | CHECKOUTDATE | bigint | Y |  |  |  |
| 31 | ITEMTYPE | nvarchar(50) |  | (N'PSEITEMGAUGEFORM') |  |  |
| 32 | ORIGIN | nvarchar(100) | Y |  |  |  |
| 33 | FIRSTPRODUCTCODE | nvarchar(100) | Y |  |  |  |
| 34 | UNIVERSAL | nvarchar(100) | Y |  |  |  |
| 35 | UNIT | nvarchar(100) | Y | (N'4') |  |  |
| 36 | VALIDITYID | nvarchar(100) | Y |  |  |  |
| 37 | FORMID | nvarchar(50) | Y |  |  |  |
| 38 | FORMTYPE | nvarchar(20) | Y | (N'PSE') |  |  |
| 39 | XLJTYPE | nvarchar(20) | Y |  |  |  |
| 40 | CONFIGNAME | nvarchar(100) | Y |  |  |  |
| 41 | SRCITEMID | nvarchar(50) | Y |  |  |  |
| 42 | GGXH | nvarchar(500) | Y |  |  |  |
| 43 | ITEMFID | nvarchar(100) | Y |  |  |  |
| 44 | ISSENDITEM | nvarchar(100) | Y |  |  |  |
| 45 | BOMFID | nvarchar(100) | Y |  |  |  |
| 46 | ISSENDBOM | nvarchar(100) | Y |  |  |  |
| 47 | ZYCS | nvarchar(1000) | Y |  |  |  |
| 48 | MATERIALCODE | nvarchar(100) | Y |  |  |  |
| 49 | FILEORNOT | nvarchar(100) | Y |  |  |  |
| 50 | SQDZT | nvarchar(100) | Y |  |  |  |

### PSEITEM$

**行数** 196,520 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y | (N'Using') |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y | (N'1') |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 22 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 23 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 24 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 25 | RELEASEDATE | bigint | Y |  |  |  |
| 26 | POSITION | nvarchar(100) |  |  |  |  |
| 27 | CURITERATION | bigint | Y | (N'1') |  |  |
| 28 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 29 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 30 | CHECKOUTDATE | bigint | Y |  |  |  |
| 31 | ITEMTYPE | nvarchar(50) |  | (N'PSEITEMGAUGEFORM') |  |  |
| 32 | ORIGIN | nvarchar(100) | Y |  |  |  |
| 33 | FIRSTPRODUCTCODE | nvarchar(100) | Y |  |  |  |
| 34 | UNIVERSAL | nvarchar(100) | Y |  |  |  |
| 35 | UNIT | nvarchar(100) | Y | (N'4') |  |  |
| 36 | VALIDITYID | nvarchar(100) | Y |  |  |  |
| 37 | FORMID | nvarchar(50) | Y |  |  |  |
| 38 | FORMTYPE | nvarchar(20) | Y | (N'PSE') |  |  |
| 39 | XLJTYPE | nvarchar(20) | Y |  |  |  |
| 40 | CONFIGNAME | nvarchar(100) | Y |  |  |  |
| 41 | SRCITEMID | nvarchar(50) | Y |  |  |  |
| 42 | MASTERID$ | nvarchar(100) | Y |  |  |  |
| 43 | GGXH | nvarchar(500) | Y |  |  |  |
| 44 | ITEMFID | nvarchar(100) | Y |  |  |  |
| 45 | ISSENDITEM | nvarchar(100) | Y |  |  |  |
| 46 | BOMFID | nvarchar(100) | Y |  |  |  |
| 47 | ISSENDBOM | nvarchar(100) | Y |  |  |  |
| 48 | ZYCS | nvarchar(1000) | Y |  |  |  |
| 49 | MATERIALCODE | nvarchar(100) | Y |  |  |  |
| 50 | FILEORNOT | nvarchar(100) | Y |  |  |  |
| 51 | SQDZT | nvarchar(100) | Y |  |  |  |

### PSEITEMASSOCIATEDOBJECT

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | LCREATETIME | bigint |  |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### PSEITEMREFERENCEOBJECT

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | LCREATETIME | bigint |  |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### PSEITEMRELATION

**行数** 46,580 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | RELATIONTYPE | nvarchar(100) | Y |  |  |  |
| 16 | MAINREFERTYPE | nvarchar(100) | Y |  |  |  |
| 17 | CLASSCODE | nvarchar(200) | Y |  |  |  |
| 18 | LZHUANYE | nvarchar(200) | Y |  |  |  |
| 19 | LZHUJICHANG | nvarchar(200) | Y |  |  |  |

### PSEITEMRELATION$

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | RELATIONTYPE | nvarchar(100) | Y |  |  |  |
| 16 | MAINREFERTYPE | nvarchar(100) | Y |  |  |  |
| 17 | CLASSCODE | nvarchar(200) | Y |  |  |  |
| 18 | LZHUANYE | nvarchar(200) | Y |  |  |  |
| 19 | LZHUJICHANG | nvarchar(200) | Y |  |  |  |
| 20 | MASTERID$ | nvarchar(100) | Y |  |  |  |

### PSEITEMVIEW

**行数** 116,038 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 7 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 8 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | CREATETIME | bigint | Y |  |  |  |
| 10 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 11 | UPDATETIME | bigint | Y |  |  |  |
| 12 | OWNUID | nvarchar(50) | Y |  |  |  |
| 13 | OWNGID | nvarchar(50) | Y |  |  |  |
| 14 | ESTATION | nvarchar(100) | Y |  |  |  |
| 15 | ICON | nvarchar(200) | Y |  |  |  |
| 16 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 17 | SERIALNUMBER | int | Y |  |  |  |
| 18 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 19 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 20 | RELEASEDATE | bigint | Y |  |  |  |
| 21 | CURITERATION | bigint | Y | (N'1') |  |  |
| 22 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 23 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 24 | CHECKOUTDATE | bigint | Y |  |  |  |
| 25 | SECURITYLEVEL | nvarchar(50) | Y |  |  |  |
| 26 | VIEWTYPE | nvarchar(100) |  |  |  |  |
| 27 | ITEMID | nvarchar(100) | Y |  |  |  |
| 28 | FORMTYPE | nvarchar(20) | Y | (N'PSE') |  |  |
| 29 | PRIORITY | int | Y | (N'1') |  |  |

### PSEITEMVIEW$

**行数** 137,746 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 7 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 8 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 9 | CREATETIME | bigint | Y |  |  |  |
| 10 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 11 | UPDATETIME | bigint | Y |  |  |  |
| 12 | OWNUID | nvarchar(50) | Y |  |  |  |
| 13 | OWNGID | nvarchar(50) | Y |  |  |  |
| 14 | ESTATION | nvarchar(100) | Y |  |  |  |
| 15 | ICON | nvarchar(200) | Y |  |  |  |
| 16 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 17 | SERIALNUMBER | int | Y |  |  |  |
| 18 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 19 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 20 | RELEASEDATE | bigint | Y |  |  |  |
| 21 | CURITERATION | bigint | Y | (N'1') |  |  |
| 22 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 23 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 24 | CHECKOUTDATE | bigint | Y |  |  |  |
| 25 | SECURITYLEVEL | nvarchar(50) | Y |  |  |  |
| 26 | VIEWTYPE | nvarchar(100) |  |  |  |  |
| 27 | ITEMID | nvarchar(100) | Y |  |  |  |
| 28 | FORMTYPE | nvarchar(20) | Y | (N'PSE') |  |  |
| 29 | PRIORITY | int | Y | (N'1') |  |  |
| 30 | MASTERID$ | nvarchar(100) | Y |  |  |  |

### PSEVERSIONRULE

**行数** 2 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | OWNUID | nvarchar(50) | Y |  |  |  |
| 8 | OWNGID | nvarchar(50) | Y |  |  |  |
| 9 | ESTATION | nvarchar(100) | Y |  |  |  |
| 10 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 11 | SERIALNUMBER | int | Y |  |  |  |
| 12 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 13 | RELEASEDATE | nvarchar(100) | Y |  |  |  |
| 14 | VALIDITYVALUE | nvarchar(100) | Y |  |  |  |
| 15 | VALIDITYTYPE | nvarchar(100) | Y |  |  |  |
| 16 | ITEMCODE | nvarchar(100) | Y |  |  |  |

### PSEVERSIONRULEDATA

**行数** 2 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | VERSIONRULEID | nvarchar(100) | Y |  |  |  |
| 7 | RULECOLUMN | nvarchar(100) | Y |  |  |  |
| 8 | RULECOLUMNVALUE | nvarchar(100) | Y |  |  |  |
| 9 | CONDITIONS | nvarchar(100) | Y |  |  |  |
| 10 | SORT | int | Y |  |  |  |

### PTTTOPTTLINK

**行数** 2 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | VERSIONEID | nvarchar(100) | Y |  |  |  |
| 17 | LINKTYPE | nvarchar(100) | Y |  |  |  |
| 18 | DELAYTIME | nvarchar(100) | Y |  |  |  |

### PTTTOPTTLINKCOPY

**行数** 1 ｜ **主键** LID, COPYBATCHNO

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | VERSIONEID | nvarchar(100) | Y |  |  |  |
| 17 | LINKTYPE | nvarchar(100) | Y |  |  |  |
| 18 | DELAYTIME | nvarchar(100) | Y |  |  |  |
| 19 | COPYBATCHNO | nvarchar(100) |  |  | PK |  |

### QPROBLEM

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y | (N'new') |  |  |
| 5 | STAGESTATEID | nvarchar(20) |  |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 16 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 17 | ISSUELEVEL | nvarchar(100) |  | (N'high') |  |  |
| 18 | YZCD | nvarchar(100) |  | (N'yiban') |  |  |
| 19 | ISSUESOURCE | nvarchar(100) | Y |  |  |  |
| 20 | WTGY | nvarchar(2000) | Y |  |  |  |
| 21 | WTFL | nvarchar(100) |  | (N'cpsj') |  |  |
| 22 | JSFL | nvarchar(100) | Y |  |  |  |
| 23 | YXJ | nvarchar(100) |  | (N'high') |  |  |
| 24 | FINDUSER | nvarchar(100) |  |  |  |  |
| 25 | FINDDATE | nvarchar(100) |  |  |  |  |
| 26 | SUBMITUSER | nvarchar(100) | Y |  |  |  |
| 27 | SUBMITDATE | nvarchar(100) | Y |  |  |  |
| 28 | ISQUESTION | nvarchar(100) | Y |  |  |  |
| 29 | RESPONSER | nvarchar(100) | Y |  |  |  |
| 30 | PLANENDDATE | nvarchar(100) |  |  |  |  |
| 31 | PLANSOLUTION | nvarchar(2000) |  |  |  |  |
| 32 | ACTUREENDDATE | nvarchar(100) | Y |  |  |  |
| 33 | ACTURESOLUTION | nvarchar(2000) | Y |  |  |  |
| 34 | SCHDULE | nvarchar(100) | Y | (N'0') |  |  |
| 35 | YZR | nvarchar(100) | Y |  |  |  |
| 36 | YZSJ | nvarchar(100) | Y |  |  |  |
| 37 | YZYJ | nvarchar(2000) | Y |  |  |  |
| 38 | ZJYY | nvarchar(100) | Y |  |  |  |
| 39 | ISAPPOINT | nvarchar(100) | Y |  |  |  |
| 40 | DENYREASON | nvarchar(200) | Y |  |  |  |
| 41 | PROJECT | nvarchar(100) | Y |  |  |  |
| 42 | PSEITEM | nvarchar(200) | Y |  |  |  |
| 43 | PSEITEMNAME | nvarchar(100) | Y |  |  |  |
| 44 | SUBMITDEPARTMENT | nvarchar(100) | Y |  |  |  |
| 45 | SOLVEDEPARTMENT | nvarchar(100) | Y |  |  |  |
| 46 | CHECKDEPARTMENT | nvarchar(100) | Y |  |  |  |

### QPROBLEMATTENTION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |
| 9 | ISVIEW | nvarchar(100) | Y |  |  |  |

### QPROBLEMDOCFILE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | QPROBLEMID | nvarchar(100) | Y |  |  |  |
| 8 | FILEEXTENT | nvarchar(100) | Y |  |  |  |
| 9 | TYPEID | nvarchar(100) | Y |  |  |  |
| 10 | FILEPATH | nvarchar(1000) | Y |  |  |  |
| 11 | FILESIZE | bigint | Y |  |  |  |
| 12 | CURVOLUMEID | nvarchar(100) | Y |  |  |  |
| 13 | FILENAME | nvarchar(100) | Y |  |  |  |

### QPROBLEMPROMOTE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ISSUEID | nvarchar(100) | Y |  |  |  |
| 7 | TYPE | nvarchar(100) | Y |  |  |  |
| 8 | BEFOREVALUE | nvarchar(100) | Y |  |  |  |
| 9 | AFTERVALUE | nvarchar(100) | Y |  |  |  |
| 10 | AUTOTYPE | nvarchar(100) | Y |  |  |  |

### QPROBLEMRELATION

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |
| 9 | CETYPE | nvarchar(100) | Y |  |  |  |
| 10 | LINKTYPE | nvarchar(100) | Y |  |  |  |

### QPROBLEMSOLUTION

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | STATEID | nvarchar(20) |  | (N'Start') |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | PEID | nvarchar(100) | Y |  |  |  |
| 8 | ZGR | nvarchar(100) |  |  |  |  |
| 9 | JHZGSJ | nvarchar(100) |  |  |  |  |
| 10 | JHZGCS | nvarchar(1000) | Y |  |  |  |
| 11 | SJZGCS | nvarchar(500) | Y |  |  |  |
| 12 | SJWCSJ | nvarchar(100) | Y |  |  |  |

### QPROBLEMTEMPLATE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 16 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 17 | ISSUELEVEL | nvarchar(100) | Y | (N'high') |  |  |
| 18 | YZCD | nvarchar(100) |  | (N'yiban') |  |  |
| 19 | ISSUESOURCE | nvarchar(100) | Y |  |  |  |
| 20 | WTGY | nvarchar(2000) | Y |  |  |  |
| 21 | WTFL | nvarchar(100) |  | (N'cpsj') |  |  |
| 22 | JSFL | nvarchar(100) | Y |  |  |  |
| 23 | YXJ | nvarchar(100) |  | (N'high') |  |  |
| 24 | FSYY | nvarchar(100) | Y |  |  |  |
| 25 | PLANSOLUTION | nvarchar(100) | Y |  |  |  |
| 26 | INVALID | nvarchar(100) | Y | (N'no') |  |  |

### QPROBLEMTODOC

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### QUALITYCHECK

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | CHECKITEM | nvarchar(2000) | Y |  |  |  |
| 16 | JISHUYAOQIU | nvarchar(2000) | Y |  |  |  |
| 17 | SHOUDUAN | nvarchar(2000) | Y |  |  |  |
| 18 | FANGAN | nvarchar(2000) | Y |  |  |  |
| 19 | CAOZUOYAOQIU | nvarchar(2000) | Y |  |  |  |

### QUERYMACHINEBASEINFO

**行数** 14 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | DESCRIPTION | nvarchar(200) | Y |  |  |  |
| 10 | ROOTPOM | nvarchar(100) |  |  |  |  |
| 11 | SHOWVIEW | ntext | Y |  |  |  |
| 12 | DATALINKMODEL | nvarchar(1000) | Y |  |  |  |
| 13 | FIELDKEY | nvarchar(100) | Y |  |  |  |
| 14 | ORGANIZATIONROLE | ntext | Y |  |  |  |
| 15 | CUSTOMQUERYNAME | nvarchar(100) | Y |  |  |  |
| 16 | CUSTOMQUERYSQLCOMMAND | ntext | Y |  |  |  |
| 17 | SEQUENCE | int | Y |  |  |  |

### QUERYMACHINEHISTORY

**行数** 1,256 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | DESCRIPTION | nvarchar(200) | Y |  |  |  |
| 10 | ROOTPOM | nvarchar(100) |  |  |  |  |
| 11 | SHOWVIEW | ntext | Y |  |  |  |
| 12 | DATALINKMODEL | nvarchar(1000) | Y |  |  |  |
| 13 | FIELDKEY | nvarchar(100) | Y |  |  |  |
| 14 | ORGANIZATIONROLE | ntext | Y |  |  |  |
| 15 | CUSTOMQUERYNAME | nvarchar(100) | Y |  |  |  |
| 16 | CUSTOMQUERYSQLCOMMAND | ntext | Y |  |  |  |
| 17 | BASEQUERYID | nvarchar(100) | Y |  |  |  |
| 18 | QUERYROLES | ntext | Y |  |  |  |

### QUERYMACHINEPOM

**行数** 30 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |
| 7 | POMOBJECTNAME | nvarchar(100) | Y |  |  |  |

### QUERYMACHINEPOMHISTORY

**行数** 1,176 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |
| 7 | POMOBJECTNAME | nvarchar(100) | Y |  |  |  |

### QUERYMACHINEPOMLINKDATA

**行数** 10 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ACOLNAME | nvarchar(100) | Y |  |  |  |
| 7 | ATYPENAME | nvarchar(100) | Y |  |  |  |
| 8 | BCOLNAME | nvarchar(100) | Y |  |  |  |
| 9 | BTYPENAME | nvarchar(100) | Y |  |  |  |
| 10 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |

### QUERYMACHINESHOWVIEW

**行数** 1,708,803 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | OBJECTTYPENAME | nvarchar(100) | Y |  |  |  |
| 9 | COLNAME | nvarchar(100) | Y |  |  |  |
| 10 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |
| 11 | COLTITLE | nvarchar(100) | Y |  |  |  |

### QUERYMACHINESTRUCTURE

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |

### QUERYPOMLINKHISTORY

**行数** 1,051 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ACOLNAME | nvarchar(100) | Y |  |  |  |
| 7 | ATYPENAME | nvarchar(100) | Y |  |  |  |
| 8 | BCOLNAME | nvarchar(100) | Y |  |  |  |
| 9 | BTYPENAME | nvarchar(100) | Y |  |  |  |
| 10 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |

### QUERYROLEHISTORY

**行数** 17,226 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |
| 9 | OPERATETYPE | nvarchar(100) | Y |  |  |  |
| 10 | COLNAME | nvarchar(100) | Y |  |  |  |
| 11 | COLTEXT | nvarchar(100) | Y |  |  |  |
| 12 | OBJECTTYPENAME | nvarchar(100) | Y |  |  |  |
| 13 | COMPARISONCONDITION | nvarchar(100) | Y |  |  |  |
| 14 | RESULTVALUE | nvarchar(150) | Y |  |  |  |
| 15 | COLTYPE | nvarchar(100) | Y |  |  |  |
| 16 | ODERNUMBER | bigint | Y |  |  |  |
| 17 | CUSTOMQUERYROLEID | nvarchar(100) | Y |  |  |  |
| 18 | CUSTOMQUERYREPLACEPARA | nvarchar(100) | Y |  |  |  |
| 19 | BASEQUERYROLEID | nvarchar(100) | Y |  |  |  |

### QUERYRULEBASEINFO

**行数** 118 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |
| 9 | OPERATETYPE | nvarchar(100) | Y |  |  |  |
| 10 | COLNAME | nvarchar(100) | Y |  |  |  |
| 11 | COLTEXT | nvarchar(100) | Y |  |  |  |
| 12 | OBJECTTYPENAME | nvarchar(100) | Y |  |  |  |
| 13 | COMPARISONCONDITION | nvarchar(100) | Y |  |  |  |
| 14 | RESULTVALUE | nvarchar(150) | Y |  |  |  |
| 15 | COLTYPE | nvarchar(100) | Y |  |  |  |
| 16 | ODERNUMBER | bigint | Y |  |  |  |
| 17 | CUSTOMQUERYROLEID | nvarchar(100) | Y |  |  |  |
| 18 | CUSTOMQUERYREPLACEPARA | nvarchar(100) | Y |  |  |  |

### QUERYRULEBUILDININFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | QUERYMACHINEID | nvarchar(100) | Y |  |  |  |
| 9 | OPERATETYPE | nvarchar(100) | Y |  |  |  |
| 10 | COLNAME | nvarchar(100) | Y |  |  |  |
| 11 | COLTEXT | nvarchar(100) | Y |  |  |  |
| 12 | OBJECTTYPENAME | nvarchar(100) | Y |  |  |  |
| 13 | COMPARISONCONDITION | nvarchar(100) | Y |  |  |  |
| 14 | RESULTVALUE | nvarchar(150) | Y |  |  |  |
| 15 | COLTYPE | nvarchar(100) | Y |  |  |  |
| 16 | ODERNUMBER | bigint | Y |  |  |  |
| 17 | MODELPROPERTY | nvarchar(100) | Y |  |  |  |

### QUEUECACHE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | TRANSHASH | nvarchar(100) | Y |  |  |  |
| 7 | QUEUENAME | nvarchar(100) | Y |  |  |  |
| 8 | DATA | ntext | Y |  |  |  |

### QUICKNAVIGATION

**行数** 45 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ECODE | nvarchar(100) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | USERID | nvarchar(100) |  |  |  |  |
| 8 | QUICKTYPE | nvarchar(100) | Y |  |  |  |
| 9 | QUICKROW | int | Y |  |  |  |
| 10 | QUICKCOLUMN | int | Y |  |  |  |
| 11 | QUICKCONTENT | nvarchar(200) | Y |  |  |  |
| 12 | QUICKTEXT | nvarchar(100) | Y |  |  |  |
| 13 | QUICKIMAGE | nvarchar(100) | Y |  |  |  |
| 14 | QUICKSORT | int | Y | (N'0') |  |  |

### RESITEMATTRELATEDCONFIG

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | RESOURCECLASSID | nvarchar(200) | Y |  |  |  |
| 7 | RESOURCEATTNAME | nvarchar(200) | Y |  |  |  |
| 8 | ITEMATTNAME | nvarchar(200) | Y |  |  |  |
| 9 | ITEMATTSOURCE | nvarchar(200) | Y |  |  |  |

### RESOURCEBASE

**行数** 247 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ICON | nvarchar(200) | Y | (N'~/InforCenter/Project/Resources/Image/Model/p_userobject.png') |  |  |
| 14 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 15 | SERIALNUMBER | int | Y |  |  |  |
| 16 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 17 | SECURITYLEVEL | nvarchar(50) | Y |  |  |  |
| 18 | RESTYPE | nvarchar(20) | Y |  |  |  |
| 19 | JOBSKILL | nvarchar(200) | Y |  |  |  |
| 20 | MAXLOAD | int | Y | (N'8') |  |  |
| 21 | STANDARDRATE | float | Y | (N'0') |  |  |
| 22 | OVERTIMERATE | float | Y | (N'0') |  |  |
| 23 | PERUSECOSE | float | Y | (N'0') |  |  |
| 24 | RESOURCEUNIT | nvarchar(50) | Y |  |  |  |
| 25 | SHORTNAME | nvarchar(50) | Y |  |  |  |
| 26 | USERRESID | nvarchar(50) | Y |  |  |  |
| 27 | PROJECTROLEID | nvarchar(100) | Y |  |  |  |

### RESOURCECLASSIFY

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 12 | SERIALNUMBER | int | Y |  |  |  |
| 13 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |

### RESOURCELINK

**行数** 248 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |

### RISK

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 16 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 17 | CLASSIFY | nvarchar(100) |  | (N'technology') |  |  |
| 18 | NATURE | nvarchar(100) |  | (N'menace') |  |  |
| 19 | PROJECT | nvarchar(200) |  |  |  |  |
| 20 | TASK | nvarchar(200) | Y |  |  |  |
| 21 | RISKLEVEL | nvarchar(100) |  | (N'high') |  |  |
| 22 | AFFECTRANGE | nvarchar(100) |  | (N'long') |  |  |
| 23 | FINDER | nvarchar(100) | Y |  |  |  |
| 24 | FINDDATE | nvarchar(100) | Y |  |  |  |
| 25 | SUBMITUSER | nvarchar(100) | Y |  |  |  |
| 26 | SUBMITDATE | nvarchar(100) | Y |  |  |  |
| 27 | RISKNEAR | nvarchar(100) | Y | (N'near') |  |  |
| 28 | ACCEPT | nvarchar(100) | Y |  |  |  |
| 29 | KNX | nvarchar(100) | Y |  |  |  |
| 30 | YZCD | nvarchar(100) | Y |  |  |  |
| 31 | FXSP | nvarchar(100) | Y |  |  |  |
| 32 | FXDL | nvarchar(100) | Y |  |  |  |
| 33 | SFZRWT | nvarchar(100) | Y | (N'no') |  |  |
| 34 | RESPONSER | nvarchar(100) | Y |  |  |  |
| 35 | DESIGNATE | nvarchar(100) | Y |  |  |  |
| 36 | YDCL | nvarchar(100) | Y |  |  |  |
| 37 | YFCS | nvarchar(100) | Y |  |  |  |
| 38 | YJJH | nvarchar(100) | Y |  |  |  |
| 39 | YJBS | nvarchar(100) | Y |  |  |  |
| 40 | DESIGNATEDATE | nvarchar(100) | Y |  |  |  |
| 41 | POSSIBILITY | nvarchar(100) | Y |  |  |  |
| 42 | SEVERITY | nvarchar(100) | Y |  |  |  |
| 43 | FAVORITE | bit | Y |  |  |  |

### RISKAIMLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |
| 9 | VERSIONEID | nvarchar(100) | Y |  |  |  |

### RISKAIMS

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  | (N'ProjectAim') |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | MINVALUE | float |  | (N'0') |  |  |
| 8 | RISKAIMSMAXVALUE | float |  | (N'0') |  |  |
| 9 | RISKID | nvarchar(100) | Y |  |  |  |

### RISKAPPRAISERLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | CEID | nvarchar(100) |  |  |  |  |
| 4 | LSEQUENCE | int |  | (N'0') |  |  |
| 5 | LQUALITY | float |  | (N'1') |  |  |
| 6 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | PEUID | nvarchar(100) | Y |  |  |  |
| 8 | CEUID | nvarchar(100) | Y |  |  |  |
| 9 | LKNX | nvarchar(100) | Y |  |  |  |
| 10 | LYZCD | nvarchar(100) | Y |  |  |  |
| 11 | LFXSP | nvarchar(100) | Y |  |  |  |
| 12 | LFXDL | nvarchar(100) | Y |  |  |  |

### RISKASSESSAIMSTITLE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | TYPE | nvarchar(100) | Y | (N'Possibility') |  |  |
| 10 | COLUMNINDEX | bigint | Y |  |  |  |
| 11 | TEXT | nvarchar(100) |  |  |  |  |

### RISKASSESSAMISDATA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | DATA | nvarchar(2000) | Y |  |  |  |
| 10 | TYPE | nvarchar(100) | Y |  |  |  |
| 11 | ORDERNUMBER | nvarchar(100) | Y |  |  |  |

### RISKASSESSINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | CONTENT | nvarchar(max) | Y |  |  |  |
| 8 | TYPE | nvarchar(100) | Y |  |  |  |

### RISKDOCLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | PEUID | nvarchar(100) | Y |  |  |  |
| 10 | CEUID | nvarchar(100) | Y |  |  |  |
| 11 | VERSIONEID | nvarchar(100) | Y |  |  |  |

### RISKDOCMODEL

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | FILENAME | nvarchar(100) | Y |  |  |  |
| 8 | FILESIZE | nvarchar(100) | Y |  |  |  |
| 9 | PATH | nvarchar(500) | Y |  |  |  |
| 10 | UPLOADTIME | nvarchar(100) | Y |  |  |  |
| 11 | UPLOADUSER | nvarchar(100) | Y |  |  |  |
| 12 | RISKID | nvarchar(100) | Y |  |  |  |
| 13 | CURVOLUMEID | nvarchar(100) | Y |  |  |  |
| 14 | FILEEXTENT | nvarchar(100) | Y |  |  |  |

### RISKTEMPLATE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | OWNUID | nvarchar(50) | Y |  |  |  |
| 11 | OWNGID | nvarchar(50) | Y |  |  |  |
| 12 | ESTATION | nvarchar(100) | Y |  |  |  |
| 13 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | SERIALNUMBER | int | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 16 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 17 | CLASSIFY | nvarchar(100) |  | (N'technology') |  |  |
| 18 | NATURE | nvarchar(100) |  | (N'menace') |  |  |
| 19 | RISKLEVEL | nvarchar(100) |  | (N'high') |  |  |
| 20 | AFFECTRANGE | nvarchar(100) |  | (N'long') |  |  |
| 21 | INVALID | nvarchar(100) | Y | (N'no') |  |  |

### ROLEINFO

**行数** 78 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) |  |  |  |  |
| 7 | UPDATETIME | bigint |  |  |  |  |
| 8 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 9 | SERIALNUMBER | int | Y |  |  |  |
| 10 | DESCRIPT | nvarchar(500) | Y |  |  |  |

### RP_CELL

**行数** 947 ｜ **主键** TEMPLATEID, CCELLPOS, RCELLPOS

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | TEMPLATEID | int |  |  | PK |  |
| 2 | CCELLPOS | int |  |  | PK |  |
| 3 | RCELLPOS | int |  |  | PK |  |
| 4 | NAME | varchar(200) | Y |  |  |  |
| 5 | CELLTYPE | int | Y |  |  |  |
| 6 | ROWSPAN | int | Y |  |  |  |
| 7 | COLSPAN | int | Y |  |  |  |
| 8 | BKCOLOR | int | Y |  |  |  |
| 9 | TEXTCOLOR | int | Y |  |  |  |
| 10 | FONT_NAME | varchar(50) | Y |  |  |  |
| 11 | FONT_HEIGHT | int | Y |  |  |  |
| 12 | FONT_WEIGHT | int | Y |  |  |  |
| 13 | FONT_STRIKEOUT | int | Y |  |  |  |
| 14 | FONT_UNDERLINE | int | Y |  |  |  |
| 15 | FONT_ITALIC | int | Y |  |  |  |
| 16 | MULTILINE | int | Y |  |  |  |
| 17 | ALIGNTYPE | int | Y |  |  |  |
| 18 | ROWSPACE | int | Y |  |  |  |
| 19 | CHARSPACE | int | Y |  |  |  |
| 20 | BORDERSPACE | int | Y |  |  |  |
| 21 | LEFTBORDER | int | Y |  |  |  |
| 22 | TOPBORDER | int | Y |  |  |  |
| 23 | RIGHTBORDER | int | Y |  |  |  |
| 24 | BOTTOMBORDER | int | Y |  |  |  |
| 25 | PRINTCELL | int | Y |  |  |  |
| 26 | CANOVER | int | Y |  |  |  |
| 27 | DBLABEL | varchar(50) | Y |  |  |  |
| 28 | INITVALUE | varchar(200) | Y |  |  |  |
| 29 | MUSTINPUT | int | Y |  |  |  |
| 30 | SAMEINPAGE | int | Y |  |  |  |
| 31 | OTHERINFO | text | Y |  |  |  |
| 32 | ZIPTYPE | int | Y |  |  |  |
| 33 | RESLIBRARY | nvarchar(2000) | Y |  |  |  |
| 34 | CONNECTCHAR | nvarchar(50) | Y |  |  |  |

### RP_COL

**行数** 51 ｜ **主键** TEMPLATEID, COLID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | TEMPLATEID | int |  |  | PK |  |
| 2 | COLID | int |  |  | PK |  |
| 3 | TABLEAREA | varchar(10) | Y |  |  |  |
| 4 | AREA | varchar(10) | Y |  |  |  |
| 5 | NAME | varchar(20) | Y |  |  |  |
| 6 | ISNOCOl | int | Y |  |  |  |
| 7 | OVERFLOW | int | Y |  |  |  |
| 8 | STARTNUM | int | Y |  |  |  |
| 9 | STEPNUM | int | Y |  |  |  |
| 10 | ITEMPROPERTY | varchar(1000) | Y |  |  |  |

### RP_FITEM

**行数** 0 ｜ **主键** REPORTID, PAGEID, ITEMID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | PAGEID | int |  |  | PK |  |
| 3 | ITEMID | int |  |  | PK |  |
| 4 | TYPE | int | Y |  |  |  |
| 5 | DATA | nvarchar(4000) | Y |  |  |  |
| 6 | ATTACHNAME | varchar(50) | Y |  |  |  |
| 7 | ATTACHDATA | nvarchar(4000) | Y |  |  |  |
| 8 | PROPERTY | text | Y |  |  |  |
| 9 | AUTHOR | varchar(100) | Y |  |  |  |
| 10 | CREATETIME | datetime | Y |  |  |  |
| 11 | ATTACHCONTENT | nvarchar(200) | Y |  |  |  |

### RP_IMAGE

**行数** 5,346 ｜ **主键** REPORTID, PAGEID, DBLABEL

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | PAGEID | int |  |  | PK |  |
| 3 | DBLABEL | varchar(50) |  |  | PK |  |
| 4 | DATA | nvarchar(4000) | Y |  |  |  |
| 5 | ATTACHNAME | varchar(200) | Y |  |  |  |
| 6 | ATTACHDATA | nvarchar(4000) | Y |  |  |  |
| 7 | DWGDATA | nvarchar(4000) | Y |  |  |  |

### RP_LOG

**行数** 0 ｜ **主键** ID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | ID | varchar(50) |  |  | PK |  |
| 2 | TYPE | int | Y |  |  |  |
| 3 | CREATETIME | datetime | Y |  |  |  |
| 4 | NOTE | varchar(1000) | Y |  |  |  |

### RP_NOTE

**行数** 0 ｜ **主键** REPORTID, NOTEID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | NOTEID | int |  |  | PK |  |
| 3 | CREATETIME | datetime | Y |  |  |  |
| 4 | NOTE | varchar(500) | Y |  |  |  |
| 5 | AUTHOR | varchar(100) | Y |  |  |  |
| 6 | OTHER | text | Y |  |  |  |

### RP_PAGE

**行数** 5,874 ｜ **主键** REPORTID, PAGEID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | PAGEID | int |  |  | PK |  |
| 3 | TEMPLATEID | int | Y |  |  |  |
| 4 | PAGENAME | nvarchar(50) | Y |  |  |  |
| 5 | PAGEUID | varchar(50) | Y |  |  |  |

### RP_PAGEPOSITION

**行数** 0 ｜ **主键** REPORTID, PAGEID, TYPE, POS

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | PAGEID | int |  |  | PK |  |
| 3 | TEMPLATEID | int | Y |  |  |  |
| 4 | TYPE | int |  |  | PK |  |
| 5 | POS | int |  |  | PK |  |
| 6 | ITEMSIZE | int | Y |  |  |  |

### RP_POSITION

**行数** 212 ｜ **主键** TEMPLATEID, TYPE, POS

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | TEMPLATEID | int |  |  | PK |  |
| 2 | TYPE | int |  |  | PK |  |
| 3 | POS | int |  |  | PK |  |
| 4 | ITEMSIZE | int | Y |  |  |  |

### RP_PROPERTY

**行数** 8,190 ｜ **主键** REPORTID, PAGEID, DBLABEL

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | PAGEID | int |  |  | PK |  |
| 3 | DBLABEL | varchar(50) |  |  | PK |  |
| 4 | PROPERTY | text | Y |  |  |  |

### RP_REPORT

**行数** 5,879 ｜ **主键** REPORTID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | TEMPLATEID | int |  |  |  |  |
| 3 | SERIESID | varchar(50) | Y |  |  |  |
| 4 | VERSION | varchar(20) | Y |  |  |  |
| 5 | RELEASESTATE | int |  |  |  |  |
| 6 | CREATEUSER | varchar(50) |  |  |  |  |
| 7 | CREATETIME | datetime |  |  |  |  |
| 8 | MODIFYUSER | varchar(50) |  |  |  |  |
| 9 | MODIFYTIME | datetime |  |  |  |  |
| 10 | OWNUSER | varchar(50) |  |  |  |  |
| 11 | ITEMMASTERID | varchar(50) | Y |  |  |  |
| 12 | ITEMMASTERCODE | nvarchar(100) | Y |  |  |  |
| 13 | ITEMMASTERNAME | nvarchar(200) | Y |  |  |  |
| 14 | PRODUCTTYPE | nvarchar(100) | Y |  |  |  |
| 15 | PRODUCTNAME | nvarchar(200) | Y |  |  |  |
| 16 | PRODUCTID | varchar(50) | Y |  |  |  |
| 17 | NAME | nvarchar(100) | Y |  |  |  |
| 18 | CODE | nvarchar(100) | Y |  |  |  |
| 19 | EXTPROA | nvarchar(100) | Y |  |  |  |
| 20 | EXTPROB | nvarchar(100) | Y |  |  |  |
| 21 | EXTPROC | nvarchar(100) | Y |  |  |  |
| 22 | EXTPROD | nvarchar(100) | Y |  |  |  |
| 23 | EXTPROE | nvarchar(100) | Y |  |  |  |
| 24 | EXTPROF | nvarchar(100) | Y |  |  |  |
| 25 | EXTPROG | nvarchar(100) | Y |  |  |  |
| 26 | EXTPROH | nvarchar(100) | Y |  |  |  |
| 27 | EXTPROI | nvarchar(100) | Y |  |  |  |
| 28 | EXTPROJ | nvarchar(100) | Y |  |  |  |
| 29 | EXTPROK | nvarchar(100) | Y |  |  |  |
| 30 | EXTPROL | nvarchar(100) | Y |  |  |  |
| 31 | EXTPROM | nvarchar(100) | Y |  |  |  |
| 32 | EXTPRON | nvarchar(100) | Y |  |  |  |
| 33 | RELEASESTATUSNAME | nvarchar(50) | Y |  |  |  |
| 34 | RELEASESTATUSICON | nvarchar(50) | Y |  |  |  |
| 35 | REBACKFLAG | nvarchar(50) | Y |  |  |  |
| 36 | RELEASEDATE | datetime | Y |  |  |  |
| 37 | STAGE | nvarchar(10) | Y |  |  |  |
| 38 | USINGSTATUS | nvarchar(10) | Y |  |  |  |
| 39 | EXTENTINFO | nvarchar(2000) | Y |  |  |  |

### RP_SPECIALCHAR

**行数** 0 ｜ **主键** ID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | ID | int |  |  | PK |  |
| 2 | TYPE | int | Y |  |  |  |
| 3 | NUM | int | Y |  |  |  |
| 4 | NAME | varchar(20) | Y |  |  |  |
| 5 | TOOLBMP | image | Y |  |  |  |
| 6 | SYSCHAR | varchar(20) | Y |  |  |  |
| 7 | PICTURE | image | Y |  |  |  |
| 8 | PICTUREPATH | varchar(1000) | Y |  |  |  |
| 9 | EID | varchar(20) | Y |  |  |  |

### RP_TABLE

**行数** 5 ｜ **主键** TEMPLATEID, AREA

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | TEMPLATEID | int |  |  | PK |  |
| 2 | AREA | varchar(50) |  |  | PK |  |
| 3 | NAME | varchar(20) | Y |  |  |  |
| 4 | NEWPAGE | int | Y |  |  |  |

### RP_TEMPLATE

**行数** 6 ｜ **主键** ID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | ID | int |  |  | PK |  |
| 2 | TYPEID | int | Y |  |  |  |
| 3 | NAME | varchar(50) | Y |  |  |  |
| 4 | VERSION | varchar(50) | Y |  |  |  |
| 5 | ISRUN | int | Y |  |  |  |
| 6 | SHOWAREA | varchar(50) | Y |  |  |  |
| 7 | SHOWTYPE | int | Y |  |  |  |
| 8 | PAPERSIZE | varchar(50) | Y |  |  |  |
| 9 | PRINTX | int | Y |  |  |  |
| 10 | PRINTY | int | Y |  |  |  |
| 11 | DIRECTION | int | Y |  |  |  |
| 12 | PRINTTYPE | int | Y |  |  |  |
| 13 | BARCODETYPE | int | Y |  |  |  |
| 14 | TEMPLATEORDER | int | Y |  |  |  |

### RP_TEMPLATETYPE

**行数** 2 ｜ **主键** TYPEID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | TYPEID | int |  |  | PK |  |
| 2 | TYPENAME | nvarchar(100) |  |  |  |  |

### RPD_LJJGK

**行数** 4,992 ｜ **主键** REPORTID, PAGEID, ROWNO

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | PAGEID | int |  |  | PK |  |
| 3 | ROWNO | int |  |  | PK |  |
| 4 | DJY | nvarchar(2000) | Y |  |  |  |
| 5 | GJY | nvarchar(2000) | Y |  |  |  |
| 6 | XIANGHAO | nvarchar(2000) | Y |  |  |  |
| 7 | CHANLIANG | nvarchar(2000) | Y |  |  |  |
| 8 | D | nvarchar(2000) | Y |  |  |  |
| 9 | F | nvarchar(2000) | Y |  |  |  |
| 10 | J | nvarchar(2000) | Y |  |  |  |
| 11 | CLDE | nvarchar(2000) | Y |  |  |  |
| 12 | ITEMMASTERCODE | nvarchar(2000) | Y |  |  |  |

### RPD_LJJGMX

**行数** 13,935 ｜ **主键** REPORTID, PAGEID, ROWNO

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | PAGEID | int |  |  | PK |  |
| 3 | ROWNO | int |  |  | PK |  |
| 4 | LJJGGX | nvarchar(2000) | Y |  |  |  |
| 5 | JGSM | nvarchar(2000) | Y |  |  |  |
| 6 | JC | nvarchar(2000) | Y |  |  |  |
| 7 | ZB | nvarchar(2000) | Y |  |  |  |
| 8 | DJ | nvarchar(2000) | Y |  |  |  |
| 9 | JS | nvarchar(2000) | Y |  |  |  |
| 10 | RS | nvarchar(2000) | Y |  |  |  |
| 11 | GS | nvarchar(2000) | Y |  |  |  |
| 12 | CZ | nvarchar(2000) | Y |  |  |  |
| 13 | JY | nvarchar(2000) | Y |  |  |  |
| 14 | GXMC | nvarchar(2000) | Y |  |  |  |
| 15 | SGSM | nvarchar(2000) | Y |  |  |  |
| 16 | SHEBEI | nvarchar(2000) | Y |  |  |  |
| 17 | WEIWAI | nvarchar(2000) | Y |  |  |  |

### RPD_ZPGYK

**行数** 885 ｜ **主键** REPORTID, PAGEID, ROWNO

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | PAGEID | int |  |  | PK |  |
| 3 | ROWNO | int |  |  | PK |  |
| 4 | DJY | nvarchar(2000) | Y |  |  |  |
| 5 | GJY | nvarchar(2000) | Y |  |  |  |
| 6 | XIANGHAO | nvarchar(2000) | Y |  |  |  |
| 7 | CHANLIANG | nvarchar(2000) | Y |  |  |  |
| 8 | DYCL | nvarchar(2000) | Y |  |  |  |
| 9 | CLDE | nvarchar(2000) | Y |  |  |  |
| 10 | SYCL | nvarchar(2000) | Y |  |  |  |
| 11 | SCBH | nvarchar(2000) | Y |  |  |  |
| 12 | ITEMMASTERCODE | nvarchar(2000) | Y |  |  |  |

### RPD_ZPGYKMX

**行数** 5,843 ｜ **主键** REPORTID, PAGEID, ROWNO

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | REPORTID | varchar(50) |  |  | PK |  |
| 2 | PAGEID | int |  |  | PK |  |
| 3 | ROWNO | int |  |  | PK |  |
| 4 | GX | nvarchar(2000) | Y |  |  |  |
| 5 | SGSM | nvarchar(2000) | Y |  |  |  |
| 6 | GJ | nvarchar(2000) | Y |  |  |  |
| 7 | GS | nvarchar(2000) | Y |  |  |  |
| 8 | CZ | nvarchar(2000) | Y |  |  |  |
| 9 | JY | nvarchar(2000) | Y |  |  |  |
| 10 | GXMC | nvarchar(2000) | Y |  |  |  |
| 11 | SHEBEI | nvarchar(2000) | Y |  |  |  |
| 12 | WEIWAI | nvarchar(2000) | Y |  |  |  |
| 13 | DJ | nvarchar(2000) | Y |  |  |  |
| 14 | HJ | nvarchar(2000) | Y |  |  |  |

### SELECTOBJSHAREINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | GROUPID | nvarchar(100) | Y |  |  |  |
| 7 | GROUPCODE | nvarchar(20) | Y |  |  |  |
| 8 | OBJECTID | nvarchar(100) | Y |  |  |  |
| 9 | SHAREEXPIREVALUE | bigint | Y |  |  |  |
| 10 | EXPIREDANDUNREFRESH | bit | Y |  |  |  |
| 11 | OBJOWNGID | nvarchar(100) | Y |  |  |  |

### SENDQGDDATAVERSION

**行数** 28,728 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | FORMTYPE | nvarchar(100) | Y | (N'PSE') |  |  |
| 9 | BOMPARENTEID | nvarchar(100) | Y |  |  |  |
| 10 | BOMPARENTEUID | nvarchar(100) | Y |  |  |  |
| 11 | PSEITEMEID | nvarchar(100) | Y |  |  |  |
| 12 | PSEITEMEUID | nvarchar(100) | Y |  |  |  |
| 13 | SENDLQUALITY | nvarchar(100) | Y |  |  |  |
| 14 | LASTSENDVERSION | nvarchar(100) | Y |  |  |  |
| 15 | SYZZ | nvarchar(100) | Y |  |  |  |
| 16 | ERPID | nvarchar(100) | Y |  |  |  |
| 17 | ERPNUMBER | nvarchar(100) | Y |  |  |  |

### SHAREREFRESHLOCK

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ISLOCK | bit | Y |  |  |  |

### SHOWVIEWCOLUMN

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | OBJECTNAME | nvarchar(100) | Y |  |  |  |
| 8 | COLUMNNAME | nvarchar(100) | Y |  |  |  |
| 9 | SHOWVIEWID | nvarchar(100) | Y |  |  |  |
| 10 | SORTNO | int |  |  |  |  |

### SHOWVIEWINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | OBJECTNAME | nvarchar(2000) | Y |  |  |  |

### SLAVETABLEPOSITIONINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | SLAVEID | nvarchar(100) | Y |  |  |  |
| 7 | MASTERID | nvarchar(100) | Y |  |  |  |
| 8 | STARTPOSITION | nvarchar(100) | Y |  |  |  |

### TASKLOGTRANCE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | TASKID | nvarchar(100) | Y |  |  |  |
| 7 | TASKEUID | nvarchar(100) | Y |  |  |  |
| 8 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 9 | ACTION | nvarchar(100) | Y |  |  |  |
| 10 | DESINFO | nvarchar(100) | Y |  |  |  |
| 11 | PARENTTASKID | nvarchar(100) | Y |  |  |  |
| 12 | TASKENAME | nvarchar(100) | Y |  |  |  |
| 13 | PROJECTENAME | nvarchar(100) | Y |  |  |  |

### TEMPTOOLING

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y | (N'Using') |  |  |
| 5 | STAGESTATEID | nvarchar(20) | Y |  |  |  |
| 6 | EVERSION | int | Y |  |  |  |
| 7 | EMINORVERSION | int | Y |  |  |  |
| 8 | EUID | nvarchar(100) | Y |  |  |  |
| 9 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 10 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 11 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 12 | CREATETIME | bigint | Y |  |  |  |
| 13 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 14 | UPDATETIME | bigint | Y |  |  |  |
| 15 | OWNUID | nvarchar(50) | Y |  |  |  |
| 16 | OWNGID | nvarchar(50) | Y |  |  |  |
| 17 | ESTATION | nvarchar(100) | Y |  |  |  |
| 18 | ICON | nvarchar(200) | Y |  |  |  |
| 19 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 20 | SERIALNUMBER | int | Y |  |  |  |
| 21 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 22 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 23 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 24 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 25 | RELEASEDATE | bigint | Y |  |  |  |
| 26 | POSITION | nvarchar(100) | Y |  |  |  |
| 27 | CURITERATION | bigint | Y | (N'1') |  |  |
| 28 | CHECKOUTUSER | nvarchar(100) | Y |  |  |  |
| 29 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |
| 30 | CHECKOUTDATE | bigint | Y |  |  |  |
| 31 | ITEMTYPE | nvarchar(50) |  |  |  |  |
| 32 | ORIGIN | nvarchar(100) | Y |  |  |  |
| 33 | FIRSTPRODUCTCODE | nvarchar(100) | Y |  |  |  |
| 34 | UNIVERSAL | nvarchar(100) | Y |  |  |  |
| 35 | UNIT | nvarchar(100) | Y | (N'1') |  |  |
| 36 | VALIDITYID | nvarchar(100) | Y |  |  |  |
| 37 | FORMID | nvarchar(50) | Y |  |  |  |
| 38 | FORMTYPE | nvarchar(20) | Y | (N'PSE') |  |  |
| 39 | XLJTYPE | nvarchar(20) | Y |  |  |  |
| 40 | CONFIGNAME | nvarchar(100) | Y |  |  |  |
| 41 | SRCITEMID | nvarchar(50) | Y |  |  |  |
| 42 | GGXH | nvarchar(100) | Y |  |  |  |

### TFOBJECT

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 12 | SERIALNUMBER | int | Y |  |  |  |
| 13 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 14 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 15 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 16 | POSITION | nvarchar(100) |  |  |  |  |
| 17 | TYPE | nvarchar(100) |  |  |  |  |
| 18 | CUTINFO | ntext | Y |  |  |  |

### TFOBJECTFOLDER

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | OWNUID | nvarchar(50) | Y |  |  |  |
| 8 | OWNGID | nvarchar(50) | Y |  |  |  |
| 9 | ESTATION | nvarchar(100) | Y |  |  |  |
| 10 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 11 | SERIALNUMBER | int | Y |  |  |  |
| 12 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 13 | TEMPLATEFOLDERID | nvarchar(50) | Y |  |  |  |

### TFOBJECTLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | VERSIONEID | nvarchar(100) | Y |  |  |  |

### TFTEMPLATE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | OWNUID | nvarchar(50) | Y |  |  |  |
| 8 | OWNGID | nvarchar(50) | Y |  |  |  |
| 9 | ESTATION | nvarchar(100) | Y |  |  |  |
| 10 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 11 | SERIALNUMBER | int | Y |  |  |  |
| 12 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |

### TFTEMPLATEFOLDER

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | OWNUID | nvarchar(50) | Y |  |  |  |
| 8 | OWNGID | nvarchar(50) | Y |  |  |  |
| 9 | ESTATION | nvarchar(100) | Y |  |  |  |
| 10 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 11 | SERIALNUMBER | int | Y |  |  |  |
| 12 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 13 | TFTEMPLATEID | nvarchar(150) | Y |  |  |  |

### TFTEMPLATELINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | ISALLOWCUT | int | Y | (N'1') |  |  |
| 16 | HOSTMACHINE | nvarchar(100) | Y |  |  |  |
| 17 | INSTAGE | nvarchar(100) | Y |  |  |  |
| 18 | FILETEMPLATEID | nvarchar(100) | Y |  |  |  |
| 19 | FILETEMPLATENAME | nvarchar(100) | Y |  |  |  |

### THEMDESIGNDATA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 6 | SERIALNUMBER | int | Y |  |  |  |
| 7 | THEMDATA | ntext | Y |  |  |  |
| 8 | DESIGNDATA | ntext | Y |  |  |  |
| 9 | TEMPORARYDATA | ntext | Y |  |  |  |

### THIRDEAISETTING

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | THIRDDB | nvarchar(200) | Y |  |  |  |
| 7 | SETTINGTYPE | nvarchar(100) | Y |  |  |  |
| 8 | SETTINGNAME | nvarchar(100) | Y |  |  |  |
| 9 | REPEATEDSEND | nvarchar(100) | Y |  |  |  |
| 10 | SENDDATAWORKFLOWSTATE | nvarchar(100) | Y |  |  |  |

### TIMEQUOTA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | EVERSION | int | Y |  |  |  |
| 6 | EUID | nvarchar(100) | Y |  |  |  |
| 7 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 8 | ELASTFINALSTATE | nvarchar(100) | Y |  |  |  |
| 9 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | CREATETIME | bigint | Y |  |  |  |
| 11 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 12 | UPDATETIME | bigint | Y |  |  |  |
| 13 | OWNUID | nvarchar(50) | Y |  |  |  |
| 14 | OWNGID | nvarchar(50) | Y |  |  |  |
| 15 | ESTATION | nvarchar(100) | Y |  |  |  |
| 16 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 17 | SERIALNUMBER | int | Y |  |  |  |
| 18 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 19 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 20 | RELEASEDATE | bigint | Y |  |  |  |
| 21 | PROCESSID | nvarchar(100) | Y |  |  |  |
| 22 | CMOPUTEPARA | nvarchar(100) | Y |  |  |  |
| 23 | TTQID | nvarchar(100) | Y |  |  |  |
| 24 | PHOURS | nvarchar(100) | Y |  |  |  |
| 25 | SHOURS | nvarchar(100) | Y |  |  |  |
| 26 | COMPUTEMODE | nvarchar(100) | Y |  |  |  |
| 27 | CALCULATOR | nvarchar(100) | Y |  |  |  |
| 28 | CALCULATTIME | nvarchar(100) | Y |  |  |  |
| 29 | MSEPOSITION | nvarchar(100) | Y |  |  |  |

### TIMEQUOTALINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |

### TIMEQUOTAPARA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | TQKEY | nvarchar(100) | Y |  |  |  |
| 16 | TQVALUE | nvarchar(100) | Y |  |  |  |
| 17 | TQEID | nvarchar(100) | Y |  |  |  |

### TIMEREXECUTERECORD

**行数** 1 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | TYPE | nvarchar(100) | Y |  |  |  |
| 7 | LASTEXECUTEDATE | bigint | Y |  |  |  |
| 8 | FAILIDS | ntext | Y |  |  |  |

### TSCLASS

**行数** 3 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | OBJECTID | nvarchar(100) | Y |  |  |  |
| 16 | ZHUANYE | nvarchar(100) | Y |  |  |  |
| 17 | ZHUJICHANG | nvarchar(100) | Y |  |  |  |
| 18 | ISCONFIG | nvarchar(100) | Y |  |  |  |

### TSCLASSATTRIBUTE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | OBJECTID | nvarchar(200) | Y |  |  |  |
| 16 | ATTRIBUTECODE | nvarchar(200) | Y |  |  |  |
| 17 | ATTRIBUTEVALUE | nvarchar(200) | Y |  |  |  |
| 18 | ZHUANYE | nvarchar(100) | Y |  |  |  |
| 19 | ZHUJICHANG | nvarchar(100) | Y |  |  |  |

### TSCLASSTOGROUP

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | OBJECTID | nvarchar(200) | Y |  |  |  |
| 16 | GROUPID | nvarchar(200) | Y |  |  |  |
| 17 | ISKNOWLEDGE | nvarchar(100) | Y |  |  |  |
| 18 | SORT | int | Y |  |  |  |
| 19 | ZHUANYE | nvarchar(100) | Y |  |  |  |
| 20 | ZHUJICHANG | nvarchar(100) | Y |  |  |  |

### TSCONFIGOBJ

**行数** 84 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 15 | PCODE | nvarchar(200) | Y |  |  |  |
| 16 | PTYPE | nvarchar(200) | Y |  |  |  |
| 17 | CURCODE | nvarchar(200) | Y |  |  |  |
| 18 | CURTYPE | nvarchar(200) | Y |  |  |  |
| 19 | SORT | int | Y |  |  |  |
| 20 | RELATEDTSCLASS | nvarchar(200) | Y |  |  |  |
| 21 | ISKNOWLEDGEATT | nvarchar(100) | Y | (N'1') |  |  |
| 22 | ISKEYATTRIBUTE | nvarchar(100) | Y | (N'1') |  |  |

### TSGROUP

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | OBJECTID | nvarchar(200) | Y |  |  |  |
| 16 | CLASSCODE | nvarchar(200) | Y |  |  |  |
| 17 | ISKNOWLEDGE | nvarchar(200) | Y |  |  |  |
| 18 | ZHUANYE | nvarchar(100) | Y |  |  |  |
| 19 | ZHUJICHANG | nvarchar(100) | Y |  |  |  |

### TSGROUPATTRIBUTE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | GROUPID | nvarchar(200) | Y |  |  |  |
| 16 | ATTRIBUTECODE | nvarchar(200) | Y |  |  |  |
| 17 | ATTRIBUTEVALUE | nvarchar(200) | Y |  |  |  |

### TSGROUPTOGROUP

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | PARENTGROUPID | nvarchar(200) | Y |  |  |  |
| 16 | CHILDGROUPID | nvarchar(200) | Y |  |  |  |
| 17 | SORT | int | Y |  |  |  |

### TSMANAGER

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | WORKPLACE | nvarchar(200) | Y |  |  |  |
| 16 | PROJECTINFO | nvarchar(100) | Y |  |  |  |
| 17 | PROCESSFLOWFILECODE | nvarchar(200) | Y |  |  |  |
| 18 | PFMEAFILECODE | nvarchar(200) | Y |  |  |  |
| 19 | CONTROLPLANFILECODE | nvarchar(200) | Y |  |  |  |
| 20 | ITEMCODE | nvarchar(200) | Y |  |  |  |
| 21 | ITEMNAME | nvarchar(200) | Y |  |  |  |
| 22 | PIVOTALDATE | nvarchar(200) | Y |  |  |  |
| 23 | PROCESSDUTY | nvarchar(200) | Y |  |  |  |
| 24 | KEYGROUP | nvarchar(200) | Y |  |  |  |
| 25 | STEPCODE | nvarchar(200) | Y |  |  |  |
| 26 | OPERATEDESCRIPTION | nvarchar(200) | Y |  |  |  |
| 27 | CHARACTERCODE | nvarchar(200) | Y |  |  |  |
| 28 | CHARACTERTYPE | nvarchar(200) | Y |  |  |  |
| 29 | CHARACTERDESCRIPTION | nvarchar(200) | Y |  |  |  |
| 30 | PROCESSCHARACTER | nvarchar(200) | Y |  |  |  |
| 31 | CHARACTERTOLERANCE | nvarchar(200) | Y |  |  |  |
| 32 | VARIATIONSOURCE | nvarchar(200) | Y |  |  |  |
| 33 | FAILUREMODELDESCRIPTION | nvarchar(200) | Y |  |  |  |
| 34 | FAILUREMODELRESULT | nvarchar(200) | Y |  |  |  |
| 35 | SEVERITY | nvarchar(200) | Y |  |  |  |
| 36 | FAILURELEVEL | nvarchar(100) | Y |  |  |  |
| 37 | FAILUREREASON | nvarchar(200) | Y |  |  |  |
| 38 | PREVENTDESCRIPTION | nvarchar(200) | Y |  |  |  |
| 39 | OCCURRENCERATE | nvarchar(200) | Y |  |  |  |
| 40 | DETECTCONTROLDESCRIPTION | nvarchar(200) | Y |  |  |  |
| 41 | DETECTRATE | nvarchar(200) | Y |  |  |  |
| 42 | RPN | nvarchar(200) | Y |  |  |  |
| 43 | ADVICEDESCRIPTION | nvarchar(200) | Y |  |  |  |
| 44 | FINISHDATE | nvarchar(200) | Y |  |  |  |
| 45 | TAKEACTIONDESCRIPTION | nvarchar(200) | Y |  |  |  |
| 46 | SEVERITYTWO | nvarchar(200) | Y |  |  |  |
| 47 | OCCURRENCERATETWO | nvarchar(200) | Y |  |  |  |
| 48 | DETECTRATETWO | nvarchar(200) | Y |  |  |  |
| 49 | RPNTWO | nvarchar(200) | Y |  |  |  |
| 50 | MEASURETECHNIQUE | nvarchar(200) | Y |  |  |  |
| 51 | SAMPLECAPACITY | nvarchar(200) | Y |  |  |  |
| 52 | SAMPLERATE | nvarchar(200) | Y |  |  |  |
| 53 | EQUIPMENTNAME | nvarchar(200) | Y |  |  |  |
| 54 | CONTROLMETHOD | nvarchar(200) | Y |  |  |  |
| 55 | RESPONSEPLAN | nvarchar(200) | Y |  |  |  |

### TTQGROUP

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | COEFFICIENTGROUP | nvarchar(100) |  | (N'1') |  |  |

### TTQGROUPLINK

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | LINKTYPE | nvarchar(100) | Y |  |  |  |

### TTQOBJ

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | COEFFICIENTQOBJ | nvarchar(100) |  | (N'1') |  |  |
| 16 | CATEGORYQOBJ | nvarchar(100) | Y | (N'PHOURS') |  |  |
| 17 | FORMULAQOBJ | nvarchar(450) | Y |  |  |  |
| 18 | CALCULATIONTYPE | nvarchar(100) | Y | (N'FormulaConstant') |  |  |

### TTQPARA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | UNITQPARA | nvarchar(100) | Y |  |  |  |
| 16 | PARATYPEQPARA | nvarchar(100) |  |  |  |  |
| 17 | FORMULAQPARA | nvarchar(max) | Y |  |  |  |
| 18 | CHOOSETYPEQPARA | int | Y | (N'0') |  |  |
| 19 | CALCULATIONTYPE | nvarchar(100) | Y | (N'FormulaConstant') |  |  |

### TTQPARAVALUE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | FORMULAVALUE | nvarchar(100) | Y |  |  |  |
| 16 | CALCULATIONTYPE | nvarchar(100) | Y |  |  |  |

### TYPICALROUTE

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | OWNUID | nvarchar(50) | Y |  |  |  |
| 7 | OWNGID | nvarchar(50) | Y |  |  |  |
| 8 | ESTATION | nvarchar(100) | Y |  |  |  |
| 9 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 10 | SERIALNUMBER | int | Y |  |  |  |
| 11 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 12 | ENAME | nvarchar(2000) | Y |  |  |  |
| 13 | ECODE | nvarchar(2000) | Y |  |  |  |
| 14 | PERGROUP | nvarchar(100) | Y |  |  |  |

### UNITVALIDITY

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | ID | nvarchar(100) | Y |  |  |  |
| 16 | STARTUNIT | nvarchar(100) | Y |  |  |  |
| 17 | ENDUNIT | nvarchar(100) | Y |  |  |  |
| 18 | PARENTID | nvarchar(100) | Y |  |  |  |
| 19 | RELATIONID | nvarchar(100) | Y |  |  |  |
| 20 | RELATIONEUID | nvarchar(100) | Y |  |  |  |

### USERGETHISTORY

**行数** 711 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 5 | UPDATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | OBJTYPE | nvarchar(100) | Y |  |  |  |
| 9 | FILEDNAME | nvarchar(100) | Y |  |  |  |
| 10 | USERID | nvarchar(100) | Y |  |  |  |
| 11 | PATH | nvarchar(2000) | Y |  |  |  |
| 12 | SHOWMODE | nvarchar(100) | Y |  |  |  |
| 13 | CODEKINDID | nvarchar(100) | Y |  |  |  |

### USERGLOBALINFO

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | GLOBALID | nvarchar(100) | Y |  |  |  |
| 7 | USERID | nvarchar(100) | Y |  |  |  |

### USERINFO

**行数** 247 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) |  |  |  |  |
| 4 | STATEID | nvarchar(20) |  |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) |  |  |  |  |
| 8 | UPDATETIME | bigint |  |  |  |  |
| 9 | ICON | nvarchar(200) | Y | (N'~/Platform/Image/Model/p_userobject.png') |  |  |
| 10 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 11 | SERIALNUMBER | int | Y |  |  |  |
| 12 | PERSONEID | nvarchar(200) |  |  |  |  |
| 13 | PASSWORD | nvarchar(200) | Y | (N'123456') |  |  |
| 14 | LANGUAGE | nvarchar(100) |  | (N'zhs') |  |  |
| 15 | TIMEZONE | nvarchar(100) |  | (N'+08:00') |  |  |
| 16 | THEME | nvarchar(100) |  | (N'cupertino') |  |  |
| 17 | HOMEPAGEMODE | nvarchar(100) |  | (N'ClassicHomePage') |  |  |
| 18 | AGENTID | nvarchar(100) | Y |  |  |  |
| 19 | AGENTSTARTDATETIME | bigint | Y | (N'0') |  |  |
| 20 | AGENTENDDATETIME | bigint | Y | (N'0') |  |  |
| 21 | WENCHATUSER | nvarchar(100) | Y |  |  |  |
| 22 | ISCADCONFIG | bit | Y | (N'0') |  |  |
| 23 | SIGNATURE | nvarchar(100) | Y |  |  |  |
| 24 | ENAMEPINYIN | nvarchar(500) |  |  |  |  |
| 25 | DWGSIGNATURE | nvarchar(100) | Y |  |  |  |
| 26 | QYWEIXINID | nvarchar(100) | Y |  |  |  |
| 27 | CHECKOPERATETIMESPAN | nvarchar(20) | Y |  |  |  |
| 28 | USERROLETYPE | nvarchar(100) | Y | (N'NormalUserType') |  |  |
| 29 | FACEID | nvarchar(200) | Y |  |  |  |
| 30 | CARDID | nvarchar(200) | Y |  |  |  |
| 31 | FEISHUID | nvarchar(50) | Y |  |  |  |
| 32 | DINGTALKID | nvarchar(50) | Y |  |  |  |
| 33 | RTX | nvarchar(100) | Y |  |  |  |

### WBSUSERINFORMATION

**行数** 26 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | USERNAME | nvarchar(1000) | Y |  |  |  |
| 7 | WBSVERSION | nvarchar(1000) | Y |  |  |  |
| 8 | IP | nvarchar(1000) | Y |  |  |  |

### WEBMODELVIEWDATA

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | VIEWDATA | ntext | Y |  |  |  |
| 10 | VIEWDICDATA | ntext | Y |  |  |  |

### WORKFLOWACTORINFO

**行数** 52,509 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 7 | TASKNODEID | nvarchar(100) | Y |  |  |  |
| 8 | ACTORINFO | ntext | Y |  |  |  |

### WORKFLOWATTACHINFO

**行数** 41,520 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 9 | ATTACHTYPE | nvarchar(100) | Y | (N'Attach') |  |  |
| 10 | OBJECTEID | nvarchar(100) | Y |  |  |  |
| 11 | OBJECTTYPE | nvarchar(100) | Y |  |  |  |
| 12 | OBJECTINITSTATE | nvarchar(100) | Y |  |  |  |
| 13 | INITSTAGESTATEID | nvarchar(100) | Y |  |  |  |
| 14 | WHETHERORNOTAUDIT | bit | Y | (N'1') |  |  |
| 15 | PROBLEMMARKER | nvarchar(100) | Y |  |  |  |
| 16 | MARKERUSER | nvarchar(100) | Y |  |  |  |
| 17 | MARKERFLOWNODEID | nvarchar(100) | Y |  |  |  |
| 18 | ATTACHORDER | int | Y |  |  |  |

### WORKFLOWCHANGESTATE

**行数** 11,931 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 7 | CHANGESTATE | nvarchar(100) | Y |  |  |  |

### WORKFLOWCLASSIFY

**行数** 5 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 4 | CREATETIME | bigint | Y |  |  |  |
| 5 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 6 | UPDATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 10 | PEID | nvarchar(100) | Y |  |  |  |

### WORKFLOWCODE

**行数** 12,258 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | FULLCODE | nvarchar(50) | Y |  |  |  |
| 7 | PRECODE | nvarchar(20) | Y |  |  |  |
| 8 | SUFFIX | int | Y |  |  |  |

### WORKFLOWENTERAUDITINFO

**行数** 328 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 7 | WORKFLOWTASKUID | nvarchar(100) | Y |  |  |  |
| 8 | OBJECTSTAGESTATE | nvarchar(100) | Y |  |  |  |

### WORKFLOWINFO

**行数** 11,930 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 8 | SERIALNUMBER | int | Y |  |  |  |
| 9 | FLOWADMINISTRATOR | nvarchar(100) | Y |  |  |  |
| 10 | FLOWPARAMETERS | ntext | Y |  |  |  |
| 11 | FLOWTEMPLATE | nvarchar(100) | Y |  |  |  |
| 12 | ISCOMMONMANGEMENT | bit | Y |  |  |  |
| 13 | TEMPLATETYPE | nvarchar(100) | Y |  |  |  |
| 14 | EXECLINES | nvarchar(2000) | Y |  |  |  |
| 15 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 16 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 17 | UPDATETIME | bigint | Y |  |  |  |

### WORKFLOWINFORM

**行数** 14,002 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | TYPE | nvarchar(100) | Y |  |  |  |
| 16 | RESULT | ntext | Y |  |  |  |
| 17 | WORKFLOWID | nvarchar(100) | Y |  |  |  |

### WORKFLOWPLACEONFILE

**行数** 3,933 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | PATHID | nvarchar(100) | Y |  |  |  |
| 7 | PATHNAME | nvarchar(2000) | Y |  |  |  |
| 8 | WORKFLOWID | nvarchar(100) | Y |  |  |  |

### WORKFLOWQUOTEATTACH

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | ATTACHID | nvarchar(100) | Y |  |  |  |
| 7 | WORKFLOWID | nvarchar(100) | Y |  |  |  |

### WORKFLOWTASKDEALWITH

**行数** 0 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |
| 16 | OBJECTID | nvarchar(100) | Y |  |  |  |
| 17 | WORKFLOWID | nvarchar(100) | Y |  |  |  |

### WORKFLOWTASKINFO

**行数** 55,897 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) | Y |  |  |  |
| 3 | STATEID | nvarchar(20) |  |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 7 | SERIALNUMBER | int | Y |  |  |  |
| 8 | WORKFLOWID | nvarchar(100) | Y |  |  |  |
| 9 | WORKFLOWNAME | nvarchar(200) | Y |  |  |  |
| 10 | WORKFLOWCODE | nvarchar(100) | Y |  |  |  |
| 11 | FLOWTEMPLATE | nvarchar(100) | Y |  |  |  |
| 12 | FLOWNODEID | nvarchar(50) | Y |  |  |  |
| 13 | TASKFLAG | nvarchar(100) | Y |  |  |  |
| 14 | TASKUID | nvarchar(50) | Y |  |  |  |
| 15 | TASKACTORID | nvarchar(50) | Y |  |  |  |
| 16 | AGENTUSERID | nvarchar(50) | Y |  |  |  |
| 17 | AGENTSTARTDATETIME | bigint | Y | (N'0') |  |  |
| 18 | AGENTENDDATETIME | bigint | Y | (N'0') |  |  |
| 19 | COMPLETEUSER | nvarchar(50) | Y |  |  |  |
| 20 | COMPLETETIME | bigint | Y |  |  |  |
| 21 | TASKRESULT | nvarchar(100) | Y |  |  |  |
| 22 | TASKOPINION | nvarchar(2000) | Y |  |  |  |
| 23 | ISCOMMONMANGEMENT | bit | Y |  |  |  |
| 24 | PREVIOUSNODEID | nvarchar(50) | Y |  |  |  |
| 25 | PLANNEDENDDATE | bigint | Y |  |  |  |
| 26 | ADJECTIVEREMINDTIME | bigint | Y |  |  |  |
| 27 | PREVIOUSTASKUID | nvarchar(50) | Y |  |  |  |
| 28 | TEMPLATETYPE | nvarchar(100) | Y |  |  |  |
| 29 | ASSISTTASKID | nvarchar(100) | Y |  |  |  |
| 30 | GROUPID | nvarchar(100) | Y |  |  |  |

### WORKFLOWTEMPLATE

**行数** 370 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | EVERSION | int | Y |  |  |  |
| 4 | EUID | nvarchar(100) | Y |  |  |  |
| 5 | ELASTINSERT | nvarchar(100) | Y |  |  |  |
| 6 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 7 | CREATETIME | bigint | Y |  |  |  |
| 8 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 9 | UPDATETIME | bigint | Y |  |  |  |
| 10 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 11 | SERIALNUMBER | int | Y |  |  |  |
| 12 | WORKFLOWTEMPLATETYPE | nvarchar(20) |  |  |  |  |
| 13 | DESCRIPTION | nvarchar(500) | Y |  |  |  |
| 14 | ISRUN | nvarchar(20) | Y |  |  |  |
| 15 | FLOWADMINISTRATOR | nvarchar(100) | Y |  |  |  |
| 16 | WORKFLOWTEMPLATEINFO | ntext | Y |  |  |  |
| 17 | PEID | nvarchar(100) |  |  |  |  |
| 18 | XMLNAME | nvarchar(100) |  |  |  |  |

### WORKLOG

**行数** 4 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | STATEID | nvarchar(20) | Y |  |  |  |
| 5 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 6 | CREATETIME | bigint | Y |  |  |  |
| 7 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 8 | UPDATETIME | bigint | Y |  |  |  |
| 9 | OWNUID | nvarchar(50) | Y |  |  |  |
| 10 | OWNGID | nvarchar(50) | Y |  |  |  |
| 11 | ESTATION | nvarchar(100) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 15 | TASKSTATE | nvarchar(100) | Y |  |  |  |
| 16 | LOGSTATE | nvarchar(100) | Y | (N'0') |  |  |
| 17 | AUDITOR | nvarchar(2000) | Y |  |  |  |
| 18 | LOGTYPE | nvarchar(100) |  |  |  |  |
| 19 | LOGCONTENT | nvarchar(2000) | Y |  |  |  |
| 20 | ACTWORK | nvarchar(100) |  |  |  |  |
| 21 | EXTRAWORK | nvarchar(100) | Y | (N'0') |  |  |
| 22 | LOGPERCENTS | nvarchar(100) |  | (N'100') |  |  |
| 23 | LOGEXPERIENCE | nvarchar(500) | Y |  |  |  |
| 24 | LOGFOUCER | nvarchar(2000) | Y |  |  |  |
| 25 | TASKEUID | nvarchar(100) | Y |  |  |  |
| 26 | PROJECTID | nvarchar(100) | Y |  |  |  |
| 27 | PROJECTCODE | nvarchar(100) | Y |  |  |  |
| 28 | LOGDATE | bigint | Y |  |  |  |
| 29 | TASKID | nvarchar(100) | Y |  |  |  |
| 30 | PROJECTNAME | nvarchar(100) | Y |  |  |  |
| 31 | REMARKCNT | int | Y | (N'0') |  |  |
| 32 | AUDITTIME | nvarchar(100) | Y |  |  |  |
| 33 | ISMULTILOGFOURCER | int | Y | (N'0') |  |  |
| 34 | ISMULTIAUDITOR | int | Y | (N'0') |  |  |

### WORKLOGREMARK

**行数** 0 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 3 | CREATETIME | bigint | Y |  |  |  |
| 4 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 5 | SERIALNUMBER | int | Y |  |  |  |
| 6 | REMARKCONTENT | nvarchar(max) | Y |  |  |  |
| 7 | WORKLOGID | nvarchar(100) | Y |  |  |  |
| 8 | REPLYREMARKID | nvarchar(100) | Y |  |  |  |
| 9 | REMARKTYPE | nvarchar(100) | Y | (N'LOGREMARK') |  |  |

### WSFOLDER

**行数** 2,677 ｜ **主键** EID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | EID | nvarchar(100) |  |  | PK |  |
| 2 | ENAME | nvarchar(200) |  |  |  |  |
| 3 | ECODE | nvarchar(100) | Y |  |  |  |
| 4 | CREATEUID | nvarchar(50) | Y |  |  |  |
| 5 | CREATETIME | bigint | Y |  |  |  |
| 6 | UPDATEUID | nvarchar(50) | Y |  |  |  |
| 7 | UPDATETIME | bigint | Y |  |  |  |
| 8 | OWNUID | nvarchar(50) | Y |  |  |  |
| 9 | OWNGID | nvarchar(50) | Y |  |  |  |
| 10 | ESTATION | nvarchar(100) | Y |  |  |  |
| 11 | ICON | nvarchar(200) | Y |  |  |  |
| 12 | ECOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | SERIALNUMBER | int | Y |  |  |  |
| 14 | OWNGROUPCODE | nvarchar(20) | Y |  |  |  |
| 15 | SHAREGROUPCODE | varchar(4000) | Y |  |  |  |
| 16 | DESCRIPTION | nvarchar(2000) | Y |  |  |  |
| 17 | WORKFLOWSTATE | nvarchar(20) | Y | (N'New') |  |  |
| 18 | POSITION | nvarchar(100) |  |  |  |  |
| 19 | INHERITPERMISSION | bit | Y | (N'1') |  |  |
| 20 | PARENTOBJECTID | nvarchar(100) | Y |  |  |  |
| 21 | CHECKOUTSTATE | nvarchar(20) | Y | (N'0') |  |  |

### WSFOLDERCONTENT

**行数** 376,564 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 11 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 12 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 13 | PEUID | nvarchar(100) | Y |  |  |  |
| 14 | CEUID | nvarchar(100) | Y |  |  |  |
| 15 | OBJECTID | nvarchar(50) | Y |  |  |  |

### WSPOSITION

**行数** 87 ｜ **主键** LID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | LID | nvarchar(100) |  |  | PK |  |
| 2 | PEID | nvarchar(100) |  |  |  |  |
| 3 | PETYPE | nvarchar(100) | Y |  |  |  |
| 4 | CEID | nvarchar(100) |  |  |  |  |
| 5 | CETYPE | nvarchar(100) | Y |  |  |  |
| 6 | LSEQUENCE | int |  | (N'0') |  |  |
| 7 | LQUALITY | float |  | (N'1') |  |  |
| 8 | LSTATION | nvarchar(100) | Y |  |  |  |
| 9 | LCREATEUID | nvarchar(50) | Y |  |  |  |
| 10 | LCREATETIME | bigint |  |  |  |  |
| 11 | LOWNUID | nvarchar(50) | Y |  |  |  |
| 12 | LOWNGID | nvarchar(50) | Y |  |  |  |
| 13 | LCOMPANYID | nvarchar(50) | Y |  |  |  |
| 14 | PEUID | nvarchar(100) | Y |  |  |  |
| 15 | CEUID | nvarchar(100) | Y |  |  |  |

### XMLPERSISTENCE

**行数** 0 ｜ **主键** XMLID

| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |
|---|---|---|---|---|---|---|
| 1 | XMLID | nvarchar(100) |  |  | PK |  |
| 2 | XMLTYPE | nvarchar(100) |  |  |  |  |
| 3 | XMLNAME | nvarchar(200) | Y |  |  |  |
| 4 | XMLINFO | ntext | Y |  |  |  |
| 5 | SORDER | int | Y | (N'0') |  |  |

## 五、外键关系

共 0 条。

| 子表.列 | → 父表.列 | 约束 |
|---|---|---|

## 六、视图清单

| 视图 |
|---|
| ACTIONRULEMESSAGEROOT |
| ACTIONRULEROOT |
| CAPPRESOURCEMANAGER |
| CLASSIFYMANAGE |
| CODESOLUTION |
| COMMANDCLUSTERROOT |
| COMPANYRULEROOT |
| CUSTOMBOARDITEMROOT |
| CUSTORMFORM |
| DOCFILETEMPLATEROOT |
| DOCMASTER |
| EAISETTINGNODE |
| ENUMERATIONS |
| FAVORITE |
| FSMANAGEROOT |
| GROUPROLE |
| INTERNALMAILNODE |
| ISSUENODE |
| ISSUEWAREHOURSE |
| ITGTPLCLASSIFYROOT |
| MCOBJROOT |
| MOBILEPAGEDEVROOT |
| MSECLASSROOT |
| MSEMATERIALMASTER |
| MSEPARAPROROOT |
| MSETIMEMASTER |
| MTGROUPROOT |
| MYPROJECTCOST |
| MYPROJECTNODE |
| MYTASK |
| ORGANIZATION |
| PLMSTATISTICSROOT |
| POSITIONMANAGER |
| PROCESSMASTER |
| PROCESSROOT |
| PROJECTFOLDERTEMPLATEROOTNODE |
| PROJECTMASTER |
| PROJECTTASKTEMPLATEROOTNODE |
| PROJECTTEMPLATEROOTNODE |
| PROJECTWORKREPORTNODE |
| PROUTEMASTER |
| PSEITEMMASTER |
| QPROBLEMNODE |
| REPORTMASTER |
| RESOURCECLASSIFYROOT |
| RISKNODE |
| RISKWAREHOURSE |
| TASKINFO_VIEW |
| TASKQUERY |
| TFTEMPLATEROOT |
| TIMEQUOTAMASTER |
| TTQGROUPROOT |
| TYPICALJOURNEYROOT |
| TYPICALPROCESSROOT |
| TYPICALROOT |
| WORKFLOWCLASSIFYROOT |
| WORKFLOWNODE |
| WORKREPORTNODE |
| WSGROUPSPACE |
| WSMYSPACE |
| WSPROJECT |
| WSPUBLICSPACE |

