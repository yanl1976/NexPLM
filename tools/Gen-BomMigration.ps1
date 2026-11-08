<#
.SYNOPSIS
    按蓝本 PSEBOMLINE 生成「BOM 样例迁移」两份 SQL（补零件 040 + BOM 行 050），dry-run。

.DESCRIPTION
    蓝本 BOM 的父子件 ID 口径不同（实测）：
    - 父件 `PEID` = `PSEITEM_<32hex>` → 对应 `PSEITEM.EID`（79,105/79,105 命中）；
    - 子件 `CEID` = `PSEITEMMASTER_<uuid>` → 必须走**视图** `PSEITEMMASTER`（`PSEITEM` 命中 0）。
    另：`USEQTY` / `TOTALQTY` 在蓝本**全为 0/空**，`ITEMROUTE` 全空，`POSNO` 有 11,522 条非空。
    因此：`qty` 一律写 1（不编造数量），`ref_designator` 取 `POSNO`，`bom_type` 取蓝本的 `Quote`。

    产出的 BOM 行要求父子件都在新系统里，所以：
    1. 先取 N 条 BOM，收集涉及的父/子件；
    2. 不在 `030-item_sample.sql` 里的零件，生成 `040-item_bom_parts.sql`（同一套映射）；
    3. 再生成 `050-bom_line.sql`，`parent_object_id/child_object_id` 从编码映射取。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File tools/Gen-BomMigration.ps1 `
        -User sa -Password ****** -Limit 100
#>
param(
    [string]$Server = '198.168.6.26,1433',
    [string]$Database = 'TCPLM',
    [string]$User,
    [string]$Password,
    [int]$Limit = 100,
    [string]$ClassSql = 'deploy/migration/010-doc_class.sql',
    [string]$ItemSql = 'deploy/migration/030-item_sample.sql',
    [string]$OutItems = 'deploy/migration/040-item_bom_parts.sql',
    [string]$OutBom = 'deploy/migration/050-bom_line.sql',
    [long]$ItemIdBase = 9200000001001,
    [long]$BomIdBase = 9300000000001,
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

# 1. 已迁移的零件编码 -> id（从 030 解析）
$itemIdByCode = @{}
$itemSqlPath = Join-Path $PSScriptRoot "..\$ItemSql"
if (-not (Test-Path $itemSqlPath)) { throw "找不到 030：$itemSqlPath（先跑 Gen-ItemMigration.ps1）" }
foreach ($line in [System.IO.File]::ReadAllLines($itemSqlPath, [System.Text.Encoding]::UTF8)) {
    if ($line -match "^SELECT\s+(\d+),\s+'PdmItem',\s+\d+,\s+'([^']*)',") {
        $itemIdByCode[$Matches[2]] = [long]$Matches[1]
    }
}

# 2. 分类编码 -> id（复用 010）
$classIdByCode = @{}
$classSqlPath = Join-Path $PSScriptRoot "..\$ClassSql"
if (Test-Path $classSqlPath) {
    foreach ($line in [System.IO.File]::ReadAllLines($classSqlPath, [System.Text.Encoding]::UTF8)) {
        if ($line -match "^SELECT\s+(\d+),\s+(?:NULL|\d+),\s+'PdmItem',\s+'([^']*)',") {
            $classIdByCode[$Matches[2]] = [long]$Matches[1]
        }
    }
}

$typeMap = @{
    'PSEITEMPRODUCTFORM'  = 'PRODUCT';  'PSEITEMPARTFORM'      = 'PART'
    'PSEITEMSTANDARDFORM' = 'STANDARD'; 'PSEITEMFIXTUREFORM'   = 'TOOL'
    'PSEITEMGAUGEFORM'    = 'GAUGE';    'PSEITEMCISFORM'       = 'CIS'
    'PSEITEMSGD'          = 'MANUAL';   'PSEITEMWAIXIE'        = 'OUTSOURCE'
    'PSEITEMFUWU'         = 'SERVICE';  'PSEITEMFEILIAO'       = 'SCRAP'
    'PSEITEMYOUWUWUZHANG' = 'PART'
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
    # 3. 取 N 条 BOM（父件走 PSEITEM、子件走 PSEITEMMASTER 视图）
    $bomSql = @"
SELECT TOP $Limit
       p.ECODE AS pcode, p.ENAME AS pname, p.ITEMTYPE AS ptype, p.GGXH AS pspec, p.UNIT AS punit, p.ORIGIN AS porigin,
       c.ECODE AS ccode, c.ENAME AS cname, c.ITEMTYPE AS ctype, c.GGXH AS cspec, c.UNIT AS cunit, c.ORIGIN AS corigin,
       l.LSEQUENCE, l.POSNO, l.LDESCRIPTION
  FROM PSEBOMLINE l
  JOIN PSEITEM p ON p.EID = l.PEID
  JOIN PSEITEMMASTER c ON c.EID = l.CEID
 WHERE p.ECODE IS NOT NULL AND p.ECODE <> '' AND c.ECODE IS NOT NULL AND c.ECODE <> ''
 ORDER BY p.ECODE, l.LSEQUENCE
"@
    $rows = Invoke-Query $bomSql

    # 4. 收集父子件（同一套字段用于补迁）
    $parts = [ordered]@{}
    $bomLines = New-Object System.Collections.Generic.List[object]
    foreach ($row in $rows.Rows) {
        $pcode = (Get-Text $row 'pcode').Trim()
        $ccode = (Get-Text $row 'ccode').Trim()
        if (-not $parts.Contains($pcode)) {
            $parts[$pcode] = [pscustomobject]@{
                Code = $pcode; Name = (Get-Text $row 'pname'); Type = (Get-Text $row 'ptype')
                Spec = (Get-Text $row 'pspec'); Unit = (Get-Text $row 'punit'); Origin = $row['porigin']
            }
        }
        if (-not $parts.Contains($ccode)) {
            $parts[$ccode] = [pscustomobject]@{
                Code = $ccode; Name = (Get-Text $row 'cname'); Type = (Get-Text $row 'ctype')
                Spec = (Get-Text $row 'cspec'); Unit = (Get-Text $row 'cunit'); Origin = $row['corigin']
            }
        }
        $bomLines.Add([pscustomobject]@{
            ParentCode = $pcode; ChildCode = $ccode
            LineNo = if ($row['LSEQUENCE'] -is [int]) { [int]$row['LSEQUENCE'] } else { 1 }
            PosNo = (Get-Text $row 'POSNO'); Remark = (Get-Text $row 'LDESCRIPTION')
            Unit = (Get-Text $row 'cunit')
        })
    }

    # 5. 补迁不在 030 里的零件（保持同一套映射）
    $extraObjs = New-Object System.Collections.Generic.List[string]
    $extraMasters = New-Object System.Collections.Generic.List[string]
    $newId = $ItemIdBase
    $addedCodes = @{}
    foreach ($code in $parts.Keys) {
        if ($itemIdByCode.ContainsKey($code)) { continue }
        $part = $parts[$code]
        $id = $newId++
        $itemIdByCode[$code] = $id
        $addedCodes[$code] = $true
        $itemType = if ($typeMap.ContainsKey($part.Type)) { $typeMap[$part.Type] } else { 'PART' }
        $name = Trunc $part.Name.Trim() 200
        $sourceType = if ($part.Origin -is [int] -and $part.Origin -eq 2) { 'BUY' } else { 'MAKE' }
        $unit = if ($part.Unit) { $part.Unit } else { 'EA' }

        $objStmt = ("INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,`n" +
                    "                          lifecycle_state, phase_state, workflow_state, revision, iteration,`n" +
                    "                          is_latest, deleted, version, ext_json, created_by, created_at,`n" +
                    "                          updated_by, updated_at)`n" +
                    "SELECT {0}, 'PdmItem', {0}, {1}, {2}, {3}, {4}, 'Draft', NULL, 'New', 1, 1,`n" +
                    "       true, false, 0, '{{}}'::jsonb, {4}, now(), {4}, now()`n" +
                    " WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = {1});") -f `
                   $id, (SqlText $code), (SqlText $name), $CompanyId, $OwnerId
        $extraObjs.Add($objStmt)

        $masterStmt = ("INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,`n" +
                       "                         source_type, std_code, remark, using_state, class_id)`n" +
                       "SELECT {0}, {1}, {2}, NULL, {3}, NULL, {4}, NULL, NULL, 'Using', NULL`n" +
                       " WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = {0})`n" +
                       "   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = {0});") -f `
                      $id, (SqlText $itemType), (SqlText (Trunc $part.Spec.Trim() 500)), (SqlText $unit), (SqlText $sourceType)
        $extraMasters.Add($masterStmt)
    }

    # 6. 生成 BOM 行
    $bomStatements = New-Object System.Collections.Generic.List[string]
    $bomSeq = 0
    $skipped = New-Object System.Collections.Generic.List[string]
    foreach ($line in $bomLines) {
        if (-not $itemIdByCode.ContainsKey($line.ParentCode) -or -not $itemIdByCode.ContainsKey($line.ChildCode)) {
            $skipped.Add("$($line.ParentCode) -> $($line.ChildCode)")
            continue
        }
        # 注意：$pid 是 PowerShell 只读变量（进程 ID），必须换名
        $parentId = $itemIdByCode[$line.ParentCode]
        $childId = $itemIdByCode[$line.ChildCode]
        if ($parentId -eq $childId) { $skipped.Add("$($line.ParentCode) 自引用"); continue }
        $id = $BomIdBase + ($bomSeq++)
        $unit = if ($line.Unit) { $line.Unit } else { 'EA' }
        $stmt = ("INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,`n" +
                 "                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,`n" +
                 "                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,`n" +
                 "                        created_by, created_at, updated_by, updated_at)`n" +
                 "SELECT {0}, {1}, {2}, 'DESIGN', {3}, 1, {4}, 0, {5}, {6}, 0, false, NULL, NULL,`n" +
                 "       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', {7}, now(), {7}, now()`n" +
                 " WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = {1} AND child_object_id = {2}`n" +
                 "                    AND view_type = 'DESIGN' AND deleted = false);") -f `
                $id, $parentId, $childId, $line.LineNo, (SqlText $unit),
                (SqlText (Trunc $line.PosNo.Trim() 120)), (SqlText (Trunc $line.Remark.Trim() 500)), $OwnerId
        $bomStatements.Add($stmt)
    }

    # 7. 写 040
    $head = New-Object System.Collections.Generic.List[string]
    $head.Add('-- =====================================================================')
    $head.Add('-- BOM 迁移补件：050 引用的父子件中，未包含在 030 样例里的零件')
    $head.Add('-- 生成器：tools/Gen-BomMigration.ps1（只读蓝本，仅生成 SQL，不执行）')
    $head.Add("-- 数据源：$Server/$Database；生成时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
    $head.Add("-- 补迁零件 $($addedCodes.Count) 个（父件走 PSEITEM、子件走 PSEITEMMASTER 视图）")
    $head.Add('-- 映射与 030 一致：ITEMTYPE → itemType、GGXH → specification、ORIGIN=2 → BUY')
    $head.Add('-- =====================================================================')
    $head.Add('')
    $head.Add('BEGIN;')
    $head.Add('')
    $body = New-Object System.Collections.Generic.List[string]
    foreach ($s in $extraObjs) { $body.Add($s) }
    $body.Add('')
    foreach ($s in $extraMasters) { $body.Add($s) }
    $body.Add('')
    $body.Add('COMMIT;')

    $targetItems = Join-Path (Get-Location) $OutItems
    $dir = Split-Path $targetItems -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllLines($targetItems, ($head + $body), (New-Object System.Text.UTF8Encoding($false)))

    # 8. 写 050
    $bomHead = New-Object System.Collections.Generic.List[string]
    $bomHead.Add('-- =====================================================================')
    $bomHead.Add('-- BOM 样例迁移：蓝本 PSEBOMLINE → bom_line')
    $bomHead.Add('-- 生成器：tools/Gen-BomMigration.ps1（只读蓝本，仅生成 SQL，不执行）')
    $bomHead.Add("-- 数据源：$Server/$Database；生成时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
    $bomHead.Add("-- 统计：取 $Limit 条 → 生成 $($bomStatements.Count) 条；跳过 $($skipped.Count) 条")
    $bomHead.Add('-- 口径：')
    $bomHead.Add('--   1) 视图 DESIGN；BOM 类型 QUOTE（蓝本 LBOMTYPE 全为 Quote）')
    $bomHead.Add('--   2) qty 一律 1：蓝本 USEQTY/TOTALQTY 实测全为 0/空，不编造数量')
    $bomHead.Add('--   3) line_no 取 LSEQUENCE；ref_designator 取 POSNO；remark 取 LDESCRIPTION')
    $bomHead.Add('--   4) 幂等：按 (parent, child, DESIGN) 判重')
    $bomHead.Add('-- 注意：执行顺序 030 → 040 → 050（050 依赖前两份的 id 映射）')
    $bomHead.Add('-- =====================================================================')
    $bomHead.Add('')
    $bomHead.Add('BEGIN;')
    $bomHead.Add('')
    $bomHead.AddRange($bomStatements)
    $bomHead.Add('')
    $bomHead.Add('COMMIT;')
    if ($skipped.Count -gt 0) {
        $bomHead.Add('')
        $bomHead.Add('-- 跳过明细（自引用或编码缺失）：')
        foreach ($s in ($skipped | Select-Object -Unique)) { $bomHead.Add("--   $s") }
    }

    $targetBom = Join-Path (Get-Location) $OutBom
    [System.IO.File]::WriteAllLines($targetBom, $bomHead, (New-Object System.Text.UTF8Encoding($false)))

    "WROTE=$OutItems parts=$($addedCodes.Count) | $OutBom lines=$($bomStatements.Count) skipped=$($skipped.Count)"
} finally {
    $conn.Close()
}
