# Snappy Leaf Decay — PRD

Status: draft (2026-11-22). Mod id `snappy_leaf_decay`, package `io.github.profetgit.snappy_leaf_decay`. Tracker: `STATUS.md`. Draft copy: `docs/draft-v1.md`.

## 1. Summary
- **Promise:** cut a tree and its leaves follow in one readable wave: each trembles, snaps out and sprays a few solid pixel chips, instead of lingering for a minute and vanishing one by one.
- **Moment (the muted 5-second shot):** an oak's last log goes, a wave of leaves runs outward over about 1.5 s, each leaf a 115 ms squash-and-snap with chips, the canopy is gone and the sapling items are on the ground.
- **Edge:** server-side fast decay is crowded (Leaves Be Gone, Fast Leaf Decay, ShatterLeaves, Accelerated Decay, Leaf Decay Instant, CanopyFade, Ripple Leaf Decay, Leaves Us In Peace). None found with an authored client animation (anticipation, snap, chips). The pop is the edge; see § 17.
- **Sides:** two halves in one Fabric 26.3 jar. Server half works with vanilla clients (fast wave, vanilla loot). Client half works on vanilla servers (pop, no tremble). Both = full effect.
- **Target:** Minecraft 26.3, Fabric only for 1.0 (workspace default); ports in § 15.

## 2. Changes from the draft
The draft is one line ("fast leaf decay mod, leafs pop in A&S style"), so no contradictions to fix; these are the gaps a literal build would hit:
- "Fast decay" and "pop" live on different sides. A pop needs the client, so the draft alone would have made a client-only mod that never speeds anything up. Fix: two halves, each useful alone (§ 1).
- Vanilla decay is a random tick, about a minute per leaf on average, with no wave. Fix: a server queue gives the wave and a readable total time (§ 5.1).
- A literal "every leaf pops" turns a 300-leaf tree into 300 overlapping sounds, chips and items. Fix: voice, chip and per-tick decay caps (§ 5.3, § 10).
- "Pop" must not mean "break": a leaf removed by decay is not a player break (no crack, no tool). Fix: the client tells them apart by state, never by guessing (§ 5.2).
- Kept on purpose: vanilla loot, vanilla persistent-leaf rule, no instant removal (an instant tree loses the moment).

## 2b. Changes during the build (2026-11-22, M1-M2; the sections below are corrected where they conflict)
- **Ripple origin:** the server cannot see the felled log, so `LeavesBlock.updateShape` notes where a distance-1 leaf lost a non-log, non-leaf neighbour (the cut) and a decaying leaf goes `delay + min(maxRipple, round(distance_to_nearest_cut * stagger)) + jitter` ticks after it became decaying. Defaults delay 2, jitter 3, stagger 7 ticks per block, maxRipple 60, maxPerTick 40. Vanilla's own distance cascade is lockstep (every leaf reaches 7 within a few ticks), so it gave no ripple; this does. A leaf with no cut nearby gets a short random spread. Measured: oak (96 leaves) first removal 6 ticks and last 26 ticks after the cut; 1,920 leaves 61 ticks.
- **Anticipation:** shed chip instead of a block tremble (see 3); no S2C packet.
- **Chips:** one opaque texel of the leaf texture each (never a transparent hole, never a multi-texel window), edge about 1-2 texels of world size, the biome tint without TerrainParticle's 60% darkening, gravity 0.7, 10-16 ticks, shrinking over the last 3. The critic rejected the first version (4-6 texel patches that hung in the air for 1.5 s and read as litter).
- **Stand-aside setting:** `server.respectRandomTickSpeed` (default true) in the config, so tests can run with `random_tick_speed 0`.
- **Compat stand-aside ids** in `DecayQueue.OTHERS` come from store listings and are unverified.
- Fixed numbers: worst server tick and mean (pillar 2), chips alive cap 600 and per-tick budgets 16 full + 64 lite (`Decays`).

## 3. Decisions
| Topic | Choice | Date |
|---|---|---|
| Variant | Sharpened: wave decay + tremble/snap/chips (over Bold flutter-on-wind, over Lean) | 2026-11-22 |
| Home | New mod, client engine synced from Snappy Blocks like Snappy Doors (over a Snappy Blocks module) | 2026-11-22 |
| Slot | SnappyDoors parked (still M0, PRD kept) | 2026-11-22 |

My calls, open to veto:
- Name **Snappy Leaf Decay** (family + the searched words). Shortlist: Leaf Pop, Poof Leaves. Slug check at M0.
- Fabric 26.3 only for 1.0.
- The anticipation is a shed chip (one chip lets go of a leaf that has just become decaying), seen by the client from the synced state: no packet, so it works on a vanilla server too. A real block tremble needs the chunk mesh hidden (Snappy Blocks' hide path with its Sodium hooks): 1.1 at the earliest.
- The mod stands aside (server half off) when `randomTickSpeed` is 0: admins use that to freeze growth and decay.
- Decays that were already pending when a chunk loaded stay vanilla (random ticks); the fast queue takes only leaves that become decaying while loaded.
- Whole tree total time target 1.0-2.0 s for an oak, at most 3 s for a jungle giant (guess, tune at feel check).
- No new keys, no GUI; one Mod Menu settings screen (SnappyBlocks pattern) at M4.

## 4. Pillars
1. **Readable wave.** A normal oak (about 80 leaves) is fully gone 1.0-2.0 s after its last log leaves; a jungle giant within 3 s. Measured by scenes `oak` and `giant` (named asserts on first and last decay tick).
2. **Cheap on the server.** A 1,900-leaf tree costs at most 2 ms in its worst tick and at most 1.5 ms mean over the busy ticks (`maxPerTick` 40). That is the cost of the removals themselves (about 20 us per leaf, drops and neighbour updates), the same total vanilla pays over a minute; the queue's own work is a heap pop. Measured by scene `perf` (timer around the queue): worst 1.6-2.0 ms, mean 0.84-0.97 ms.
3. **Cheap on the client.** At most 1 ms per frame with 500 leaves popping at once, at most 600 live chips. Measured by scene `crowd` (frame time with and without the mod, same seed).
4. **Nothing lost, nothing new.** Loot per leaf equals vanilla's loot table; player-placed (persistent) leaves never decay; a decay never touches a block that stopped being a decaying leaf. Measured by scene `loot` (1,000 leaves, drop counts within a binomial band of vanilla's) and `stale`.
5. **Native and still.** Solid-texel chips only, 60 fps, no flicker, nothing teleports. Measured by `wb img flicker` on scenes `oak` and `giant`, and the director's verdict on stills.

## 5. Features (1.0)

### 5.1 Server: decay wave
- A leaf that becomes decaying (`DISTANCE == 7`, not persistent) is queued with a due tick = now + `delay` + per-leaf `jitter` (guess 6 ticks + 0-8 ticks, tune at feel check).
- The wave direction is an M1 spike (§ 12): option A uses vanilla's own distance cascade (one hop per tick) as the wave; option B orders by distance from the removed log. Default: from the cut outward, then top to bottom within a ring.
- At due time re-read the block: if it is still the same leaf and still decaying, call vanilla's own removal path (`dropResources` then `removeBlock`, same as `LeavesBlock.randomTick`); otherwise drop the entry. No second drop path.
- At most `max_per_tick` decays per level per tick (guess 64); the rest keep their order and run next tick.
- `/tick freeze` or a frozen tick manager: the queue does not advance (check `tickRateManager().runsNormally()`).
- `doTileDrops` false: no drops, blocks still removed. `randomTickSpeed` 0: the half stands aside (§ 3).
- The queue is per `ServerLevel`, held in a level-keyed map and cleared when the level unloads (no static state across singleplayer worlds). Entries in unloaded chunks are dropped; those leaves fall back to vanilla random ticks.
- Mangrove, azalea, cherry, pale oak and any other `LeavesBlock` subclass are covered; blocks that are not `LeavesBlock` (some modded leaves) are not (§ 16).
- Other fast-decay mods installed: the server half logs once and stays off (`Compat`).

### 5.2 Client: tremble and snap
- Decay vs break is decided by state: a leaf block that disappears while its last known state was decaying (`DISTANCE == 7`, not persistent) and no player break claim covers that position is a decay. Anything else (a player's break, Timber's or Veinminer's removal, explosions) is not ours and gets no pop.
- **Tremble** (anticipation, 2-4 ticks, guess): when the S2C warning names a leaf, it shakes by up to 1/16 block in two axes with a tiny colour dip toward yellow-brown. No tremble without the packet.
- **Snap** (about 115 ms, guess): the block squashes 12% on the long axis in 2 ticks, then shrinks to nothing in 2 ticks with a hard stop at the end (hit-stop feel, no rubber). One block shrinking away, never mini copies of the block (rejected 2026-10-29, Snappy Blocks).
- **Chips:** 3-5 solid-texel chips per leaf (chip = 1-2 texels sampled from that leaf's own texture, biome tint applied), thrown outward from the cut origin, fall with vanilla particle gravity, gone in 0.5-0.8 s. Pooled, cap 600 live; nearest leaves get the full count, leaves beyond 24 blocks get one chip, beyond 48 none (distance LOD).
- Driven by the game's frame delta (never wall-clock), interruptible: if the chunk reloads or the leaf is replaced mid-snap, the copy is dropped, not finished.
- Fast graphics (opaque leaves, `cutoutLeaves` false): the copy renders opaque like the mesh; chips unchanged.
- A decay in an unloaded or far chunk beyond render distance plays nothing.

### 5.3 Sound
- Vanilla leaf break sound per decay, but at most `sound_voices` (guess 6) per tick per level on the client, pitch jitter ±10%, volume falls with distance by vanilla attenuation. The rest of the wave is silent, not queued.
- Subtitles: the same cap, so a collapse shows a few "Leaves broken" lines, not a screen of them.
- Tests run muted: the feel check has a listening step (§ 13).

### 5.4 Config (live, no rebuild)
- `config/snappy_leaf_decay.json`, re-read within about 2 s (SnappyBlocks `Config`/`Tune.DEFAULTS` pattern; `-Dsnappy_leaf_decay.tune.<key>` overrides in tests).
- Server keys: `enabled`, `delay`, `jitter`, `max_per_tick`, `wave_order` (`cut`, `top`, `random`). Client keys: `enabled`, `intensity` (50-150), `calm` (no tremble, shorter snap, one chip), `chips`, `sound`, `tremble`.
- Every guess number in this PRD is a key here or in `Tune.DEFAULTS`.

## 6. Controls
| Input | Context | Action |
|---|---|---|
| none | | No keys, no commands in 1.0. Settings only in Mod Menu (M4) and the JSON file. |

## 7. Multiplayer and security
- The server half is server-authoritative; it changes nothing a vanilla server wouldn't do, only sooner, and it does not read client input.
- No packets at all (build change 2026-11-22): the client reads the synced leaf state, so a vanilla client sees the fast decay and a modded client on a vanilla server still gets the pop.
- No claim or permission interplay: leaf decay is a world rule, not a player action; protected regions that cancel block changes see decay as vanilla random ticks would be (it uses the same removal call).
- Dedicated server with two clients (one with the mod, one vanilla) is an exit check at M3 (`dev/server/mp.py`).

## 8. Data and persistence
- Nothing is saved: the queue and all animation state are transient. Quitting mid-wave leaves some leaves at distance 7; vanilla random ticks finish them after the next load.
- Mod removed from a world: nothing is left behind, the world loads, leaves decay at vanilla speed again.
- Mod and config updates: the config has a `version` field and unknown keys are ignored; Minecraft updates need no datafixes (no stored stacks or blocks).

## 9. Compatibility
| Mod | Expected | Checked by | Milestone |
|---|---|---|---|
| Sodium | pop copies draw, no hook crash | scene `oak --addon sodium` plus descriptor-checked mixin plugin | M3 |
| Iris + Complementary | copies lit like the mesh, or chips only under shaders | stills with and without shaders; mean-brightness diff | M3 |
| Fair Weather | leaf sway on the mesh; the copy starts from the swayed pose or sways too, no jump at the first frame | stills at the snap's frame 0 vs 1, `wb img diff` | M3 |
| Snappy Blocks | one pop per decay, not two; Snappy Blocks owns player breaks, this mod owns decays | scene `oak` with both jars, count of pop copies per position | M1 (open question 1) |
| Timber, Enchanted Timber | the felled tree's leaves leave with the fall (no pop); leftover decaying leaves after the fall pop in the wave | scene `timber` with both jars | M3 |
| Veinminer | no effect on logs; leaf removal not ours | scene `veinminer` | M3 |
| Flashback | pops recorded and replayed; no extra entities in the replay | one replay re-shoot, frame diff | M3 |
| Other fast-decay mods | server half off, client pop still works | scene `aside` with a stub jar (`dev/compat/stubs.py`) | M3 |
| Fresh Animations, EMF/ETF, Punchy | no interaction expected | profile boot, no errors | M3 |

## 10. Performance
- Server: queue add O(1) on the leaf's own tick, one hash lookup at due time; worst tick for a 2,000-leaf tree at most 2 ms, mean under 0.3 ms (scene `perf`). `max_per_tick` bounds the worst case at any tree size.
- Client: at most 1 ms per frame with 500 simultaneous pops, 600 chips (scene `crowd`, compared with the mod disabled on the same seed). Chips pooled, copies reuse Snappy Blocks' draw path; no allocation per frame in the steady state (check allocation rate in `crowd`).
- Network: no extra packets.
- Worst case: `/fill` of 50,000 non-persistent leaves with no logs: the decay spreads over `ceil(50000 / 64)` ticks (about 40 s) with the tick time bounded; measured once in `perf`.

## 11. Technical design
- Template: `mods/SnappyBlocks` for the client engine (`pop/`, `burst/`, `Chip`, `Clock`, `Config`, `Compat`, `selftest/`); `tools/Studio/new_mod.py` for the Stonecutter 26.3 Fabric scaffold. How Snappy Doors takes the engine from Snappy Blocks: read `mods/SnappyDoors/PRD.md` § 11 at M0 and copy that method.
- Packages: `server/` (`DecayQueue`, `Waves`, `Warn`), `client/` (`Decays`, `Tremble`, `Snap`, `Chips`), `net/` (`Hello` C2S, `DecayWarn` S2C), `mixin/` (`LeavesBlockMixin`, client `ClientLevelMixin`, renderer hooks as Snappy Blocks), `selftest/`.
- Server mixin: `LeavesBlock.tick` TAIL (after the distance update, vanilla `tick` at LeavesBlock.java:76) enqueues a leaf whose new state is decaying and whose old state was not. Vanilla `randomTick`/`decaying` stay untouched and remain the fallback.
- Vanilla paths checked 2026-11-22 (26.3 source): `LeavesBlock.isRandomlyTicking` (`DISTANCE == 7 && !PERSISTENT`), `randomTick` (drop then `removeBlock`), `updateShape` (schedules a tick with delay 1 on distance change). Not yet verified in game: the order in which leaves reach distance 7 after a cut (M1).
- Client: block changes via the same `ClientLevelMixin` hook Snappy Blocks uses; the last known state before removal decides decay vs break. Render by the copy path Snappy Blocks' break burst uses (own copy, mesh cell hidden for the snap).
- Loaders and versions: Fabric API on 26.3. No NeoForge/Forge-specific code in 1.0; keep `platform/` as Snappy Blocks does so ports stay mechanical.
- Sodium/Iris hooks need descriptor-checked mixin plugins (`MixinPlugin` pattern; `require = 0` is not enough).

## 12. Risks and spikes
| Risk | Spike | Milestone | Fallback |
|---|---|---|---|
| Vanilla's distance cascade is not a clean outward wave | Log first and last decay tick per leaf over an oak, dump tick vs distance from the cut | M1 | Explicit order from the removed log (`wave_order`) |
| Client cannot tell decay from break on a vanilla server | Scene asserts: 200 decays, 200 player breaks, 200 Timber removals classified | M1 | Treat every removal of a decaying leaf as ours; skip tremble |
| Chips and copies too costly at 500 leaves | `crowd` frame time, allocation rate | M1 | Lean: copies only, no chips, tighter LOD |
| Pop copy lit differently under shaders (Snappy Blocks found +4 of 125 under Complementary) | Stills mean-brightness | M3 | Chips only under a shader pack |
| Fair Weather sway makes the copy jump at frame 0 | Frame 0 vs 1 diff | M3 | Copy applies the same sway offset, or no copy under Fair Weather |
| Two mods pop the same leaf | Count copies per position with Snappy Blocks | M1 | Small stand-aside hook in Snappy Blocks (version bump there) |
| Item and sound spam from a big collapse | `loot` and `crowd` counts of item entities and voices | M1 | Caps already in § 5.3; item merge radius unchanged |
| Pasted builds (Litematica, `/fill`) with non-persistent leaves vanish in seconds | `perf` scene plus the description text | M2 | `enabled` per level (open question 2) |

## 13. Milestones
Each milestone: build green, version bump, installed in the Fabric 26.3 profile, committed locally. About eight at most; this is seven.

| | Milestone | Exit check |
|---|---|---|
| M0 | Scaffold | builds; `mt.py SnappyLeafDecay quick` boots; `studio.py start` took the slot; CurseForge slug and competitor download counts recorded in § 17 |
| M1 | Spike + PoC clip: server queue, minimal client snap and chips on one oak | Stop/go below; scene `oak` asserts: all leaves gone 1.0-2.0 s after the last log, no log or persistent leaf touched; scene `classify` 100% on 600 removals; scene `perf` worst tick ≤ 2 ms at 2,000 leaves; scene `crowd` ≤ 1 ms/frame at 500 pops; `wb img flicker` clean; one 60 fps clip of the oak collapse |
| M1b | Feel check | The director's verdict via `studio.py verdict` on script below; tunables become config values |
| M2 | Polish: tremble packet, chip art per leaf type and tint, tremble curve, sound caps, `calm` | `contact sheet` of 6 leaf types through `mod-assets` checks and `critic` PASS; scene `loot` and `stale` pass; muted-run sound counts ≤ 6 per tick |
| M3 | Compat and servers | Every row of § 9 checked; `dev/server/mp.py` PASS with one mod client and one vanilla client, nobody kicked, vanilla client sees decay within the pillar time |
| M4 | Settings screen + release prep | Mod Menu screen toggles each key live; release lane: full tier, matrix, mp, flicker, stills |
| M5 | Release (the director says release) | `release` skill gate: all § 4 pillar scenes green on the release jar, CurseForge file listed with 26.3 + Fabric tags, live page checked after upload |

**M1 stop/go rule (written before the result):** GO if the oak asserts, `classify`, `perf` and `crowd` pass and the clip reads as one wave with no flicker. SWITCH to Lean (copies only, no chips) if `crowd` passes only without chips. STOP if no wave order (cascade or explicit) gives a readable 1-2 s collapse at 20 ticks per second, or if decay and break cannot be told apart on a vanilla server.

**Feel check (M1b, about 3 minutes, `studio.py play SnappyLeafDecay`, any world with trees):**
1. Fell an oak with an axe and step back: does the wave read as one motion, or as scattered pops? Too slow, too fast?
2. Look at one leaf up close during the wave: is the snap crisp (hard stop, no rubber)? Do the chips look like the leaf's own pixels?
3. Fell a jungle giant and a dark oak: is the total time still good, any stutter in frame rate?
4. Listen: is the rustle too loud or too busy? Turn volume up and down once.
5. Stand under a tree on a vanilla server (or with the server half off): does the pop alone feel right without the tremble?
Judge: wave speed and order, snap crispness, chip look, loudness. Tunables: `delay`, `jitter`, `wave_order`, `intensity`, `chips`, `sound`, `tremble` in `config/snappy_leaf_decay.json`, live.

## 14. Release
- CurseForge only (Modrinth withheld; do not resubmit). Gate 5 goes through the `release` skill: version and changelog, full tier, release art as 60 fps WebP, description kit (Ko-fi, BisectHosting, promo tiles), AI disclosure, one confirmation of every outward action.
- Page title carries the searched words ("Snappy Leaf Decay"); description lead is the 5-second shot as a framed 60 fps WebP; the recipe-image rule does not apply (no crafting).
- Nothing is published, pushed or uploaded by this plan.

## 15. Roadmap
- 1.0: Fabric 26.3, `LeavesBlock` leaves.
- 1.1: ports through Stonecutter, Snappy Blocks' matrix as reference (26.2, then NeoForge/Forge, then 1.21.x); modded non-`LeavesBlock` leaves via a block tag.
- 1.2: Bold variant elements: leaves flutter down on Fair Weather's wind with a canopy hold-then-release collapse; only if the director asks after 1.0.

## 16. Cut
- Flutter on wind and falling leaf entities (Bold, to 1.2); would add entities and a wind dependency.
- Modded leaves outside `LeavesBlock`, saplings pile control, per-biome chip colours beyond vanilla tint, per-tree-type tuning.
- Instant decay: loses the moment and the readable wave.
- Any keybinding, command or GUI beyond Mod Menu.
- A Snappy Blocks module: decided against (§ 3).

## 17. Prior art
Checked 2026-11-22 (web search; download counts and loaders not yet read, M0 task):
- Leaves Be Gone (CurseForge): removes leaves about a second after the logs, tag-based, vanilla clients OK.
- Fast Leaf Decay (Forge): about 5 s default, no client needed. Leaves Us In Peace (Fabric/Quilt, server-only): configurable, optional break sound and particles on decay.
- Accelerated Decay: removes leaves as soon as possible instead of speeding ticks. ShatterLeaves: 1 tick delay, gamerule-adjustable.
- CanopyFade: about 2 s progressive fade, vanilla clients OK. Leaf Decay Instant: optional cascading chain mode. Ripple Leaf Decay: smooth outward ripple, Fabric 26.1 only.
- Vanilla: random tick decay, about a minute per leaf; no animation.
- Name and slug: `snappy-leaf-decay` unchecked; CurseForge bans "Mod", "Vanilla", "Minecraft" in names.
- Author's own: Snappy Blocks owns player breaks and the chip look; Timber fells whole trees and leaves its own leftovers; Fair Weather sways leaves.

## 18. Coverage
| Dimension | Answered in |
|---|---|
| Sides | § 1, § 5, § 7 |
| Multiplayer | § 7, M3 exit check |
| Persistence | § 8 |
| Data safety | § 4 pillar 4, § 5.1, § 12 |
| Uninstall | § 8 |
| Updates | § 8 |
| Config | § 5.4 |
| Performance | § 4, § 10 |
| Compatibility | § 9 |
| Versions | § 1, § 15 |
| Controls | § 6 (none) |
| Discoverability | N/A: it happens on its own; settings in Mod Menu (M4) |
| Accessibility | `calm` mode (no tremble, shorter snap, one chip); subtitle cap § 5.3; no colour-only cue (the tremble is motion) |
| Localisation | N/A in 1.0 except settings strings in a lang file (M4) |
| Game modes | Creative, adventure: unchanged (decay is world-driven); spectator: nothing to see; hardcore and peaceful: no effect |
| Dimensions | Any dimension with `LeavesBlock` leaves; Nether and End have none vanilla; `randomTickSpeed` 0 stands aside |
| Command bypass | `/setblock` and `/fill` of non-persistent leaves follow the same queue, bounded by `max_per_tick` (§ 10); Litematica pastes decay like vanilla, only faster (§ 12) |

## 19. Open questions
1. Does Snappy Blocks already pop leaves removed by decay? Default: this mod owns decay, Snappy Blocks owns player breaks; settle by the M1 two-jar count.
2. Should pasted or decorative non-persistent leaves be protected? Default: no, same as vanilla but faster; revisit if the feel check or a user note says so.
3. Wave order: from the cut outward, top-down, or random? Default `cut`; the director decides at the feel check.
4. Answered by the build: no tremble packet; the shed chip is the anticipation (see 2b). Real block tremble: 1.1.
