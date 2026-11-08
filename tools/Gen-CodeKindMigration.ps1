<#
.SYNOPSIS
    按蓝本 CODEKIND / CODEVALUE / CLASSIFYCLASS.CLASSCODEPATH 生成编码种类与分类路径迁移 SQL（dry-run）。

.DESCRIPTION
    蓝本模型（EXP-20261108-44 取证）：
      CODEKIND(17) → CODERULE(58 码段) → CODEVALUE(1,170 码值)
      分类用 CLASSCODEPATH 绑定：`[种类] ; [码值1] ; [码值2] ; [*]`
      例：100102 → 元器件编码规则/1001/02/* → 1001 + 02 + 00001

    本脚本产出 `deploy/migration/060-code_kind_value.sql`：
      1) code_kind：只迁"有分类绑定"的种类（其余现场不取号，不必迁）；
      2) code_rule：每个种类一条，段定义 `VALUE,VALUE,SEQ(5)`；
      3) code_segment_value：路径用到的码值（大类/小类，含名称与 value_path）；
      4) UPDATE doc_class.code_path：258 条分类的绑定路径。

    只读蓝本，产物需在目标库执行；执行前必须先跑 V81 / 010。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File tools/Gen-CodeKindMigration.ps1 `
        -User sa -Password ******
#>
param(
    [string]$Server = '198.168.6.26,1433',
    [string]$Database = 'TCPLM',
    [string]$User,
    [string]$Password,
    [string]$OutFile = 'deploy/migration/060-code_kind_value.sql',
    [long]$ValueIdBase = 9500000000001,
    [long]$RuleIdBase = 930201
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

function ShortId([string]$eid) {
    # CODEKIND_<32hex> / CODEVALUE_<32hex> / CODERULE_<32hex> -> 去掉前缀后的前 16 hex
    # （CLASSCODEPATH 的段就是 16 hex，例如 61a20f96312e419c）
    $raw = $eid
    if ($raw -match '^(CODEKIND|CODEVALUE|CODERULE)_(.+)$') { $raw = $Matches[2] }
    if ($raw.Length -le 16) { return $raw }
    return $raw.Substring(0, 16)
}

try {
    # 1. 只读：种类、码值、分类路径
    $kinds = Invoke-Query @'
SELECT EID, ENAME, SERIALNUMBER FROM CODEKIND ORDER BY SERIALNUMBER
'@
    $kindByShort = @{}
    foreach ($row in $kinds.Rows) {
        $eid = Get-Text $row 'EID'
        $kindByShort[(ShortId $eid)] = [pscustomobject]@{
            LegacyId = $eid
            Name     = (Get-Text $row 'ENAME')
            Sort     = if ($row.IsNull('SERIALNUMBER')) { 0 } else { [int]$row['SERIALNUMBER'] }
        }
    }

    $values = Invoke-Query @'
SELECT EID, CODEVALUE, ENAME, SERIALNUMBER, PARENTVALUE FROM CODEVALUE ORDER BY SERIALNUMBER
'@
    $valueByShort = @{}
    foreach ($row in $values.Rows) {
        $eid = Get-Text $row 'EID'
        $valueByShort[(ShortId $eid)] = [pscustomobject]@{
            Code = (Get-Text $row 'CODEVALUE')
            Name = (Get-Text $row 'ENAME')
        }
    }

    $classes = Invoke-Query @'
SELECT ECODE, ENAME, CLASSCODEPATH
  FROM CLASSIFYCLASS
 WHERE CLASSCODEPATH IS NOT NULL AND CLASSCODEPATH <> ''
 ORDER BY ECODE
'@

    # 2. 解析路径，收集用到的种类与码值
    $usedKinds = [ordered]@{}
    $valueRows = [ordered]@{}   # "kind|path" -> @{kind,segment,path,code,name}
    $classPaths = New-Object System.Collections.Generic.List[object]
    $bad = New-Object System.Collections.Generic.List[string]

    foreach ($row in $classes.Rows) {
        $classCode = Get-Text $row 'ECODE'
        $raw = (Get-Text $row 'CLASSCODEPATH').Trim()
        $parts = $raw.Split(';')
        if ($parts.Length -lt 4) { $bad.Add("$classCode 路径段数不足：$raw"); continue }
        $kindShort = $parts[0].Substring(0, [Math]::Min(16, $parts[0].Length))
        if (-not $kindByShort.ContainsKey($kindShort)) { $bad.Add("$classCode 种类未找到：$($parts[0])"); continue }
        $kind = $kindByShort[$kindShort]
        if (-not $usedKinds.Contains($kind.LegacyId)) { $usedKinds[$kind.LegacyId] = $kind }

        $codes = @()
        $pathSoFar = @()
        foreach ($i in 1..($parts.Length - 2)) {
            $segShort = $parts[$i].Substring(0, [Math]::Min(16, $parts[$i].Length))
            if (-not $valueByShort.ContainsKey($segShort)) { $bad.Add("$classCode 码值未找到：$($parts[$i])"); $codes = @(); break }
            $v = $valueByShort[$segShort]
            $codes += $v.Code
            $pathSoFar += $v.Code
            $key = "$($kind.LegacyId)|$($pathSoFar -join '/')"
            if (-not $valueRows.Contains($key)) {
                $valueRows[$key] = [pscustomobject]@{
                    Kind = $kind.LegacyId
                    Segment = if ($i -eq 1) { '大类' } else { '小类' }
                    Path = ($pathSoFar -join '/')
                    Code = $v.Code
                    Name = $v.Name
                }
            }
        }
        if ($codes.Count -eq 0) { continue }
        $classPaths.Add([pscustomobject]@{
            ClassCode = $classCode
            Kind = $kind.LegacyId
            KindShort = $kindShort
            Path = (($codes -join '/') + '/*')
        })
    }

    # 3. 组装 SQL
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('-- =====================================================================')
    $lines.Add('-- 编码种类 + 码值 + 分类路径迁移：蓝本 CODEKIND/CODEVALUE/CLASSCODEPATH → 新系统三层')
    $lines.Add('-- 生成器：tools/Gen-CodeKindMigration.ps1（只读蓝本，仅生成 SQL，不执行）')
    $lines.Add("-- 数据源：$Server/$Database；生成时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
    $lines.Add("-- 统计：分类路径 $($classPaths.Count) 条；种类 $($usedKinds.Count) 个；码值 $($valueRows.Count) 条；解析失败 $($bad.Count) 条")
    $lines.Add('-- 口径：')
    $lines.Add('--   1) 只迁"有 CLASSCODEPATH 绑定"的分类（258 条），其余 6 条不自动取号')
    $lines.Add('--   2) 每个种类一条 code_rule：段定义 VALUE,VALUE,SEQ(5)')
    $lines.Add('--   3) code_path 形如 `Kind_<16hex>/1001/02/*`（* 表示流水）')
    $lines.Add('-- 注意：执行前必须先跑 V81（建表）与 010（分类树）。')
    $lines.Add('-- =====================================================================')
    $lines.Add('')
    $lines.Add('BEGIN;')
    $lines.Add('')
    $lines.Add('-- ---- 1. 编码种类 + 对应规则 ----')
    $ruleSeq = 0
    foreach ($legacyId in $usedKinds.Keys) {
        $kind = $usedKinds[$legacyId]
        $short = ShortId $legacyId
        $kindCode = "Kind_$short"
        $ruleSeq++
        $segmentsJson = '[{"type":"VALUE"},{"type":"VALUE"},{"type":"SEQ","width":5,"radix":"DECIMAL"}]'
        $ruleStmt = ("INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status,`n" +
                     "                       version_no, created_at, updated_at, current_no, last_period, object_type, remark)`n" +
                     "SELECT {0}, {1}, {2}, {3}::jsonb, '{{}}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, NULL, 'PdmItem',`n" +
                     "       {4}`n" +
                     " WHERE NOT EXISTS (SELECT 1 FROM code_rule WHERE code = {1});") -f `
                    ($RuleIdBase + $ruleSeq), (SqlText $kindCode), (SqlText $kind.Name), (SqlText $segmentsJson),
                    (SqlText "EXP-20261108-44：种类 $($kind.Name)（蓝本 $legacyId）")
        $lines.Add($ruleStmt)
        $kindStmt = ("INSERT INTO code_kind (code, name, legacy_id, rule_code, sort)`n" +
                     "SELECT {0}, {1}, {2}, {0}, {3}`n" +
                     " WHERE NOT EXISTS (SELECT 1 FROM code_kind WHERE code = {0});") -f `
                    (SqlText $kindCode), (SqlText $kind.Name), (SqlText $legacyId), $kind.Sort
        $lines.Add($kindStmt)
    }
    $lines.Add('')
    $lines.Add('-- ---- 2. 码值（value_path 表达码值树） ----')
    $valueSeq = 0
    foreach ($key in $valueRows.Keys) {
        $v = $valueRows[$key]
        $short = ShortId $v.Kind
        $valueSeq++
        $stmt = ("INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)`n" +
                 "SELECT {0}, {1}, {2}, {3}, {4}, {5}, {6}`n" +
                 " WHERE NOT EXISTS (SELECT 1 FROM code_segment_value`n" +
                 "                    WHERE kind_code = {1} AND value_path = {3} AND deleted = false);") -f `
                ($ValueIdBase + $valueSeq), (SqlText "Kind_$short"), (SqlText $v.Segment),
                (SqlText $v.Path), (SqlText $v.Code), (SqlText $v.Name), $valueSeq
        $lines.Add($stmt)
    }
    $lines.Add('')
    $lines.Add('-- ---- 3. 分类绑定路径 ----')
    foreach ($cp in $classPaths) {
        $path = "Kind_$($cp.KindShort)/$($cp.Path)"
        # 先算变量再 Add：Add((...) -f a, b) 会被解析成 Add(((...) -f a), b)
        $updStmt = ("UPDATE doc_class SET code_path = {0}, updated_at = now()`n" +
                    " WHERE entity_code = 'PdmItem' AND code = {1} AND deleted = false;") -f `
                   (SqlText $path), (SqlText $cp.ClassCode)
        $lines.Add($updStmt)
    }
    $lines.Add('')
    $lines.Add('COMMIT;')
    if ($bad.Count -gt 0) {
        $lines.Add('')
        $lines.Add('-- 解析失败明细：')
        foreach ($b in ($bad | Select-Object -Unique)) { $lines.Add("--   $b") }
    }

    $target = Join-Path (Get-Location) $OutFile
    $dir = Split-Path $target -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllLines($target, $lines, (New-Object System.Text.UTF8Encoding($false)))
    "WROTE=$target classes=$($classPaths.Count) kinds=$($usedKinds.Count) values=$($valueRows.Count) bad=$($bad.Count)"
} finally {
    $conn.Close()
}
