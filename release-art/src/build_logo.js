// Snappy Blocks icon scene (Blockbench, free format). Three of the author's own block textures (Having a Blast grass +
// stone, Veinminer gold ore) take turns on one spot: each pops in with an overshoot, holds, breaks into chips as the next
// one pops in. Loop = 3 x P seconds, no empty frame. Camera: true isometric (pitch 30 deg, yaw 45), like Minecraft's block icons.
//   eval(readFileSync(this file)); window.PLG = PLG; PLG.loadTextures()    // then, in a later call:
//   PLG.build(); PLG.animate(); PLG.camera(ZOOM)                            // then: PLG.render(0, 197)
var PLG = (function () {
  const fs = require('fs');
  const DIR = '/home/emppu/Projects/Minecraft Datapacks/mods/SnappyLeafDecay/release-art/src/';
  const TEX = DIR + 'sprites/logo/';
  const FPS = 60, DT = 1 / FPS, P = 1.8, NB = 1, LEN = P * NB;
  const HOLD_END = 1.20;                     // a block's break starts this long after its pop starts; gone at +0.30
  const MATS = ['oak'];
  const CHIPSHEET = { oak: 'chips_oak' };
  const CAM_TARGET = [0, 4, 0], CAM_POS = [0, 64, 103.92], CAM_ZOOM = 0.8143;
  const PITCH = -Math.atan2(CAM_POS[1] - CAM_TARGET[1], CAM_POS[2] - CAM_TARGET[2]) * 180 / Math.PI;
  const O = new THREE.Vector3(...CAM_TARGET);
  const q = t => Math.round(t * FPS) / FPS;
  const NCHIP = 14;

  const tex = {};
  function loadTextures() {
    Texture.all.slice().forEach(t => t.remove(true));
    const names = [];
    MATS.forEach(m => ['top', 'left', 'right'].forEach(s => names.push(m + '_' + s)));
    Object.values(CHIPSHEET).forEach(n => names.push(n));
    for (const n of names) {
      const url = 'data:image/png;base64,' + fs.readFileSync(TEX + n + '.png').toString('base64');
      tex[n] = new Texture({ name: n + '.png' }).fromDataURL(url).add(false);
    }
    return names.join(',');
  }
  function ensureTex() {
    if (!Object.keys(tex).length) Texture.all.forEach(t => { tex[t.name.replace('.png', '')] = t; });
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
      const spec = faceTex[f];
      if (spec) c.faces[f].extend({ texture: tex[spec[0]].uuid, uv: spec[1] });
      else c.faces[f].extend({ texture: null });
    }
    return c;
  }
  function mulberry(a) {
    return function () { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; };
  }
  const G = {};
  const CHIPS = [];   // CHIPS[b][k] = {x0, y0, vx, vy, size, tile, life, ground}
  MATS.forEach((m, b) => {
    const r = mulberry(11 + b * 7);
    CHIPS[b] = [];
    for (let k = 0; k < NCHIP; k++) {
      const ang = (15 + r() * 150) * Math.PI / 180, sp = 22 + r() * 26;
      CHIPS[b].push({ x0: (r() - 0.5) * 12, y0: 1 + r() * 6, vx: Math.cos(ang) * sp * 1.0 * (r() < 0.5 ? -1 : 1), vy: Math.sin(ang) * sp * 1.3,
        size: [3.0, 2.6, 2.6, 2.2][Math.floor(r() * 4)], tile: Math.floor(r() * 8), life: 0, ground: -10 + (r() - 0.5) * 2 });
      // keep every chip inside the icon window: |x| <= 13 at landing, apex <= 19 (window is x +-16, y -13..20 around the block)
      const c = CHIPS[b][k], GR = 168;
      const disc = c.vy * c.vy + 2 * GR * (c.y0 - c.ground);
      c.vy = Math.min(c.vy, Math.sqrt(2 * GR * (18 - c.y0)));
      const tc = (c.vy + Math.sqrt(c.vy * c.vy + 2 * GR * (c.y0 - c.ground))) / GR;
      const xf = c.x0 + c.vx * tc;
      if (Math.abs(xf) > 12.5) c.vx = (Math.sign(xf) * 12.5 - c.x0) / tc;
    }
  });

  function build() {
    ensureTex();
    Animation.all.slice().forEach(a => a.remove(false));
    Outliner.root.slice().forEach(n => n.remove(false));
    MATS.forEach((m, b) => {
      const g = G['blk_' + m] = group('blk_' + m, [0, 0, 0], undefined, [0, 45, 0]);
      cube('cube_' + m, [-8, 0, -8], [8, 16, 8], g, {
        up: [m + '_top', [0, 0, 16, 16]], west: [m + '_left', [0, 0, 16, 16]], south: [m + '_right', [0, 0, 16, 16]],
        east: [m + '_left', [0, 0, 16, 16]], north: [m + '_right', [0, 0, 16, 16]], down: [m + '_right', [0, 0, 16, 16]],
      });
    });
    G.screen = group('screen', O.toArray(), undefined, [PITCH, 0, 0]);
    MATS.forEach((m, b) => CHIPS[b].forEach((c, k) => {
      const z = 40 + k * 0.1 + b * 2, h = 0.5;
      const ty0 = CAM_TARGET[1];   // group origins are absolute: pivot each chip at the screen group's origin O
      const g = G[`chip_${b}_${k}`] = group(`chip_${b}_${k}`, [0, ty0, z], G.screen);
      const tx = (c.tile % 4) * 4, ty = Math.floor(c.tile / 4) * 4;
      cube('chip', [-h, ty0 - h, z], [h, ty0 + h, z], g, { south: [CHIPSHEET[m], [tx, ty, tx + 4, ty + 4]] });
      g.__size = c.size;
    }));
    Canvas.updateAll();
    return Outliner.elements.length;
  }

  // ---- animation ----
  const EASE = { lin: u => u, in: u => u * u, out: u => 1 - (1 - u) * (1 - u), smooth: u => u * u * (3 - 2 * u) };
  const KEYS = [[0, [0.62, 0.62], 'lin'], [0.16, [1.09, 1.09], 'out'], [0.27, [0.975, 0.975], 'smooth'], [0.38, [1.015, 1.015], 'smooth'],
    [0.48, [1, 1], 'smooth'], [HOLD_END, [1, 1], 'lin'], [HOLD_END + 0.06, [1.14, 0.86], 'out'], [HOLD_END + 0.12, [1.14, 0.86], 'lin'],
    [HOLD_END + 0.30, [0, 0], 'in']];
  function blockScale(tt) {
    if (tt < -1e-6 || tt > HOLD_END + 0.30 + 1e-6) return [0, 0];
    for (let i = 1; i < KEYS.length; i++) {
      const a = KEYS[i - 1], b = KEYS[i];
      if (tt <= b[0] + 1e-9) {
        const u = EASE[b[2]](Math.min(1, Math.max(0, (tt - a[0]) / (b[0] - a[0]))));
        return [a[1][0] + (b[1][0] - a[1][0]) * u, a[1][1] + (b[1][1] - a[1][1]) * u];
      }
    }
    return [0, 0];
  }
  function chipState(c, age) {
    const GR = 168;
    const disc = c.vy * c.vy + 2 * GR * (c.y0 - c.ground);
    const tc = disc > 0 ? (c.vy + Math.sqrt(disc)) / GR : 0;
    const life = tc + 0.12;
    if (age < 0 || age > life) return null;
    const a = Math.min(age, tc);
    const x = c.x0 + c.vx * a, y = age >= tc ? c.ground : c.y0 + c.vy * a - 0.5 * GR * a * a;
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
  function animate() {
    ensureTex(); ensureG();
    Animation.all.slice().forEach(a => a.remove(false));
    A = new Animation({ name: 'icon_loop', length: LEN, loop: 'loop', snapping: FPS }).add(false);
    A.select();
    const n = Math.round(LEN * FPS);
    for (let f = 0; f <= n; f++) {
      const t = f * DT;
      MATS.forEach((m, b) => {
        const [sx, sy] = blockScale(t - b * P);
        K(G['blk_' + m], 'scale', t, [sx, sy, sx]);
        CHIPS[b].forEach((c, k) => {
          const st = chipState(c, (((t - b * P - (HOLD_END + 0.06)) % LEN) + LEN) % LEN);
          const g = G[`chip_${b}_${k}`];
          if (st) { K(g, 'position', t, [st[0], st[1], 0]); K(g, 'scale', t, st[2]); }
          else K(g, 'scale', t, 0);
        });
      });
    }
    Animator.preview();
    return 'keys for ' + (n + 1) + ' frames';
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
    dir = dir || DIR + 'frames_logo/';
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
    for (let f = 0; f <= Math.round(LEN * FPS); f++) {
      setTime(f * DT);
      Group.all.forEach(g => { const s = g.mesh.scale; if (s.x < 0 || s.y < 0 || s.z < 0) bad.push(g.name + '@' + f); });
    }
    return bad.length ? bad.join(',') : 'no negative scale';
  }
  return { FPS, DT, LEN, P, G, tex, loadTextures, build, animate, camera, render, setTime, scaleSweep, q };
})();
