-- Popping Leaves release art: 16x16 textures drawn from scratch (hue-shifted ramps, light from the top left).
-- Run through the aseprite MCP:  dofile(".../release-art/src/draw_textures.lua")
dofile("/home/emppu/Projects/Minecraft Datapacks/.claude/skills/pack-icon-animation/assets/pixel_art.lua")
local OUT = "/home/emppu/Projects/Minecraft Datapacks/mods/SnappyLeafDecay/release-art/src/sprites/"

-- ramps, index 1 = darkest
local LEAF = { a = "#17402B", b = "#25602F", c = "#3B8236", d = "#5BA43C", e = "#85C446", f = "#B5E25F" }
local BARK = { a = "#2B1E12", b = "#45301C", c = "#5F4426", d = "#7E5C33", e = "#A07B45", f = "#C29A5C" }
local ORDER = "abcdef"

local function grid(fill) local g = {} for y = 0, 15 do g[y] = {} for x = 0, 15 do g[y][x] = fill end end return g end
local function stamp(g, s, ox, oy)
  for r = 1, #s do for c = 1, #s[r] do
    local ch = s[r]:sub(c, c)
    if ch ~= "." then g[(oy + r - 1) % 16][(ox + c - 1) % 16] = ch end
  end end
end
local function step(ch, n)
  local i = ORDER:find(ch, 1, true) + n
  if i < 1 then i = 1 elseif i > #ORDER then i = #ORDER end
  return ORDER:sub(i, i)
end
local function rows_of(g)
  local rows = {}
  for y = 0, 15 do
    local r = {}
    for x = 0, 15 do r[x + 1] = g[y][x] end
    rows[y + 1] = table.concat(r)
  end
  return rows
end
-- 1 px bevel: light top and left edge, dark bottom and right edge, so equal cubes read apart in the icon
local function bevel(g)
  for i = 0, 15 do
    g[0][i] = step(g[0][i], 1); g[i][0] = step(g[i][0], 1)
    g[15][i] = step(g[15][i], -1); g[i][15] = step(g[i][15], -1)
  end
  g[0][15] = step(g[0][15], 0); g[15][0] = step(g[15][0], 0)
end
local function save(name, g, legend) PA.sprite_from_grid(OUT .. name, rows_of(g), legend) end

-- ---------------------------------------------------------------- leaves: clumps of leaves, lit top left, dark gaps
local C1 = { ".eed..", "efdddc", "edddcc", ".dccb.", "..bb.." }
local C2 = { "..fed..", ".feddc.", "fedddcc", "edddccb", ".dcccb.", "..bbb.." }
local C3 = { ".ed..", "edddc", ".dccb", "..bb." }
local leaves = grid("b")
for _, s in ipairs({
  { C2, 1, 0 }, { C1, 9, 1 }, { C3, 5, 3 }, { C2, 10, 5 }, { C1, 0, 6 }, { C3, 6, 8 }, { C2, 2, 10 }, { C1, 10, 11 }, { C3, 13, 2 }, { C3, 0, 13 },
}) do stamp(leaves, s[1], s[2], s[3]) end
-- dark pockets in the gaps (down-right of every clump), a few bright specks
local pocket = {}
for y = 0, 15 do for x = 0, 15 do
  if leaves[y][x] == "b" and (leaves[(y - 1) % 16][(x - 1) % 16] ~= "b" or leaves[(y - 1) % 16][x] ~= "b" and leaves[y][(x - 1) % 16] ~= "b") then pocket[#pocket + 1] = { x, y } end
end end
for _, p in ipairs(pocket) do leaves[p[2]][p[1]] = "a" end
bevel(leaves)
local LL = {} for k, v in pairs(LEAF) do LL[k] = v end
save("leaves_top", leaves, LL)
local function shade(g, n)
  local o = grid("a")
  for y = 0, 15 do for x = 0, 15 do o[y][x] = step(g[y][x], n) end end
  return o
end
-- sides: the same texture, one and two steps down the ramp (the floor of the ramp is a darker tone)
local LEAF_DEEP = { a = "#0F2A1F", b = "#17402B", c = "#25602F", d = "#3B8236", e = "#5BA43C", f = "#85C446" }
local LEAF_DEEPER = { a = "#0A1C16", b = "#0F2A1F", c = "#17402B", d = "#25602F", e = "#3B8236", f = "#5BA43C" }
save("leaves_left", leaves, LEAF_DEEP)
save("leaves_right", leaves, LEAF_DEEPER)

-- ---------------------------------------------------------------- oak log: bark sides, ring top
local bark = grid("c")
local colbase = { "b", "c", "d", "d", "c", "b", "c", "d", "e", "d", "c", "b", "b", "c", "d", "c" }
for x = 0, 15 do for y = 0, 15 do bark[y][x] = colbase[x + 1] end end
local marks = { { 1, 3 }, { 1, 4 }, { 5, 9 }, { 5, 10 }, { 6, 1 }, { 11, 6 }, { 11, 7 }, { 11, 8 }, { 14, 12 }, { 14, 13 }, { 3, 13 }, { 8, 5 }, { 8, 6 }, { 2, 0 }, { 12, 14 } }
for _, m in ipairs(marks) do bark[m[2]][m[1]] = step(bark[m[2]][m[1]], -2) end
local lights = { { 3, 2 }, { 3, 3 }, { 9, 8 }, { 9, 9 }, { 9, 10 }, { 14, 4 }, { 14, 5 }, { 6, 12 }, { 6, 13 }, { 6, 14 }, { 12, 10 } }
for _, m in ipairs(lights) do bark[m[2]][m[1]] = step(bark[m[2]][m[1]], 1) end
bevel(bark)
save("log_side_top", bark, BARK)  -- the lit bark (used for the up-facing neighbour only if needed)
local BARK_L = { a = "#1E140B", b = "#2B1E12", c = "#45301C", d = "#5F4426", e = "#7E5C33", f = "#A07B45" }
local BARK_R = { a = "#150E07", b = "#1E140B", c = "#2B1E12", d = "#45301C", e = "#5F4426", f = "#7E5C33" }
save("log_left", bark, BARK_L)
save("log_right", bark, BARK_R)
local top = grid("e")
for y = 0, 15 do for x = 0, 15 do
  local d = math.max(math.abs(x - 7.5), math.abs(y - 7.5)) + 0.35 * (math.abs(x - 7.5) + math.abs(y - 7.5)) / 2
  local ch
  if d >= 8.2 then ch = "c" elseif d >= 7.3 then ch = "b" elseif d >= 6.2 then ch = "d"
  elseif d >= 5.2 then ch = "f" elseif d >= 4.3 then ch = "e" elseif d >= 3.4 then ch = "d"
  elseif d >= 2.4 then ch = "e" elseif d >= 1.4 then ch = "d" else ch = "c" end
  top[y][x] = ch
end end
for _, c in ipairs({ { 0, 0 }, { 15, 0 }, { 0, 15 }, { 15, 15 } }) do top[c[2]][c[1]] = "b" end
bevel(top)
save("log_top", top, BARK)

-- ---------------------------------------------------------------- items
PA.sprite_from_grid(OUT .. "axe_item", {
  "................",
  "....rrrrrr......",
  "....rwwlhhrr....",
  "....rwllhhhsr...",
  "....rwllhhhhsr..",
  "....rwlhhhsrkmnr",
  ".....rwlhhrkmnr.",
  "......rrrrkmnr..",
  "........rkmnr...",
  ".......rkmnr....",
  "......rkmnr.....",
  ".....rkmnr......",
  "....rkmnr.......",
  "...rkmnr........",
  "..rkmnr.........",
  "..rrrr..........",
}, { r = "#2C3142", w = "#EEF1F7", l = "#BAC2D4", h = "#8089A3", s = "#5A617C", k = "#3A2412", m = "#6E4A26", n = "#946A38" })

PA.sprite_from_grid(OUT .. "apple_item", {
  "................",
  ".......k.gg.....",
  "......k.ggg.....",
  ".....rrkrr......",
  "...rrmmrmmrr....",
  "..rmwwmmmmmmr...",
  "..rmwmmmmmmmsr..",
  "..rmmmmmmmmmsr..",
  "..rmmmmmmmmssr..",
  "..rmmmmmmmmssr..",
  "..rsmmmmmmmssr..",
  "...rsmmmmmssr...",
  "....rsssmssr....",
  ".....rrrrrr.....",
  "................",
  "................",
}, { r = "#4A1020", m = "#D4343A", w = "#FF8A7A", s = "#962235", k = "#4A2F18", g = "#5BA43C" })

PA.sprite_from_grid(OUT .. "sapling_item", {
  "................",
  ".......hh.......",
  "......hggh......",
  "..hh..hgfg..hh..",
  ".hggh.hggg.hggh.",
  ".hgfggh.g.hgfgh.",
  "..hggggg.gggggh.",
  "...hhggg.gghhh..",
  ".....hggggh.....",
  "......hgggh.....",
  ".......kg.......",
  ".......kg.......",
  ".......kd.......",
  ".......kd.......",
  "......kdd.......",
  "................",
}, { h = "#236B33", g = "#5BB23E", f = "#C3EA6C", k = "#43251A", d = "#85522E" })

-- ---------------------------------------------------------------- chips: 16x16 sheet = 4x4 tiles of 4x4 px, lit top left, shaded bottom right
local function chip_sheet(name, tiles)
  local px = {}
  for t, tile in ipairs(tiles) do
    local tx, ty = ((t - 1) % 4) * 4, ((t - 1) // 4) * 4
    for y = 0, 3 do for x = 0, 3 do
      local c = tile[2]
      if x == 0 or y == 0 then c = tile[1] end
      if x == 3 or y == 3 then c = tile[3] end
      if (x == 0 and y == 3) or (x == 3 and y == 0) then c = tile[2] end
      px[PA.key(tx + x, ty + y)] = c
    end end
  end
  PA.save_pixels(OUT .. name, 16, 16, px)
end
local G1 = { "#C3EA6C", "#8ED14E", "#378F38" }
local G2 = { "#8ED14E", "#5BB23E", "#236B33" }
local G3 = { "#5BB23E", "#378F38", "#236B33" }
chip_sheet("chips_leaf", { G1, G2, G1, G3, G2, G1, G3, G2 })
local B1 = { "#E2B66A", "#C8944F", "#85522E" }
local B2 = { "#C8944F", "#A8703C", "#633922" }
local B3 = { "#A8703C", "#85522E", "#43251A" }
chip_sheet("chips_bark", { B1, B2, B2, B3, B2, B1, B3, B2 })
print("ok")
