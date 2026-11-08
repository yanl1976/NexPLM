#requires -Version 5.1
<#
  只读导出 TCPLM（PLM 蓝本库）的结构字典，生成 Markdown。
  凭据来源优先级：-User/-Password 参数 > NexPLM/.env（DB_* 或 SSH_*）> 环境变量。
  全程只执行 SELECT / sys.* / INFORMATION_SCHEMA 查询，不做任何写入。
#>
param(
    [string]$Server = '198.168.6.26,1433',
    [string]$Database = 'TCPLM',
    [string]$User,
    [string]$Password,
    [string]$OutFile = 'docs/13-TCPLM数据库结构.md'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not [System.IO.Path]::IsPathRooted($OutFile)) { $OutFile = Join-Path $root $OutFile }

if (-not $User -or -not $Password) {
    $cfg = @{}
    $envPath = Join-Path $root '.env'
    if (Test-Path $envPath) {
        Get-Content $envPath | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
            $k, $v = $_ -split '=', 2
            $cfg[$k.Trim()] = $v.Trim()
        }
    }
    if (-not $User) {
        $User = if ($cfg['DB_USER']) { $cfg['DB_USER'] } elseif ($cfg['SSH_USER']) { $cfg['SSH_USER'] } else { $env:NEXPLM_DB_USER }
    }
    if (-not $Password) {
        $Password = if ($cfg['DB_PASSWORD']) { $cfg['DB_PASSWORD'] } elseif ($cfg['SSH_PASSWORD']) { $cfg['SSH_PASSWORD'] } else { $env:NEXPLM_DB_PASSWORD }
    }
}

$builder = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$builder['Server'] = $Server
$builder['Initial Catalog'] = $Database
$builder['User ID'] = $User
$builder['Password'] = $Password
$builder['TrustServerCertificate'] = $true
$builder['Connect Timeout'] = 20
$builder['Application Name'] = 'NexPLM-SchemaDump-ReadOnly'

$conn = New-Object System.Data.SqlClient.SqlConnection $builder.ConnectionString
$conn.Open()

function Invoke-Query([string]$sql) {
    $cmd = New-Object System.Data.SqlClient.SqlCommand($sql, $conn)
    $cmd.CommandTimeout = 180
    $table = New-Object System.Data.DataTable
    $table.Load($cmd.ExecuteReader())
    # 逗号包一层：避免 PowerShell 把 DataTable 展开成 DataRow 集合
    return , $table
}

function Format-Type($type, $len, $prec, $scale) {
    switch -Regex ($type) {
        '^(char|varchar|nchar|nvarchar|binary|varbinary)$' {
            if ($len -eq -1) { return "$type(max)" } else { return "$type($len)" }
        }
        '^(decimal|numeric)$' { return "$type($prec,$scale)" }
        '^(datetime2|time|datetimeoffset)$' { return "$type($scale)" }
        default { return "$type" }
    }
}

$versionRaw = [string](Invoke-Query "SELECT @@VERSION AS v").Rows[0].v
$version = ($versionRaw -split "`n")[0].Trim()
$sizeMb = (Invoke-Query "SELECT CAST(SUM(CAST(size AS bigint))*8/1024 AS bigint) AS mb FROM $Database.sys.database_files").Rows[0].mb
$dbMeta = (Invoke-Query "SELECT compatibility_level, collation_name, create_date FROM master.sys.databases WHERE name = '$Database'").Rows[0]

$tables = Invoke-Query @"
SELECT s.name AS sch, t.name AS tbl, t.object_id AS oid,
       ISNULL(SUM(p.rows), 0) AS rows
  FROM sys.tables t
  JOIN sys.schemas s ON s.schema_id = t.schema_id
  LEFT JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0,1)
 GROUP BY s.name, t.name, t.object_id
 ORDER BY t.name
"@

$columns = Invoke-Query @"
SELECT TABLE_SCHEMA AS sch, TABLE_NAME AS tbl, ORDINAL_POSITION AS pos, COLUMN_NAME AS col,
       DATA_TYPE AS type, CHARACTER_MAXIMUM_LENGTH AS len, NUMERIC_PRECISION AS prec,
       NUMERIC_SCALE AS scale, IS_NULLABLE AS nullable, COLUMN_DEFAULT AS dflt
  FROM INFORMATION_SCHEMA.COLUMNS
 ORDER BY TABLE_NAME, ORDINAL_POSITION
"@

$descriptions = @{}
$descTable = Invoke-Query "SELECT major_id, minor_id, CAST(value AS nvarchar(1000)) AS v FROM sys.extended_properties WHERE name = 'MS_Description'"
foreach ($row in $descTable.Rows) {
    $key = "$($row.major_id):$($row.minor_id)"
    $descriptions[$key] = $row.v
}

$primaryKeys = @{}
$pkTable = Invoke-Query @"
SELECT i.object_id AS oid, c.name AS col
  FROM sys.indexes i
  JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
  JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
 WHERE i.is_primary_key = 1
 ORDER BY i.object_id, ic.key_ordinal
"@

# 列注释按 sys.extended_properties(minor_id = column_id) 存，需要 object_id + column_id 的映射
$columnIdMap = @{}
$columnIdTable = Invoke-Query "SELECT object_id AS oid, column_id AS cid, name AS col FROM sys.columns"
foreach ($row in $columnIdTable.Rows) {
    $columnIdMap["$($row.oid):$($row.col)"] = $row.cid
}
foreach ($row in $pkTable.Rows) {
    $oid = [string]$row.oid
    if (-not $primaryKeys.ContainsKey($oid)) { $primaryKeys[$oid] = New-Object System.Collections.ArrayList }
    [void]$primaryKeys[$oid].Add($row.col)
}

$foreignKeys = Invoke-Query @"
SELECT fk.name AS fk, tp.name AS child, cp.name AS child_col, tr.name AS parent, cr.name AS parent_col
  FROM sys.foreign_keys fk
  JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
  JOIN sys.tables tp ON tp.object_id = fkc.parent_object_id
  JOIN sys.columns cp ON cp.object_id = fkc.parent_object_id AND cp.column_id = fkc.parent_column_id
  JOIN sys.tables tr ON tr.object_id = fkc.referenced_object_id
  JOIN sys.columns cr ON cr.object_id = fkc.referenced_object_id AND cr.column_id = fkc.referenced_column_id
 ORDER BY tp.name, fk.name, fkc.constraint_column_id
"@

$views = Invoke-Query "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.VIEWS ORDER BY TABLE_NAME"

# 按表聚合列
$columnsByTable = @{}
foreach ($row in $columns.Rows) {
    $name = $row.tbl
    if (-not $columnsByTable.ContainsKey($name)) { $columnsByTable[$name] = New-Object System.Collections.ArrayList }
    [void]$columnsByTable[$name].Add($row)
}

$sb = New-Object System.Text.StringBuilder
function W([string]$text) { [void]$sb.AppendLine($text) }

W '# TCPLM（PLM 蓝本库）数据库结构字典'
W ''
W "> 由 ``tools/Dump-TcplmSchema.ps1`` **只读**生成（仅 SELECT / sys.* / INFORMATION_SCHEMA），生成时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')。"
W '> 用途：蓝本取证与迁移对照的唯一结构基线；**任何写入操作都不在本脚本能力范围内**。'
W ''
W '## 一、实例与库'
W ''
W '| 项 | 值 |'
W '|---|---|'
W "| 服务器 | $Server |"
W "| 数据库 | $Database |"
W "| 版本 | $version |"
W "| 大小 | $([string]::Format('{0:N0}', [long]$sizeMb)) MB |"
W "| 兼容级别 | $($dbMeta.compatibility_level) |"
W "| 排序规则 | $($dbMeta.collation_name) |"
W "| 创建时间 | $($dbMeta.create_date) |"
W "| 表 / 视图 | $($tables.Rows.Count) / $($views.Rows.Count) |"
W "| 列总数 | $($columns.Rows.Count) |"
W ''

W '## 二、行数 Top 30'
W ''
W '| 表 | 行数 | 列数 | 主键 |'
W '|---|---|---|---|'
$top = $tables.Rows | Sort-Object { [long]$_.rows } -Descending | Select-Object -First 30
foreach ($t in $top) {
    $pk = if ($primaryKeys.ContainsKey([string]$t.oid)) { $primaryKeys[[string]$t.oid] -join ', ' } else { '' }
    $cols = if ($columnsByTable.ContainsKey($t.tbl)) { $columnsByTable[$t.tbl].Count } else { 0 }
    W "| $($t.tbl) | $([string]::Format('{0:N0}', [long]$t.rows)) | $cols | $pk |"
}
W ''

W '## 三、表清单（按表名排序）'
W ''
W '| # | 表 | 行数 | 列数 | 主键 | 说明 |'
W '|---|---|---|---|---|---|'
$i = 0
foreach ($t in $tables.Rows) {
    $i++
    $pk = if ($primaryKeys.ContainsKey([string]$t.oid)) { $primaryKeys[[string]$t.oid] -join ', ' } else { '' }
    $cols = if ($columnsByTable.ContainsKey($t.tbl)) { $columnsByTable[$t.tbl].Count } else { 0 }
    $desc = if ($descriptions.ContainsKey("$($t.oid):0")) { $descriptions["$($t.oid):0"] } else { '' }
    $desc = ($desc -replace '\|', '\|').Trim()
    W "| $i | $($t.tbl) | $([string]::Format('{0:N0}', [long]$t.rows)) | $cols | $pk | $desc |"
}
W ''

W '## 四、表结构明细'
W ''
foreach ($t in $tables.Rows) {
    $pk = @()
    if ($primaryKeys.ContainsKey([string]$t.oid)) { $pk = $primaryKeys[[string]$t.oid] }
    $desc = if ($descriptions.ContainsKey("$($t.oid):0")) { $descriptions["$($t.oid):0"] } else { '' }
    W "### $($t.tbl)"
    W ''
    $header = "**行数** $([string]::Format('{0:N0}', [long]$t.rows))"
    if ($pk.Count -gt 0) { $header += " ｜ **主键** " + ($pk -join ', ') }
    if ($desc) { $header += " ｜ $($desc.Trim())" }
    W $header
    W ''
    W '| # | 列 | 类型 | 可空 | 默认 | 键 | 说明 |'
    W '|---|---|---|---|---|---|---|'
    if ($columnsByTable.ContainsKey($t.tbl)) {
        foreach ($c in $columnsByTable[$t.tbl]) {
            $type = Format-Type $c.type $c.len $c.prec $c.scale
            $nullable = if ($c.nullable -eq 'YES') { 'Y' } else { '' }
            $key = if ($pk -contains $c.col) { 'PK' } else { '' }
            $dflt = if ($c.dflt -is [System.DBNull] -or $c.dflt -eq $null) { '' } else { ([string]$c.dflt).Trim() }
            $cid = if ($columnIdMap.ContainsKey("$($t.oid):$($c.col)")) { $columnIdMap["$($t.oid):$($c.col)"] } else { $null }
            $cdesc = if ($cid -ne $null -and $descriptions.ContainsKey("$($t.oid):$cid")) { ($descriptions["$($t.oid):$cid"] -replace '\|', '\|').Trim() } else { '' }
            W "| $($c.pos) | $($c.col) | $type | $nullable | $(($dflt -replace '\|','\|')) | $key | $cdesc |"
        }
    }
    W ''
}

W '## 五、外键关系'
W ''
W "共 $($foreignKeys.Rows.Count) 条。"
W ''
W '| 子表.列 | → 父表.列 | 约束 |'
W '|---|---|---|'
foreach ($fk in $foreignKeys.Rows) {
    W "| $($fk.child).$($fk.child_col) | $($fk.parent).$($fk.parent_col) | $($fk.fk) |"
}
W ''

W '## 六、视图清单'
W ''
W '| 视图 |'
W '|---|'
foreach ($v in $views.Rows) { W "| $($v.TABLE_NAME) |" }
W ''

$conn.Close()
$text = $sb.ToString()
[System.IO.File]::WriteAllText($OutFile, $text, (New-Object System.Text.UTF8Encoding($false)))
Write-Output "WROTE $OutFile ($([math]::Round((Get-Item $OutFile).Length / 1KB, 1)) KB, tables=$($tables.Rows.Count), views=$($views.Rows.Count), columns=$($columns.Rows.Count), fks=$($foreignKeys.Rows.Count))"
