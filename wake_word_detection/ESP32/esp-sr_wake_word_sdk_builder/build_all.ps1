# ===========================================================================
# Wake Word SDK Multi-Chip Build Script
# ===========================================================================
#
# This script automatically builds wake_word_sdk static library for all
# supported ESP32 chips.
#
# Usage:
#   .\build_all.ps1
#
# After build, static libraries are saved to:
#   components/wake_word_sdk/lib/${chip}/libwake_word_sdk.a
#
# Supported chips:
#   - esp32, esp32s3, esp32p4
#
# Notes:
#   - ESP32-S3 and ESP32-P4 support full features (AFE + Multinet)
#   - ESP32 only supports simple Wakenet
#   - ESP32-P4 requires --preview flag
#
# ===========================================================================

# Strict mode: stop on error
$ErrorActionPreference = "Stop"

# ===========================================================================
# Configuration
# ===========================================================================

# Supported chip list
# Note: Only includes chips that support wake word detection
$chips = @(
    "esp32",
    "esp32s3",
    "esp32p4"
)

# Chips that require --preview flag
$previewChips = @(
    "esp32p4"
)

# Static library output directory
$libDir = "components/wake_word_sdk/lib"

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

Write-Header "Wake Word SDK Multi-Chip Build"
Write-Host "Supported chips: $($chips -join ', ')"
Write-Host ""

# Record build results
$results = @{}

# Save original directory
$originalDir = Get-Location

# Iterate through all chips
foreach ($chip in $chips) {
    Write-Header "Building $chip"
    
    # Build directory
    $buildDir = "build_$chip"
    
    # ===========================================================================
    # Step 1: Check if --preview flag is needed
    # ===========================================================================
    $isPreview = $previewChips -contains $chip
    if ($isPreview) {
        Write-Host "Note: $chip is a preview chip, using --preview flag" -ForegroundColor Yellow
    }
    
    # ===========================================================================
    # Step 2: Clear IDF_TARGET environment variable
    # ===========================================================================
    $env:IDF_TARGET = $null
    
    # ===========================================================================
    # Step 3: Clean previous build directory
    # ===========================================================================
    if (Test-Path $buildDir) {
        Write-Host "Cleaning previous build directory: $buildDir"
        Remove-Item -Recurse -Force $buildDir -ErrorAction SilentlyContinue
    }
    
    # ===========================================================================
    # Step 4: Set target chip
    # ===========================================================================
    Write-Host "Setting target chip to $chip..."
    
    # Delete default build directory
    if (Test-Path "build") {
        Remove-Item -Recurse -Force "build" -ErrorAction SilentlyContinue
    }
    
    # Execute set-target
    if ($isPreview) {
        $output = idf.py --preview set-target $chip 2>&1
    } else {
        $output = idf.py set-target $chip 2>&1
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host $output -ForegroundColor Yellow
        Write-Error "Failed: Cannot set target chip $chip"
        $results[$chip] = "Failed: set-target"
        continue
    }
    
    # ===========================================================================
    # Step 5: Build
    # ===========================================================================
    Write-Host "Building $chip..."
    
    if ($isPreview) {
        idf.py --preview build
    } else {
        idf.py build
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed: Build $chip failed"
        $results[$chip] = "Failed: build"
        continue
    }
    
    # ===========================================================================
    # Step 6: Check if static library is generated
    # ===========================================================================
    $libPath = "$libDir/$chip/libwake_word_sdk.a"
    if (Test-Path $libPath) {
        # Get file size
        $size = [math]::Round((Get-Item $libPath).Length / 1KB, 1)
        Write-Success "Success: $chip ($size KB)"
        $results[$chip] = "Success ($size KB)"
    }
    else {
        Write-Warning "Warning: Static library not found $libPath"
        $results[$chip] = "Warning: Library not found"
    }
    
    # ===========================================================================
    # Step 7: Rename build directory
    # ===========================================================================
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
Write-Host "Static library directory: $libDir" -ForegroundColor Cyan
Write-Host ""

# Display results table
$successCount = 0
$failCount = 0

foreach ($chip in $chips) {
    $status = $results[$chip]
    if ($status -like "Success*") {
        Write-Host "  $chip`t$status" -ForegroundColor Green
        $successCount++
    }
    elseif ($status -like "Failed*") {
        Write-Host "  $chip`t$status" -ForegroundColor Red
        $failCount++
    }
    else {
        Write-Host "  $chip`t$status" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Total: $successCount success, $failCount failed" -ForegroundColor Cyan

# List generated library files
Write-Host ""
Write-Host "Generated library files:" -ForegroundColor Cyan
Get-ChildItem -Recurse $libDir -Filter "*.a" -ErrorAction SilentlyContinue | ForEach-Object {
    $size = [math]::Round($_.Length / 1KB, 1)
    Write-Host "  $($_.Directory.Name)/$($_.Name) ($size KB)"
}

Write-Host ""
Write-Header "Build Complete"

# If there are failures, return non-zero exit code
if ($failCount -gt 0) {
    exit 1
}
