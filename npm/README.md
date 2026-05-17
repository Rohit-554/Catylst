# catylst

Generate customized **Kotlin Multiplatform** projects with an interactive wizard.

Instead of cloning a template and manually deleting files, just run `catylst` and answer a few questions. You get a ready-to-build KMP project with only the features you want.

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
- **Features** — pick from AI, Notifications, Permissions, Room, Preferences, Ktor, Server
- **AI provider** — Claude / Groq / Gemini (multi-select)
- **Sample code** — include demo screens or start clean
- **Theme** — pick any hex color, get a full Material 3 light + dark theme
- **Agent Skills** — install bloom-build, bloom-navigate, and community skills

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
| `--features`, `-f` | Comma-separated: `ai,notifications,permissions,room,preferences,ktor,server` |
| `--no-sample` | Exclude sample/demo code |
| `--ai-provider`, `-a` | `claude`, `groq`, `gemini`, `none` |
| `--theme-color` | Seed color hex e.g. `#6750A4` |
| `--theme-expressive` | Enable Material 3 Expressive motion |
| `--output`, `-o` | Output directory (default: current dir) |

## What gets generated

```
MyApp/
├── androidApp/          # Android entry point
├── composeApp/          # Shared KMP code
│   ├── src/commonMain/
│   ├── src/androidMain/
│   ├── src/iosMain/
│   └── src/desktopMain/
├── iosApp/              # iOS Xcode project
└── .claude/skills/      # AI agent skills (if selected)
```

## Updating

Re-run install to get the latest template and CLI:

```bash
npm install -g catylst
```

## Links

- [GitHub](https://github.com/rohit-554/Catylst)
- [Report an issue](https://github.com/rohit-554/Catylst/issues)
