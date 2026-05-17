#!/usr/bin/env node
// catylst.js — bin entry point
// Launches the Catylst CLI JAR from ~/.catylst/

const { spawn } = require("child_process");
const path = require("path");
const os = require("os");
const fs = require("fs");

const TEMPLATE_DIR = path.join(os.homedir(), ".catylst", "template");
const JAR_PATH = path.join(os.homedir(), ".catylst", "catylst-cli.jar");
const WORK_DIR = path.join(TEMPLATE_DIR, "cli-generator");

if (!fs.existsSync(JAR_PATH)) {
  console.error(
    "\x1b[33m✗  catylst-cli.jar not found. Re-run: npm install -g catylst\x1b[0m"
  );
  process.exit(1);
}

if (!fs.existsSync(WORK_DIR)) {
  console.error(
    "\x1b[33m✗  Template not found at ~/.catylst/template. Re-run: npm install -g catylst\x1b[0m"
  );
  process.exit(1);
}

const args = process.argv.slice(2);

const child = spawn("java", ["-jar", JAR_PATH, ...args], {
  cwd: WORK_DIR,
  stdio: "inherit",
});

child.on("exit", (code) => process.exit(code ?? 0));
