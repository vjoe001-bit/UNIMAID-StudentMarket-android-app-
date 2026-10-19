# UNIMAID StudentMarket — CodeMagic Signing Pipeline & Keystore Guide

This guide details the release signing pipeline for **UNIMAID StudentMarket**, explains the exact root cause of the CodeMagic upload error, and describes how the automated CI/CD pipeline validates keystores and verifies artifact signatures.

---

## 1. CodeMagic Reference Name vs. Key Alias

A common point of confusion when setting up CodeMagic Android code signing:

| Term | What It Is | Where It Is Used |
| :--- | :--- | :--- |
| **Reference Name** | An arbitrary identifier (label) defined by you in CodeMagic to reference an uploaded keystore. | In `codemagic.yaml` under `environment.android_signing:` (e.g. `unimaid_keystore`). |
| **Key Alias** | The internal name assigned to the private key entry inside the keystore file when it was created with `keytool`. | In the keystore itself and passed to Gradle as `CM_KEY_ALIAS` (e.g. `upload` or `unimaid_key`). |

> **IMPORTANT**: The reference name in CodeMagic **must match** the entry in `codemagic.yaml`:
> ```yaml
> environment:
>   android_signing:
>     - unimaid_keystore
> ```

---

## 2. Why the Dashboard Upload Error Occurs (`"Keystore was tampered with, or password was incorrect"`)

### Pre-Build Layer Distinction
This error occurs in **CodeMagic's web browser interface** during file upload, before any CI runner boots, before `codemagic.yaml` is parsed, and before Gradle runs.

When a keystore file is submitted in CodeMagic Team Settings, CodeMagic's server immediately executes Java's `KeyStore.load()`:
```java
KeyStore.getInstance("PKCS12").load(uploadedStream, enteredPassword.toCharArray());
```
If the password cannot decrypt the file or the HMAC integrity check fails, Java throws:
`java.io.IOException: Keystore was tampered with, or password was incorrect`

### Underlying Causes:
1. **Password Mismatch**: The password typed into CodeMagic's "Keystore password" field does not match the password specified when creating the keystore.
2. **PKCS12 Keystore vs Key Password Divergence**: Modern keystores use PKCS12. PKCS12 does not support differing passwords for the keystore and the private key alias. **Both passwords must be identical**.
3. **Invalid File Format**: Uploading a base64 string, text export, or certificate (`.cer` / `.pem`) instead of the binary `.jks` or `.p12` file.
4. **Typo / Copy-Paste Artifacts**: Hidden trailing whitespace or characters when entering credentials.

> Because this validation happens on CodeMagic's server during upload, **edits to `codemagic.yaml` or Gradle cannot bypass or fix an incorrect upload password**. The correct credentials must be provided in CodeMagic.

---

## 3. How to Generate a 100% Compliant PKCS12 Keystore

If the password to your existing keystore is unknown or unusable, generate a clean, standard PKCS12 keystore using Java's `keytool`:

```bash
keytool -genkeypair -v \
  -keystore unimaid_market_upload.jks \
  -storetype PKCS12 \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias unimaid_key \
  -storepass "<your_keystore_password>" \
  -keypass "<your_keystore_password>" \
  -dname "CN=UNIMAID StudentMarket, OU=Engineering, O=University of Maiduguri, L=Maiduguri, ST=Borno, C=NG"
```

*Note: Replace `<your_keystore_password>` with your chosen private password. Ensure `-storepass` and `-keypass` are identical.*

---

## 4. Supported Environment Variables

CodeMagic's managed Android signing integration exposes the following variables when `android_signing` is configured:

| Variable | Description |
| :--- | :--- |
| `CM_KEYSTORE_PATH` | Absolute path to the keystore file on the build runner. |
| `CM_KEYSTORE_PASSWORD` | Password to decrypt the keystore file. |
| `CM_KEY_ALIAS` | Alias identifying the private-key entry. |
| `CM_KEY_PASSWORD` | Password for the private-key entry. |

The pipeline also supports `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` as standard fallbacks, or `KEYSTORE_BASE64` for direct base64-encoded file provisioning.

---

## 5. How the Automated Pipeline Validates & Verifies Artifacts

The pipeline executes in strict sequential order:

```
[1. Set up Environment]
       │
       ▼
[2. Resolve Keystore Path in Shell]
       │
       ▼
[3. Validate Keystore & Extract Fingerprint (Fail Closed)]
       │
       ▼
[4. Run Unit Test Suite]
       │
       ▼
[5. Build Release Artifact (APK or AAB)]
       │
       ▼
[6. Verify Digital Signature with apksigner / jarsigner (Fail Closed)]
       │
       ▼
[7. Clean Up Temporary Credentials]
```

### Keystore Validation (`keytool`):
- Verifies the keystore file exists and is readable.
- Validates that the keystore can be unlocked with the password without echoing credentials.
- Checks that the alias exists and is of type `PrivateKeyEntry`.
- Extracts the certificate SHA-256 fingerprint into memory for artifact verification.
- **Fail Closed**: If any check fails, the build terminates with `exit 1` immediately before compiling release binaries.

### Release APK Verification (`apksigner`):
- Locates `apksigner` in the Android SDK build-tools.
- Runs `apksigner verify --verbose --print-certs`.
- Verifies APK Signature Scheme v2 compliance.
- Extracts the signer certificate SHA-256 digest from the generated APK and matches it against the expected keystore fingerprint.
- If signatures do not match or verification fails, the pipeline exits with `1`.

### Release AAB Verification (`jarsigner`):
- Runs `jarsigner -verify -verbose -certs` on the `.aab` bundle archive.
- Confirms `jar verified.` and validates certificate chain.
- If invalid, the pipeline exits with `1`.

---

## 6. Actions Required in the CodeMagic Dashboard

The repository code, Gradle configuration, and `codemagic.yaml` workflows are fully configured and verified. To produce a production-signed build on CodeMagic:

1. **Log in to CodeMagic**: Go to [https://codemagic.io/](https://codemagic.io/).
2. **Configure Android Keystore**:
   - Go to **Team settings** (or Account settings) > **Code signing identities** > **Android keystores**.
   - Click **Add keystore**.
   - Upload your `.jks` file.
   - Set **Reference name**: `unimaid_keystore`.
   - Enter your **Keystore password**, **Key alias**, and **Key password**.
   - Click **Save** and verify the green success badge.
3. **Configure Supabase Environment Variables**:
   - Go to your application dashboard > **Environment variables**.
   - Add group `supabase_config` with:
     - `SUPABASE_URL`
     - `SUPABASE_ANON_KEY`
4. **Trigger Build**:
   - Click **Start new build** > select workflow **`android-apk-release`** or **`android-bundle-release`**.
