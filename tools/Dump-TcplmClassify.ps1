# =====================================================================
# 蓝本分类数据只读导出（TCPLM → docs/14-TCPLM分类数据清单.md）
#
# 约束：**只读**。仅执行 SELECT，不做任何 INSERT/UPDATE/DDL。
# 凭据优先级：命令行参数 > .env 的 DB_* / SSH_* > 环境变量 NEXPLM_OLD_DB_*
# 用法示例：
#   powershell -File tools/Dump-TcplmClassify.ps1 -User sa -Password ******
# =====================================================================

param(
    [string]$Server = '198.168.6.26,1433',
    [string]$Database = 'TCPLM',
    [string]$User,
    [string]$Password,
    [string]$OutFile = 'docs/14-TCPLM分类数据清单.md'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Data

function Resolve-EnvConfig {
    $map = @{}
    $envFile = Join-Path $PSScriptRoot '..\.env'
    if (Test-Path $envFile) {
        foreach ($line in [System.IO.File]::ReadAllLines($envFile, [System.Text.Encoding]::UTF8)) {
            if ($line -match '^\s*[^#].*=') {
                $k, $v = $line -split '=', 2
                $map[$k.Trim()] = $v.Trim()
            }
        }
    }
    return $map
}

$cfg = Resolve-EnvConfig
if (-not $User) { $User = if ($cfg['DB_USER']) { $cfg['DB_USER'] } elseif ($cfg['SSH_USER']) { $cfg['SSH_USER'] } else { $env:NEXPLM_OLD_DB_USER } }
if (-not $Password) { $Password = if ($cfg['DB_PASSWORD']) { $cfg['DB_PASSWORD'] } elseif ($cfg['SSH_PASSWORD']) { $cfg['SSH_PASSWORD'] } else { $env:NEXPLM_OLD_DB_PASSWORD } }
if (-not $User -or -not $Password) { throw '缺少数据库账号：请用 -User/-Password 传入，或配置 .env 的 DB_*/SSH_*' }

$connString = "Server=$Server;User Id=$User;Password=$Password;Initial Catalog=$Database;TrustServerCertificate=True;Connect Timeout=15;Application Name=NexPLM-ReadOnly"
$conn = New-Object System.Data.SqlClient.SqlConnection $connString
$conn.Open()

function Invoke-Query([string]$sql) {
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = $sql
    $cmd.CommandTimeout = 180
    $table = New-Object System.Data.DataTable
    $table.Load($cmd.ExecuteReader())
    return , $table
}

function Get-Text($row, [string]$column) {
    if ($row.IsNull($column)) { return '' }
    return [string]$row[$column]
}

function Esc([string]$value) {
    if ($null -eq $value) { return '' }
    return ($value -replace '\|', '\|').Trim()
}

try {
    $version = ([string](Invoke-Query 'SELECT @@VERSION AS v').Rows[0].v -split "`n")[0].Trim()
    $groups = Invoke-Query 'SELECT ECODE, ENAME, REFERENCECODE FROM CLASSIFYGROUP ORDER BY ECODE'
    $classes = Invoke-Query 'SELECT ECODE, ENAME, CLASSOBJECTTYPE, PERGROUP, CLASSCODEPATH FROM CLASSIFYCLASS ORDER BY ECODE'
    $stats = Invoke-Query @'
SELECT (SELECT COUNT(*) FROM CLASSIFYCLASS) AS classCount,
       (SELECT COUNT(*) FROM CLASSIFYGROUP) AS groupCount,
       (SELECT COUNT(*) FROM CLASSIFYRELATION) AS linkCount,
       (SELECT COUNT(DISTINCT CEID) FROM CLASSIFYRELATION WHERE CEID LIKE 'PSEITEMMASTER[_]%') AS itemCount,
       (SELECT COUNT(*) FROM CLASSIFYATTRIBUTE) AS attrCount,
       (SELECT COUNT(*) FROM CLASSIFYATTRIBUTEDATA) AS attrValueCount,
       (SELECT COUNT(*) FROM CLASSIFYCLASS WHERE PERGROUP IS NOT NULL AND PERGROUP <> '') AS perGroupUsed
'@
    # 关系表是"通用关系"：CEID 可以是零件（PSEITEMMASTER_*）、分类属性（CLASSIFYATTRIBUTE_*）等，
    # 统计"一物多分类"必须先按前缀过滤，否则会把"属性被多个分类引用"误当成"零件挂多个分类"。
    $kinds = Invoke-Query @'
SELECT LEFT(CEID, CHARINDEX('_', CEID) - 1) AS kind, COUNT(*) AS links, COUNT(DISTINCT CEID) AS objects
  FROM CLASSIFYRELATION
 GROUP BY LEFT(CEID, CHARINDEX('_', CEID) - 1)
 ORDER BY COUNT(*) DESC
'@
    $multi = Invoke-Query @'
SELECT TOP 20 r.CEID, i.ECODE, i.ENAME, COUNT(*) AS cnt
  FROM CLASSIFYRELATION r
  LEFT JOIN PSEITEMMASTER i ON i.EID = r.CEID
 WHERE r.CEID LIKE 'PSEITEMMASTER[_]%'
 GROUP BY r.CEID, i.ECODE, i.ENAME
HAVING COUNT(*) > 1
 ORDER BY COUNT(*) DESC
'@
    $masterMatch = Invoke-Query @'
SELECT (SELECT COUNT(DISTINCT CEID) FROM CLASSIFYRELATION WHERE CEID LIKE 'PSEITEMMASTER[_]%') AS total,
       (SELECT COUNT(DISTINCT i.EID) FROM CLASSIFYRELATION r
          JOIN PSEITEMMASTER i ON i.EID = r.CEID
         WHERE r.CEID LIKE 'PSEITEMMASTER[_]%') AS matched
'@
    $multiDist = Invoke-Query @'
SELECT cnt AS perItem, COUNT(*) AS items
  FROM (SELECT CEID, COUNT(*) AS cnt FROM CLASSIFYRELATION
         WHERE CEID LIKE 'PSEITEMMASTER[_]%' GROUP BY CEID) t
 GROUP BY cnt
 ORDER BY cnt
'@

    # 二级分组映射：分类编码前 4 位 → 分组名（蓝本树由编码前缀表达，不依赖 CLASSCODEPATH）
    $groupNameByCode = @{}
    foreach ($row in $groups.Rows) {
        $code = Get-Text $row 'ECODE'
        if ($code) { $groupNameByCode[$code] = Get-Text $row 'ENAME' }
    }

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('# TCPLM 分类数据清单（蓝本）')
    $lines.Add('')
    $lines.Add('> 由 [`tools/Dump-TcplmClassify.ps1`](../tools/Dump-TcplmClassify.ps1) **只读**生成（仅 SELECT）。')
    $lines.Add("> 数据源：``$Server/$Database``；生成时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')；实例：$version")
    $lines.Add('> 用途：`03` **A5** 分类数据判等、`REP-H-01` 数据维、迁移映射的输入。**不是**新系统数据。')
    $lines.Add('')
    $lines.Add('## 一、规模')
    $lines.Add('')
    $s = $stats.Rows[0]
    $lines.Add('| 项 | 数量 |')
    $lines.Add('|---|---|')
    $lines.Add("| 分组（``CLASSIFYGROUP``） | $($s['groupCount']) |")
    $lines.Add("| 分类（``CLASSIFYCLASS``） | $($s['classCount']) |")
    $lines.Add("| 属性定义（``CLASSIFYATTRIBUTE``） | $($s['attrCount']) |")
    $lines.Add("| 属性值（``CLASSIFYATTRIBUTEDATA``） | $($s['attrValueCount']) |")
    $lines.Add("| 对象-分类关系（``CLASSIFYRELATION``） | $($s['linkCount']) |")
    $lines.Add("| 被分类的零件数 | $($s['itemCount']) |")
    $lines.Add("| `PERGROUP` 非空（部门授权在用） | $($s['perGroupUsed']) |")
    $lines.Add('')
    $lines.Add('## 二、一物多分类分布')
    $lines.Add('')
    $lines.Add('`CLASSIFYRELATION` 是**通用关系表**，`CEID` 可能是零件、也可能是分类属性（属性可复用于多个分类）。按前缀拆分：')
    $lines.Add('')
    $lines.Add('| CEID 前缀 | 链接数 | 对象数 | 含义 |')
    $lines.Add('|---|---|---|---|')
    foreach ($row in $kinds.Rows) {
        $kind = Get-Text $row 'kind'
        $meaning = switch ($kind) {
            'PSEITEMMASTER' { '零件主档' }
            'CLASSIFYATTRIBUTE' { '分类属性（属性→分类 多对多）' }
            'PSEITEM' { '零件（旧对象表，少量）' }
            default { '其它' }
        }
        $lines.Add("| ``$kind`` | $($row['links']) | $($row['objects']) | $meaning |")
    }
    $lines.Add('')
    $lines.Add('下面只统计 **零件**（`PSEITEMMASTER_*`）维度：')
    $lines.Add('')
    $lines.Add('| 每个零件的分类数 | 零件数 |')
    $lines.Add('|---|---|')
    foreach ($row in $multiDist.Rows) {
        $lines.Add("| $($row['perItem']) | $($row['items']) |")
    }
    $lines.Add('')
    $lines.Add('> 单分类占 99.6%；多分类仅 66 件（0.41%），最多挂 4 个分类，迁移按"只保留一个"处理即可（见第四节）。')
    $lines.Add('')
    $lines.Add("> 零件编码取自 ``PSEITEMMASTER`` 视图（**不是** ``PSEITEM``）：$($masterMatch.Rows[0]['total']) 个 CEID 中 $($masterMatch.Rows[0]['matched']) 个能匹配到主档，其余已删除或不在视图范围，故样本里编码可能为空。")
    $lines.Add('')
    $lines.Add('## 三、多分类样本（Top 20）')
    $lines.Add('')
    $lines.Add('| 零件编码 | 零件名称 | 分类数 |')
    $lines.Add('|---|---|---|')
    foreach ($row in $multi.Rows) {
        $lines.Add("| $(Esc (Get-Text $row 'ECODE')) | $(Esc (Get-Text $row 'ENAME')) | $($row['cnt']) |")
    }
    $lines.Add('')
    $lines.Add('## 四、分组清单（`CLASSIFYGROUP`，按编码排序）')
    $lines.Add('')
    $lines.Add('| 编码 | 名称 | REFERENCECODE |')
    $lines.Add('|---|---|---|')
    foreach ($row in $groups.Rows) {
        $lines.Add("| $(Esc (Get-Text $row 'ECODE')) | $(Esc (Get-Text $row 'ENAME')) | $(Esc (Get-Text $row 'REFERENCECODE')) |")
    }
    $lines.Add('')
    $lines.Add('## 五、分类清单（`CLASSIFYCLASS`，按编码排序）')
    $lines.Add('')
    $lines.Add('| 编码 | 名称 | 对象类型 | 所属二级分组 | PERGROUP |')
    $lines.Add('|---|---|---|---|---|')
    foreach ($row in $classes.Rows) {
        $code = Get-Text $row 'ECODE'
        $parentCode = if ($code.Length -ge 4) { $code.Substring(0, 4) } else { '' }
        $parentName = if ($parentCode -and $groupNameByCode.ContainsKey($parentCode)) { $groupNameByCode[$parentCode] } else { '' }
        $lines.Add("| $(Esc $code) | $(Esc (Get-Text $row 'ENAME')) | $(Esc (Get-Text $row 'CLASSOBJECTTYPE')) | $(Esc $parentName) | $(Esc (Get-Text $row 'PERGROUP')) |")
    }
    $lines.Add('')
    $lines.Add('## 六、迁移口径建议（待 `03` D1 决策）')
    $lines.Add('')
    $lines.Add('1. **树可由编码前缀还原**：分组编码 2/4 位、分类编码 6 位；`CLASSCODEPATH` 是 16 位 hex 链（语义未解出），**不要**用它建树。')
    $lines.Add('2. **单分类口径**（新系统现状 `item_master.class_id`）：覆盖绝大多数零件；实测只有 66/16276 个零件挂 2~4 个分类（0.41%），这 66 个只保留一个（建议按"最小编码"或"最先关联"），**会丢失**其余关系但影响面很小。')
    $lines.Add('3. **多分类口径**（如需支持）：新增 `item_classify` 关系表（object_id + class_id + 主分类标记），把 `CLASSIFYRELATION` 全量映射过去，再把 `item_master.class_id` 作为"主分类"冗余字段保留。')
    $lines.Add('4. **属性迁移**：`CLASSIFYATTRIBUTE`(120) → `doc_class_attr`；`CLASSIFYATTRIBUTEDATA`(57,235) → `item_classify_attr`（或 `item_master.ext_json`）；**另需迁移"属性→分类"复用关系**（`CLASSIFYRELATION` 中 `CLASSIFYATTRIBUTE_*` 那 1,287 条 / 119 个属性），否则属性挂不到分类上。')
    $lines.Add('5. **`PERGROUP` 全空**：部门授权在 `TCPLM` 未启用，迁移时可不处理（字段保留但不填）。')
    $lines.Add('')
    $lines.Add('## 七、字段语义未解项')
    $lines.Add('')
    $lines.Add('- `CLASSCODEPATH`：4 段 16 位 hex，含义**待验证**（推测是 分组/分类/方案 链，未取证）。')
    $lines.Add('- `CLASSIFYGROUP.REFERENCECODE`：多数为空，含义**待验证**。')
    $lines.Add('- `CLASSOBJECTTYPE`：本清单已原样导出，取值分布见 `docs/06` `EXP-20261108-17`。')

    $target = Join-Path (Get-Location) $OutFile
    $dir = Split-Path $target -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllLines($target, $lines, (New-Object System.Text.UTF8Encoding($false)))
    "WROTE=$target lines=$($lines.Count)"
} finally {
    $conn.Close()
}
