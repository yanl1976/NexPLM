<#
.SYNOPSIS
    按蓝本 CLASSIFYATTRIBUTE + CLASSIFYRELATION 生成「分类属性」迁移 SQL（dry-run，不执行）。

.DESCRIPTION
    蓝本的分类属性是「全局定义 + 多分类复用」：
    - CLASSIFYATTRIBUTE 120 条定义（实测 DATATYPE 全是 String200、LOV 全空）；
    - CLASSIFYRELATION 里 CEID 以 CLASSIFYATTRIBUTE_ 开头的 1,287 条，表示"属性挂在哪些分类上"。

    新系统 doc_class_attr 是「分类私有」：一条属性属于某一个 class_id。因此按关系展开——
    1,287 条关系 → 1,287 条属性定义（同一属性在 N 个分类下各一份，语义与蓝本"按分类填值"等价）。

    三条必须记住的口径（写进文件头，便于 review）：
    1. **属性编码加 `ATTR` 前缀**：蓝本编码是纯数字（000001），新系统
       DocClassService.CODE_PATTERN 要求字母开头，否则后续编辑会被拒；
    2. **`required` 一律 false**：蓝本 ISEMPTY 实测 0=113 / 1=7，方向（True 是"可空"还是"空值占位"）
       未取证，不猜；文件末尾附 7 条待确认清单，确认后批量 UPDATE 即可；
    3. **`value_type` 一律 STRING**：DATATYPE 全是 String200，无需映射表。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File tools/Gen-DocClassAttrMigration.ps1 `
        -User sa -Password ******
#>
param(
    [string]$Server = '198.168.6.26,1433',
    [string]$Database = 'TCPLM',
    [string]$User,
    [string]$Password,
    [string]$ClassSql = 'deploy/migration/010-doc_class.sql',
    [string]$OutFile = 'deploy/migration/020-doc_class_attr.sql',
    [long]$IdBase = 9100000000001
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

# 1. 从 010 解析「分类编码 -> id」，保证两份 SQL 的 class_id 一致
$classSqlPath = Join-Path $PSScriptRoot "..\$ClassSql"
if (-not (Test-Path $classSqlPath)) { throw "找不到分类迁移 SQL：$classSqlPath（先跑 Gen-DocClassMigration.ps1）" }
$classIdByCode = @{}
foreach ($line in [System.IO.File]::ReadAllLines($classSqlPath, [System.Text.Encoding]::UTF8)) {
    if ($line -match "^SELECT\s+(\d+),\s+(?:NULL|\d+),\s+'PdmItem',\s+'([^']*)',") {
        $classIdByCode[$Matches[2]] = [long]$Matches[1]
    }
}
if ($classIdByCode.Count -eq 0) { throw "010 里没有解析到分类节点：$classSqlPath" }

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

function SqlText([string]$value) {
    if ($null -eq $value) { return 'NULL' }
    return "'" + ($value -replace "'", "''") + "'"
}

try {
    # 2. 只读：属性定义（含 ISEMPTY 原值，便于生成待确认清单）
    $attrs = Invoke-Query @'
SELECT ECODE, ENAME, ISEMPTY, CLASSATTRNO
  FROM CLASSIFYATTRIBUTE
 ORDER BY ECODE
'@
    $attrByCode = @{}
    foreach ($row in $attrs.Rows) {
        $code = Get-Text $row 'ECODE'
        $attrByCode[$code] = [pscustomobject]@{
            Code    = $code
            Name    = Get-Text $row 'ENAME'
            IsEmpty = $row['ISEMPTY']
            Sort    = if ($row.IsNull('CLASSATTRNO')) { 0 } else { [int]$row['CLASSATTRNO'] }
        }
    }

    # 3. 只读：属性 → 分类关系（1,287 条 / 119 属性 / 205 分类）
    $links = Invoke-Query @'
SELECT p.ECODE AS classCode, c.ECODE AS attrCode
  FROM CLASSIFYRELATION l
  JOIN CLASSIFYCLASS p ON p.EID = l.PEID
  JOIN CLASSIFYATTRIBUTE c ON c.EID = l.CEID
 WHERE l.CEID LIKE 'CLASSIFYATTRIBUTE[_]%'
 ORDER BY p.ECODE, c.ECODE
'@

    # 4. 生成 INSERT（属性编码加 ATTR 前缀）
    $sql = New-Object System.Collections.Generic.List[string]
    $seq = 0
    $skipped = @()
    $pendingRequired = New-Object System.Collections.Generic.List[string]

    foreach ($row in $links.Rows) {
        $classCode = Get-Text $row 'classCode'
        $attrCode = Get-Text $row 'attrCode'
        if (-not $classIdByCode.ContainsKey($classCode)) {
            $skipped += ("分类 {0} 不在 010 中" -f $classCode)
            continue
        }
        if (-not $attrByCode.ContainsKey($attrCode)) {
            $skipped += ("属性 {0} 不在 CLASSIFYATTRIBUTE 中" -f $attrCode)
            continue
        }
        $classId = $classIdByCode[$classCode]
        $attr = $attrByCode[$attrCode]
        $newCode = 'ATTR' + $attr.Code
        $id = $IdBase + ($seq++)
        if ($attr.IsEmpty -eq $true) {
            $pendingRequired.Add(("{0} {1}（分类 {2}）" -f $attr.Code, $attr.Name, $classCode))
        }

        $stmt = ("INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)`n" +
                 "SELECT {0}, {1}, {2}, {3}, 'STRING', NULL, false, true, NULL, {4}, false, now(), now()`n" +
                 " WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = {1} AND code = {2});") -f `
                $id, $classId, (SqlText $newCode), (SqlText $attr.Name), $attr.Sort
        $sql.Add($stmt)
    }

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('-- =====================================================================')
    $lines.Add('-- 分类属性迁移：蓝本 CLASSIFYATTRIBUTE + CLASSIFYRELATION → 新系统 doc_class_attr')
    $lines.Add("-- 生成器：tools/Gen-DocClassAttrMigration.ps1（只读蓝本，仅生成 SQL，不执行）")
    $lines.Add("-- 数据源：$Server/$Database；生成时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
    $lines.Add("-- 统计：属性关系 $($links.Rows.Count) 条 → 生成 $($sql.Count) 条；跳过 $($skipped.Count) 条")
    $lines.Add('-- 口径：')
    $lines.Add('--   1) 属性编码 = ATTR + 蓝本编码（蓝本是纯数字 000001，新系统要求字母开头）')
    $lines.Add('--   2) value_type 一律 STRING（蓝本 DATATYPE 全为 String200）')
    $lines.Add('--   3) required 一律 false + inheritable true（ISEMPTY 方向未取证，见文件末尾待确认清单）')
    $lines.Add('--   4) class_id 取自 010-doc_class.sql 的 id 映射，保证两份文件一致')
    $lines.Add('-- 注意：执行前需先执行 010（分类树），并确认目标库写权限与业务窗口。')
    $lines.Add('-- =====================================================================')
    $lines.Add('')
    $lines.Add('BEGIN;')
    $lines.Add('')
    foreach ($line in $sql) { $lines.Add($line) }
    $lines.Add('')
    $lines.Add('COMMIT;')
    $lines.Add('')
    $lines.Add('-- ---------------------------------------------------------------------')
    $lines.Add('-- 待确认：蓝本 ISEMPTY=1 的属性（7 条口径未定，暂按 required=false）')
    $lines.Add('-- 若确认「ISEMPTY=1 表示允许为空」，则当前设置正确，无需改动；')
    $lines.Add('-- 若确认「ISEMPTY=0 表示可空」，需把 ISEMPTY=0 的那批改为 required=true（谨慎：会影响录入）。')
    $lines.Add('-- ---------------------------------------------------------------------')
    foreach ($item in ($pendingRequired | Select-Object -Unique)) {
        $lines.Add("--   ISEMPTY=1: $item")
    }
    if ($skipped.Count -gt 0) {
        $lines.Add('')
        $lines.Add('-- 跳过明细：')
        foreach ($item in ($skipped | Select-Object -Unique)) { $lines.Add("--   $item") }
    }

    $target = Join-Path (Get-Location) $OutFile
    $dir = Split-Path $target -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllLines($target, $lines, (New-Object System.Text.UTF8Encoding($false)))
    "WROTE=$target rows=$($sql.Count) skipped=$($skipped.Count) pendingRequired=$($pendingRequired.Count)"
} finally {
    $conn.Close()
}
