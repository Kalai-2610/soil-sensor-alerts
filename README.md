# Soil Sensor Alerts Android App

Displays `timestamp`, `field_id`, `crop`, and `sms_alert_message` from:

`https://wh-integration-assets.onrender.com/open/v1/sensor-result`

Pagination request:
`?page=<page>&size=10`

## Build
Open this folder in Android Studio, let Gradle sync, then:

Build > Build Bundle(s) / APK(s) > Build APK(s)

Or from a machine with Gradle available:

`./gradlew assembleDebug`

APK:
`app/build/outputs/apk/debug/app-debug.apk`
