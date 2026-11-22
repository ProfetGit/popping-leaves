// Popping Leaves icon scene (Blockbench, free format). A mini tree (log + 4 leaf cubes): an axe chops the log, the log
// shrinks away in brown chips, then the leaves pop one after another in a ripple (green chips), saplings and an apple
// drop and bounce, get picked up, and the tree boings back. Loop LEN s at 60 fps, true isometric camera (pitch 30).
//   eval(readFileSync(this file)); window.PL = PL; PL.loadTextures()   // then, in a later call:
//   PL.build(); PL.animate(); PL.camera(); PL.render(0, PL.N - 1)
var PL = (function () {
  const fs = require('fs');
  const DIR = '/home/emppu/Projects/Minecraft Datapacks/mods/SnappyLeafDecay/release-art/src/';
  const TEX = DIR + 'sprites/';
  const FPS = 60, DT = 1 / FPS, LEN = 3.3, N = Math.round(LEN * FPS);
  const RAD = Math.PI / 180;
  const CAM_TARGET = [0, 26, 0], CAM_POS = [0, 90, 155.88], CAM_ZOOM = 0.8143;
  const PITCH = -Math.atan2(CAM_POS[1] - CAM_TARGET[1], CAM_POS[2] - CAM_TARGET[2]) * 180 / Math.PI;
  const O = new THREE.Vector3(...CAM_TARGET);
  const q = t => Math.round(t * FPS) / FPS;
  const worldToScreen = w => new THREE.Vector3(...w).sub(O).applyEuler(new THREE.Euler(-PITCH * RAD, 0, 0)).add(O);
  const veinToWorld = a => new THREE.Vector3(...a).applyEuler(new THREE.Euler(0, 45 * RAD, 0));
  const blockCentre = b => worldToScreen(veinToWorld([b[0] * 16, b[1] * 16 + 8, b[2] * 16]).toArray());
  const Z_AXE = 30, Z_ITEM = 40, Z_CHIP = 50;

  // block 0 is the log, the rest are leaves in ripple order
  const BLOCKS = [[0, 0, 0], [0, 1, 0], [-1, 1, -1], [1, 1, 1], [0, 2, 0]];
  const SHEET = ['chips_bark', 'chips_leaf', 'chips_leaf', 'chips_leaf', 'chips_leaf'];
  const NCHIP = [12, 10, 10, 10, 10];
  const T = {
    windStart: 0.30, cocked: 0.62, mid: 0.665, impact: 0.70, stopEnd: 0.78,
    break: [0.80, 1.02, 1.12, 1.17, 1.30],      // pop start per block (log, A, B, C, D)
    respawn: [2.42, 2.52, 2.59, 2.64, 2.74],
    pickup: [1.98, 2.14, 2.04],
  };
  const HOLD = 0.06, GONE = 0.30;

  const tex = {};
  function loadTextures() {
    Texture.all.slice().forEach(t => t.remove(true));
    const names = ['leaves_top', 'leaves_left', 'leaves_right', 'log_top', 'log_left', 'log_right', 'chips_leaf', 'chips_bark',
      'axe_item', 'apple_item', 'sapling_item'];
    for (const n of names) {
      const url = 'data:image/png;base64,' + fs.readFileSync(TEX + n + '.png').toString('base64');
      tex[n] = new Texture({ name: n + '.png' }).fromDataURL(url).add(false);
    }
    return names.join(',');
  }
  function ensureTex() {
    if (!Object.keys(tex).length) Texture.all.forEach(t => { tex[t.name.replace('.png', '')] = t; });
  }
  function pixels(name) {
    const img = tex[name].img;
    const cv = document.createElement('canvas');
    cv.width = img.naturalWidth; cv.height = img.naturalHeight;
    const ctx = cv.getContext('2d'); ctx.drawImage(img, 0, 0);
    const d = ctx.getImageData(0, 0, cv.width, cv.height).data, out = [];
    for (let y = 0; y < cv.height; y++) for (let x = 0; x < cv.width; x++) if (d[(y * cv.width + x) * 4 + 3] > 127) out.push([x, y]);
    return out;
  }
  function group(name, origin, parent, rotation) {
    const g = new Group({ name, origin, rotation: rotation || [0, 0, 0] });
    g.addTo(parent); g.init();
    return g;
  }
  const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];
  function cube(name, from, to, parent, faceTex) {
    const c = new Cube({ name, from, to, box_uv: false });
    c.addTo(parent); c.init();
    for (const f of FACES) {
      const spec = faceTex[f] || faceTex.all;
      if (spec) c.faces[f].extend({ texture: tex[spec[0]].uuid, uv: spec[1] });
      else c.faces[f].extend({ texture: null });
    }
    return c;
  }
  function extrude(prefix, texName, pivotPx, at, s, parent) {
    const k = 16 / tex[texName].img.naturalWidth;   // texture uv space is 16 units across whatever the pixel size
    for (const [x, y] of pixels(texName)) {
      const x0 = at[0] + (x - pivotPx[0]) * s, y0 = at[1] + (pivotPx[1] - y - 1) * s;
      cube(prefix, [x0, y0, at[2] - s / 2], [x0 + s, y0 + s, at[2] + s / 2], parent, { all: [texName, [x * k, y * k, (x + 1) * k, (y + 1) * k]] });
    }
  }
  function mulberry(a) {
    return function () { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; };
  }

  // ---- geometry ----
  const AXE_PX = 1.0, AXE_PIV = [10.5, 18.5], AXE_TIP = [1.5, 5.5];
  const ITEM_PX = 0.8, ITEM_PIV = [8, 15], ITEM_H = 13 * 0.8;
  const AXE = { rest: -8, cocked: -32, impact: 62 };
  const DROPS = [
    { tex: 'sapling_item', from: 1, spawn: 1.18, slot: -21, apex: 11, flight: 0.46 },
    { tex: 'apple_item', from: 4, spawn: 1.40, slot: -10, apex: 12, flight: 0.46 },
    { tex: 'sapling_item', from: 3, spawn: 1.30, slot: 7, apex: 10, flight: 0.44 },
  ];
  function rot2(v, deg) {
    const a = deg * RAD;
    return [v[0] * Math.cos(a) - v[1] * Math.sin(a), v[0] * Math.sin(a) + v[1] * Math.cos(a)];
  }
  function layout() {
    const target = worldToScreen(veinToWorld([1, 11, 8]).toArray());
    const tipOff = rot2([(AXE_TIP[0] - AXE_PIV[0]) * AXE_PX, (AXE_PIV[1] - AXE_TIP[1]) * AXE_PX], AXE.impact);
    const grip = { x: target.x - tipOff[0], y: target.y - tipOff[1] };
    const rowY = worldToScreen([0, 0, 0]).y - 1;
    return { grip, target, rowY, centres: BLOCKS.map(blockCentre) };
  }
  const CHIPS = [];
  BLOCKS.forEach((b, i) => {
    const r = mulberry(31 + i * 7);
    CHIPS[i] = [];
    for (let k = 0; k < NCHIP[i]; k++) {
      const ang = (20 + r() * 140) * Math.PI / 180, sp = 20 + r() * 24;
      CHIPS[i].push({ x0: (r() - 0.5) * 10, y0: (r() - 0.4) * 8, vx: Math.cos(ang) * sp * (r() < 0.5 ? -1 : 1), vy: Math.sin(ang) * sp * 1.1,
        size: [3.0, 2.6, 2.6, 2.2][Math.floor(r() * 4)], tile: Math.floor(r() * 8) });
    }
  });

  const G = {};
  function build() {
    ensureTex();
    Animation.all.slice().forEach(a => a.remove(false));
    Outliner.root.slice().forEach(n => n.remove(false));
    const L = layout();
    G.vein = group('vein', [0, 0, 0], undefined, [0, 45, 0]);
    BLOCKS.forEach((b, i) => {
      const [cx, cy, cz] = [b[0] * 16, b[1] * 16, b[2] * 16];
      const piv = i === 0 ? [cx, cy, cz] : [cx, cy + 8, cz];
      const g = G['blk_' + i] = group('blk_' + i, piv, G.vein);
      const m = i === 0 ? 'log' : 'leaves';
      cube('block_' + i, [cx - 8, cy, cz - 8], [cx + 8, cy + 16, cz + 8], g, {
        up: [m + '_top', [0, 0, 16, 16]], west: [m + '_left', [0, 0, 16, 16]], south: [m + '_right', [0, 0, 16, 16]],
        east: [m + '_left', [0, 0, 16, 16]], north: [m + '_right', [0, 0, 16, 16]], down: [m + '_right', [0, 0, 16, 16]],
      });
    });
    G.screen = group('screen', O.toArray(), undefined, [PITCH, 0, 0]);
    G.axe = group('axe', [L.grip.x, L.grip.y, Z_AXE], G.screen);
    extrude('axe', 'axe_item', AXE_PIV, [L.grip.x, L.grip.y, Z_AXE], AXE_PX, G.axe);
    BLOCKS.forEach((b, i) => CHIPS[i].forEach((c, k) => {
      const z = Z_CHIP + k * 0.1 + i * 2, h = 0.5;
      const g = G[`chip_${i}_${k}`] = group(`chip_${i}_${k}`, [0, O.y, z], G.screen);
      const tx = (c.tile % 4) * 4, ty = Math.floor(c.tile / 4) * 4;
      cube('chip', [-h, O.y - h, z], [h, O.y + h, z], g, { south: [SHEET[i], [tx, ty, tx + 4, ty + 4]] });
    }));
    DROPS.forEach((d, i) => {
      const it = G['item_' + i] = group('item_' + i, [d.slot, L.rowY, Z_ITEM + i], G.screen);
      const sp = G['spin_' + i] = group('spin_' + i, [d.slot, L.rowY + ITEM_H / 2, Z_ITEM + i], it);
      extrude('drop' + i, d.tex, ITEM_PIV, [d.slot, L.rowY, Z_ITEM + i], ITEM_PX, sp);
    });
    Canvas.updateAll();
    return Outliner.elements.length;
  }

  // ---- animation ----
  const EASE = { lin: u => u, in: u => u * u, out: u => 1 - (1 - u) * (1 - u), smooth: u => u * u * (3 - 2 * u) };
  function curve(keys, tt) {
    for (let i = 1; i < keys.length; i++) {
      const a = keys[i - 1], b = keys[i];
      if (tt <= b[0] + 1e-9) {
        const u = EASE[b[2]](Math.min(1, Math.max(0, (tt - a[0]) / (b[0] - a[0]))));
        return [a[1][0] + (b[1][0] - a[1][0]) * u, a[1][1] + (b[1][1] - a[1][1]) * u];
      }
    }
    return keys[keys.length - 1][1];
  }
  const BREAK = [[0, [1, 1], 'lin'], [HOLD, [1.14, 0.86], 'out'], [HOLD + 0.06, [1.14, 0.86], 'lin'], [GONE, [0, 0], 'in']];
  const BOING = [[0, [0, 0], 'lin'], [0.08, [0.8, 1.3], 'out'], [0.16, [1.25, 0.78], 'smooth'], [0.24, [0.92, 1.1], 'smooth'],
    [0.32, [1.04, 0.97], 'smooth'], [0.40, [1, 1], 'smooth']];
  function blockScale(i, t) {
    const tb = T.break[i], tr = T.respawn[i];
    if (t < tb) return [1, 1];
    if (t < tb + GONE) return curve(BREAK, t - tb);
    if (t < tr) return [0, 0];
    if (t < tr + 0.40) return curve(BOING, t - tr);
    return [1, 1];
  }
  // log gets a hit squash at the impact before it breaks; leaves tremble just before their pop
  function blockExtra(i, t) {
    if (i === 0) {
      const u = (t - T.impact) / 0.06;
      if (u >= 0 && u <= 1) return { s: [1 + 0.18 * Math.sin(u * Math.PI), 1 - 0.14 * Math.sin(u * Math.PI)], shake: 0 };
      return { s: [1, 1], shake: 0 };
    }
    const dt = T.break[i] - t;
    if (dt > 0 && dt <= 0.12) return { s: [1, 1], shake: 0.4 * (Math.floor(dt * FPS) % 2 ? 1 : -1) };
    return { s: [1, 1], shake: 0 };
  }
  const GR = 168;
  function chipState(c, age) {
    const life = 0.55;
    if (age < 0 || age > life) return null;
    const x = c.x0 + c.vx * age, y = c.y0 + c.vy * age - 0.5 * GR * age * age;
    let s = c.size;
    if (age > life - 0.10) s = c.size * 0.65;
    if (age > life - 0.05) s = 0;
    return s > 0 ? [x, y, s] : null;
  }
  let A = null;
  function K(g, ch, t, v) {
    const [x, y, z] = typeof v === 'number' ? [v, v, v] : v;
    A.getBoneAnimator(g).addKeyframe({ channel: ch, time: q(t), interpolation: 'linear', data_points: [{ x, y, z }] });
  }
  function ensureG() { if (!Object.keys(G).length) Group.all.forEach(g => { G[g.name] = g; }); }

  function axeRot(t) {
    const W = T.windStart, rr = AXE.rest;
    const keys = [[0, rr, 'lin'], [W, rr, 'lin'], [W + 0.10, rr - 5, 'smooth'], [T.cocked - 0.12, AXE.cocked + 8, 'smooth'], [T.cocked, AXE.cocked, 'smooth'],
      [T.mid, 16, 'lin'], [T.impact, AXE.impact, 'lin'], [T.stopEnd, AXE.impact, 'lin'],
      [T.stopEnd + 0.10, AXE.impact - 22, 'out'], [T.stopEnd + 0.20, AXE.impact - 14, 'smooth'], [T.stopEnd + 0.34, rr + 14, 'smooth'],
      [T.stopEnd + 0.46, rr - 5, 'smooth'], [T.stopEnd + 0.60, rr, 'smooth'], [LEN, rr, 'lin']];
    for (let i = 1; i < keys.length; i++) {
      const a = keys[i - 1], b = keys[i];
      if (t <= b[0] + 1e-9) return a[1] + (b[1] - a[1]) * EASE[b[2]](Math.min(1, Math.max(0, (t - a[0]) / (b[0] - a[0]))));
    }
    return rr;
  }
  function dropState(d, i, L, t) {
    const sx = L.centres[d.from].x, sy = L.centres[d.from].y + 13;   // start above the cube's top so the drop never sits inside it
    const tx = d.slot, ty = L.rowY;
    const age = t - d.spawn, F = d.flight;
    const pick = T.pickup[i];
    if (age < 0 || t > pick + 0.26) return null;
    let x, y, sq = [1, 1], rot = 0;
    if (age < F) {
      const dy = ty - sy, H = d.apex;
      const g = Math.pow((Math.sqrt(2 * H) + Math.sqrt(2 * Math.max(0.01, H - dy))) / F, 2), vy = Math.sqrt(2 * g * H);
      x = sx + (tx - sx) / F * age; y = sy + vy * age - g * age * age / 2;
      rot = (tx < sx ? 1 : -1) * 360 * (1 - age / F);
    } else {
      x = tx; y = ty;
      const a = age - F;
      if (a < 0.16) { const u = a / 0.16; y += 4 * 4 * u * (1 - u); sq = a < 0.04 ? [1.3, 0.7] : [1, 1 + 0.15 * (1 - u)]; }
      else if (a < 0.28) { const u = (a - 0.16) / 0.12; y += 4 * 1.2 * u * (1 - u); sq = a < 0.2 ? [1.15, 0.85] : [1, 1]; }
      if (a < 0.04) sq = [1.3 - 0.3 * (a / 0.04), 0.7 + 0.3 * (a / 0.04)];
    }
    const pa = t - pick;
    if (pa >= 0) {
      if (pa < 0.10) { const u = pa / 0.10; sq = [1 + 0.3 * u, 1 + 0.3 * u]; y += 3 * Math.sin(u * Math.PI / 2); }
      else { const u = Math.min(1, (pa - 0.10) / 0.16); const s = 1.3 * (1 - u); sq = [s, s]; y += 3; }
    }
    return { x, y, sq, rot };
  }
  function animate() {
    ensureTex(); ensureG();
    Animation.all.slice().forEach(a => a.remove(false));
    A = new Animation({ name: 'icon_loop', length: LEN, loop: 'loop', snapping: FPS }).add(false);
    A.select();
    const L = layout();
    const R0 = L.grip;
    const live = {};   // sparse keys for chips and drops: zero outside their lifetime
    for (let f = 0; f <= N; f++) {
      const t = f * DT;
      K(G.axe, 'rotation', t, [0, 0, axeRot(t)]);
      BLOCKS.forEach((b, i) => {
        const ex = blockExtra(i, t), sc = blockScale(i, t);
        K(G['blk_' + i], 'scale', t, [sc[0] * ex.s[0], sc[1] * ex.s[1], sc[0] * ex.s[0]]);
        K(G['blk_' + i], 'position', t, [ex.shake, 0, 0]);
        CHIPS[i].forEach((c, k) => {
          const st = chipState(c, t - (T.break[i] + HOLD));
          const g = G[`chip_${i}_${k}`];
          const key = `${i}_${k}`;
          const cen = L.centres[i];
          if (st) {
            if (!live[key] && f > 0) { K(g, 'scale', t - DT, 0); }
            live[key] = true;
            K(g, 'position', t, [cen.x + st[0], cen.y + st[1] - O.y, 0]); K(g, 'scale', t, st[2]);
          } else {
            if (live[key] || f === 0) { K(g, 'scale', t, 0); }
            live[key] = false;
          }
        });
      });
      DROPS.forEach((d, i) => {
        const st = dropState(d, i, L, t), it = G['item_' + i], sp = G['spin_' + i], key = 'd' + i;
        if (st) {
          if (!live[key]) K(it, 'scale', t - DT, 0);
          live[key] = true;
          K(it, 'position', t, [st.x - d.slot, st.y - L.rowY, 0]);
          K(it, 'scale', t, [st.sq[0], st.sq[1], st.sq[0]]);
          K(sp, 'rotation', t, [0, 0, st.rot]);
        } else {
          if (live[key] || f === 0) K(it, 'scale', t, 0);
          live[key] = false;
        }
      });
    }
    Animator.preview();
    return 'keys for ' + (N + 1) + ' frames';
  }

  // ---- camera + render ----
  function camera(zoom) {
    const p = Preview.selected;
    p.setProjectionMode(true);
    p.camera.position.set(...CAM_POS);
    p.controls.target.set(...CAM_TARGET);
    p.camera.lookAt(...CAM_TARGET);
    p.camera.zoom = zoom || CAM_ZOOM; p.camera.updateProjectionMatrix();
    p.controls.update();
  }
  function setTime(t) { Timeline.setTime(t); Animator.preview(); }
  function render(first, last, res, dir) {
    res = res || 1600;
    dir = dir || DIR + 'frames/';
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const shot = f => new Promise(done => {
      setTime(f * DT);
      Screencam.advancedScreenshot(Preview.selected, { angle_preset: 'view', resolution: [res, res], anti_aliasing: 'none', shading: false }, url => {
        fs.writeFileSync(dir + 'frame_' + String(f).padStart(3, '0') + '.png', Buffer.from(url.split(',')[1], 'base64'));
        done();
      });
    });
    return (async () => { for (let f = first; f <= last; f++) await shot(f); return `rendered ${first}..${last}`; })();
  }
  function scaleSweep() {
    const bad = [];
    for (let f = 0; f <= N; f++) {
      setTime(f * DT);
      Group.all.forEach(g => { const s = g.mesh.scale; if (s.x < 0 || s.y < 0 || s.z < 0) bad.push(g.name + '@' + f); });
    }
    return bad.length ? bad.join(',') : 'no negative scale';
  }
  return { FPS, DT, LEN, N, G, tex, T, loadTextures, build, animate, camera, render, setTime, scaleSweep, q, layout };
})();
