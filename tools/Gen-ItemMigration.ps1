<#
.SYNOPSIS
    按蓝本 PSEITEM 生成「零部件**样例**迁移」SQL（每类 N 条，dry-run，不执行）。

.DESCRIPTION
    现场现有物料均为测试数据，**不做全量迁移**，按 ITEMTYPE 每类抽 15 条（默认），
    用于验证字段映射、类型映射与分类归属是否可用；全量留给 A3 窗口。

    映射口径（写进生成文件头，便于 review）：
    1. ITEMTYPE 11 类 → 新系统 itemType（EXP-20261108-36 已定映射表）；
    2. PSEITEM + PSEBASEFORM 按 `b.ITEMID = i.EID` 合成一个对象（实测 116,035/116,035 全命中）；
    3. 分类归属取「单分类口径」：同一零件多条分类关系时取编码最小的一个（多分类 66 件，docs/14）；
    4. ORIGIN → source_type：`1`=MAKE / `2`=BUY（**方向待现场确认**，蓝本 1=47,597 / 2=47,548）；
    5. 规格型号取 `PSEITEM.GGXH`；重量 `WEIGHT` 是 nvarchar，能转成数字才落库；
    6. 编码直接沿用蓝本 `ECODE`；同批内重复时追加 `-<序号>` 并在文件里标注。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File tools/Gen-ItemMigration.ps1 `
        -User sa -Password ****** -PerType 15
#>
param(
    [string]$Server = '198.168.6.26,1433',
    [string]$Database = 'TCPLM',
    [string]$User,
    [string]$Password,
    [string]$ClassSql = 'deploy/migration/010-doc_class.sql',
    [string]$OutFile = 'deploy/migration/030-item_sample.sql',
    [int]$PerType = 15,
    [long]$IdBase = 9200000000001,
    [long]$CompanyId = 1,
    [long]$OwnerId = 1
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

# 分类编码 -> doc_class.id（来自 010，保证与分类树一致）
$classSqlPath = Join-Path $PSScriptRoot "..\$ClassSql"
$classIdByCode = @{}
if (Test-Path $classSqlPath) {
    foreach ($line in [System.IO.File]::ReadAllLines($classSqlPath, [System.Text.Encoding]::UTF8)) {
        if ($line -match "^SELECT\s+(\d+),\s+(?:NULL|\d+),\s+'PdmItem',\s+'([^']*)',") {
            $classIdByCode[$Matches[2]] = [long]$Matches[1]
        }
    }
}

# 蓝本 ITEMTYPE -> 新系统 itemType（对照 EXP-20261108-36）
$typeMap = @{
    'PSEITEMPRODUCTFORM'    = 'PRODUCT'
    'PSEITEMPARTFORM'       = 'PART'
    'PSEITEMSTANDARDFORM'   = 'STANDARD'
    'PSEITEMFIXTUREFORM'    = 'TOOL'
    'PSEITEMGAUGEFORM'      = 'GAUGE'
    'PSEITEMCISFORM'        = 'CIS'
    'PSEITEMSGD'            = 'MANUAL'
    'PSEITEMWAIXIE'         = 'OUTSOURCE'
    'PSEITEMFUWU'           = 'SERVICE'
    'PSEITEMFEILIAO'        = 'SCRAP'
    'PSEITEMYOUWUWUZHANG'   = 'PART'
}

$connString = "Server=$Server;User Id=$User;Password=$Password;Initial Catalog=$Database;TrustServerCertificate=True;Connect Timeout=15;Application Name=NexPLM-ReadOnly"
$conn = New-Object System.Data.SqlClient.SqlConnection $connString
$conn.Open()

function Invoke-Query([string]$sql) {
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = $sql
    $cmd.CommandTimeout = 300
    $table = New-Object System.Data.DataTable
    $table.Load($cmd.ExecuteReader())
    return , $table
}

function Get-Text($row, [string]$column) {
    if ($row.IsNull($column)) { return '' }
    return [string]$row[$column]
}

function SqlText([string]$value) {
    if ($null -eq $value) { return 'NULL' }
    return "'" + ($value -replace "'", "''") + "'"
}

function Trunc([string]$value, [int]$max) {
    if (-not $value) { return '' }
    if ($value.Length -le $max) { return $value }
    return $value.Substring(0, $max)
}

try {
    # 每类取 N 条，优先取"已有分类归属"的（保证分类迁移能对上）
    $sql = @"
WITH picked AS (
    SELECT i.EID, i.ECODE, i.ENAME, i.ITEMTYPE, i.ORIGIN, i.UNIT, i.GGXH,
           ROW_NUMBER() OVER (PARTITION BY i.ITEMTYPE
                              ORDER BY CASE WHEN EXISTS (SELECT 1 FROM CLASSIFYRELATION r WHERE r.CEID = i.EID)
                                            THEN 0 ELSE 1 END, i.ECODE) AS rn
      FROM PSEITEM i
     WHERE i.ECODE IS NOT NULL AND i.ECODE <> '' AND i.ENAME IS NOT NULL AND i.ENAME <> ''
)
SELECT p.EID, p.ECODE, p.ENAME, p.ITEMTYPE, p.ORIGIN, p.UNIT, p.GGXH,
       b.MATERIAL, b.PROVIDER, b.DWGNO, b.WEIGHT, b.STANDARDNO, b.REMARKS,
       (SELECT TOP 1 c.ECODE FROM CLASSIFYRELATION r
          JOIN CLASSIFYCLASS c ON c.EID = r.PEID
         WHERE r.CEID = p.EID
         ORDER BY c.ECODE) AS classCode
  FROM picked p
  LEFT JOIN PSEBASEFORM b ON b.ITEMID = p.EID
 WHERE p.rn <= $PerType
 ORDER BY p.ITEMTYPE, p.ECODE
"@
    $rows = Invoke-Query $sql

    $objects = New-Object System.Collections.Generic.List[string]
    $masters = New-Object System.Collections.Generic.List[string]
    $usedCodes = @{}
    $duplicates = New-Object System.Collections.Generic.List[string]
    $typeCount = @{}
    $classHit = 0
    $seq = 0

    foreach ($row in $rows.Rows) {
        $seq++
        $rawType = Get-Text $row 'ITEMTYPE'
        $itemType = if ($typeMap.ContainsKey($rawType)) { $typeMap[$rawType] } else { 'PART' }
        if (-not $typeCount.ContainsKey($itemType)) { $typeCount[$itemType] = 0 }
        $typeCount[$itemType]++

        $code = (Get-Text $row 'ECODE').Trim()
        if ($usedCodes.ContainsKey($code)) {
            $duplicates.Add($code)
            $code = "$code-$seq"
        }
        $usedCodes[$code] = $true

        $name = Trunc (Get-Text $row 'ENAME').Trim() 200
        $id = $IdBase + $seq
        $origin = $row['ORIGIN']
        $sourceType = if ($origin -is [int] -and $origin -eq 2) { 'BUY' } else { 'MAKE' }
        $unit = Get-Text $row 'UNIT'
        if (-not $unit) { $unit = 'EA' }
        $spec = Trunc (Get-Text $row 'GGXH').Trim() 500
        $material = Trunc ((Get-Text $row 'MATERIAL').Trim()) 64

        $weightExpr = 'NULL'
        $weightRaw = (Get-Text $row 'WEIGHT').Trim()
        $weightNum = 0.0
        if ($weightRaw -and [double]::TryParse($weightRaw, [ref]$weightNum)) {
            $weightExpr = [string]::Format([Globalization.CultureInfo]::InvariantCulture, '{0}', $weightNum)
        }

        $classCode = Get-Text $row 'classCode'
        $classIdExpr = 'NULL'
        if ($classCode -and $classIdByCode.ContainsKey($classCode)) {
            $classIdExpr = [string]$classIdByCode[$classCode]
            $classHit++
        }

        # 先算成变量再 Add：Add((...) -f a, b) 会被解析成 Add(((...) -f a), b)
        $objStmt = ("INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,`n" +
                    "                          lifecycle_state, phase_state, workflow_state, revision, iteration,`n" +
                    "                          is_latest, deleted, version, ext_json, created_by, created_at,`n" +
                    "                          updated_by, updated_at)`n" +
                    "SELECT {0}, 'PdmItem', {0}, {1}, {2}, {3}, {4}, 'Draft', NULL, 'New', 1, 1,`n" +
                    "       true, false, 0, '{{}}'::jsonb, {4}, now(), {4}, now()`n" +
                    " WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = {1});") -f `
                   $id, (SqlText $code), (SqlText $name), $CompanyId, $OwnerId
        $objects.Add($objStmt)

        $masterStmt = ("INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,`n" +
                       "                         source_type, std_code, remark, using_state, class_id)`n" +
                       "SELECT {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}, 'Using', {9}`n" +
                       " WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = {0})`n" +
                       "   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = {0});") -f `
                      $id, (SqlText $itemType), (SqlText $spec), (SqlText $material), (SqlText $unit), $weightExpr,
                      (SqlText $sourceType), (SqlText (Trunc ((Get-Text $row 'STANDARDNO').Trim()) 64)),
                      (SqlText (Trunc ((Get-Text $row 'REMARKS').Trim()) 500)), $classIdExpr
        $masters.Add($masterStmt)
    }

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('-- =====================================================================')
    $lines.Add('-- 零部件**样例**迁移：蓝本 PSEITEM + PSEBASEFORM → plm_object + item_master')
    $lines.Add("-- 生成器：tools/Gen-ItemMigration.ps1（只读蓝本，仅生成 SQL，不执行）")
    $lines.Add("-- 数据源：$Server/$Database；生成时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
    $lines.Add("-- 统计：每类 $PerType 条，共 $seq 条对象；分类命中 $classHit 条；重复编码 $($duplicates.Count) 条（已加后缀）")
    $lines.Add('-- 按类型：' + (($typeCount.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join ', '))
    $lines.Add('-- 口径：')
    $lines.Add('--   1) ITEMTYPE 11 类 → itemType（EXP-20261108-36 映射表）；未知类型归 PART')
    $lines.Add('--   2) 规格型号取 PSEITEM.GGXH；材料取 PSEBASEFORM.MATERIAL；重量仅数字可转时落库')
    $lines.Add('--   3) 分类归属为单分类口径（多分类取编码最小），class_id 取自 010-doc_class.sql')
    $lines.Add('--   4) ORIGIN=2 → BUY，其余 → MAKE（方向待现场确认）')
    $lines.Add('--   5) 幂等：每个对象/子表都带 NOT EXISTS，可重复执行')
    $lines.Add('-- 注意：这是**样例**，不是全量；全量迁移留给 A3 窗口（改为 CSV + COPY）')
    $lines.Add('-- =====================================================================')
    $lines.Add('')
    $lines.Add('BEGIN;')
    $lines.Add('')
    foreach ($line in $objects) { $lines.Add($line) }
    $lines.Add('')
    foreach ($line in $masters) { $lines.Add($line) }
    $lines.Add('')
    $lines.Add('COMMIT;')
    if ($duplicates.Count -gt 0) {
        $lines.Add('')
        $lines.Add('-- 蓝本中跨类型重复的编码（已追加序号避免撞唯一索引）：')
        foreach ($d in ($duplicates | Select-Object -Unique)) { $lines.Add("--   $d") }
    }

    $target = Join-Path (Get-Location) $OutFile
    $dir = Split-Path $target -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllLines($target, $lines, (New-Object System.Text.UTF8Encoding($false)))
    "WROTE=$target objects=$seq classes=$classHit dupCodes=$($duplicates.Count)"
} finally {
    $conn.Close()
}
