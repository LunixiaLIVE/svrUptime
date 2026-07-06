<div align="center">

# ⏱️ svrUptime

### Broadcasts server uptime at each in-game day start and evening.

![](https://img.shields.io/badge/Fabric-DBA463?style=for-the-badge&logoColor=white)&nbsp;![](https://img.shields.io/badge/NeoForge-F16436?style=for-the-badge&logoColor=white)&nbsp;![](https://img.shields.io/badge/Paper-2A9DF4?style=for-the-badge&logoColor=white)&nbsp;

[![](https://img.shields.io/badge/Download_on-Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/project/svruptime)&nbsp;[![](https://img.shields.io/badge/Download_on-CurseForge-F16436?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/svruptime)

![](https://img.shields.io/badge/Minecraft-26.x_%7C_1.21.x-62B47A?style=flat-square) ![](https://img.shields.io/badge/Side-Single_Player_%26_Server-8E44AD?style=flat-square) ![](https://img.shields.io/badge/Fabric_API-required_on_Fabric-4A90D9?style=flat-square) ![](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

</div>

---

svrUptime keeps a quiet running tally of how long your world has existed and announces it to everyone at the two natural turning points of the Minecraft day — **dawn and dusk**. No dashboards, no scoreboard clutter: just a clean chat readout when the sun comes up, a short flavor line when night falls, and an `/uptime` command for anyone curious in between. It runs **entirely server-side**, so vanilla clients can connect and see every message, and it works just as well in single-player.

## ✨ Features

- **Two daily broadcasts, on the in-game clock.** A full uptime report at **dawn** (the start of each Minecraft day, ~06:00) and a short flavor line at **dusk** (~18:00, tick 12000). Each fires exactly once per day.
- **Dual time views.** Every report shows uptime two ways: **Minecraft Time** (total in-game days rolled up into a months → years → decades → … → millennia calendar) and **Real Time** (actual elapsed play time, days → decades).
- **`/uptime` on demand.** Anyone can query the current uptime at any moment and choose how large a unit to roll up to — from **days** all the way to **millennia**.
- **Configurable morning & evening messages.** Set your own dawn and dusk text — standard Minecraft color codes supported — in a plain JSON file.
- **Independent toggles.** Turn the uptime broadcast, the morning message, and the evening message on or off separately — live, in-game, no restart.
- **Live reload.** Edit the config and apply it instantly with an admin command.
- **Server-side & universal.** One jar runs on **Fabric and NeoForge**; vanilla clients need nothing, and it works in single-player too.

## 🔧 How it works

### ⏱️ Uptime tracking

svrUptime reads the overworld's **total world age** (game time in ticks) every server tick — the same counter Minecraft persists with your save, so uptime survives restarts and reflects the true age of the world, not just the current session. That tick total is converted into two parallel readouts:

- **Minecraft Time** — total in-game days cascaded into a tidy calendar: **30 days = 1 month**, **12 months (360 days) = 1 year**, then decades, centuries, and millennia. The current day is 1-indexed, so a brand-new world reads *"Day 1."*
- **Real Time** — total real seconds (ticks ÷ 20) cascaded into **days → months (30d) → years (365d) → decades**, so you can see how long the server has actually been running.

Both readouts are cached and only recomputed once per in-game day, so the per-tick check stays essentially free.

### 🌅 Morning & evening broadcasts

Each server tick, the mod watches the overworld clock:

- **Dawn** — when the day counter rolls over (the start of a new Minecraft day, ~06:00), it broadcasts the full **`=== Server Uptime ===`** report to every online player, followed by your **morning message**.
- **Dusk** — when the clock crosses **tick 12000** (~18:00), it sends your **evening message**.

Each event fires **once per day**. On the first tick after a load, svrUptime records the current day *without* firing anything, so you never get a spurious broadcast the instant the server boots.

### ♻️ Reload & persistence

All five settings live in `config/svruptime.json`. The in-game toggles write their changes straight back to that file, and **`/uptime admin reload`** re-reads it on the fly — so you can change your messages or flip a switch without a restart, and the command state and the file never drift apart.

## ⌨️ Commands

Everything hangs off **`/uptime`**, with **`/ut`** as a shorthand alias for all of it. `query` and `help` are open to everyone; the `admin` branch requires **gamemaster / operator permission**.

| Command | Access | What it does |
|---|:---:|---|
| `/uptime query` | Everyone | Show the full uptime report (rolled all the way up to millennia) |
| `/uptime query <unit>` | Everyone | Same report, capped at a chosen largest unit — `days`, `months`, `years`, `decades`, `centuries`, `millennia` |
| `/uptime help` | Everyone | List available commands (admin lines shown only to admins) |
| `/uptime admin broadcast enable\|disable` | Admin | Toggle the daily uptime report |
| `/uptime admin morning enable\|disable` | Admin | Toggle the dawn (morning) message |
| `/uptime admin evening enable\|disable` | Admin | Toggle the dusk (evening) message |
| `/uptime admin status` | Admin | Show current toggles, total world ticks, and real days elapsed |
| `/uptime admin reload` | Admin | Re-read `svruptime.json` from disk |

> [!TIP]
> `/ut` is a full redirect — anywhere you'd type `/uptime`, `/ut` works identically (`/ut query years`, `/ut admin status`, …). Picking a smaller `<unit>` keeps everything in that unit; e.g. `/uptime query days` reports the whole age as a single day count.

## 💡 Use cases

- **Long-running SMPs & anarchy servers** — give the world a sense of history; *"Day 4,217"* hits differently than a blank morning sky.
- **Seasonal & milestone events** — reword the morning message to count toward an event, mark a season, or celebrate a server anniversary.
- **Ambience** — a gentle sunrise/sunset flavor line that makes a server feel alive without spamming chat.
- **Admin at-a-glance** — `/uptime admin status` gives a quick read on real uptime and world age for anyone helping run the box.
- **Single-player journaling** — watch your own world quietly rack up in-game centuries and real-time days.

## ⚙️ Configuration

The config lives at **`config/svruptime.json`**, is created on first launch with the defaults below, and hot-reloads with **`/uptime admin reload`**. Both message strings accept standard Minecraft **`§` color/format codes**.

```jsonc
{
  "broadcastEnabled": true,      // send the full uptime report at dawn
  "morningMessageEnabled": true, // send the morning message at dawn
  "eveningMessageEnabled": true, // send the evening message at dusk
  "morningMessage": "§6☀ A new day has begun.",
  "eveningMessage": "§9☾ The night has come."
}
```

> [!NOTE]
> The three `*Enabled` flags are the same switches flipped by `/uptime admin …`, and those in-game changes are written straight back to this file — so the command state and the file never disagree.

## 📦 Versions &amp; downloads

> [!NOTE]
> This repo uses a **branch-per-version** layout. This `main` branch is **documentation only** — the code for each Minecraft version lives on its own branch, each with an independent history and its own `CHANGELOG.md`.

| Branch | Minecraft | Loaders | Dependencies | Log |
|:------:|:---------:|:-------:|:------------:|:---:|
| [`multi_26.2`](https://github.com/LunixiaLIVE/svrUptime/tree/multi_26.2) | 26.2.x | Fabric · NeoForge | Fabric API *(Fabric only)* | [📄](https://github.com/LunixiaLIVE/svrUptime/blob/multi_26.2/CHANGELOG.md) |
| [`multi_26.1`](https://github.com/LunixiaLIVE/svrUptime/tree/multi_26.1) | 26.1, 26.1.1, 26.1.2 | Fabric · NeoForge | Fabric API *(Fabric only)* | [📄](https://github.com/LunixiaLIVE/svrUptime/blob/multi_26.1/CHANGELOG.md) |
| [`multi_1.21.11`](https://github.com/LunixiaLIVE/svrUptime/tree/multi_1.21.11) | 1.21.11 | Fabric · NeoForge | Fabric API *(Fabric only)* | [📄](https://github.com/LunixiaLIVE/svrUptime/blob/multi_1.21.11/CHANGELOG.md) |
| [`multi_1.21`](https://github.com/LunixiaLIVE/svrUptime/tree/multi_1.21) | 1.21 – 1.21.10 | Fabric · NeoForge | Fabric API *(Fabric only)* | [📄](https://github.com/LunixiaLIVE/svrUptime/blob/multi_1.21/CHANGELOG.md) |
| [`plugin_1.21.11`](https://github.com/LunixiaLIVE/svrUptime/tree/plugin_1.21.11) | 1.21.11 | Paper | Paper (no extra deps) | — |

> [!TIP]
> Every `multi_*` branch builds **one jar that runs on both Fabric and NeoForge**. On 26.x that's a shared universal jar (Minecraft is unobfuscated there); on 1.21.x it's a jar-in-jar bundle (`-multi.jar`) with the Fabric and NeoForge builds nested inside, each loader picking its own. Per-loader `-fabric` / `-neoforge` jars are produced too (`build/staging/`). Fully self-contained — **no extra library mods to install**.

<details>
<summary>🛠️ <b>Building from source</b></summary>

Each code branch is a self-contained Gradle project. Grab the branch for your Minecraft version:

```bash
git clone -b multi_26.2 https://github.com/LunixiaLIVE/svrUptime.git
cd svrUptime
./gradlew build
```

The universal jar lands in `build/libs/` — drop it into your `mods/` folder on either loader.
</details>

## 📄 License

Released under the **MIT License**.

<div align="center"><sub>⛏️ Part of <a href="https://github.com/LunixiaLIVE/Lunixia-Minecraft-QOL-Mods">Lunixia's Minecraft QOL Mods</a>.</sub></div>
