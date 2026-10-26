[CmdletBinding()]
param(
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._-]*$')]
    [string]$SshAlias = 'dnrestaurant-oracle',
    [switch]$UseWslSsh,
    [string]$WslKeyPath = '/home/lamphnam/ssh-key-2026-03-25.key',
    [ValidatePattern('^[A-Za-z0-9._-]+@[A-Za-z0-9.-]+$')]
    [string]$WslRemote = 'ubuntu@140.245.100.91'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Invoke-CheckedCommand {
    param(
        [string]$Command,
        [string[]]$Arguments
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Lệnh $Command lỗi (mã $LASTEXITCODE). Đã dừng triển khai."
    }
}

# WSL SSH mode keeps the private key inside Ubuntu/WSL; Windows handles builds only.
function Invoke-CheckedWslCommand {
    param([string[]]$CommandArgs)
    & wsl.exe --exec @CommandArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Lenh WSL loi (ma $LASTEXITCODE): $($CommandArgs[0]). Da dung trien khai."
    }
}

function Convert-ToWslPath {
    param([string]$WindowsPath)
    $convertedPath = & wsl.exe --exec wslpath -a -u $WindowsPath
    if ($LASTEXITCODE -ne 0 -or -not $convertedPath) {
        throw "Khong chuyen duoc duong dan Windows sang WSL: $WindowsPath"
    }
    return ([string]($convertedPath | Select-Object -First 1)).Trim()
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendPath = Join-Path $projectRoot 'backend/restaurant-management'
$frontendPath = Join-Path $projectRoot 'frontend'
$releaseHelper = Join-Path $PSScriptRoot 'deployment/oracle-release.sh'
$releaseId = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$releasePath = Join-Path $projectRoot ".cache/deploy/$releaseId"
$packagePath = Join-Path $releasePath 'package'
$archivePath = Join-Path $releasePath 'release.tar.gz'
$checksumPath = Join-Path $releasePath 'release.tar.gz.sha256'
$uploadHelperPath = Join-Path $releasePath 'oracle-release.sh'

$requiredCommands = @('java', 'bun', 'tar')
if ($UseWslSsh) {
    $requiredCommands += 'wsl.exe'
} else {
    $requiredCommands += @('ssh', 'scp')
}
foreach ($command in $requiredCommands) {
    Get-Command $command -ErrorAction Stop | Out-Null
}
if (-not (Test-Path -LiteralPath $releaseHelper -PathType Leaf)) {
    throw "Thiếu script kích hoạt bản triển khai: $releaseHelper"
}

if ($UseWslSsh) {
    Write-Host "Kiểm tra private key trong WSL: $WslKeyPath"
    Invoke-CheckedWslCommand -CommandArgs @('test', '-r', $WslKeyPath)
    Write-Host "Kiểm tra kết nối SSH từ WSL: $WslRemote"
    Invoke-CheckedWslCommand -CommandArgs @('ssh', '-o', 'IdentitiesOnly=yes', '-i', $WslKeyPath, $WslRemote, 'true')
} else {
    Write-Host "Kiểm tra kết nối SSH: $SshAlias"
    Invoke-CheckedCommand -Command 'ssh' -Arguments @($SshAlias, 'true')
}

Push-Location $backendPath
try {
    Write-Host 'Đang build và kiểm thử backend...'
    Invoke-CheckedCommand -Command '.\mvnw.cmd' -Arguments @('clean', 'verify')
} finally {
    Pop-Location
}

Push-Location $frontendPath
try {
    Write-Host 'Đang cài thư viện theo lockfile, kiểm thử, lint và build frontend...'
    Invoke-CheckedCommand -Command 'bun' -Arguments @('install', '--frozen-lockfile')
    Invoke-CheckedCommand -Command 'bun' -Arguments @('run', 'test')
    Invoke-CheckedCommand -Command 'bun' -Arguments @('run', 'lint')
    Invoke-CheckedCommand -Command 'bun' -Arguments @('run', 'build')
} finally {
    Pop-Location
}

$jarFiles = @(Get-ChildItem -LiteralPath (Join-Path $backendPath 'target') -Filter '*.jar' -File |
    Where-Object { $_.Name -notmatch '-(sources|javadoc|plain)\.jar$' })
if ($jarFiles.Count -ne 1) {
    throw 'Cần đúng một file JAR ứng dụng trong thư mục target của backend.'
}
$distPath = Join-Path $frontendPath 'dist'
if (-not (Test-Path -LiteralPath (Join-Path $distPath 'index.html') -PathType Leaf)) {
    throw 'Frontend chưa tạo được file dist/index.html.'
}

New-Item -ItemType Directory -Path $packagePath -Force | Out-Null
Copy-Item -LiteralPath $jarFiles[0].FullName -Destination (Join-Path $packagePath 'app.jar')
Copy-Item -LiteralPath $distPath -Destination (Join-Path $packagePath 'web') -Recurse
Invoke-CheckedCommand -Command 'tar' -Arguments @('-czf', $archivePath, '-C', $packagePath, 'app.jar', 'web')

$archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($checksumPath, "$archiveHash  release.tar.gz`n", $utf8NoBom)
$helperText = [System.IO.File]::ReadAllText($releaseHelper).Replace("`r`n", "`n")
[System.IO.File]::WriteAllText($uploadHelperPath, $helperText, $utf8NoBom)

$remoteDirectory = "dnrestaurant-upload/$releaseId"
if ($UseWslSsh) {
    Invoke-CheckedWslCommand -CommandArgs @('ssh', '-o', 'IdentitiesOnly=yes', '-i', $WslKeyPath, $WslRemote, "mkdir -p -- $remoteDirectory")
    Write-Host "Đang upload bản triển khai $releaseId bằng SCP của WSL..."
    $wslArchivePath = Convert-ToWslPath -WindowsPath $archivePath
    $wslChecksumPath = Convert-ToWslPath -WindowsPath $checksumPath
    $wslHelperPath = Convert-ToWslPath -WindowsPath $uploadHelperPath
    Invoke-CheckedWslCommand -CommandArgs @(
        'scp', '-o', 'IdentitiesOnly=yes', '-i', $WslKeyPath,
        $wslArchivePath, $wslChecksumPath, $wslHelperPath,
        "${WslRemote}:$remoteDirectory/"
    )
    Write-Host 'Đang kích hoạt bản triển khai bằng SSH của WSL. sudo có thể yêu cầu mật khẩu Ubuntu.'
    Invoke-CheckedWslCommand -CommandArgs @(
        'ssh', '-tt', '-o', 'IdentitiesOnly=yes', '-i', $WslKeyPath, $WslRemote,
        "sudo bash $remoteDirectory/oracle-release.sh deploy $releaseId"
    )
} else {
    Invoke-CheckedCommand -Command 'ssh' -Arguments @($SshAlias, "mkdir -p -- $remoteDirectory")
    Write-Host "Đang upload bản triển khai $releaseId..."
    Invoke-CheckedCommand -Command 'scp' -Arguments @(
        $archivePath, $checksumPath, $uploadHelperPath, "${SshAlias}:$remoteDirectory/"
    )
    Write-Host 'Đang kích hoạt bản triển khai. sudo có thể yêu cầu mật khẩu Ubuntu.'
    Invoke-CheckedCommand -Command 'ssh' -Arguments @(
        '-t', $SshAlias, "sudo bash $remoteDirectory/oracle-release.sh deploy $releaseId"
    )
}

Write-Host "Đã triển khai: $releaseId"
Write-Host 'Backend đã qua kiểm tra khởi động. Kiểm tiếp HTTPS, đăng nhập và SSE.'

