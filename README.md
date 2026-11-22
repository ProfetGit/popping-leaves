# Snappy Leaf Decay

Cut a tree and its leaves follow in one wave: each leaf snaps out and sprays a few pixel chips, instead of lingering for a minute and vanishing one by one.

- **Fast ripple decay (server).** Leaves that lost their tree go within about a second or two, starting at the cut and spreading outward. Same drops as vanilla (saplings, sticks, apples), same rules (leaves you placed never decay).
- **The pop (client).** A decaying leaf lets go of a chip, then squashes and shrinks away with a short spray of solid pixels cut from its own texture, in its biome colour, with a soft rustle.
- **Two halves, either works alone.** On a vanilla client the server half still makes the decay fast. On a vanilla server the client half still adds the pop to ordinary decay.

Minecraft 26.3, Fabric, Fabric API. Settings in Mod Menu or `config/snappy_leaf_decay.json` (live reload). Reduced motion mode available.

Compatible with Sodium and shader packs, Timber, Veinminer. Other fast leaf decay mods: the server half steps aside.

Made with AI assistance (code, assets, text).
