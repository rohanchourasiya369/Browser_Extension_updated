# 🛡️ PromptGuard — Enterprise AI Firewall

> A Chrome/Edge/Brave/Firefox browser extension that intercepts every prompt sent
> to AI tools (ChatGPT, Gemini, Copilot, Claude), scans for sensitive data,
> and blocks, redacts, or alerts — all logged to a PostgreSQL database with
> a real-time Enterprise Security Dashboard.

---

## 📁 Project Structure

```
promptguard/
├── extension/                     ← Load this folder in Chrome/Edge/Brave
│   ├── manifest.json
│   ├── background.js              ← Service worker: heartbeat, role check, browser detect
│   ├── content.js                 ← Intercepts prompts on AI sites
│   ├── icons/
│   │   ├── icon16.png
│   │   ├── icon48.png
│   │   └── icon128.png
│   └── popup/
│       ├── popup.html             ← Extension popup UI
│       └── popup.js               ← Tabs: Test / Settings / Admin
│
├── frontend-dashboard/
│   └── index.html                 ← Open in any browser — no server needed
│
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── resources/
│       │   ├── application.properties
│       │   └── schema.sql
│       └── java/com/promptguard/
│           ├── config/            ← DatabaseInitializer (migration + user seeding)
│           ├── controller/        ← REST API endpoints
│           ├── detector/          ← PII / Secret / Source Code / Keyword engines
│           ├── model/             ← PromptRequest, PromptResponse, etc.
│           ├── repository/        ← SQL queries (JdbcTemplate)
│           └── service/           ← AuditService, PolicyEngine, etc.
│
└── README.md
```

---

## ✅ Prerequisites

| Tool        | Minimum Version | Check Command          |
|-------------|-----------------|------------------------|
| Java        | 17              | `java -version`        |
| Maven       | 3.8             | `mvn -version`         |
| PostgreSQL  | 13              | `psql --version`       |
| Chrome/Edge/Brave | Any       | —                      |

---

## 🚀 Step-by-Step Setup

---

### STEP 1 — Create PostgreSQL Database

Open **DBeaver** → right-click **Databases** → **Create New Database**

```
Database name:  prompt_guard
```

> ⚠️ Tables are created **automatically** on first backend start. Do NOT run schema.sql manually.

---

### STEP 2 — Configure Database Password

Open `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/prompt_guard
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD    ← change ONLY this line
```

---

### STEP 3 — Start the Backend

```powershell
cd backend
mvn clean spring-boot:run
```

**✅ Expected console output on first run:**
```
=== PromptGuard DB Init ===
⏭️  browser_name column already exists
✅ Seeded user: admin-user (ADMIN)
✅ Seeded user: rohan-user (USER)
✅ Seeded user: kushal-user (USER)
✅ Tables ready — users: 3, audit_logs: 0
=== DB Init Complete ===
```

**Verify backend is running:**
→ Open: `http://localhost:8080/api/v1/health`
→ Expected: `{"status":"UP","service":"PromptGuard"}`

---

### STEP 4 — Load the Extension in Chrome/Edge/Brave

1. Open browser → address bar → `chrome://extensions` → Enter
2. Enable **Developer mode** toggle (top-right corner)
3. Click **Load unpacked**
4. Select the `extension/` folder
5. 🛡️ PromptGuard icon appears in your toolbar

> Works in: **Chrome**, **Edge**, **Brave** (all Chromium-based)

---

### STEP 5 — Configure the Extension

1. Click the 🛡️ PromptGuard icon in the toolbar
2. Go to **⚙️ Settings** tab
3. Fill in:
   - **Employee ID** → `rohan-user` (or `admin-user`)
   - **Backend API URL** → `http://localhost:8080`
4. Click **Save Settings**

**✅ Backend console confirms:**
```
[Heartbeat] userId=rohan-user browser=Chrome
[Heartbeat] userId=rohan-user browser=Brave
[Heartbeat] userId=rohan-user browser=Edge
```

---

### STEP 6 — Open the Dashboard

1. Open `frontend-dashboard/index.html` in your browser
   (drag & drop the file into Chrome, or File → Open)
2. Login screen shows all users from the database
3. Select your user → **Sign In**

> Only users that exist in the `users` table can log in.

---

## 🌐 Browser Detection — How It Works

PromptGuard correctly identifies **all major browsers**:

| Browser | Detection Method |
|---------|-----------------|
| **Brave** | `navigator.brave.isBrave()` async API — checked FIRST |
| **Edge**  | `userAgent.includes("Edg/")` |
| **Opera** | `userAgent.includes("OPR/")` |
| **Firefox** | `userAgent.includes("Firefox/")` |
| **Safari** | `userAgent.includes("Safari/")` without `"Chrome"` |
| **Chrome** | `userAgent.includes("Chrome/")` — checked LAST |

> ⚠️ **Why Brave was showing as Chrome before:**
> Brave uses an identical Chrome user agent string for privacy reasons.
> The only way to detect Brave is via `navigator.brave.isBrave()` — an async API
> that must be checked **before** the Chrome check. This is now fixed.

**Console output per browser:**
```
[Heartbeat] userId=admin-user browser=Chrome    ← in Google Chrome
[Heartbeat] userId=admin-user browser=Brave     ← in Brave
[Heartbeat] userId=admin-user browser=Edge      ← in Microsoft Edge
[Heartbeat] userId=admin-user browser=Firefox   ← in Firefox
[Heartbeat] userId=admin-user browser=Safari    ← in Safari
```

---

## 📱 Responsive Design

The dashboard is fully responsive across all screen sizes:

| Screen | Width | Layout |
|--------|-------|--------|
| **Desktop** | ≥ 1100px | 6-col stat row, 2-col chart grid, fixed viewport (no scroll) |
| **Tablet**  | 641–1099px | 3-col stat row, single column charts, scrollable |
| **Mobile**  | ≤ 640px | 2-col stat row, stacked cards, tab bar at top |

**Responsive is implemented via React `useBreakpoint` hook** (not CSS media queries),
so it works perfectly with React inline styles:

```javascript
function useBreakpoint() {
  const [w, setW] = useState(() => window.innerWidth);
  useEffect(() => {
    const fn = () => setW(window.innerWidth);
    window.addEventListener("resize", fn);
    return () => window.removeEventListener("resize", fn);
  }, []);
  return { isDesktop: w >= 1100, isTablet: w >= 641 && w < 1100, isMobile: w < 641 };
}
```

---

## 👥 Default Users

| User ID       | Name   | Role  | Dashboard Access               |
|---------------|--------|-------|--------------------------------|
| `admin-user`  | Admin  | ADMIN | Full dashboard — all users     |
| `rohan-user`  | Rohan  | USER  | Own logs and stats only        |
| `kushal-user` | Kushal | USER  | Own logs and stats only        |

**Add more users** in DBeaver:
```sql
INSERT INTO users (user_id, display_name, role) VALUES ('alice', 'Alice', 'USER');
```

---

## 🔍 Detection Engines

| Engine              | What It Detects                       | Example                              |
|---------------------|---------------------------------------|--------------------------------------|
| `SecretDetector`    | API keys, AWS credentials, tokens     | `AKIAIOSFODNN7EXAMPLE`, `sk-abc123`  |
| `PiiDetector`       | SSN, credit card, phone, email, IP    | `123-45-6789`, `4111111111111111`    |
| `SourceCodeDetector`| SQL queries, code, class definitions  | `SELECT * FROM`, `public class Foo`  |
| `KeywordDetector`   | Confidential business keywords        | `confidential`, `internal use only`  |

---

## ⚙️ Policy Actions

| Action   | Risk Level  | What Happens                          | User Sees           |
|----------|-------------|---------------------------------------|---------------------|
| `ALLOW`  | NONE / LOW  | Prompt sent through silently          | Nothing             |
| `ALERT`  | MEDIUM      | Prompt sent + warning shown           | ⚠️ Orange toast    |
| `REDACT` | HIGH        | Sensitive text removed, rest sent     | ✏️ Purple toast    |
| `BLOCK`  | CRITICAL    | Prompt completely stopped             | 🚫 Red toast        |

---

## 🌐 API Reference

### POST `/api/v1/prompts` — Check a prompt
```json
Request:
{
  "userId":      "rohan-user",
  "tool":        "ChatGPT",
  "browserName": "Chrome",
  "prompt":      "My SSN is 123-45-6789",
  "timestamp":   "2026-03-16T12:00:00"
}
Response:
{
  "action":           "REDACT",
  "reason":           "PII detected: SSN",
  "riskScore":        75,
  "riskLevel":        "HIGH",
  "redactedPrompt":   "My SSN is [REDACTED]",
  "processingTimeMs": 8
}
```

### POST `/api/v1/heartbeat` — Extension keepalive (every 30s)
```json
Request:  { "userId": "rohan-user", "browserName": "Brave", "status": "ACTIVE" }
Console:  [Heartbeat] userId=rohan-user browser=Brave
```

### Analytics Endpoints
```
GET /api/analytics/risk-summary         → Counts + block rate
GET /api/analytics/tool-usage           → Prompts per AI tool
GET /api/analytics/risk-breakdown       → Risk type distribution
GET /api/analytics/recent-logs          → Last 200 logs (admin)
GET /api/analytics/my-prompts?userId=X  → User's own logs
GET /api/analytics/users                → All users (login screen)
GET /api/v1/users/{userId}/role         → User's role
GET /api/v1/health                      → Health check
```

---

## 🗄️ Database Schema

```sql
CREATE TABLE users (
    user_id      VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(200),
    role         VARCHAR(20) CHECK (role IN ('ADMIN','USER')),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id                 UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            VARCHAR(100) NOT NULL,
    tool               VARCHAR(100),    -- ChatGPT | Gemini | Copilot | Claude | PopupTest
    browser_name       VARCHAR(50),     -- Chrome | Edge | Brave | Firefox | Safari
    original_prompt    TEXT,
    redacted_prompt    TEXT,
    highest_risk_type  VARCHAR(50),     -- SECRET | PII | SOURCE_CODE | KEYWORD | NONE
    risk_score         INTEGER,         -- 0–100
    risk_level         VARCHAR(20),     -- NONE | LOW | MEDIUM | HIGH | CRITICAL
    action             VARCHAR(20),     -- ALLOW | ALERT | REDACT | BLOCK
    action_reason      TEXT,
    processing_time_ms BIGINT,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🧪 Test Cases

### TC-01 — Safe Prompt → ALLOW ✅
```
Site:     https://chatgpt.com
Prompt:   How do I reverse a string in Python?
Expected: No toast. Prompt sent normally.
Console:  [AuditService] ✅ Saved — action=ALLOW
DB:       action=ALLOW, risk_score=0
```

### TC-02 — SQL Query → BLOCK 🚫
```
Site:     https://chatgpt.com
Prompt:   Fix: SELECT * FROM users WHERE password='admin123'
Expected: Red toast "BLOCK — Source code detected"
Console:  [AuditService] ✅ Saved — action=BLOCK
DB:       highest_risk_type=SOURCE_CODE
```

### TC-03 — SSN → REDACT ✏️
```
Site:     https://gemini.google.com
Prompt:   My SSN is 123-45-6789, help fill this form
Expected: Purple toast, SSN replaced by [REDACTED]
DB:       original_prompt has SSN, redacted_prompt has [REDACTED]
```

### TC-04 — AWS Key → BLOCK 🚫
```
Site:     https://copilot.microsoft.com
Prompt:   My AWS key is AKIAIOSFODNN7EXAMPLE
Expected: Red toast "BLOCK — Secret/credential detected"
DB:       highest_risk_type=SECRET
```

### TC-05 — Confidential Keyword → ALERT ⚠️
```
Site:     https://chatgpt.com
Prompt:   Here is our internal use only Q3 roadmap
Expected: Orange warning toast, prompt still sent
DB:       action=ALERT
```

### TC-06 — Credit Card → REDACT ✏️
```
Popup Test tab
Prompt:   My credit card is 4111111111111111
Expected: REDACT result, card number removed
DB:       tool=PopupTest, browser_name=Chrome/Brave/Edge
```

### TC-07 — Browser Detection (All Browsers)
```
In Chrome:  [Heartbeat] userId=... browser=Chrome
In Brave:   [Heartbeat] userId=... browser=Brave
In Edge:    [Heartbeat] userId=... browser=Edge
In Firefox: [Heartbeat] userId=... browser=Firefox
In Safari:  [Heartbeat] userId=... browser=Safari
DB:         browser_name column shows correct browser per row
```

### TC-08 — Responsive Dashboard
```
Desktop (≥1100px):
  - 6 stat cards in one row
  - Line chart left + Donut right
  - Recent Alerts left + AI Tool Usage right
  - Compliance row full width
  - No page scroll

Tablet (641–1099px):
  - 3 stat cards per row
  - Charts stack in single column
  - Page scrolls normally

Mobile (≤640px):
  - 2 stat cards per row
  - All sections stacked
  - Tab bar shown at top below nav
```

### TC-09 — Dashboard Login Gate
```
Steps:    Open frontend-dashboard/index.html
Expected: Shows admin-user, rohan-user, kushal-user only
          rohan-user → sees only own logs
          admin-user → sees ALL users + full analytics
```

### TC-10 — Non-Admin Cannot Disable Extension
```
Steps:    Set userId=rohan-user → try to disable protection in popup
Expected: Toggle snaps back ON
Console:  [PromptGuard] 🚨 Alert sent [DISABLE_ATTEMPT] user="rohan-user"
```

---

## 📋 Use Cases

| # | Scenario | Result |
|---|---|---|
| UC-01 | Developer pastes DB password into ChatGPT | BLOCKED before reaching AI |
| UC-02 | HR includes employee SSNs in prompt | PII auto-REDACTED |
| UC-03 | Compliance needs audit of all AI usage | Admin dashboard: full log with user, browser, risk |
| UC-04 | IT needs to know which browser employees use | `browser_name` shows Chrome/Brave/Edge per row |
| UC-05 | Employee views own AI usage history | Login as user → own logs only |
| UC-06 | Employee tries to disable PromptGuard | Alert sent, toggle locked |
| UC-07 | Team uses mix of Chrome and Brave | Both detected correctly in DB |

---

## 🔧 Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| `Could not load icon` when loading extension | Icons missing | Load `extension/` folder (contains `icons/` subfolder) |
| `[Heartbeat] browser=Chrome` in Brave | Old code | Use this zip — Brave fixed via `navigator.brave.isBrave()` |
| `[Heartbeat] userId=anonymous-user` | Employee ID not saved | Popup → Settings → enter ID → Save |
| Dashboard shows no users | Backend not running | Run `mvn clean spring-boot:run` |
| `[AuditService] ❌ Failed` | UUID type mismatch (old bug) | Use this zip — `id` removed from INSERT, auto-generated by PostgreSQL |
| Charts show "No data yet" | No prompts sent yet | Send prompts, click Refresh |
| Dashboard scrolls on desktop | Old CSS approach | Fixed — uses React `useBreakpoint` hook |

---

## 🏗️ Tech Stack

| Layer     | Technology                        |
|-----------|-----------------------------------|
| Extension | Chrome Manifest V3                |
| Dashboard | React 18 + Chart.js 4.4 (CDN, no build needed) |
| Backend   | Spring Boot 3.2, Java 17          |
| Database  | PostgreSQL 13+                    |
| DB Layer  | HikariCP + JdbcTemplate (no JPA)  |

---

*PromptGuard v1.0 — Enterprise AI Security*
"# Browser_Extension_updated" 
