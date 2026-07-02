# svrUptime

Broadcasts server uptime at each in-game day start and evening.
**Client & server.**

## Features

- Broadcasts uptime at 6am and 6pm in-game
- /uptime query in days → millennia
- Configurable morning/evening messages, /ut alias

## Versions & downloads

This repository uses a **branch-per-version** layout: this `main` branch is documentation only — the code for each Minecraft version lives on its own branch, each with its own history and `CHANGELOG.md`.

| Branch | Minecraft | Loaders | Dependencies | Notes |
|--------|-----------|---------|--------------|-------|
| [`multi_26.2`](https://github.com/LunixiaLIVE/svrUptime/tree/multi_26.2) | 26.2.x | Fabric · NeoForge | Fabric API *(Fabric only)* | [changelog](https://github.com/LunixiaLIVE/svrUptime/blob/multi_26.2/CHANGELOG.md) |
| [`multi_26.1`](https://github.com/LunixiaLIVE/svrUptime/tree/multi_26.1) | 26.1, 26.1.1, 26.1.2 | Fabric · NeoForge | Fabric API *(Fabric only)* | [changelog](https://github.com/LunixiaLIVE/svrUptime/blob/multi_26.1/CHANGELOG.md) |
| [`multi_1.21.11`](https://github.com/LunixiaLIVE/svrUptime/tree/multi_1.21.11) | 1.21.11 | Fabric · NeoForge | Fabric API *(Fabric only)* | [changelog](https://github.com/LunixiaLIVE/svrUptime/blob/multi_1.21.11/CHANGELOG.md) |
| [`multi_1.21`](https://github.com/LunixiaLIVE/svrUptime/tree/multi_1.21) | 1.21 – 1.21.10 | Fabric · NeoForge | Fabric API *(Fabric only)* | [changelog](https://github.com/LunixiaLIVE/svrUptime/blob/multi_1.21/CHANGELOG.md) |
| [`plugin_1.21.11`](https://github.com/LunixiaLIVE/svrUptime/tree/plugin_1.21.11) | 1.21.11 | Paper | Paper (no extra deps) | — |

The `multi_*` branches each build a single **universal** jar that runs on **both** Fabric and NeoForge (per-loader `-fabric` / `-neoforge` jars are also produced), and are fully standalone — **no Architectury API at runtime**.

## License

MIT
