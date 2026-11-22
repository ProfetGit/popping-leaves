-- Popping Leaves release art: pixel lettering (banner title, tagline, chip) in the Snappy Blocks style.
dofile("/home/emppu/Projects/Minecraft Datapacks/.claude/skills/pack-icon-animation/assets/pixel_art.lua")
local OUT = "/home/emppu/Projects/Minecraft Datapacks/mods/SnappyLeafDecay/release-art/src/sprites/"
local key = PA.key
local INK = "#12263F"
local N8 = { { -1, -1 }, { 0, -1 }, { 1, -1 }, { -1, 0 }, { 1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } }
local function outline(px, colour)
  local solid = {}
  for k in pairs(px) do solid[k] = true end
  for k in pairs(solid) do
    local x, y = k % 4096, k // 4096
    for _, d in ipairs(N8) do
      local n = key(x + d[1], y + d[2])
      if not solid[n] then px[n] = colour end
    end
  end
end
local function title(path, text, style_of)
  local mask, right = PA.layout(text, PA.TITLE, 2, 4, 1, 1)
  local px, ext = {}, {}
  for k, v in pairs(mask) do px[k] = style_of(v[2]).bands[v[1]] end
  for k, v in pairs(mask) do
    local x, y = k % 4096, k // 4096
    local st = style_of(v[2])
    for d = 1, #st.extrude do
      local n = key(x, y + d)
      if mask[n] then break end
      if not ext[n] or ext[n].d > d then ext[n] = { d = d, col = st.extrude[d] } end
    end
  end
  for n, e in pairs(ext) do px[n] = e.col end
  outline(px, INK)
  PA.save_pixels(path, right + 1, 14, px)
end
local LIME = { bands = { "#E6FF9A", "#B5E25F", "#B5E25F", "#B5E25F", "#85C446", "#85C446", "#85C446", "#5BA43C", "#5BA43C", "#5BA43C" },
               extrude = { "#2E6E34", "#17402B" } }
local CREAM = { bands = { "#FFFBF0", "#FFF1D6", "#FFF1D6", "#FFF1D6", "#F5DDB0", "#F5DDB0", "#F5DDB0", "#E6C48E", "#E6C48E", "#E6C48E" },
                extrude = { "#A8744A", "#6E4730" } }
title(OUT .. "title_popping", "POPPING", function(i) return CREAM end)
title(OUT .. "title_leaves", "LEAVES", function(i) return LIME end)
title(OUT .. "title_both", "POPPING LEAVES", function(i) return i <= 7 and CREAM or LIME end)
PA.label_sprite(OUT .. "tagline", "TREE DOWN. LEAVES POP.", function(i) return i > 11 and "#B5E25F" or "#FFFFFF" end, INK)
PA.label_sprite(OUT .. "chip_client", "SERVER + CLIENT", function(i) return "#FFFFFF" end, INK)
print("ok")
