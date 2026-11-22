#!/usr/bin/env python3
"""Stand-in mods for the `aside` test addon (PRD 9): jars with only a fabric.mod.json, carrying the ids of mods that
already speed leaf decay up, so the server half must stay off. Writes dev/compat/mods/*.jar (git-ignored)."""
import json
import pathlib
import zipfile

OUT = pathlib.Path(__file__).resolve().parent / "mods"
STUBS = [("leavesbegone", "Leaves Be Gone"), ("fastleafdecay", "Fast Leaf Decay")]

OUT.mkdir(exist_ok=True)
for old in OUT.glob("*.jar"):
    old.unlink()
for mod_id, name in STUBS:
    meta = {"schemaVersion": 1, "id": mod_id, "version": "0.0.0", "name": name, "environment": "*",
            "description": "Snappy Leaf Decay test stand-in: metadata only, no code."}
    jar = OUT / f"{mod_id}-0.0.0.jar"
    with zipfile.ZipFile(jar, "w") as z:
        z.writestr("fabric.mod.json", json.dumps(meta, indent=2))
    print(jar.name)
