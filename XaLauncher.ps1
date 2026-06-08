# ============================================================
#  XaClient offline launcher  (Minecraft 1.21.4 + Fabric)
#  Downloads Java, Minecraft, assets, Fabric and the client,
#  then launches the game directly (offline, like run.bat).
# ============================================================
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# ---- pinned versions ----
$MC      = '1.21.4'
$LOADER  = '0.16.14'
$FABRICAPI = '0.110.5+1.21.4'
$REPO    = $env:REPO; if (-not $REPO) { $REPO = 'Xani4kaGitHub/XaClient' }

# ---- paths ----
$ROOT    = Join-Path $env:APPDATA '.xaclient'
$LIBDIR  = Join-Path $ROOT 'libraries'
$NATDIR  = Join-Path $ROOT 'natives'
$ASSETS  = Join-Path $ROOT 'assets'
$MODS    = Join-Path $ROOT 'mods'
$JREDIR  = Join-Path $ROOT 'jre'
$VERJSON = Join-Path $ROOT 'client.json'
$CLIENT  = Join-Path $ROOT 'client.jar'
foreach ($d in @($ROOT,$LIBDIR,$NATDIR,$ASSETS,$MODS,$JREDIR)) { New-Item -ItemType Directory -Force -Path $d | Out-Null }

$e = [char]27
function info($m) { Write-Host "$e[95m  >> $e[97m$m$e[0m" }
function warn($m) { Write-Host "$e[91m  >> $m$e[0m" }
$H = @{ 'User-Agent' = 'XaLauncher' }

function Get-Json($url) { Invoke-RestMethod -Headers $H -Uri $url }

function Download($url, $dest) {
    if ((Test-Path $dest) -and ((Get-Item $dest).Length -gt 0)) { return }
    $dir = Split-Path $dest -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $wc = New-Object System.Net.WebClient
    $wc.Headers.Add('User-Agent', 'XaLauncher')
    $wc.DownloadFile($url, $dest)
    $wc.Dispose()
}

# parallel downloader for a list of @{url;dest} (used for assets)
function Download-Many($items, $label) {
    $todo = @($items | Where-Object { -not ((Test-Path $_.dest) -and ((Get-Item $_.dest).Length -gt 0)) })
    $total = $todo.Count
    if ($total -eq 0) { info "$label : up to date"; return }
    info "$label : $total files ..."
    $pool = [RunspaceFactory]::CreateRunspacePool(1, 16); $pool.Open()
    $jobs = @()
    foreach ($it in $todo) {
        $ps = [PowerShell]::Create(); $ps.RunspacePool = $pool
        [void]$ps.AddScript({
            param($u, $d)
            try {
                $dir = Split-Path $d -Parent
                if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
                $wc = New-Object System.Net.WebClient
                $wc.Headers.Add('User-Agent', 'XaLauncher')
                $wc.DownloadFile($u, $d); $wc.Dispose()
            } catch {}
        }).AddArgument($it.url).AddArgument($it.dest)
        $jobs += [pscustomobject]@{ ps = $ps; handle = $ps.BeginInvoke() }
    }
    $done = 0
    foreach ($j in $jobs) { $j.ps.EndInvoke($j.handle); $j.ps.Dispose(); $done++; if ($done % 200 -eq 0) { info "  $done / $total" } }
    $pool.Close()
}

function Lib-Allowed($lib) {
    # OS-rule eval for windows; arch-filter natives
    $name = $lib.name
    if ($name -match 'natives-') {
        if ($name -notmatch 'natives-windows') { return $false }      # other OS natives
        if ($name -match 'natives-windows-(arm64|x86)') { return $false } # we target x64
    }
    if (-not $lib.rules) { return $true }
    $allowed = $false
    foreach ($r in $lib.rules) {
        $osMatch = (-not $r.os) -or ($r.os.name -eq 'windows')
        if ($r.action -eq 'allow' -and $osMatch) { $allowed = $true }
        if ($r.action -eq 'disallow' -and $osMatch) { $allowed = $false }
    }
    return $allowed
}

function Maven-ToPath($name) {
    # group:artifact:version[:classifier] -> group/artifact/version/artifact-version[-classifier].jar
    $p = $name.Split(':')
    $g = $p[0].Replace('.', '/'); $a = $p[1]; $v = $p[2]
    $cls = if ($p.Count -ge 4) { '-' + $p[3] } else { '' }
    "$g/$a/$v/$a-$v$cls.jar"
}

function Offline-UUID($name) {
    $md5 = [System.Security.Cryptography.MD5]::Create()
    $b = $md5.ComputeHash([Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$name"))
    $b[6] = ($b[6] -band 0x0f) -bor 0x30   # version 3
    $b[8] = ($b[8] -band 0x3f) -bor 0x80   # variant
    $hex = ($b | ForEach-Object { $_.ToString('x2') }) -join ''
    "$($hex.Substring(0,8))-$($hex.Substring(8,4))-$($hex.Substring(12,4))-$($hex.Substring(16,4))-$($hex.Substring(20,12))"
}

# ===================== JAVA 21 =====================
$JAVA = $null
$bundled = Get-ChildItem $JREDIR -Recurse -Filter 'javaw.exe' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($bundled) { $JAVA = $bundled.FullName }
if (-not $JAVA) {
    info 'Downloading Java 21 (one time) ...'
    $jreZip = Join-Path $ROOT 'jre.zip'
    Download 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk' $jreZip
    info 'Extracting Java ...'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($jreZip, $JREDIR)
    Remove-Item $jreZip -Force
    $bundled = Get-ChildItem $JREDIR -Recurse -Filter 'javaw.exe' -ErrorAction SilentlyContinue | Select-Object -First 1
    $JAVA = $bundled.FullName
}
info "Java: $JAVA"

# ===================== MINECRAFT META =====================
info 'Fetching Minecraft metadata ...'
$man = Get-Json 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
$verEntry = $man.versions | Where-Object { $_.id -eq $MC } | Select-Object -First 1
if (-not $verEntry) { warn "version $MC not found"; Read-Host; exit 1 }
$vj = Get-Json $verEntry.url
$vj | ConvertTo-Json -Depth 30 | Set-Content $VERJSON

# client.jar
Download $vj.downloads.client.url $CLIENT

# ===================== LIBRARIES =====================
info 'Downloading Minecraft libraries ...'
$cp = New-Object System.Collections.Generic.List[string]
foreach ($lib in $vj.libraries) {
    if (-not (Lib-Allowed $lib)) { continue }
    if (-not $lib.downloads.artifact) { continue }
    $dest = Join-Path $LIBDIR ($lib.downloads.artifact.path -replace '/', '\')
    Download $lib.downloads.artifact.url $dest
    $cp.Add($dest)
}

# extract native dlls to NATDIR
info 'Extracting natives ...'
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($lib in $vj.libraries) {
    if ($lib.name -notmatch 'natives-windows$') { continue }
    if (-not (Lib-Allowed $lib)) { continue }
    $jar = Join-Path $LIBDIR ($lib.downloads.artifact.path -replace '/', '\')
    if (-not (Test-Path $jar)) { continue }
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
        foreach ($e2 in $zip.Entries) {
            if ($e2.Name -match '\.(dll)$') {
                $out = Join-Path $NATDIR $e2.Name
                if (-not (Test-Path $out)) { [System.IO.Compression.ZipFileExtensions]::ExtractToFile($e2, $out, $true) }
            }
        }
        $zip.Dispose()
    } catch {}
}

# ===================== FABRIC =====================
info 'Downloading Fabric loader ...'
$fp = Get-Json "https://meta.fabricmc.net/v2/versions/loader/$MC/$LOADER/profile/json"
$FABRIC_MAIN = $fp.mainClass
foreach ($lib in $fp.libraries) {
    $rel = Maven-ToPath $lib.name
    $dest = Join-Path $LIBDIR ($rel -replace '/', '\')
    $base = $lib.url; if (-not $base.EndsWith('/')) { $base += '/' }
    Download ($base + $rel) $dest
    $cp.Add($dest)
}

# ===================== MODS (fabric-api + xaclient) =====================
info 'Downloading Fabric API ...'
Download "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$FABRICAPI/fabric-api-$FABRICAPI.jar" (Join-Path $MODS "fabric-api-$FABRICAPI.jar")

info 'Updating XaClient ...'
try {
    $rel = Get-Json "https://api.github.com/repos/$REPO/releases/latest"
    $asset = $rel.assets | Where-Object { $_.name -like '*.jar' -and $_.name -notlike '*sources*' } | Select-Object -First 1
    if ($asset) {
        $verFile = Join-Path $MODS '.xaclient_version'
        $local = if (Test-Path $verFile) { (Get-Content $verFile -Raw).Trim() } else { '' }
        if ($local -ne $rel.tag_name) {
            Get-ChildItem $MODS -Filter 'xaclient*.jar' -EA SilentlyContinue | Remove-Item -Force
            Download $asset.browser_download_url (Join-Path $MODS $asset.name)
            Set-Content $verFile $rel.tag_name
            info "XaClient updated to $($rel.tag_name)"
        } else { info "XaClient is up to date ($($rel.tag_name))" }
    }
} catch { warn "client update skipped: $($_.Exception.Message)" }

# ===================== ASSETS =====================
info 'Fetching asset index ...'
$aiId = $vj.assetIndex.id
$idxFile = Join-Path $ASSETS "indexes\$aiId.json"
Download $vj.assetIndex.url $idxFile
$idx = Get-Content $idxFile -Raw | ConvertFrom-Json
if ($env:SKIPASSETS -eq '1') { warn 'SKIPASSETS=1 -> skipping asset objects' }
$assetItems = @()
foreach ($p in $idx.objects.PSObject.Properties) {
    $hash = $p.Value.hash; $sub = $hash.Substring(0, 2)
    $assetItems += @{ url = "https://resources.download.minecraft.net/$sub/$hash"; dest = (Join-Path $ASSETS "objects\$sub\$hash") }
}
if ($env:SKIPASSETS -ne '1') { Download-Many $assetItems 'Assets' }

# ===================== LAUNCH =====================
# de-duplicate libraries: keep only the highest version per group/artifact
# (Minecraft ships asm 9.6, Fabric ships asm 9.8 -> keep 9.8 only)
$byKey = @{}
foreach ($jar in $cp) {
    $fn = Split-Path $jar -Leaf
    # strip version: artifact-1.2.3[-classifier].jar -> key = artifact (+ classifier)
    if ($fn -match '^(?<a>.+?)-(?<v>\d[\w.]*)(?<c>-[a-z0-9]+(-[a-z0-9]+)*)?\.jar$') {
        $key = $Matches['a'] + $Matches['c']
        $ver = $Matches['v']
    } else {
        $key = $fn; $ver = '0'
    }
    if (-not $byKey.ContainsKey($key)) {
        $byKey[$key] = @{ ver = $ver; path = $jar }
    } else {
        # compare versions numerically where possible
        $old = $byKey[$key].ver
        $cmp = 0
        try { $cmp = ([version]($ver -replace '[^0-9.]','')).CompareTo([version]($old -replace '[^0-9.]','')) } catch { $cmp = [string]::Compare($ver, $old) }
        if ($cmp -gt 0) { $byKey[$key] = @{ ver = $ver; path = $jar } }
    }
}
$cpFinal = New-Object System.Collections.Generic.List[string]
$cpFinal.Add($CLIENT)
foreach ($k in $byKey.Keys) { $cpFinal.Add($byKey[$k].path) }
$classpath = ($cpFinal -join ';')
$nick = $env:NICK; if (-not $nick) { $nick = 'Player' }
$ram  = $env:RAM;  if (-not $ram)  { $ram = '4096' }
$uuid = Offline-UUID $nick

$args = @(
    "-Xmx${ram}M",
    "-Djava.library.path=$NATDIR",
    "-Dorg.lwjgl.librarypath=$NATDIR",
    '-cp', $classpath,
    $FABRIC_MAIN,
    '--username', $nick,
    '--version', $fp.id,
    '--gameDir', $ROOT,
    '--assetsDir', $ASSETS,
    '--assetIndex', $aiId,
    '--uuid', $uuid,
    '--accessToken', '0',
    '--clientId', '0',
    '--xuid', '0',
    '--userType', 'legacy',
    '--versionType', 'release'
)

if ($env:DRYRUN -eq '1') {
    info 'DRY RUN - launch command:'
    Write-Host ("`"$JAVA`" " + ($args -join ' '))
    exit 0
}

info "Launching Minecraft as $nick (${ram}MB) ..."
& $JAVA @args
