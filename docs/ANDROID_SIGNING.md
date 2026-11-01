# Nuvio Android App Signing & Key Management

This guide documents the procedures for generating release keystores, configuring GitHub Actions signing secrets, and safeguarding cryptographic certificates.

---

## 1. Cryptographic Certificate Requirements

Android security requires that every installed package update be signed with the **exact same cryptographic key** as the currently installed package. If a key is lost or rotated without APK Signature Scheme v3 lineage, users will encounter `INSTALL_FAILED_UPDATE_INCOMPATIBLE` and will be forced to uninstall the application, losing local databases and configurations.

- **Algorithm**: RSA 2048-bit
- **Validity**: 10,000+ days (minimum recommended 25+ years)
- **Key Store Type**: PKCS12 (`.keystore` or `.jks`)

---

## 2. Generating a Release Keystore

A PowerShell helper script is provided in `scripts/generate-release-keystore.ps1`.

### Generating via PowerShell Script
```powershell
.\scripts\generate-release-keystore.ps1 -KeystorePath "nuvio-release.keystore" -Alias "nuviokey"
```
The script will:
1. Prompt securely for keystore and key passwords.
2. Invoke JDK `keytool` to create the PKCS12 keystore.
3. Generate a Base64-encoded string file (`nuvio-release.keystore.base64.txt`) ready for GitHub Secrets.

### Generating Manually via `keytool`
```powershell
keytool -genkeypair -v `
  -keystore nuvio-release.keystore `
  -alias nuviokey `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -storetype PKCS12
```

---

## 3. Configuring GitHub Actions CI/CD Secrets

Release builds run on GitHub Actions runners without persistent storage. The signing keystore is supplied via encrypted secrets.

1. Convert the `.keystore` file to Base64:
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("nuvio-release.keystore")) | Out-File -Encoding ascii keystore_base64.txt
   ```
2. Navigate to your repository: **Settings > Secrets and variables > Actions > New repository secret**.
3. Add the following secrets:
   - `ANDROID_KEYSTORE_BASE64`: Paste the content of `keystore_base64.txt`.
   - `ANDROID_KEYSTORE_PASSWORD`: The keystore password.
   - `ANDROID_KEY_ALIAS`: `nuviokey` (or your chosen alias).
   - `ANDROID_KEY_PASSWORD`: The key password.

---

## 4. Local Release Build Configuration (Optional)

For local release builds, configure `signing.properties` in the project root (this file is excluded from Git via `.gitignore`):

```properties
KEYSTORE_FILE=C:/path/to/nuvio-release.keystore
KEYSTORE_PASSWORD=your_store_pass
KEY_ALIAS=nuviokey
KEY_PASSWORD=your_key_pass
```

When building release APKs via Gradle:
```powershell
.\gradlew.bat assembleRelease
```
Gradle will automatically read `signing.properties` or fallback to debug signing if not configured.

---

## 5. Verifying Release Signatures

Verify that the output APK is properly signed using Android SDK `apksigner`:

```powershell
$apksigner = "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\apksigner.bat"
& $apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```
Expected output:
```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
Number of signers: 1
```

---

## 6. Critical Security Reminders

> [!CAUTION]
> **Backup your keystore immediately.**
> Store an offline backup of `nuvio-release.keystore` and its passwords in an encrypted password manager or secure vault. Once public users install Nuvio, losing this key prevents publishing updates to those installations permanently.
