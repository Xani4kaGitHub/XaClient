$ErrorActionPreference = 'Stop'
$repo = $env:REPO
$mods = $env:MODS
$e = [char]27
function ok($m)  { Write-Host "$e[95m  >> $e[97m$m$e[0m" }
function bad($m) { Write-Host "$e[91m  >> $m$e[0m" }

try {
    $h = @{ 'User-Agent' = 'XaClient' }
    $r = Invoke-RestMethod -Headers $h "https://api.github.com/repos/$repo/releases/latest"
    $tag = $r.tag_name
    $asset = $r.assets | Where-Object { $_.name -like '*.jar' -and $_.name -notlike '*sources*' } | Select-Object -First 1
    if (-not $asset) { bad 'No .jar found in the latest release'; exit 1 }

    $verFile = Join-Path $mods '.xaclient_version'
    $local = if (Test-Path $verFile) { (Get-Content $verFile -Raw).Trim() } else { '' }

    if ($local -eq $tag) {
        ok "Latest version installed ($tag)"
        exit 0
    }

    ok "Downloading $tag ..."
    Get-ChildItem $mods -Filter 'xaclient*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
    Invoke-WebRequest -Headers $h $asset.browser_download_url -OutFile (Join-Path $mods $asset.name)
    Set-Content $verFile $tag
    ok "Updated to $tag"
}
catch {
    bad ("Error: " + $_.Exception.Message)
}
