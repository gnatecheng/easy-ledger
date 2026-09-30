**English** | [中文](README.zh-CN.md)

[Homepage: Etai Apps — Easy Ledger section](https://etais.dev/#qingjizhang)

# Easy Ledger

A local-first personal finance Android app. Data lives in SQLite (Room) on your phone—no login, no server.

- **GitHub**: [gnatecheng/easy-ledger](https://github.com/gnatecheng/easy-ledger) (renamed from `qingjizhang`; old URLs redirect)
- **Releases**: [Download APK](https://github.com/gnatecheng/easy-ledger/releases)
- **App name**: **Easy Ledger** (Chinese UI: **轻记账**)
- **Package**: `com.qingjizhang.app`
- **Version**: 1.3.2
- **Minimum OS**: Android 8.0 (API 26)
- **Stack**: Kotlin, Jetpack Compose, Material 3, Room, Navigation Compose

## Features

- **Transactions**: amount, date/time, income/expense, category, account, note, tags; quick add; month switch; full-text search on notes/tags; filter by category, account, tag, amount range, income/expense/transfer; clear filters in one tap.
- **Transfers**: move money between accounts (from/to, amount, date, note); both balances update; excluded from monthly income/expense totals; shown with a distinct style in the list.
- **Categories**: separate expense and income sets; editable names and colors; Chinese default categories on first launch (food, transport, shopping, housing, salary, investments, …).
- **Accounts**: cash, bank cards, credit cards, Alipay, WeChat Pay, etc.; balances follow transactions.
- **Import / export**
  - Export CSV (spreadsheet-friendly) and JSON (full backup: transactions, categories, accounts, budgets, reminder settings).
  - Import with preview, common column name detection (date, amount, category, note, …), and duplicate handling: skip duplicates / import all / overwrite.
  - One-tap backup/restore via system share or “Save file”.
- **Statistics**: monthly income, expense, and balance (vs. last month); category pie chart; 12-month trend; filter by time, account, tag; share monthly report image or export PDF.
- **Budgets & reminders**: monthly total + per-category budgets; 80% warning and 100% over-budget alert; configurable large-transaction threshold; gentle home nudge after days without entries.
- **Appearance & language**: follow system / light / dark theme; switch Chinese and English UI (Mine → Settings).
- **About**: version, build time, GitHub repo link (Mine → About).
- **Recurring entries**: daily / weekly / monthly rules; due items generated when you open the app; edit, pause, or delete rules.
- **Home-screen widget**: this month’s income, expense, balance (green/red); tap opens the app; refreshes after you log transactions.
- **Receipt photos**: capture or pick from gallery when logging; photos stay on device; thumbnail, replace, delete.
- **Demo data**: sample transactions on first open; clear or restore from Mine → Reminder settings.

## Changelog

### 1.3.2

- Fix in-app English/Chinese switch (AppCompat locales and sync with system per-app language).

### 1.3.1

- English bottom navigation and stats legend layout (single-line labels, readable category names).
- Home budget/large-transaction alert cards use adapted colors in dark mode.

### 1.3.0

- Full English UI (`values-en` strings; language in Settings).
- Theme: follow system, light, dark (replaces a single dark toggle).
- About screen: version, build time, project link.

### 1.2.0

- Transaction search/filters, account transfers, dark mode, and more (see git history).

## Build locally

Requires JDK 17+ and Android SDK (compileSdk 35).

```bash
# Point to your SDK
echo "sdk.dir=/path/to/Android/sdk" > local.properties

# Debug APK
./gradlew :app:assembleDebug

# Signed release APK (demo keystore in repo)
./gradlew :app:assembleRelease
```

Output:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

The demo signing config is for sideloading only—not for store upload. Passwords are in `signingConfigs.release` in `app/build.gradle.kts`.

When you push a `v*` tag, the [Release APK workflow](https://github.com/gnatecheng/easy-ledger/blob/main/.github/workflows/release-apk.yml) publishes a debug build asset named `easy-ledger-{versionName}-{tag}.apk` (e.g. `easy-ledger-1.3.2-v1.3.2.apk`).

## Related open-source projects

Other repos by the same author (use new paths after GitHub renames):

- [gnatecheng/c-week](https://github.com/gnatecheng/c-week) (formerly `C-week`)
- [gnatecheng/group-matters](https://github.com/gnatecheng/group-matters) (formerly `class-activity-record`)

## Install on a phone

1. Copy `app-release.apk` to the device.
2. In system settings, allow your file manager or browser to install unknown apps.
3. Open the APK and install. If Play Protect warns about an unknown app, choose to install anyway (self-signed, not a store build).

## Import / export notes

### CSV columns

Recommended header (Chinese or English):

`日期,时间,类型,金额,分类,账户,备注,标签,转入账户`

- Type: `支出` / `收入` / `转账` (also recognizes expense / income / transfer)
- Amount: `35.50` without currency symbol; sums cleanly in spreadsheets
- Date: `2026-09-01`, `2026/09/01`, `2026年9月1日`, etc.

Missing categories or accounts are created on import.

### JSON backup

Created by “Save JSON backup” in the app; contains the full ledger. Preview before restore:

- **Import skipping duplicates**: dedupe by date + amount + type + note
- **Import all**: allow duplicate rows
- **Replace existing data**: wipe then restore (keep another copy first)

## Project layout

```
app/src/main/java/com/qingjizhang/app/
  data/      Room entities, DAOs, repository, backup import/export, seed data
  domain/    Models and money/date helpers
  ui/        Compose screens (Home / Transactions / Stats / Budget / Mine)
```

## Privacy

No accounts, no cloud sync. Backup files leave the device only when you share or save them.
