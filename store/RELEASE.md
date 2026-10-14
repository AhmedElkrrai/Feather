# Feather: making a release

## One-time: create the signing key
Keep the key and its passwords somewhere safe and **backed up**. If it is lost, updates of the app cannot be published under the same listing (Play App Signing, if enabled in the Play Console, reduces that risk because Google holds the app signing key and you hold only an upload key).

```bash
keytool -genkeypair -v -keystore feather-release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias feather
```

Put the keystore outside the repository, or inside it under a name that `.gitignore` already skips (`*.jks`, `*.keystore`). Then create `keystore.properties` in the project root (also ignored by git):

```properties
storeFile=feather-release.jks
storePassword=...
keyAlias=feather
keyPassword=...
```

`storeFile` is resolved from the project root. Without this file the release build is simply unsigned.

## Every release
1. Raise `versionCode` (a whole number that only goes up) and set `versionName` in `app/build.gradle.kts`.
2. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` and the same for `ar`.
3. Check the open items in `store/STORE.md`.
4. Build:

```bash
./gradlew bundleRelease     # app/build/outputs/bundle/release/app-release.aab  (for Google Play)
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk     (for F-Droid, GitHub releases)
```

5. Install the release APK on a real phone and go through the checklist below; R8 can remove things a debug build keeps, so a release build must be tried, not assumed.
6. Upload `app/build/outputs/mapping/release/mapping.txt` with the bundle.
7. Tag the commit (`git tag v1.0.0 && git push --tags`).

## Release checklist (on a phone, release build)
- App opens; existing data from the previous version is still there (upgrade over the old build, do not uninstall).
- Add a task by typing "Call mom tomorrow 5pm !high #family" in English and an Arabic sentence; the chips appear.
- A reminder arrives with the screen off; Complete and Snooze work from the notification.
- A repeating task moves to its next date when finished.
- Export a backup, change something, import with Merge, then with Replace; the data is as expected.
- Automatic backup: choose a folder, tap "Back up now", look at the file in the folder.
- Widgets: add each one, tick a task in the Today widget.
- Share text from the browser into Feather; the Quick Settings tile; the launcher shortcut.
- Dark, pure black, light; Arabic (right-to-left) on the main screens; font size at the largest setting.
- TalkBack: a task row reads once, its actions (done, delete, pin) are offered, the checkbox is named after the task.
- Settings > Reminder reliability shows the right state and its fix buttons open the right system screens.
