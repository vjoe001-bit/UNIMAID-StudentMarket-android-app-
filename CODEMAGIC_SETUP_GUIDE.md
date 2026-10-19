# UNIMAID StudentMarket — CodeMagic CI/CD Configuration Guide

This guide explains how to build both **debug** and **signed release** Android APKs for **UNIMAID StudentMarket** using CodeMagic CI/CD.

---

## 1. Workflows Overview

`codemagic.yaml` defines two simple, focused Linux workflows:

| Workflow Name | Purpose | Keystore Required? | Build Command | Output Artifact |
| :--- | :--- | :--- | :--- | :--- |
| **`android-debug`** | Automated testing, continuous integration, and rapid testing on devices. | **No** (uses standard Android debug signing) | `./gradlew :app:assembleDebug --no-daemon --stacktrace` | `app/build/outputs/apk/debug/*.apk` |
| **`android-release`** | Production release builds for official student distribution. | **Yes** (uses managed `unimaid_keystore`) | `./gradlew :app:assembleRelease --no-daemon --stacktrace` | `app/build/outputs/apk/release/*.apk` |

Both workflows run on Linux (`linux_x2`, 8 vCPUs, 32 GB RAM) with Java 21 and the existing project Gradle wrapper.

---

## 2. Building a Debug APK (No Keystore Needed)

You do **not** need to configure or upload a release keystore to build a debug APK:

1. Log into [CodeMagic](https://codemagic.io/).
2. Select the **UNIMAID StudentMarket** project.
3. Click **Start new build**.
4. Choose the workflow: **`android-debug`**.
5. Select the branch (e.g., `main`).
6. Click **Start build**.

When complete, download `app-debug.apk` directly from the **Artifacts** tab or through the email notification sent to `victoriadrambi001@gmail.com`.

---

## 3. Configuring the Release Keystore in CodeMagic

To build an official release APK via `android-release`:

### Step 1: Keystore Requirements
The release keystore must be a valid **PKCS12** or JKS file with matching keystore and key passwords. If generating a fresh upload keystore via Java `keytool`:

```bash
keytool -genkeypair -v \
  -keystore unimaid_market_upload.jks \
  -storetype PKCS12 \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias unimaid_key \
  -storepass "YourChosenPassword" \
  -keypass "YourChosenPassword" \
  -dname "CN=UNIMAID StudentMarket, OU=Engineering, O=University of Maiduguri, L=Maiduguri, ST=Borno, C=NG"
```

### Step 2: Upload to CodeMagic
1. In CodeMagic, navigate to **Team settings** (or Account settings) > **Code signing identities** > **Android keystores**.
2. Click **Add keystore**.
3. Upload your binary `.jks` file.
4. Fill in the exact fields:
   - **Reference name**: `unimaid_keystore` *(must match `unimaid_keystore` in `codemagic.yaml`)*.
   - **Keystore password**: Your password.
   - **Key alias**: `unimaid_key`.
   - **Key password**: Your password.
5. Click **Save** and verify the green checkmark.

---

## 4. Building a Signed Release APK

1. In CodeMagic, click **Start new build**.
2. Select the workflow: **`android-release`**.
3. Select your branch or release tag.
4. Click **Start build**.

The workflow will:
- Verify that `CM_KEYSTORE_PATH` and credentials exist.
- Compile and assemble the release APK using `./gradlew :app:assembleRelease --no-daemon --stacktrace`.
- Strictly enforce release signing (the build fails if release signing information is missing; it never falls back to debug signing).
- Verify the APK signature with `apksigner` if available.
- Export `app-release.apk` to Artifacts and send an email notification upon completion.

---

## 5. Troubleshooting Errors

### A. Signing Errors vs. Upload Errors
- **"Keystore was tampered with, or password was incorrect"**: This error occurs in CodeMagic's web interface *during upload* if the password entered does not match the keystore HMAC or if keystore and key passwords differ. This is not caused by `codemagic.yaml`. Ensure passwords match and the file is raw binary.
- **"SigningConfig 'release' is missing required property 'storeFile'"**: This occurs if `android-release` is triggered without a configured keystore in CodeMagic. Upload the keystore with reference name `unimaid_keystore` to resolve.

### B. Build-Machine and Billing Errors
- **"Build instance type not available" / "Account build minute limit reached"**: This indicates your CodeMagic account has exhausted its free build minutes or does not have access to the requested machine type. Check your CodeMagic billing/usage under Account Settings. Both workflows use `linux_x2`, the standard Linux instance for Android builds.
- **"Build timeout after 60 minutes"**: Workflows have `max_build_duration: 60`. Typical clean builds complete in 3–5 minutes. If a timeout occurs, check network connectivity to external repositories.

---

## 6. Security Rules

- **Never commit keystores to Git**: `.gitignore` excludes `*.jks`, `*.keystore`, `*.p12`, `key.properties`, and `.env`.
- **Never print passwords in scripts**: All passwords are read securely from CodeMagic's encrypted environment variables.
