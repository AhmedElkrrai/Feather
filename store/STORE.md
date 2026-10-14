# Feather: store submission notes

Everything needed to fill in a Google Play Console listing and to submit to F-Droid. Texts for the listing are in `fastlane/metadata/android/<locale>/` (English `en-US` and Arabic `ar`); F-Droid reads that folder as it is, and the same texts can be pasted into Play. The privacy policy is `store/PRIVACY.md` and must be published at a public address before submitting.

## Before anything else (open items the code cannot settle)
- [ ] **Donation link.** Set `SupportLinks.DONATION_URL` in `feature/settings/SupportLinks.kt`. While it is empty the "Support Feather" row is hidden. Check Google Play's current policy on links to external donations (and on whether a non-profit-style donation link inside an app is allowed) before shipping a version with the link.
- [ ] **Exact alarms.** The app declares `SCHEDULE_EXACT_ALARM`. Check the current Play Console declaration for it. If a "core functionality" declaration is required, the honest answer is that reminders at the time chosen by the user are the main feature of a to-do app. Do not switch to `USE_EXACT_ALARM`: it is for alarm clocks and calendars.
- [ ] **Privacy policy address and contact address** in `store/PRIVACY.md`.
- [ ] **App icon and feature graphic.** The launcher icon is now the bird (a code-drawn first identity). `store/graphics/` has the 512x512 icon and the 1024x500 feature graphic as SVG; export them to PNG (see its README). Replace the artwork with a designer's if there is one.
- [ ] **Screenshots** (phone, at least 2 per language): the task list, the quick add sheet with chips, the editor, the repeat dialog, a widget, the settings. Take them in English and in Arabic (right-to-left), light and dark.
- [ ] **Release signing** and a first release build (see `RELEASE.md`).

## Play Console answers
- **Category:** Productivity. **Free**, no in-app purchases, **no ads**.
- **Target audience:** 13 and up is fine; the app has no content aimed at children.
- **Content rating questionnaire:** no violence, no user-generated content shared with others, no location, no purchases. Expect "Everyone".
- **Data safety form:** *Does your app collect or share any of the required user data types?* **No.** Data is not collected, not shared, and the app has no network permission. *Is all data encrypted in transit?* Not applicable (no data leaves the device). *Can users request data deletion?* Data is local; uninstalling removes it. Mention in the free-text field that optional exports and backups are saved by the user to files of their choice, and that Android's own backup may copy the app data under the user's system settings.
- **Permissions justification:** notifications (reminders and daily summary), exact alarms (reminders at the chosen minute), boot completed (set reminders again after a restart). No other permission is requested; there is no storage, contacts, location, microphone or network permission.
- **App access:** no login needed.
- **News, health, finance, government features:** none.
- **Languages:** English and Arabic (right-to-left).

## F-Droid
- All dependencies are AndroidX, Dagger/Hilt, kotlinx and Material components under the Apache 2.0 licence; there is no Google Play Services, Firebase or other proprietary library.
- Voice input hands the request to the phone's speech recognizer through a system intent; that is the phone's service, not a library in the app. If the F-Droid maintainers flag it, describe it as an optional feature.
- The metadata folder `fastlane/metadata/android/` has the title, short and full descriptions and the changelog for version code 1. Add a changelog file named after each new `versionCode`.
- Add screenshots under `fastlane/metadata/android/<locale>/images/phoneScreenshots/`.
- A reproducible build needs a fixed JDK and Gradle wrapper (already in the repository) and no signing keys in the repository.

## After release
- Upload the R8 `mapping.txt` of every release to the Play Console (Android vitals) so crash traces are readable.
- Keep the reminder reliability guide's vendor list current; it links to dontkillmyapp.com.
- Watch the first reviews for reminder complaints from OPPO, Xiaomi, Huawei and Samsung phones: they are the likely ones.
