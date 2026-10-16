// E5-S2: open every page at phone, tablet and desktop widths and report what the story forbids:
// sideways scrolling, inputs without a label, images without alt text, buttons without a name.
// Needs the app up (make up) and Google Chrome. usage: node scripts/check-responsive.mjs [movieId] [personId]
import { spawn } from 'node:child_process';

// macOS path by default; set CHROME=/path/to/chrome on another OS
const CHROME = process.env.CHROME ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const BASE = 'http://localhost:5173';
const [movieId = '1', personId = '1'] = process.argv.slice(2);
const PAGES = ['/', `/movies/${movieId}`, '/movies/new', `/movies/${movieId}/edit`, '/people', `/people/${personId}`, '/people/new', `/people/${personId}/edit`, '/search?q=a'];
const WIDTHS = [375, 768, 1280];

// What the page reports about itself once rendered; runs inside the browser.
const AUDIT = `JSON.stringify({
	viewport: document.documentElement.clientWidth,
	scrollWidth: document.documentElement.scrollWidth,
	unlabelled: [...document.querySelectorAll('input, select, textarea')].filter(el =>
		!el.labels?.length && !el.getAttribute('aria-label') && !el.getAttribute('aria-labelledby')).map(el => el.id || el.name || el.type),
	noAlt: [...document.querySelectorAll('img')].filter(img => !img.hasAttribute('alt')).map(img => img.src),
	namelessButtons: [...document.querySelectorAll('button')].filter(b =>
		!(b.textContent || '').trim() && !b.getAttribute('aria-label')).length,
	columns: (() => { const g = document.querySelector('.grid'); if (!g) return null;
		return getComputedStyle(g).gridTemplateColumns.split(' ').length; })()
})`;

const chrome = spawn(CHROME, ['--headless=new', '--disable-gpu', '--remote-debugging-port=9333', '--no-first-run', 'about:blank'], { stdio: 'ignore' });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let target;
for (let i = 0; i < 40 && !target; i++) {
	await sleep(250);
	try { target = (await (await fetch('http://localhost:9333/json')).json()).find((t) => t.type === 'page'); } catch {}
}
if (!target) { chrome.kill(); throw new Error('chrome did not start'); }
const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((r) => (ws.onopen = r));

// DevTools protocol: every request carries an id and the answer carries the same id, so each pending
// promise is resolved when its number comes back.
let nextId = 0;
const pending = new Map();
ws.onmessage = (event) => {
	const message = JSON.parse(event.data);
	if (!message.id || !pending.has(message.id)) return;
	pending.get(message.id)(message.result);
	pending.delete(message.id);
};
function send(method, params = {}) {
	return new Promise((resolve) => {
		const id = ++nextId;
		pending.set(id, resolve);
		ws.send(JSON.stringify({ id, method, params }));
	});
}

let failures = 0;
for (const width of WIDTHS) {
	await send('Emulation.setDeviceMetricsOverride', { width, height: 900, deviceScaleFactor: 1, mobile: width < 768 });
	for (const path of PAGES) {
		await send('Page.navigate', { url: BASE + path });
		await sleep(1500);
		const r = await send('Runtime.evaluate', { expression: AUDIT, returnByValue: true });
		const a = JSON.parse(r.result.value);
		const problems = [];
		if (a.scrollWidth > a.viewport) problems.push(`scrolls sideways (${a.scrollWidth} > ${a.viewport})`);
		if (a.unlabelled.length) problems.push(`unlabelled fields: ${a.unlabelled.join(', ')}`);
		if (a.noAlt.length) problems.push(`${a.noAlt.length} image(s) without alt`);
		if (a.namelessButtons) problems.push(`${a.namelessButtons} button(s) without a name`);
		const cols = a.columns === null ? '' : ` grid ${a.columns} col`;
		console.log(`${problems.length ? 'FAIL' : 'PASS'}  ${String(width).padStart(4)}px ${path.padEnd(22)}${cols}${problems.length ? '  ' + problems.join('; ') : ''}`);
		failures += problems.length;
	}
}
ws.close(); chrome.kill();
console.log(failures ? `RESPONSIVE: ${failures} problem(s)` : 'RESPONSIVE: ALL PASSED');
process.exit(failures ? 1 : 0);
