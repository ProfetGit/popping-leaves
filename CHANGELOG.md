# Changelog

## 0.2.0 — 2026-11-22
Ports: Minecraft 26.3, 26.2, 1.21.11, 1.21.1 and 1.20.1 on Fabric, Quilt, Forge and NeoForge (NeoForge 1.20.1 runs the Forge jar).
- Fabric API is no longer needed: the server tick, the config poll and the client tick run from mixins, so every loader shares one code path.
- Optional on either side: a vanilla client can join a server with the mod and the other way round (Forge/NeoForge `displayTest` is set to ignore the version).
- Before 26.3 the pop copy is drawn in vanilla's own buffers (1.21.1, 1.20.1) or its submit storage (1.21.11, 26.2) and hands over to the chunk mesh when the mesh is swapped in. Sodium and Iris on those versions are untested: the hand-off falls back to a 150 ms watchdog.

## 0.1.0 — 2026-11-22
First release.
- Server: decaying leaves go in a ripple from the cut, vanilla loot, per-tick cap, stops under `/tick freeze`, steps aside at `randomTickSpeed` 0 and next to other fast-decay mods.
- Client: tremble-free pop with shed chip, squash, shrink, solid-texel chips and a capped rustle; reduced motion mode.
- Config: live-reloaded `config/snappy_leaf_decay.json`, Mod Menu screen.
