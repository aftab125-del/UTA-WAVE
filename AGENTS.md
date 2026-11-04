# UTA-WAVE Engineering & Update Rules

## Mandatory In-App Update Notification Rule
Whenever ANY code change or feature modification is made in UTA-wave:
1. **Bump Version**: Increment `versionCode` by 1 and update `versionName` in both:
   - `app/build.gradle.kts`
   - `version.json`
2. **Update Changelog**: Write clear, descriptive bullet points in `version.json` describing the newly introduced changes/fixes.
3. **Update README.md**: Always update the release version badge and download links in `README.md` (e.g., `Download-Android%20APK%20(vX.X.X)` and `Download UTA-wave vX.X.X APK`) to match the new version.
4. **Compile APK**: Run `./gradlew assembleDebug` to build the new release.
5. **Copy Output**: Copy `app/build/outputs/apk/debug/app-debug.apk` to `app-latest.apk`.
6. **Git Commit & Push**: Commit the changes along with `version.json`, `app-latest.apk`, and `README.md`, and push to `origin main` and `origin feature/kmp-windows-sync`.

This ensures that any existing user running the current APK on their device will immediately receive:
- The system status bar notification: "UTA-wave Update Available"
- The in-app modal update dialog with the changelog and "Download now" button.
- The GitHub repository README always reflects the exact latest version badge and download links.
