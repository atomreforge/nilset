# Release Signing & Pre-release Guide

## 1. Create a local release keystore

Run this from the repository root. `keytool` is bundled with the JDK used by Android Studio/Gradle.

```powershell
New-Item -ItemType Directory -Force -Path .\keystores
keytool -genkeypair -v `
  -alias nilset-release `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10950 `
  -keystore .\keystores\nilset-release.jks
```

`keytool` prompts for the keystore password, key password, and certificate fields. Do not put the password on the command line or in shell history.

Keep the keystore and password safe. Losing either makes future release updates impossible for the same `applicationId`.

## 2. Configure signing locally

Copy the template:

```powershell
Copy-Item .\keystore.properties.example .\keystore.properties
```

Edit `keystore.properties` with real local values:

```properties
NILSET_RELEASE_STORE_FILE=keystores/nilset-release.jks
NILSET_RELEASE_STORE_PASSWORD=your-real-keystore-password
NILSET_RELEASE_KEY_ALIAS=nilset-release
NILSET_RELEASE_KEY_PASSWORD=your-real-key-password
```

`NILSET_RELEASE_STORE_FILE` is relative to the repository root, but an absolute path is also supported. This file and common keystore files are ignored by Git.

Environment variables override properties:

```text
NILSET_RELEASE_STORE_FILE
NILSET_RELEASE_STORE_PASSWORD
NILSET_RELEASE_KEY_ALIAS
NILSET_RELEASE_KEY_PASSWORD
```

## 3. Build and verify

```powershell
.\gradlew.bat clean testDebugUnitTest lintRelease assembleRelease
```

The APK is created at:

```text
app/build/outputs/apk/release/
```

Verify the signature and hash before sending it to testers:

```powershell
Get-ChildItem .\app\build\outputs\apk\release\*.apk
Get-FileHash .\app\build\outputs\apk\release\*.apk -Algorithm SHA256
```

If the optional R8 mapping exists, keep it together with the release build:

```text
app/build/outputs/mapping/release/mapping.txt
```

## 4. GitHub Pre-release signing

The GitHub release workflow falls back to the debug signing config when signing secrets are absent. This is intentional for a first internal pre-release, but it is not a production release signature.

To sign CI builds, add repository secrets in GitHub under **Settings → Secrets and variables → Actions**:

| Secret | Meaning |
| --- | --- |
| `NILSET_RELEASE_KEYSTORE_BASE64` | Base64-encoded `.jks`/`.keystore` file |
| `NILSET_RELEASE_STORE_PASSWORD` | Keystore password |
| `NILSET_RELEASE_KEY_ALIAS` | Key alias |
| `NILSET_RELEASE_KEY_PASSWORD` | Key password |

Create the base64 value on Windows with:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\keystores\nilset-release.jks"))
```

The workflow decodes the keystore to `keystores/nilset-release.jks` when the base64 secret is present. Empty/missing signing values always trigger the configured debug-signing fallback.

## 5. Publish a pre-release

After the changes are pushed, create and push the pre-release tag:

```powershell
git tag -a v0.3.0-pre.1 -m "pre-release: release engineering baseline and signed Android package workflow"
git push origin v0.3.0-pre.1
```

The GitHub workflow will create a GitHub Pre-release and attach the release APK.
