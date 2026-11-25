![Popping Leaves](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/banner.webp)

<p align="center">
<a href="https://github.com/ProfetGit"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/buttons/github.webp" alt="GitHub" width="23.96%"></a>
<a href="https://ko-fi.com/profetgit"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/buttons/kofi.webp" alt="Ko-fi" width="23.96%"></a>
</p>

**Tree down. Leaves pop.** Cut a tree and its leaves follow in one fast wave: each leaf snaps out and sprays a few pixel chips, instead of lingering for a minute and vanishing one by one. A mod for Minecraft Java 26.3, 26.2, 1.21.11, 1.21.1 and 1.20.1 on Fabric, Quilt, Forge and NeoForge, with a server half and a client half; either one works alone.

![Features](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/desc/title-features.webp)

- **Fast ripple decay (server).** Leaves that lost their tree go within a second or two, starting at the cut and spreading outward.
- **The pop (client).** A decaying leaf lets go of a chip, then squashes and shrinks away with a short spray of solid pixels cut from its own texture, in its biome colour, with a soft rustle.
- **Vanilla rules.** Same drops (saplings, sticks, apples), and leaves you placed never decay.
- **Vanilla client, modded server.** The decay is fast, players without the mod just see the leaves vanish.
- **Modded client, vanilla server.** Ordinary decay gets the pop.
- **Big trees.** Capped per tick, so a jungle giant clears without a lag spike.

![Four Popping Leaves clips: an oak, a modded pink maple from Biomes O' Plenty, a plain oak and a giant spruce, each crown popping away in a wave](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/clips/showcase.webp)

![How to use](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/desc/title-how-to-use.webp)

Install it and fell a tree. Nothing to press and nothing to learn.

![Settings](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/desc/title-settings.webp)

Open the settings screen from Mod Menu (optional). It has a master switch for the pop, an intensity from 50 to 150%, **Reduced motion** (smaller movement, one chip per leaf), a rustle sound, the shed chip, and a switch for the fast decay of the world's server. The same settings live in `config/snappy_leaf_decay.json`, which also holds the server numbers (first delay, ripple speed, decays per tick) and every pop timing. Edits are picked up within about two seconds, no restart.

![Compatibility](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/desc/title-compatibility.webp)

- Works with Sodium and Iris shader packs (tested with Complementary), Fair Weather, Timber and Veinminer on 26.3. On the older versions only vanilla rendering is tested so far.
- Cut by a tree feller or by hand, the leaves follow the same way.
- Modded trees work out of the box if their leaves are built on vanilla's leaf block. Tested with Biomes O' Plenty on Fabric 26.3: 19 of 20 leaf types decay fast, Null Leaves keep their normal speed.
- Other mods that already speed up leaf decay make the server half stand aside, so nothing is doubled.
- With `randomTickSpeed` 0 the server half stands aside too, as vanilla decay would not run either.

![Installation](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/desc/title-installation.webp)

Put the jar for your Minecraft version and loader in your `mods` folder. Fabric and Quilt use the Fabric jar and need no other mod (Fabric API is not required); on 1.20.1 NeoForge runs the Forge jar. Install it on the server for the fast decay, on the client for the pop, or on both.

![More from Profet](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/desc/title-more-from-profet.webp)

<!-- promo:start -->
<p align="center">
<a href="https://www.curseforge.com/minecraft/mc-mods/travelers-lantern"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/promo/travelers-lantern.webp" alt="Traveler's Lantern: Your light. Hands free. Client or server." width="49%"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/far-out-zoom"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/promo/far-out-zoom.webp" alt="Far Out Zoom: The horizon, up close. Client side." width="49%"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/tidy-pockets"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/promo/tidy-pockets.webp" alt="Tidy Pockets: One click. All sorted. Client side." width="49%"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/having-a-blast"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/promo/having-a-blast.webp" alt="Having a Blast: One boom. Bouncy blocks. Client or server." width="49%"></a>
</p>
<!-- promo:end -->

![Support](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/desc/title-support.webp)

Popping Leaves is free. If it makes felling trees more fun, a coffee helps fund the next update.

[![Support me on Ko-fi](https://raw.githubusercontent.com/ProfetGit/assets/main/kofi-banner.webp)](https://ko-fi.com/profetgit)

Want your own server to play on with friends? My BisectHosting affiliate link gives you 25% off the first month, and I get a small commission.

[![Get 25% off your first month at BisectHosting](https://raw.githubusercontent.com/ProfetGit/assets/main/bisecthosting-banner.webp)](https://url-shortener.curseforge.com/Pp2BN)

![License](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/desc/title-license.webp)

All rights reserved; see [LICENSE](https://github.com/ProfetGit/popping-leaves/blob/main/LICENSE).

Made with AI assistance (code, assets, text). Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

![](https://raw.githubusercontent.com/ProfetGit/popping-leaves/main/docs/desc/divider.webp)
