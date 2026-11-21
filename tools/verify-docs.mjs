import http from "node:http";
import { spawn } from "node:child_process";
import { mkdtempSync, readFileSync, writeFileSync, mkdirSync, rmSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, extname, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const docsDir = join(root, "docs");
const outDir = join(root, "verify_out");
mkdirSync(outDir, { recursive: true });

const CHROME = process.env.CHROME || "C:/Program Files/Google/Chrome/Application/chrome.exe";
const MIME = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml"
};

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const server = http.createServer((req, res) => {
  const rel = (req.url || "/").split("?")[0];
  const file = join(docsDir, rel === "/" ? "index.html" : rel.replace(/^\/+/, ""));
  if (!existsSync(file)) {
    res.writeHead(404);
    res.end("Not Found");
    return;
  }
  res.writeHead(200, { "Content-Type": MIME[extname(file)] || "application/octet-stream" });
  res.end(readFileSync(file));
});

await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
const port = server.address().port;
const baseUrl = `http://127.0.0.1:${port}/`;

const userDataDir = mkdtempSync(join(tmpdir(), "sourcecraft-chrome-"));
const chrome = spawn(
  CHROME,
  [
    "--headless=new",
    "--disable-gpu",
    "--no-sandbox",
    "--remote-debugging-port=0",
    `--user-data-dir=${userDataDir}`,
    "--window-size=1440,900",
    "about:blank"
  ],
  { stdio: ["ignore", "pipe", "pipe"] }
);

let wsUrl = null;
const startTime = Date.now();
while (!wsUrl && Date.now() - startTime < 15000) {
  await sleep(150);
  const active = join(userDataDir, "DevToolsActivePort");
  if (existsSync(active)) {
    const lines = readFileSync(active, "utf8").trim().split(/\r?\n/);
    if (lines.length >= 2) {
      wsUrl = `ws://127.0.0.1:${lines[0]}${lines[1]}`;
    }
  }
}

if (!wsUrl) {
  chrome.kill();
  server.close();
  throw new Error("Chrome DevToolsActivePort did not appear.");
}

// Get target page WebSocket URL
const pages = await (await fetch(wsUrl.replace(/^ws:/, "http:").replace(/\/devtools\/browser\/.*$/, "/json/list"))).json();
const pageTarget = pages.find((p) => p.type === "page");
const ws = new WebSocket(pageTarget.webSocketDebuggerUrl);
await new Promise((res, rej) => {
  ws.addEventListener("open", res);
  ws.addEventListener("error", rej);
});

let msgId = 1;
const pending = new Map();
const errors = [];

ws.addEventListener("message", (ev) => {
  const data = JSON.parse(ev.data);
  if (data.id && pending.has(data.id)) {
    const { resolve, reject } = pending.get(data.id);
    pending.delete(data.id);
    if (data.error) reject(new Error(JSON.stringify(data.error)));
    else resolve(data.result);
  } else if (data.method === "Runtime.exceptionThrown") {
    errors.push("Exception: " + JSON.stringify(data.params.exceptionDetails?.text || data.params));
  } else if (data.method === "Log.entryAdded" && data.params.entry?.level === "error") {
    errors.push("Console error: " + data.params.entry.text);
  }
});

function cdp(method, params = {}) {
  const id = msgId++;
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject });
    ws.send(JSON.stringify({ id, method, params }));
  });
}

async function evalJs(expression) {
  const res = await cdp("Runtime.evaluate", { expression, returnByValue: true, awaitPromise: true });
  return res.result?.value;
}

await cdp("Page.enable");
await cdp("Runtime.enable");
await cdp("Log.enable");
await cdp("Emulation.setDeviceMetricsOverride", { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false });
await cdp("Page.navigate", { url: baseUrl });
await sleep(900);

// 1. Capture desktop top & interactive playground
const desktopCheck = await evalJs(`(() => {
  const rows = document.querySelectorAll('#cvarTableBody tr').length;
  const presets = document.querySelectorAll('#presetButtons button').length;
  const hScroll = document.documentElement.scrollWidth > window.innerWidth;
  return { rows, presets, hScroll, speed: document.getElementById('hudSpeed').textContent };
})()`);

const shotTop = await cdp("Page.captureScreenshot", { format: "png" });
writeFileSync(join(outDir, "docs_desktop_top.png"), Buffer.from(shotTop.data, "base64"));

await evalJs(`document.getElementById('playground').scrollIntoView({ behavior: 'instant' });`);
await sleep(350);
// Click CS:S Surf preset & test filter
await evalJs(`(() => {
  const btns = [...document.querySelectorAll('#presetButtons button')];
  const surf = btns.find(b => b.textContent.includes('Surf'));
  if (surf) surf.click();
})()`);
await sleep(500);

const surfCheck = await evalJs(`(() => ({
  aa: document.getElementById('valAirAccel').textContent,
  tick: document.getElementById('valTickrate').textContent,
  speed: document.getElementById('hudSpeed').textContent,
  cmd: document.getElementById('cmdBatchOut').textContent
}))()`);

const shotLab = await cdp("Page.captureScreenshot", { format: "png" });
writeFileSync(join(outDir, "docs_desktop_lab.png"), Buffer.from(shotLab.data, "base64"));

await evalJs(`document.getElementById('cvars').scrollIntoView({ behavior: 'instant' });`);
await sleep(200);
const shotCvars = await cdp("Page.captureScreenshot", { format: "png" });
writeFileSync(join(outDir, "docs_desktop_cvars.png"), Buffer.from(shotCvars.data, "base64"));

// Test CVar search filtering
await evalJs(`(() => {
  const inp = document.getElementById('cvarSearch');
  inp.value = 'stamina';
  inp.dispatchEvent(new Event('input'));
})()`);
const filterCount = await evalJs(`document.querySelectorAll('#cvarTableBody tr').length`);

// Reset search and capture mobile 390x844
await evalJs(`(() => {
  const inp = document.getElementById('cvarSearch');
  inp.value = '';
  inp.dispatchEvent(new Event('input'));
})()`);
await cdp("Emulation.setDeviceMetricsOverride", { width: 390, height: 844, deviceScaleFactor: 2, mobile: true });
await evalJs(`window.scrollTo(0, 0);`);
await sleep(300);
const mobileHScroll = await evalJs(`document.documentElement.scrollWidth > window.innerWidth`);
const shotMob = await cdp("Page.captureScreenshot", { format: "png" });
writeFileSync(join(outDir, "docs_mobile_top.png"), Buffer.from(shotMob.data, "base64"));

ws.close();
chrome.kill();
server.close();
try { rmSync(userDataDir, { recursive: true, force: true }); } catch {}

console.log(JSON.stringify({ desktopCheck, surfCheck, filterCount, mobileHScroll, errors }, null, 2));
if (errors.length > 0 || desktopCheck.hScroll || mobileHScroll || desktopCheck.rows !== 29 || desktopCheck.presets !== 6 || filterCount < 1) {
  process.exit(1);
}
