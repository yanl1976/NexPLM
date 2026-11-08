<#
.SYNOPSIS
    按蓝本 CLASSIFYLINK 生成「零部件分类」迁移 SQL（dry-run，不执行）。

.DESCRIPTION
    蓝本的分类树 = CLASSIFYMANAGE → CLASSIFYGROUP(根) → CLASSIFYGROUP(嵌套) → CLASSIFYCLASS。
    实测 305 条链接：4 根 + 37 分组嵌套 + 264 分类，每节点单父、无孤儿（docs/06 EXP-20261108-32）。

    本脚本只读蓝本（SELECT），把树按拓扑序展开成 doc_class 的幂等 INSERT：
    - id 用固定基址 + 序号（9_000_000_000_001 起），便于 parent_id 稳定引用，
      且落在雪花 id（约 1.9e18）之外，不会与运行期新建的分类撞号；
    - path 按新系统口径 `/父编码/子编码`（根为 `/编码`）逐层拼接；
    - 每条 INSERT 带 `WHERE NOT EXISTS (同 entity_code + code)`，可重复执行。

    生成物是 **SQL 文本**，需要目标库写权限时手工执行（A3 迁移演练前不再改脚本）。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File tools/Gen-DocClassMigration.ps1 `
        -User sa -Password ****** -OutFile deploy/migration/010-doc_class.sql
#>
param(
    [string]$Server = '198.168.6.26,1433',
    [string]$Database = 'TCPLM',
    [string]$User,
    [string]$Password,
    [string]$OutFile = 'deploy/migration/010-doc_class.sql',
    [string]$EntityCode = 'PdmItem',
    [long]$IdBase = 9000000000001
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

function SqlText([string]$value) {
    if ($null -eq $value) { return 'NULL' }
    return "'" + ($value -replace "'", "''") + "'"
}

try {
    # 1. 三类链接（只读）
    $roots = Invoke-Query @'
SELECT g.ECODE, g.ENAME, g.SERIALNUMBER
  FROM CLASSIFYLINK l
  JOIN CLASSIFYGROUP g ON g.EID = l.CEID
  JOIN CLASSIFYMANAGE m ON m.EID = l.PEID
 ORDER BY g.ECODE
'@
    $groupLinks = Invoke-Query @'
SELECT p.ECODE AS parentCode, c.ECODE AS code, c.ENAME AS name, c.SERIALNUMBER
  FROM CLASSIFYLINK l
  JOIN CLASSIFYGROUP p ON p.EID = l.PEID
  JOIN CLASSIFYGROUP c ON c.EID = l.CEID
 WHERE l.PETYPE = 'CLASSIFYGROUP' AND l.CETYPE = 'CLASSIFYGROUP'
 ORDER BY c.ECODE
'@
    $classLinks = Invoke-Query @'
SELECT p.ECODE AS parentCode, c.ECODE AS code, c.ENAME AS name, c.SERIALNUMBER
  FROM CLASSIFYLINK l
  JOIN CLASSIFYGROUP p ON p.EID = l.PEID
  JOIN CLASSIFYCLASS c ON c.EID = l.CEID
 WHERE l.CETYPE = 'CLASSIFYCLASS'
 ORDER BY c.ECODE
'@

    # 2. 建节点表（code → parent/name/sort）
    $nodes = [ordered]@{}
    foreach ($row in $roots.Rows) {
        $code = Get-Text $row 'ECODE'
        $nodes[$code] = [pscustomobject]@{
            Code   = $code
            Name   = Get-Text $row 'ENAME'
            Parent = $null
            Sort   = if ($row.IsNull('SERIALNUMBER')) { 0 } else { [int]$row['SERIALNUMBER'] }
        }
    }
    foreach ($row in $groupLinks.Rows) {
        $code = Get-Text $row 'code'
        $nodes[$code] = [pscustomobject]@{
            Code   = $code
            Name   = Get-Text $row 'name'
            Parent = Get-Text $row 'parentCode'
            Sort   = if ($row.IsNull('SERIALNUMBER')) { 0 } else { [int]$row['SERIALNUMBER'] }
        }
    }
    foreach ($row in $classLinks.Rows) {
        $code = Get-Text $row 'code'
        $nodes[$code] = [pscustomobject]@{
            Code   = $code
            Name   = Get-Text $row 'name'
            Parent = Get-Text $row 'parentCode'
            Sort   = if ($row.IsNull('SERIALNUMBER')) { 0 } else { [int]$row['SERIALNUMBER'] }
        }
    }

    # 3. 校验：父必须存在（否则不生成，避免生成孤儿子树）
    $missing = @()
    foreach ($key in $nodes.Keys) {
        $parent = $nodes[$key].Parent
        if ($parent -and -not $nodes.Contains($parent)) {
            $missing += ("{0} -> {1}" -f $key, $parent)
        }
    }
    if ($missing.Count -gt 0) {
        throw ("存在父级缺失的节点，拒绝生成：" + ($missing -join '; '))
    }

    # 4. 拓扑展开（根优先，BFS），分配 id 与 path
    $childrenOf = @{}
    foreach ($key in $nodes.Keys) {
        $parent = $nodes[$key].Parent
        if ($parent) {
            if (-not $childrenOf.ContainsKey($parent)) { $childrenOf[$parent] = New-Object System.Collections.Generic.List[string] }
            $childrenOf[$parent].Add($key)
        }
    }
    $rootCodes = @($nodes.Keys | Where-Object { -not $nodes[$_].Parent })

    $lines = New-Object System.Collections.Generic.List[string]
    $seq = 0
    $emitted = 0
    $maxDepth = 0

    $idOf = @{}
    $queue = New-Object System.Collections.Generic.Queue[object]
    foreach ($rc in $rootCodes) { $queue.Enqueue([pscustomobject]@{ Code = $rc; ParentPath = ''; Depth = 1 }) }

    $sql = New-Object System.Collections.Generic.List[string]
    while ($queue.Count -gt 0) {
        $item = $queue.Dequeue()
        $node = $nodes[$item.Code]
        $path = if ($item.ParentPath) { "$($item.ParentPath)/$($node.Code)" } else { "/$($node.Code)" }
        $id = $IdBase + ($seq++)
        if ($item.Depth -gt $maxDepth) { $maxDepth = $item.Depth }

        # BFS 保证父先于子出队，故这里一定能取到父的 id
        $parentExpr = if ($node.Parent) { [string]$idOf[$node.Parent] } else { 'NULL' }
        # 先算成变量再 Add：直接写 $sql.Add((...) -f a, b, c) 会被解析成
        # Add(((...) -f a), b, c)，即只有 a 参与格式化
        $stmt = ("INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)`n" +
                 "SELECT {0}, {1}, {2}, {3}, {4}, {5}, {6}, 'ACTIVE', false, 0, now(), now()`n" +
                 " WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = {2} AND code = {3});") -f `
                $id, $parentExpr, (SqlText $EntityCode), (SqlText $node.Code), (SqlText $node.Name),
                (SqlText $path), $node.Sort
        $sql.Add($stmt)
        $idOf[$node.Code] = $id
        $emitted++

        if ($childrenOf.ContainsKey($node.Code)) {
            foreach ($child in ($childrenOf[$node.Code] | Sort-Object { $nodes[$_].Sort }, { $_ })) {
                $queue.Enqueue([pscustomobject]@{ Code = $child; ParentPath = $path; Depth = $item.Depth + 1 })
            }
        }
    }

    # 5. 组装文件
    $lines.Add('-- =====================================================================')
    $lines.Add('-- 零部件分类迁移：蓝本 CLASSIFYLINK → 新系统 doc_class')
    $lines.Add("-- 生成器：tools/Gen-DocClassMigration.ps1（只读蓝本，仅生成 SQL，不执行）")
    $lines.Add("-- 数据源：$Server/$Database；生成时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
    $lines.Add("-- 统计：节点 $emitted 个（根 $($rootCodes.Count) + 分组 $($groupLinks.Rows.Count) + 分类 $($classLinks.Rows.Count)），最大深度 $maxDepth")
    $lines.Add("-- 口径：entity_code=$EntityCode；path 形如 /父编码/子编码；幂等（同 code 已存在则跳过）")
    $lines.Add('-- 注意：执行前需确认目标库写权限与业务窗口（A3 迁移演练）；本文件未包含分类属性迁移。')
    $lines.Add('-- =====================================================================')
    $lines.Add('')
    $lines.Add('BEGIN;')
    $lines.Add('')
    foreach ($line in $sql) { $lines.Add($line) }
    $lines.Add('')
    $lines.Add('COMMIT;')

    $target = Join-Path (Get-Location) $OutFile
    $dir = Split-Path $target -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllLines($target, $lines, (New-Object System.Text.UTF8Encoding($false)))
    "WROTE=$target nodes=$emitted depth=$maxDepth"
} finally {
    $conn.Close()
}
