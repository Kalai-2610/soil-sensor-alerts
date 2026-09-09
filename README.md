# Soil Sensor Alerts — Expo React Native

This project displays timestamp, field ID, crop, and SMS alert messages from:

https://wh-integration-assets.onrender.com/open/v1/sensor-result

## Build the APK with GitHub — no Android Studio and no Expo account

1. Create a GitHub repository, for example `soil-sensor-alerts`.
2. Upload all files from this project to the repository.
3. Commit/push to the `main` branch.
4. Open the repository's **Actions** tab.
5. Select **Build Android APK**.
6. Click **Run workflow** if it did not start automatically.
7. Wait for the workflow to finish.
8. Open the completed workflow run.
9. Under **Artifacts**, download `SoilSensorAlerts-debug-apk`.
10. Extract the downloaded artifact ZIP and install `app-debug.apk` on your Android phone.

The workflow uses GitHub-hosted runners, generates the native Android project with Expo prebuild, builds the APK with Gradle, and uploads the APK as a GitHub Actions artifact.

No Android Studio and no Expo account are required for this build workflow.
