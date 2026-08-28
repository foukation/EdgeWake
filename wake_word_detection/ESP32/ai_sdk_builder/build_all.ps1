# ===========================================================================
# AI SDK Multi-Chip Build Script
# ===========================================================================
#
# This script automatically builds AI SDK static libraries for all supported
# ESP32 chips.
#
# Usage:
#   .\build_all.ps1
#
# After build, static libraries are saved to:
#   components/ai_sdk/lib/${chip}/libai_sdk.a
#
# Supported chips:
#   - esp32, esp32s2, esp32s3 (Xtensa architecture)
#   - esp32c2, esp32c3, esp32c5, esp32c6, esp32c61, esp32p4 (RISC-V architecture)
#
# Note:
#   - This script uses separate build directories for each chip
#   - Each chip's build is independent and won't affect others
#   - The IDF_TARGET environment variable is cleared before each build
#   - Preview chips (esp32c5, esp32c61, esp32p4) require --preview flag
#
# ===========================================================================

# Strict mode: stop on errors
$ErrorActionPreference = "Stop"

# ===========================================================================
# Configuration
# ===========================================================================

# Supported chip list
$chips = @(
    "esp32",
    "esp32s2",
    "esp32s3",
    "esp32c2",
    "esp32c3",
    "esp32c5",
    "esp32c6",
    "esp32c61",
    "esp32p4"
)

# Preview chips that require --preview flag
# These chips are still in preview status in ESP-IDF 5.4
# and require the --preview option to use
$previewChips = @(
    "esp32c5",
    "esp32c61",
    "esp32p4"
)

# Output directory (backup)
$outputDir = "output"

# AI SDK library directory
$aiSdkLibDir = "components/ai_sdk/lib"

# ===========================================================================
# Function Definitions
# ===========================================================================

function Write-Header {
    param([string]$message)
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host $message -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$message)
    Write-Host $message -ForegroundColor Green
}

function Write-Error {
    param([string]$message)
    Write-Host $message -ForegroundColor Red
}

function Write-Warning {
    param([string]$message)
    Write-Host $message -ForegroundColor Yellow
}

# ===========================================================================
# Main Program
# ===========================================================================

Write-Header "AI SDK Multi-Chip Build"
Write-Host "Supported chips: $($chips -join ', ')"
Write-Host "Preview chips (require --preview): $($previewChips -join ', ')"
Write-Host ""

# Create output directory
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

# Record build results
$results = @{}

# Save original directory
$originalDir = Get-Location

# Iterate through all chips
foreach ($chip in $chips) {
    Write-Header "Building $chip"
    
    # Build directory for this chip
    $buildDir = "build_$chip"
    
    # ===========================================================================
    # Step 1: Determine if this chip requires --preview flag
    # ===========================================================================
    # Some chips (esp32c5, esp32c61, esp32p4) are still in preview status
    # and require the --preview option to be passed to idf.py
    $isPreview = $previewChips -contains $chip
    if ($isPreview) {
        Write-Host "Note: $chip is a preview chip, using --preview flag" -ForegroundColor Yellow
    }
    
    # ===========================================================================
    # Step 2: Clear IDF_TARGET environment variable
    # ===========================================================================
    # This is important! If IDF_TARGET is set from a previous build,
    # set-target will fail with "Target 'xxx' is not consistent with target 'yyy'"
    $env:IDF_TARGET = $null
    
    # ===========================================================================
    # Step 3: Clean previous build directory (if exists)
    # ===========================================================================
    # Remove the chip-specific build directory to ensure a clean build
    if (Test-Path $buildDir) {
        Write-Host "Cleaning previous build directory: $buildDir"
        Remove-Item -Recurse -Force $buildDir -ErrorAction SilentlyContinue
    }
    
    # ===========================================================================
    # Step 4: Set target chip
    # ===========================================================================
    # Note: set-target does NOT support -B parameter!
    # It always uses the default "build" directory.
    # We'll rename it after set-target completes.
    Write-Host "Setting target chip to $chip..."
    
    # Remove default build directory if exists
    if (Test-Path "build") {
        Remove-Item -Recurse -Force "build" -ErrorAction SilentlyContinue
    }
    
    # Execute set-target (uses default "build" directory)
    # For preview chips, add --preview flag
    if ($isPreview) {
        $output = idf.py --preview set-target $chip 2>&1
    } else {
        $output = idf.py set-target $chip 2>&1
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host $output -ForegroundColor Yellow
        Write-Error "FAILED: Cannot set target chip $chip"
        $results[$chip] = "FAILED: set-target"
        continue
    }
    
    # ===========================================================================
    # Step 5: Build
    # ===========================================================================
    Write-Host "Building $chip..."
    
    # Execute build (uses default "build" directory)
    # For preview chips, add --preview flag
    if ($isPreview) {
        idf.py --preview build
    } else {
        idf.py build
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "FAILED: Build $chip failed"
        $results[$chip] = "FAILED: build"
        continue
    }
    
    # ===========================================================================
    # Step 6: Check if static library was generated
    # ===========================================================================
    # The library is generated in the default build directory by CMakeLists.txt.source
    # POST_BUILD command, which copies it to lib/${IDF_TARGET}/libai_sdk.a
    $libPath = "$aiSdkLibDir/$chip/libai_sdk.a"
    if (Test-Path $libPath) {
        # Copy to output directory (backup)
        $dstDir = "$outputDir/$chip"
        New-Item -ItemType Directory -Force -Path $dstDir | Out-Null
        Copy-Item $libPath $dstDir
        
        # Get file size
        $size = [math]::Round((Get-Item $libPath).Length / 1KB, 1)
        Write-Success "SUCCESS: $chip ($size KB)"
        $results[$chip] = "SUCCESS ($size KB)"
    }
    else {
        Write-Warning "WARNING: Static library not found at $libPath"
        $results[$chip] = "WARNING: Library not found"
    }
    
    # ===========================================================================
    # Step 7: Rename build directory for this chip (optional, for debugging)
    # ===========================================================================
    # Rename "build" to "build_${chip}" so we can keep all build artifacts
    if (Test-Path "build") {
        Rename-Item "build" $buildDir -ErrorAction SilentlyContinue
    }
}

# Return to original directory
Set-Location $originalDir

# ===========================================================================
# Build Results Summary
# ===========================================================================

Write-Header "Build Results Summary"

Write-Host ""
Write-Host "Library directory: $aiSdkLibDir" -ForegroundColor Cyan
Write-Host ""

# Display results table
$successCount = 0
$failCount = 0

foreach ($chip in $chips) {
    $status = $results[$chip]
    if ($status -like "SUCCESS*") {
        Write-Host "  $chip`t$status" -ForegroundColor Green
        $successCount++
    }
    elseif ($status -like "FAILED*") {
        Write-Host "  $chip`t$status" -ForegroundColor Red
        $failCount++
    }
    else {
        Write-Host "  $chip`t$status" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Total: $successCount succeeded, $failCount failed" -ForegroundColor Cyan

# List generated library files
Write-Host ""
Write-Host "Generated library files:" -ForegroundColor Cyan
Get-ChildItem -Recurse $aiSdkLibDir -Filter "*.a" -ErrorAction SilentlyContinue | ForEach-Object {
    $size = [math]::Round($_.Length / 1KB, 1)
    Write-Host "  $($_.Directory.Name)/$($_.Name) ($size KB)"
}

Write-Host ""
Write-Header "Build Complete"

# Return non-zero exit code if there are failures
if ($failCount -gt 0) {
    exit 1
}
