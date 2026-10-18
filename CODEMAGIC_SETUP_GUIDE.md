# UNIMAID StudentMarket — Codemagic CI/CD Setup Guide

This guide explains how to build and download signed release APKs and Google Play App Bundles (AAB) using Codemagic CI/CD.

---

## 1. Quick Overview

The repository contains an optimized `codemagic.yaml` at its root with two workflows:
- **`android-apk-release`**: Builds the release APK (`app-release.apk`) for immediate testing, sideloading, or distributing directly to University of Maiduguri students.
- **`android-bundle-release`**: Builds the production Android App Bundle (`app-release.aab`) for Google Play Console submission.

Both workflows run on Codemagic's **`linux_x2`** runner (8 vCPUs, 32 GB RAM) with Gradle build caching enabled to minimize build minutes and cost.

---

## 2. Steps to Connect & Build on Codemagic

### Step 1: Add Project to Codemagic
1. Log in to [Codemagic](https://codemagic.io/).
2. Click **Add application**.
3. Select your Git provider (GitHub / GitLab / Bitbucket) and choose the `UNIMAID StudentMarket` repository.
4. Select **Android** project type.

### Step 2: Configure Environment Variables in Codemagic
1. In the Codemagic project dashboard, go to the **Environment variables** tab.
2. Create a variable group named **`supabase_config`**:
   - `SUPABASE_URL`: `https://uwgroqsvslyilomumial.supabase.co`
   - `SUPABASE_ANON_KEY`: Your Supabase public anon key (`sb_publishable_...`)
   - Mark both as secure.

### Step 3: Configure Android Code Signing (Optional for Testing, Required for Play Store)
If you want to sign the release APK with your custom upload key:
1. Go to **Team settings** > **Code signing identities** > **Android keystores**.
2. Click **Add keystore**.
3. Upload your `.jks` or `.keystore` file.
4. Fill in:
   - **Reference name**: `unimaid_keystore` (matches `codemagic.yaml`)
   - **Keystore password**: Your keystore password
   - **Key alias**: Your key alias (e.g. `upload`)
   - **Key password**: Your key password
5. Click **Save**.

*Note: If no keystore is uploaded, `codemagic.yaml` automatically uses a safe debugConfig signing fallback so builds succeed and APKs remain installable for testing.*

### Step 4: Start the Build
1. Click **Start new build**.
2. Select the **`android-apk-release`** workflow (or `android-bundle-release`).
3. Select the branch (e.g., `main`).
4. Click **Start build**.

---

## 3. Downloading Artifacts

Once the build finishes:
- You will receive an email at `victoriadrambi001@gmail.com` with the build summary.
- The signed APK will be available in the **Artifacts** tab on Codemagic:
  - `app-release.apk`
  - Mapping and test reports
