<#
.SYNOPSIS
    统计并清理内嵌 PostgreSQL 的测试残留（%TEMP%\epg* 目录 + 占用它们的 postgres.exe）。

.DESCRIPTION
    每个测试类各起一个内嵌 PG 实例，目录落在 %TEMP%\epg*；进程死掉后目录不自动删除，
    全量跑一轮约新增 30 个目录（≈1 GB）。累积到几十轮会把 C 盘撑满，
    表现为 `mvn test` 报 `initdb.exe failed`（见 docs/06 EXP-20261108-22 第 6 条）。

    默认 **dry-run**（只统计不删）；加 -Force 才会停进程并删目录。
    只处理 %TEMP% 下的 epg* 目录，**不会碰** dev 库（_pgdata，通常在项目盘）。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File tools/Clean-TestPg.ps1
    powershell -NoProfile -ExecutionPolicy Bypass -File tools/Clean-TestPg.ps1 -Force
#>
param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$temp = $env:TEMP

function Get-EpgStats {
    $dirs = Get-ChildItem $temp -Directory -Filter 'epg*' -ErrorAction SilentlyContinue
    $mb = 0.0
    foreach ($d in $dirs) {
        $sum = (Get-ChildItem $d.FullName -Recurse -File -ErrorAction SilentlyContinue |
                Measure-Object -Property Length -Sum).Sum
        if ($sum) { $mb += $sum / 1MB }
    }
    $procs = @()
    foreach ($p in (Get-CimInstance Win32_Process -Filter "Name='postgres.exe'" -ErrorAction SilentlyContinue)) {
        if ($p.CommandLine -and $p.CommandLine -match 'epg') {
            $procs += $p
        }
    }
    return [pscustomobject]@{
        Dirs   = @($dirs)
        SizeMb = [math]::Round($mb, 0)
        Procs  = $procs
    }
}

$stats = Get-EpgStats
$free = [math]::Round(([System.IO.DriveInfo]::new('C')).AvailableFreeSpace / 1GB, 2)

Write-Host ""
Write-Host "== 内嵌 PG 测试残留 ==" -ForegroundColor Cyan
Write-Host ("TEMP          : {0}" -f $temp)
Write-Host ("epg 目录      : {0} 个，{1} MB" -f $stats.Dirs.Count, $stats.SizeMb)
Write-Host ("残留 postgres : {0} 个（命令行含 epg）" -f $stats.Procs.Count)
Write-Host ("C 盘可用      : {0} GB" -f $free)

if (-not $Force) {
    if ($stats.Dirs.Count -eq 0 -and $stats.Procs.Count -eq 0) {
        Write-Host "已干净，无需清理。" -ForegroundColor Green
    } else {
        Write-Host "DRY-RUN：加 -Force 才会停进程并删目录。" -ForegroundColor Yellow
    }
    exit 0
}

# 1. 先停占用目录的进程（不停进程会因文件占用删不掉）
$stopped = 0
foreach ($p in $stats.Procs) {
    try {
        Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop
        $stopped++
    } catch {
        Write-Warning ("停止进程 {0} 失败：{1}" -f $p.ProcessId, $_.Exception.Message)
    }
}
if ($stopped -gt 0) {
    Start-Sleep -Seconds 3
}

# 2. 再删目录
$ok = 0
$fail = 0
foreach ($d in $stats.Dirs) {
    try {
        Remove-Item $d.FullName -Recurse -Force -ErrorAction Stop
        $ok++
    } catch {
        $fail++
    }
}

$after = Get-EpgStats
$freeAfter = [math]::Round(([System.IO.DriveInfo]::new('C')).AvailableFreeSpace / 1GB, 2)
Write-Host ""
Write-Host ("已停止进程 {0} 个；删除目录 {1} 个，失败 {2} 个" -f $stopped, $ok, $fail) -ForegroundColor Green
Write-Host ("剩余 epg 目录 {0} 个 / {1} MB；C 盘可用 {2} GB（清理前 {3} GB）" -f `
        $after.Dirs.Count, $after.SizeMb, $freeAfter, $free)
if ($fail -gt 0) {
    Write-Warning "有目录删除失败：可能仍被进程占用，重跑一次 -Force 即可。"
}
