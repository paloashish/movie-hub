// Screenshot a URL at a given emulated device width with headless Chrome over the DevTools protocol.
// usage: node shot.mjs <url> <width> <height> <out.png>
import { spawn } from 'node:child_process';
import { writeFileSync } from 'node:fs';

const [url, width, height, out] = process.argv.slice(2);
const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', [
	'--headless=new', '--disable-gpu', '--remote-debugging-port=9333', '--no-first-run', 'about:blank'
], { stdio: 'ignore' });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let target;
for (let i = 0; i < 40 && !target; i++) {
	await sleep(250);
	try {
		const list = await (await fetch('http://localhost:9333/json')).json();
		target = list.find((t) => t.type === 'page');
	} catch {}
}
if (!target) { chrome.kill(); throw new Error('chrome did not start'); }

const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((r) => (ws.onopen = r));
let id = 0;
const pending = new Map();
ws.onmessage = (e) => { const m = JSON.parse(e.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m.result); pending.delete(m.id); } };
const send = (method, params = {}) => new Promise((r) => { const n = ++id; pending.set(n, r); ws.send(JSON.stringify({ id: n, method, params })); });

await send('Emulation.setDeviceMetricsOverride', { width: +width, height: +height, deviceScaleFactor: 1, mobile: true });
await send('Page.navigate', { url });
await sleep(2500);
const m = await send('Runtime.evaluate', { expression: 'JSON.stringify({viewport: document.documentElement.clientWidth, scrollWidth: document.documentElement.scrollWidth})', returnByValue: true });
console.log(m.result.value);
const shot = await send('Page.captureScreenshot', { format: 'png' });
writeFileSync(out, Buffer.from(shot.data, 'base64'));
ws.close();
chrome.kill();
