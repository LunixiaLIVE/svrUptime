# svrUptime — Changelog

Broadcasts server uptime at each in-game day start and evening.
Works on client and server.

Format based on [Keep a Changelog](https://keepachangelog.com/); versioning per [SemVer](https://semver.org/).

## [1.2.5] — 2026-07-02

Multi-loader release for **Minecraft 1.21 – 1.21.10** (a single jar covering the entire 1.21 line up to 1.21.10).

### Added
- **Fabric + NeoForge** support from a single **universal** jar (per-loader `-fabric` / `-neoforge` jars are also produced).
- **Minecraft 1.21, 1.21.1, …, 1.21.10** compatibility in one jar. svrUptime's only version-sensitive API is the command permission check, which stayed stable across this whole range.

### Notes
- The jar stops at **1.21.10**. Minecraft **1.21.11** removed `CommandSourceStack.hasPermission(int)` in favour of a new `permissions()` API — a hard break on both loaders (not a bridgeable rename) — so 1.21.11 lives on the separate `multi_1.21.11` branch.

### Changed
- **No Architectury API required** — svrUptime is fully standalone. Events are wired natively (Fabric API on Fabric, the NeoForge event bus on NeoForge).

### Dependencies
- **Fabric jar:** Minecraft 1.21–1.21.10, Fabric Loader >= 0.19.2, Fabric API *(Fabric only)*
- **NeoForge jar:** Minecraft 1.21–1.21.10, NeoForge 21.0.x–21.10.x  *(no Fabric API, no Architectury)*
