// keep-alive.js
// Keeps the Render deployment warm by pinging the health endpoint every 30 seconds
// Run with: node keep-alive.js
// Run in background with: node keep-alive.js &  (Linux/Mac)
//                          start /B node keep-alive.js  (Windows)

const HEALTH_URL = "https://amalitech-deg-project-based-challenges-9f5j.onrender.com/api/monitors/health";
const INTERVAL_MS = 30000; // 30 seconds

let pingCount = 0;
let successCount = 0;
let failCount = 0;

function timestamp() {
  return new Date().toISOString().replace("T", " ").substring(0, 19);
}

async function ping() {
  pingCount++;
  try {
    const start = Date.now();
    const res = await fetch(HEALTH_URL);
    const elapsed = Date.now() - start;
    const data = await res.json();

    if (res.ok && data.status === "healthy") {
      successCount++;
      console.log(`[${timestamp()}] PING #${pingCount} OK (${elapsed}ms) - service: ${data.service} | activeTimers: ${data.activeTimers ?? 0} | db: ${data.database}`);
    } else {
      failCount++;
      console.log(`[${timestamp()}] PING #${pingCount} WARN (${elapsed}ms) - status: ${res.status}`);
    }
  } catch (err) {
    failCount++;
    console.log(`[${timestamp()}] PING #${pingCount} FAIL - ${err.message}`);
  }

  console.log(`  Stats: ${successCount} success | ${failCount} failed | ${pingCount} total`);
}

async function start() {
  console.log("========================================");
  console.log("   WATCHDOG KEEP-ALIVE SERVICE");
  console.log("========================================");
  console.log(`Target : ${HEALTH_URL}`);
  console.log(`Interval: every ${INTERVAL_MS / 1000} seconds`);
  console.log(`Started : ${timestamp()}`);
  console.log("========================================\n");
  console.log("Press Ctrl+C to stop\n");

  // Ping immediately on start
  await ping();

  // Then ping every 30 seconds
  setInterval(ping, INTERVAL_MS);
}

start();
