# Network Rules Summary (Android)

A guided, read-only inventory app for a Samsung Galaxy phone. It displays installed apps, lets the user record the network category seen in Samsung Settings, counts each recorded category, supports search, persists choices locally, and exports a CSV.

## Important limitation

This app does **not** automatically read Samsung's `Allowed networks for apps` preference. Android's public connectivity APIs do not expose that Samsung-specific per-app setting. Each category in the app is user-recorded after checking the phone's Settings. `Not verified` entries are excluded from the three category counts. The app does not modify any network setting and does not use root, a VPN, or an accessibility service.

## Build

1. Install Android Studio with Android SDK Platform 35 and a compatible JDK (JDK 17 is recommended for Android Gradle Plugin 8.7.x).
2. Open this folder as an existing Android Studio project.
3. Allow Gradle to download the Android Gradle Plugin and Kotlin plugin.
4. Build > Build APK(s).
5. The debug APK, if build succeeds, is normally written to `app/build/outputs/apk/debug/app-debug.apk`.

This environment did not have Android SDK/Gradle installed, so an APK could not be built or device-tested here. This source project is a starter implementation, not a claim of a tested APK.

## Privacy

No internet permission is declared. Recorded categories are stored in local SharedPreferences. CSV export uses Android's document picker and only writes when the user chooses a destination. The app requests broad installed-app visibility because listing all installed apps is its core function; this permission can be restricted by Google Play policy if distributing through Play Store.


## Cloud build with GitHub Actions

This project includes `.github/workflows/android-apk.yml`. Push the project to a GitHub repository with Actions enabled. The workflow runs on pushes to `main`/`master` and can also be started manually from the repository's Actions tab. After a successful run, open the run and download the `NetworkRulesSummary-debug-apk` artifact. GitHub packages that artifact as a ZIP containing `app-debug.apk`.

The workflow has not been run or validated in this environment. A green run is required before treating the APK as built.
