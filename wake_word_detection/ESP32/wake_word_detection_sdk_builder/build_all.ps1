# ===========================================================================
# Wake Word Detection SDK 多芯片编库脚本（当前仅 esp32s3）
# ===========================================================================
#
# 用法：
#   .\build_all.ps1
#
# 编译完成后，静态库位于：
#   components/wake_word_detection_sdk/lib/${chip}/libwake_word_detection_sdk.a
#   output/${chip}/libwake_word_detection_sdk.a （备份）
#
# 说明：
#   - 基于 TFLite Micro + 512KB tensor_arena（PSRAM），目前只支持 esp32s3
#   - 后续要加 esp32p4 时，把芯片加进 $chips 即可（P4 需 --preview）
# ===========================================================================

$ErrorActionPreference = "Stop"

# 支持的芯片列表（当前仅 esp32s3）
$chips = @(
    "esp32s3"
)

# 需要 --preview 的芯片
$previewChips = @(
    "esp32p4"
)

$libName = "libwake_word_detection_sdk.a"
$sdkLibDir = "components/wake_word_detection_sdk/lib"
$outputDir = "output"

$originalDir = Get-Location
$results = @{}

foreach ($chip in $chips) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Building $chip" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan

    $buildDir = "build_$chip"
    $isPreview = $previewChips -contains $chip

    # 清空 IDF_TARGET，避免 set-target 冲突
    $env:IDF_TARGET = $null

    # 清理旧的构建目录
    if (Test-Path $buildDir) { Remove-Item -Recurse -Force $buildDir -ErrorAction SilentlyContinue }
    if (Test-Path "build")   { Remove-Item -Recurse -Force "build"   -ErrorAction SilentlyContinue }

    # set-target
    Write-Host "Setting target to $chip..."
    if ($isPreview) { idf.py --preview set-target $chip } else { idf.py set-target $chip }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "FAILED: set-target $chip" -ForegroundColor Red
        $results[$chip] = "FAILED: set-target"
        continue
    }

    # build
    Write-Host "Building $chip..."
    if ($isPreview) { idf.py --preview build } else { idf.py build }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "FAILED: build $chip" -ForegroundColor Red
        $results[$chip] = "FAILED: build"
        continue
    }

    # 检查产物
    $libPath = "$sdkLibDir/$chip/$libName"
    if (Test-Path $libPath) {
        $dstDir = "$outputDir/$chip"
        New-Item -ItemType Directory -Force -Path $dstDir | Out-Null
        Copy-Item $libPath $dstDir -Force
        $size = [math]::Round((Get-Item $libPath).Length / 1KB, 1)
        Write-Host "SUCCESS: $chip ($size KB)" -ForegroundColor Green
        $results[$chip] = "SUCCESS ($size KB)"
    }
    else {
        Write-Host "WARNING: library not found at $libPath" -ForegroundColor Yellow
        $results[$chip] = "WARNING: not found"
    }

    # 重命名构建目录留档
    if (Test-Path "build") { Rename-Item "build" $buildDir -ErrorAction SilentlyContinue }
}

Set-Location $originalDir

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build Results" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
foreach ($chip in $chips) {
    Write-Host ("  {0}`t{1}" -f $chip, $results[$chip])
}
Write-Host ""
Get-ChildItem -Recurse $sdkLibDir -Filter "*.a" -ErrorAction SilentlyContinue | ForEach-Object {
    $size = [math]::Round($_.Length / 1KB, 1)
    Write-Host ("  {0}/{1} ({2} KB)" -f $_.Directory.Name, $_.Name, $size)
}
