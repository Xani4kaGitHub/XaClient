$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$remote = 'https://github.com/Xani4kaGitHub/XaClient.git'

$ver = Read-Host 'New version (e.g. 2.1)'
if (-not $ver) { Write-Host 'Version not set' -ForegroundColor Red; Read-Host 'Press Enter'; exit 1 }

Write-Host ""
Write-Host "  [*] Bumping version to $ver ..." -ForegroundColor Cyan

(Get-Content build.gradle) -replace "^version = '.*'", ("version = '" + $ver + "'") | Set-Content build.gradle
(Get-Content src/main/resources/fabric.mod.json) -replace '("version":\s*")[^"]*(")', ('${1}' + $ver + '${2}') | Set-Content src/main/resources/fabric.mod.json

if (-not (Test-Path .git)) {
    Write-Host "  [*] Initializing git repository ..." -ForegroundColor Cyan
    git init
    git branch -M main
    git remote add origin $remote
}

git add -A
git commit -m "Release v$ver"
git tag "v$ver"
git push -u origin main
git push origin "v$ver"

Write-Host ""
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [+] Done. GitHub Actions will build and publish Release v$ver." -ForegroundColor Green
    Write-Host "      Progress: https://github.com/Xani4kaGitHub/XaClient/actions"
} else {
    Write-Host "  [!] Something failed during git push (see messages above)." -ForegroundColor Red
}
Write-Host ""
Read-Host 'Press Enter to close'
