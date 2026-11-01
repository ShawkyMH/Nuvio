# Nuvio GitHub Setup & Repository Configuration

This guide details the GitHub repository configuration, authentication guidelines, and GitHub Secrets required for automated CI/CD and in-app updates.

---

## 1. Repository Information

- **Owner**: `ShawkyMH`
- **Repository Name**: `Nuvio`
- **URL**: `https://github.com/ShawkyMH/Nuvio`
- **Default Branch**: `main`
- **Visibility**: **Public**

### Why Public Visibility?
Nuvio features an unauthenticated, zero-telemetry in-app update mechanism that directly queries the GitHub Releases REST API:
```
GET https://api.github.com/repos/ShawkyMH/Nuvio/releases/latest
```
By maintaining public visibility for the release repository:
1. End-user Android devices do not require user accounts, API keys, or personal access tokens to check for or download updates.
2. Standard rate limits (60 unauthenticated requests/hour per IP) are more than sufficient for user-triggered or periodic checks.
3. Full transparency of release packages, source code, and cryptographic SHA-256 signatures is guaranteed.

---

## 2. GitHub CLI Setup & Authentication

The project is managed locally using the GitHub CLI (`gh`).

### Verify Authentication
```powershell
gh auth status
```
Expected output:
```
github.com
  ✓ Logged in to github.com account ShawkyMH (keyring)
  - Active account: true
  - Git operations protocol: https
```

### Initial Login (if needed on a new machine)
```powershell
gh auth login --web -p https
```
Follow the browser prompt to securely link your GitHub account. **Never paste personal access tokens or passwords into terminal chat or source files.**

---

## 3. Remote Configuration

Verify that the Git remote is properly configured:
```powershell
git remote -v
```
Output:
```
origin  https://github.com/ShawkyMH/Nuvio.git (fetch)
origin  https://github.com/ShawkyMH/Nuvio.git (push)
```

---

## 4. Configuring GitHub Actions Secrets for Release Signing

To enable automated release signing in `.github/workflows/android-release.yml`, configure the following repository secrets under **Settings > Secrets and variables > Actions**:

| Secret Name | Description | Example / Source |
|---|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded binary string of `nuvio-release.keystore` | Generated via `scripts/generate-release-keystore.ps1` |
| `ANDROID_KEYSTORE_PASSWORD` | Store password for the keystore | Defined during keystore generation |
| `ANDROID_KEY_ALIAS` | Key alias name within the keystore | `nuviokey` |
| `ANDROID_KEY_PASSWORD` | Key password for the specific alias | Defined during keystore generation |

You can also set these secrets directly via the GitHub CLI:
```powershell
gh secret set ANDROID_KEYSTORE_BASE64 < keystore_base64.txt
gh secret set ANDROID_KEYSTORE_PASSWORD --body "your_store_pass"
gh secret set ANDROID_KEY_ALIAS --body "nuviokey"
gh secret set ANDROID_KEY_PASSWORD --body "your_key_pass"
```
Once configured, the GitHub Actions release workflow will decode the keystore on the build runner during tag pushes and produce cryptographically signed APKs.
