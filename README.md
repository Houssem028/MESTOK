# MESTOK

A smart messaging companion for TikTok users.

## Android MVP
- Dark, Arabic-first MESTOK home screen.
- User-controlled notification access for TikTok notifications.
- User-controlled display-over-other-apps permission.
- Floating MESTOK bubble when a supported TikTok notification arrives.
- On-device storage of the latest notification preview only.

## Important limitations
MESTOK does not access TikTok private APIs, bypass app protections, or promise access to every TikTok message. It can only process notifications Android makes available after the user explicitly enables notification access. Direct quick replies are possible only when a notification exposes a supported reply action.

## Build
GitHub Actions builds a debug APK on pushes to main or the MVP branch. Open Actions, select Build MESTOK Android APK, and download the MESTOK-debug-apk artifact after a successful run.

## Project details
- Package: com.mestok.app
- Minimum Android: 8.0 (API 26)
- Target SDK: 35
- Version: 0.1.0
