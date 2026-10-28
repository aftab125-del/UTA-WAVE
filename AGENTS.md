# UTA-WAVE Engineering & Update Rules

## Mandatory In-App Update Notification Rule
Whenever ANY code change or feature modification is made in UTA-wave:
1. **Bump Version**: Increment `versionCode` by 1 and update `versionName` in both:
   - `app/build.gradle.kts`
   - `version.json`
2. **Update Changelog**: Write clear, descriptive bullet points in `version.json` describing the newly introduced changes/fixes.
3. **Compile APK**: Run `./gradlew assembleDebug` to build the new release.
4. **Copy Output**: Copy `app/build/outputs/apk/debug/app-debug.apk` to `app-latest.apk`.
5. **Git Commit & Push**: Commit the changes along with `version.json` and `app-latest.apk`, and push to `origin main`.

This ensures that any existing user running the current APK on their device will immediately receive:
- The system status bar notification: "UTA-wave Update Available"
- The in-app modal update dialog with the changelog and "Download now" button.
