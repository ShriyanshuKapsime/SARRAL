# Firebase Setup Checker
Write-Host ""
Write-Host "========================================"
Write-Host "   Firebase Setup Checker for SARRAL"
Write-Host "========================================"
Write-Host ""

$hasIssues = $false

# Check google-services.json
Write-Host "Checking google-services.json..."
if (Test-Path "app/google-services.json") {
    $content = Get-Content "app/google-services.json" -Raw
    if ($content -match "YOUR_PROJECT_ID") {
        Write-Host "[ERROR] Still using PLACEHOLDER file!" -ForegroundColor Red
        Write-Host "        You MUST replace with real Firebase config" -ForegroundColor Yellow
        Write-Host "        See TROUBLESHOOTING_FIREBASE.md" -ForegroundColor Yellow
        $hasIssues = $true
    } else {
        Write-Host "[OK] Real Firebase config detected" -ForegroundColor Green
    }
} else {
    Write-Host "[ERROR] File not found!" -ForegroundColor Red
    $hasIssues = $true
}

Write-Host ""
Write-Host "========================================"
if ($hasIssues) {
    Write-Host "ISSUES FOUND!" -ForegroundColor Red
    Write-Host ""
    Write-Host "THE INTERNAL ERROR IS CAUSED BY:" -ForegroundColor Yellow
    Write-Host "Using placeholder google-services.json" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "TO FIX:" -ForegroundColor Cyan
    Write-Host "1. Go to https://console.firebase.google.com/" -ForegroundColor White
    Write-Host "2. Create a project and add Android app" -ForegroundColor White
    Write-Host "3. Download the REAL google-services.json" -ForegroundColor White
    Write-Host "4. Replace app/google-services.json with it" -ForegroundColor White
    Write-Host "5. Enable Email/Password auth in Firebase" -ForegroundColor White
    Write-Host "6. Sync Gradle and rebuild" -ForegroundColor White
    Write-Host ""
    Write-Host "Read TROUBLESHOOTING_FIREBASE.md for details" -ForegroundColor Cyan
} else {
    Write-Host "ALL CHECKS PASSED!" -ForegroundColor Green
    Write-Host "You can now build and run the app" -ForegroundColor Green
}
Write-Host "========================================"
Write-Host ""

# Offer to open documentation
Write-Host "Need help? Press:" -ForegroundColor Cyan
Write-Host "  [T] to open Troubleshooting guide" -ForegroundColor White
Write-Host "  [S] to open Setup instructions" -ForegroundColor White
Write-Host "  [Any other key] to exit" -ForegroundColor White
Write-Host ""

$key = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
if ($key.Character -eq 't' -or $key.Character -eq 'T') {
    Start-Process "TROUBLESHOOTING_FIREBASE.md"
} elseif ($key.Character -eq 's' -or $key.Character -eq 'S') {
    Start-Process "FIREBASE_SETUP_INSTRUCTIONS.md"
}
