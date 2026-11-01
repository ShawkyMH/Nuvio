# Nuvio — Release Keystore Generator Helper Script
param(
    [string]$KeystorePath = "release.keystore",
    [string]$Alias = "nuvio-release",
    [string]$StorePass = "",
    [string]$KeyPass = "",
    [string]$DName = "CN=Nuvio, OU=Mobile, O=Nuvio, L=Cairo, ST=Cairo, C=EG"
)

if ([string]::IsNullOrWhiteSpace($StorePass)) {
    $StorePass = Read-Host -Prompt "Enter Keystore Password (at least 6 characters)" -AsSecureString
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($StorePass)
    $StorePass = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
}

if ([string]::IsNullOrWhiteSpace($KeyPass)) {
    $KeyPass = $StorePass
}

Write-Host "==> Generating release keystore at: $KeystorePath" -ForegroundColor Cyan

& keytool -genkeypair -v `
    -keystore $KeystorePath `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass $StorePass `
    -keypass $KeyPass `
    -dname $DName

if ($LASTEXITCODE -eq 0 -and (Test-Path $KeystorePath)) {
    Write-Host "==> Keystore generated successfully!" -ForegroundColor Green
    
    $bytes = [System.IO.File]::ReadAllBytes($KeystorePath)
    $base64 = [Convert]::ToBase64String($bytes)
    
    $base64File = "$KeystorePath.base64.txt"
    $base64 | Out-File -FilePath $base64File -Encoding utf8
    
    Write-Host ""
    Write-Host "================ GITHUB SECRETS CONFIGURATION ================" -ForegroundColor Yellow
    Write-Host "Add the following secrets to GitHub: Settings -> Secrets and variables -> Actions"
    Write-Host "1. ANDROID_KEYSTORE_BASE64 : [Saved to $base64File]"
    Write-Host "2. ANDROID_KEYSTORE_PASSWORD : <your-storepass>"
    Write-Host "3. ANDROID_KEY_ALIAS : $Alias"
    Write-Host "4. ANDROID_KEY_PASSWORD : <your-keypass>"
    Write-Host "==============================================================" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "[IMPORTANT] NEVER commit $KeystorePath or $base64File to Git!" -ForegroundColor Red
} else {
    Write-Host "Keystore generation failed. Ensure Java keytool is in your PATH." -ForegroundColor Red
}
