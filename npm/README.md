# catylst

[![npm version](https://img.shields.io/npm/v/catylst?style=flat-square&color=CB3837&logo=npm&logoColor=white)](https://www.npmjs.com/package/catylst)
[![AGP](https://img.shields.io/badge/AGP-9.0-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/build)
[![Room](https://img.shields.io/badge/Room-3.1-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/jetpack/androidx/releases/room)
[![Navigation3](https://img.shields.io/badge/Navigation-3.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://developer.android.com/jetpack/androidx/releases/navigation)
[![Material3](https://img.shields.io/badge/Material-M3%20Expressive-F06292?style=flat-square)](https://m3.material.io)
[![AI Skills](https://img.shields.io/badge/AI%20Skills-bloom--build%20%7C%20bloom--navigate-FF6F00?style=flat-square)](https://github.com/rohit-554/Catylst)

Generate customized **Kotlin Multiplatform** projects with an interactive wizard.

Instead of cloning a template and manually deleting files, just run `catylst --interactive` and answer a few questions. You get a ready-to-build KMP project - Android, iOS, Desktop - with only the features you want.

## Requirements

- **JDK 17+** — [Download from Adoptium](https://adoptium.net)
- **Git**

## Install

```bash
npm install -g catylst
```

## Usage

```bash
catylst --interactive
```

The wizard walks you through:

- **Package name** — e.g. `com.example.myapp`
- **App name** — e.g. `MyApp`
- **Features** — AI, Notifications, Permissions, Room 3.1, Preferences, Ktor, Server
- **AI provider** — Claude / Groq / Gemini (multi-select)
- **Sample code** — include demo screens or start clean
- **Theme** — pick any hex color, get a full M3 Expressive light + dark theme
- **Agent Skills** — install `bloom-build`, `bloom-navigate`, and 6 community skills

### Non-interactive

```bash
catylst --package com.example.myapp --name MyApp --features ai,room,ktor
```

### All options

| Option | Description |
|--------|-------------|
| `--interactive`, `-i` | Run interactive wizard |
| `--package`, `-p` | Application package name |
| `--name`, `-n` | Application display name |
| `--features`, `-f` | `ai,notifications,permissions,room,preferences,ktor,server` |
| `--no-sample` | Exclude sample/demo code |
| `--ai-provider`, `-a` | `claude`, `groq`, `gemini`, `none` |
| `--theme-color` | Seed color hex e.g. `#6750A4` |
| `--theme-expressive` | Enable Material 3 Expressive motion |
| `--output`, `-o` | Output directory (default: current dir) |

## What gets generated

```
MyApp/
├── androidApp/          # Android entry point (AGP 9)
├── composeApp/          # Shared KMP code
│   ├── src/commonMain/  # Navigation3 · Room 3.1 · Ktor 3 · Koin
│   ├── src/androidMain/
│   ├── src/iosMain/
│   └── src/desktopMain/
├── iosApp/              # iOS Xcode project
└── .claude/skills/      # bloom-build · bloom-navigate (if selected)
```

## AI Agent Skills

Skills are installed into `.claude/skills/` and picked up automatically by Claude Code.

| Skill | What it does |
|-------|-------------|
| `bloom-build` | Add screens end-to-end — composable, navigation, ViewModel, Room Entity/DAO/Repository, Koin DI |
| `bloom-navigate` | Modify your project — swap AI provider, configure notifications/permissions, remove features |

## Updating

Re-run to get the latest template and CLI:

```bash
npm install -g catylst
```

## Links

- [GitHub](https://github.com/rohit-554/Catylst)
- [Report an issue](https://github.com/rohit-554/Catylst/issues)
