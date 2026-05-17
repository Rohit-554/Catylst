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
  "codeload.github.com",
];

const CATYLST_DIR = path.join(os.homedir(), ".catylst");
const TEMPLATE_DIR = path.join(CATYLST_DIR, "template");
const JAR_PATH = path.join(CATYLST_DIR, "catylst-cli.jar");

const green = (s) => `\x1b[32m${s}\x1b[0m`;
const yellow = (s) => `\x1b[33m${s}\x1b[0m`;
const cyan = (s) => `\x1b[36m${s}\x1b[0m`;
const bold = (s) => `\x1b[1m${s}\x1b[0m`;
const dim = (s) => `\x1b[2m${s}\x1b[0m`;

console.log("");
console.log(bold("  Catylst KMP Project Generator"));
console.log(dim("  ────────────────────────────────────────"));
console.log("");

// ── 1. Check Java ────────────────────────────────────────────────────────────

function checkJava() {
  const result = spawnSync("java", ["-version"], { encoding: "utf8" });
  const output = result.stderr || result.stdout || "";
  const match = output.match(/version "(\d+)/);
  if (!match) {
    console.error(yellow("  ✗  Java not found. Install JDK 17+ from https://adoptium.net"));
    process.exit(1);
  }
  const major = parseInt(match[1], 10);
  if (major < 17) {
    console.error(
      yellow(`  ✗  JDK 17+ required (found ${major}). Install from https://adoptium.net`)
    );
    process.exit(1);
  }
  console.log(green(`  ✓  Java ${major}`));
}

// ── 2. Clone / update template ───────────────────────────────────────────────

function setupTemplate() {
  fs.mkdirSync(CATYLST_DIR, { recursive: true });

  const isGitRepo = fs.existsSync(path.join(TEMPLATE_DIR, ".git"));

  if (isGitRepo) {
    process.stdout.write(cyan("  ↻  Updating template..."));
    // Use spawn array form — no shell interpolation, no injection risk
    const result = spawnSync("git", ["pull", "--quiet", "--rebase"], {
      cwd: TEMPLATE_DIR,
      stdio: "pipe",
    });
    if (result.status === 0) {
      console.log("\r" + green("  ✓  Template updated    "));
    } else {
      console.log("\r" + yellow("  ⚠  Could not update template (offline?). Using existing."));
    }
  } else {
    console.log(cyan("  ↓  Cloning template to ~/.catylst/template ..."));
    fs.rmSync(TEMPLATE_DIR, { recursive: true, force: true });
    // Use spawn array form — REPO_URL and TEMPLATE_DIR are never shell-interpolated
    const result = spawnSync(
      "git",
      ["clone", "--depth", "1", "--quiet", REPO_URL, TEMPLATE_DIR],
      { stdio: "pipe" }
    );
    if (result.status !== 0) {
      const msg = result.stderr ? result.stderr.toString().trim() : "unknown error";
      console.error(yellow("  ✗  Failed to clone template. Is git installed?"));
      console.error(dim(`     ${msg}`));
      process.exit(1);
    }
    console.log(green("  ✓  Template ready"));
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
    console.log(green("  ✓  Using local build"));
    return Promise.resolve();
  }

  return new Promise((resolve, reject) => {
    process.stdout.write(cyan("  ↓  Downloading catylst-cli.jar ..."));

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
              console.log("\r" + green("  ✓  CLI ready                      "));
              console.log(dim(`     SHA-256: ${digest}`));
              resolve();
            });
          });

          file.on("error", (err) => {
            fs.unlink(tmpPath, () => {});
            reject(err);
          });
        })
        .on("error", (err) => {
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

  console.log("");
  console.log(dim("  ────────────────────────────────────────"));
  console.log("  " + green(bold("All done!")) + " Start your project:");
  console.log("");
  console.log("  " + cyan("catylst --interactive"));
  console.log("");
  console.log("  " + dim("Or non-interactive:"));
  console.log("  " + dim("catylst --package com.example.app --name MyApp"));
  console.log("");
})();
