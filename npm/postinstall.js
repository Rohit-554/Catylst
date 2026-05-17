#!/usr/bin/env node
// postinstall.js — runs after `npm install -g catylst`
// 1. Checks for Java 17+
// 2. Clones (or updates) the Catylst template to ~/.catylst/template
// 3. Downloads the CLI JAR to ~/.catylst/catylst-cli.jar

const { spawnSync } = require("child_process");
const https = require("https");
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const os = require("os");

const REPO_URL = "https://github.com/rohit-554/Catylst.git";
const JAR_URL =
  "https://github.com/rohit-554/Catylst/releases/latest/download/catylst-cli.jar";

// Trusted hosts for redirect following — no other host is allowed
const TRUSTED_HOSTS = [
  "github.com",
  "objects.githubusercontent.com",
  "releases.githubusercontent.com",
  "release-assets.githubusercontent.com",
  "codeload.github.com",
];

const CATYLST_DIR = path.join(os.homedir(), ".catylst");
const TEMPLATE_DIR = path.join(CATYLST_DIR, "template");
const JAR_PATH = path.join(CATYLST_DIR, "catylst-cli.jar");

const green  = (s) => `\x1b[32m${s}\x1b[0m`;
const yellow = (s) => `\x1b[33m${s}\x1b[0m`;
const cyan   = (s) => `\x1b[36m${s}\x1b[0m`;
const bold   = (s) => `\x1b[1m${s}\x1b[0m`;
const dim    = (s) => `\x1b[2m${s}\x1b[0m`;
const purple = (s) => `\x1b[35m${s}\x1b[0m`;

/*
 * npm v7+ pipes ALL postinstall stdout/stderr — nothing reaches the terminal.
 * fs.writeSync on /dev/tty bypasses the pipe synchronously; no open handles,
 * no event-loop delay, process exits the moment the last write completes.
 * Falls back to stderr fd (2) on CI / Windows where /dev/tty is unavailable.
 */
let ttyFd = 2;
try {
  fs.accessSync("/dev/tty", fs.constants.W_OK);
  ttyFd = fs.openSync("/dev/tty", "w");
} catch (_) {}
const print    = (s) => fs.writeSync(ttyFd, s + "\n");
const printRaw = (s) => fs.writeSync(ttyFd, s);

// ── Tips & jokes shown while cloning / downloading ───────────────────────────

const MESSAGES = [
  "tip      Room 3.1 auto-generates all your DAO queries at compile time.",
  "tip      Navigation3 uses type-safe routes — no more string typos in nav graphs.",
  "tip      Swap AI providers by changing one line in AppModule.kt.",
  "tip      bloom-build helps you scaffold a full feature in seconds with Claude Code.",
  "tip      Material 3 Expressive ships spring-based motion out of the box.",
  "tip      Run ./gradlew :composeApp:kspAndroidMain after every Entity change.",
  "tip      Koin multiplatform means one DI graph for Android, iOS, and Desktop.",
  "tip      Use bloom-navigate to cleanly remove any feature you do not need.",
  "tip      AGP 9 brings predictive back gesture support by default.",
  "tip      commonMain code compiles to all targets — write once, ship everywhere.",
  "joke     Why do Kotlin developers stay calm? Because they know how to handle exceptions.",
  "joke     A null pointer walks into a bar. The bartender says: we don't serve your type here.",
  "joke     Why did the Android developer quit? Too many fragments.",
  "joke     Kotlin: where semicolons go to retire.",
  "joke     iOS dev asks: what is Gradle? Android dev weeps softly.",
  "joke     There are only 10 types of developers: those who understand binary and those who do not.",
  "joke     A git push a day keeps the merge conflicts away. Usually.",
  "joke     My code works. I have no idea why. Shipping it anyway.",
];

let tipTimer = null;
let tipIndex  = 0;

function startTips() {
  // Shuffle so order is different each install
  const msgs = [...MESSAGES].sort(() => Math.random() - 0.5);
  tipIndex = 0;

  function showNext() {
    const msg  = msgs[tipIndex % msgs.length];
    const kind = msg.startsWith("joke") ? purple("joke ") : cyan("tip  ");
    const text = msg.replace(/^(tip|joke)\s+/, "");
    printRaw(`\r  ${kind}  ${dim(text)}${" ".repeat(10)}`);
    tipIndex++;
    tipTimer = setTimeout(showNext, 3000);
  }

  showNext();
}

function stopTips(finalLine) {
  if (tipTimer) { clearTimeout(tipTimer); tipTimer = null; }
  printRaw(`\r${finalLine}${" ".repeat(30)}\n`);
}

// ── Header ───────────────────────────────────────────────────────────────────

print("");
print(bold("  Catylst KMP Project Generator"));
print(dim("  ────────────────────────────────────────"));
print("");

// ── 1. Check Java ────────────────────────────────────────────────────────────

function checkJava() {
  const result = spawnSync("java", ["-version"], { encoding: "utf8" });
  const output = result.stderr || result.stdout || "";
  const match = output.match(/version "(\d+)/);
  if (!match) {
    print(yellow("  ✗  Java not found. Install JDK 17+ from https://adoptium.net"));
    process.exit(1);
  }
  const major = parseInt(match[1], 10);
  if (major < 17) {
    print(
      yellow(`  ✗  JDK 17+ required (found ${major}). Install from https://adoptium.net`)
    );
    process.exit(1);
  }
  print(green(`  ✓  Java ${major}`));
}

// ── 2. Clone / update template ───────────────────────────────────────────────

function setupTemplate() {
  fs.mkdirSync(CATYLST_DIR, { recursive: true });

  const isGitRepo = fs.existsSync(path.join(TEMPLATE_DIR, ".git"));

  if (isGitRepo) {
    startTips();
    const result = spawnSync("git", ["pull", "--quiet", "--rebase"], {
      cwd: TEMPLATE_DIR,
      stdio: "pipe",
    });
    if (result.status === 0) {
      stopTips(green("  ✓  Template updated"));
    } else {
      stopTips(yellow("  ⚠  Could not update template (offline?). Using existing."));
    }
  } else {
    printRaw(dim("  ↓  Cloning template — hang tight...\n"));
    startTips();
    fs.rmSync(TEMPLATE_DIR, { recursive: true, force: true });
    const result = spawnSync(
      "git",
      ["clone", "--depth", "1", "--quiet", REPO_URL, TEMPLATE_DIR],
      { stdio: "pipe" }
    );
    if (result.status !== 0) {
      stopTips("");
      const msg = result.stderr ? result.stderr.toString().trim() : "unknown error";
      print(yellow("  ✗  Failed to clone template. Is git installed?"));
      print(dim(`     ${msg}`));
      process.exit(1);
    }
    stopTips(green("  ✓  Template ready"));
  }
}

// ── 3. Download JAR ──────────────────────────────────────────────────────────

function isTrustedHost(urlString) {
  try {
    const { hostname } = new URL(urlString);
    return TRUSTED_HOSTS.some((h) => hostname === h || hostname.endsWith("." + h));
  } catch {
    return false;
  }
}

function downloadJar() {
  // Dev mode — use local build if available
  const localJar = path.join(
    TEMPLATE_DIR,
    "cli-generator",
    "build",
    "libs",
    "cli-generator-1.0.0.jar"
  );
  if (fs.existsSync(localJar)) {
    fs.copyFileSync(localJar, JAR_PATH);
    print(green("  ✓  Using local build"));
    return Promise.resolve();
  }

  return new Promise((resolve, reject) => {
    printRaw(dim("  ↓  Downloading CLI — almost there...\n"));
    startTips();

    // Write to a temp file first — atomic rename prevents race conditions
    const tmpPath = JAR_PATH + ".tmp." + process.pid;

    function get(url, redirectCount = 0) {
      if (redirectCount > 5) return reject(new Error("Too many redirects"));

      // Validate redirect destination stays on trusted hosts
      if (!isTrustedHost(url)) {
        return reject(new Error(`Redirect to untrusted host blocked: ${url}`));
      }

      https
        .get(url, { headers: { "User-Agent": "catylst-npm-installer" } }, (res) => {
          if (res.statusCode === 301 || res.statusCode === 302) {
            const location = res.headers.location;
            if (!location) return reject(new Error("Redirect with no Location header"));
            return get(location, redirectCount + 1);
          }
          if (res.statusCode !== 200) {
            return reject(new Error(`HTTP ${res.statusCode}`));
          }

          // Stream to temp file with restricted permissions (owner read/write only)
          const file = fs.createWriteStream(tmpPath, { mode: 0o600 });
          const hash = crypto.createHash("sha256");

          res.on("data", (chunk) => hash.update(chunk));
          res.pipe(file);

          file.on("finish", () => {
            file.close(() => {
              // Atomic rename — prevents TOCTOU race where another process
              // could read a partially-written file
              try {
                fs.renameSync(tmpPath, JAR_PATH);
              } catch (e) {
                fs.unlinkSync(tmpPath);
                return reject(e);
              }

              const digest = hash.digest("hex");
              stopTips(green("  ✓  CLI ready"));
              print(dim(`     SHA-256: ${digest}`));
              resolve();
            });
          });

          file.on("error", (err) => {
            stopTips("");
            fs.unlink(tmpPath, () => {});
            reject(err);
          });
        })
        .on("error", (err) => {
          stopTips("");
          fs.unlink(tmpPath, () => {});
          reject(err);
        });
    }

    get(JAR_URL);
  });
}

// ── run ───────────────────────────────────────────────────────────────────────

(async () => {
  checkJava();
  setupTemplate();
  await downloadJar();

  print("");
  print(dim("  ────────────────────────────────────────"));
  print("  " + green(bold("All done!")) + " Start your project:");
  print("");
  print("  " + cyan("catylst --interactive"));
  print("");
  print("  " + dim("Or non-interactive:"));
  print("  " + dim("catylst --package com.example.app --name MyApp"));
  print("");
  print(yellow("  Before building, add your Android SDK path to local.properties:"));
  print(dim("  sdk.dir=/Users/<you>/Library/Android/sdk"));
  print("");
  process.exit(0);
})();
