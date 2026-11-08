#requires -Version 5.1
<#
  依赖漏洞扫描（docs/07 待整改 ②）。

  用法：
    pwsh deploy/ci/security-scan.ps1                 # 扫仓库文件系统（默认）
    pwsh deploy/ci/security-scan.ps1 -Target image -Image nexplm:latest

  退出码：0=无高危；1=发现 HIGH/CRITICAL（CI 应失败）；2=本机未安装 trivy。
#>
param(
    [ValidateSet('fs', 'image')] [string]$Target = 'fs',
    [string]$Image = 'nexplm:latest',
    [string]$Severity = 'HIGH,CRITICAL',
    [string]$OutDir = 'deploy/ci/reports'
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Set-Location $root

$trivy = Get-Command trivy -ErrorAction SilentlyContinue
if (-not $trivy) {
    Write-Host 'trivy not found. Install one of:' -ForegroundColor Yellow
    Write-Host '  winget install AquaSecurity.Trivy'
    Write-Host '  https://github.com/aquasecurity/trivy/releases  (put trivy.exe on PATH)'
    exit 2
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$report = Join-Path $OutDir "trivy-$Target-$stamp.json"

if ($Target -eq 'image') {
    & trivy image --severity $Severity --ignore-unfixed --format json -o $report --exit-code 1 $Image
} else {
    & trivy fs --severity $Severity --ignore-unfixed --format json -o $report --exit-code 1 `
        --skip-dirs node_modules --skip-dirs target --skip-dirs .git .
}
$code = $LASTEXITCODE
Write-Host "report: $report"
Write-Host "exit: $code (1 = $Severity found)"
exit $code
