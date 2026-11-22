-- Popping Leaves logo: the Timber leaf cube in three leaf colours (oak green, spring yellow-green, autumn gold), each with
-- its own chip sheet. Timber's leaf textures are 5-step slices of one 7-step ramp (top = steps 3..7, left 2..6, right 1..5);
-- every variant re-maps the same slices onto its own 7-step ramp.
dofile("/home/emppu/Projects/Minecraft Datapacks/.claude/skills/pack-icon-animation/assets/pixel_art.lua")
local SRC = "/home/emppu/Projects/Minecraft Datapacks/mods/SnappyLeafDecay/release-art/src/sprites/"
local OUT = SRC .. "logo/"
local MASTER = { "#0F3325", "#17492D", "#236B33", "#378F38", "#5BB23E", "#8ED14E", "#C3EA6C" }
local RAMPS = {
  oak = MASTER,
  birch = { "#2B4A1C", "#3E6420", "#5A8527", "#7FA830", "#A6C73E", "#C9DE5A", "#E8F28E" },
  autumn = { "#4A1F14", "#6E2F18", "#A04A1C", "#CC6E22", "#E8932E", "#F7B84A", "#FFE08A" },
}
local SLICE = { top = 2, left = 1, right = 0 }
for v, ramp in pairs(RAMPS) do
  for face, k in pairs(SLICE) do
    local map = {}
    for i = 1, 5 do map[MASTER[k + i]] = ramp[k + i] end
    PA.remap(SRC .. "leaves_" .. face .. ".png", OUT .. v .. "_" .. face, map)
  end
  local function tile(a, b, c) return { ramp[a], ramp[b], ramp[c] } end
  local tiles = { tile(7, 6, 4), tile(6, 5, 3), tile(7, 6, 4), tile(5, 4, 3), tile(6, 5, 3), tile(7, 6, 4), tile(5, 4, 3), tile(6, 5, 3) }
  local px = {}
  for t, ti in ipairs(tiles) do
    local tx, ty = ((t - 1) % 4) * 4, ((t - 1) // 4) * 4
    for y = 0, 3 do for x = 0, 3 do
      local c = ti[2]
      if x == 0 or y == 0 then c = ti[1] end
      if x == 3 or y == 3 then c = ti[3] end
      if (x == 0 and y == 3) or (x == 3 and y == 0) then c = ti[2] end
      px[PA.key(tx + x, ty + y)] = c
    end end
  end
  PA.save_pixels(OUT .. "chips_" .. v, 16, 16, px)
end
print("ok")
