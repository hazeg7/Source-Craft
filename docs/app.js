(() => {
  "use strict";

  const PRESETS = [
    {
      id: "css_classic",
      name: "CS:S Bhop 100t",
      badge: "100T · AA100",
      tickrate: 100,
      airAccelerate: 100,
      accelerate: 5.5,
      friction: 4.0,
      stopspeed: 75,
      maxspeed: 250,
      airMaxWishspeed: 30,
      autobhop: false,
      enableBhop: true,
      staminaMax: 0,
      fallDamage: false
    },
    {
      id: "css_comp",
      name: "CS:S Match 66t",
      badge: "66T · AA10",
      tickrate: 66,
      airAccelerate: 10,
      accelerate: 5.5,
      friction: 4.0,
      stopspeed: 75,
      maxspeed: 250,
      airMaxWishspeed: 30,
      autobhop: false,
      enableBhop: false,
      staminaMax: 80,
      fallDamage: true
    },
    {
      id: "css_surf",
      name: "CS:S Surf 66t",
      badge: "SURF · AA150",
      tickrate: 66,
      airAccelerate: 150,
      accelerate: 10.0,
      friction: 4.0,
      stopspeed: 75,
      maxspeed: 250,
      airMaxWishspeed: 30,
      autobhop: true,
      enableBhop: true,
      staminaMax: 0,
      fallDamage: false
    },
    {
      id: "css_autobhop",
      name: "Auto-Bhop 100t",
      badge: "AUTO · AA1000",
      tickrate: 100,
      airAccelerate: 1000,
      accelerate: 10.0,
      friction: 4.0,
      stopspeed: 75,
      maxspeed: 250,
      airMaxWishspeed: 30,
      autobhop: true,
      enableBhop: true,
      staminaMax: 0,
      fallDamage: false
    },
    {
      id: "css_kz",
      name: "KZ / Climb 100t",
      badge: "KZ · AA100",
      tickrate: 100,
      airAccelerate: 100,
      accelerate: 5.5,
      friction: 4.0,
      stopspeed: 75,
      maxspeed: 250,
      airMaxWishspeed: 30,
      autobhop: false,
      enableBhop: true,
      staminaMax: 0,
      fallDamage: true
    },
    {
      id: "survival_hybrid",
      name: "Survival Hybrid",
      badge: "SMP · AA60",
      tickrate: 100,
      airAccelerate: 60,
      accelerate: 5.5,
      friction: 4.5,
      stopspeed: 75,
      maxspeed: 250,
      airMaxWishspeed: 30,
      autobhop: false,
      enableBhop: true,
      staminaMax: 40,
      fallDamage: true
    }
  ];

  const CVARS = [
    ["sv_gravity", "800", "50–2400", "Air & Ground", "World gravity in HU/s² (CS:S default 800)"],
    ["sv_accelerate", "5.5", "1–25", "Air & Ground", "Ground acceleration multiplier"],
    ["sv_airaccelerate", "100", "1–2000", "Air & Ground", "Air-strafe acceleration (10 comp, 100 bhop, 150 surf)"],
    ["sv_friction", "4", "0–20", "Air & Ground", "Ground friction coefficient (CS:S default 4)"],
    ["sv_stopspeed", "75", "10–300", "Air & Ground", "Minimum speed used for ground friction drop (CS:S 75)"],
    ["sv_maxspeed", "250", "80–600", "Air & Ground", "Base running cap in HU/s (250 knife speed)"],
    ["sv_maxvelocity", "3500", "500–10000", "Air & Ground", "Global per-axis velocity clamp in HU/s"],
    ["sv_air_max_wishspeed", "30", "5–150", "Air & Ground", "Air wishspeed clamp driving Source air-strafe gain (30)"],
    ["sv_jump_impulse", "302", "120–600", "Air & Ground", "Vertical jump takeoff velocity in HU/s (~57 HU jump)"],
    ["sv_autobunnyhopping", "0", "0 / 1", "Bhop & Stamina", "Hold Space to auto-jump on ground contact"],
    ["sv_enablebunnyhopping", "1", "0 / 1", "Bhop & Stamina", "Uncapped bhop speed (0 enforces Valve 1.1x cap)"],
    ["sv_staminamax", "0", "0–100", "Bhop & Stamina", "Maximum CS:S stamina pool (80 comp, 0 bhop)"],
    ["sv_staminajumpcost", "0.08", "0–0.5", "Bhop & Stamina", "Stamina fraction added on each jump"],
    ["sv_staminalandcost", "0.05", "0–0.5", "Bhop & Stamina", "Stamina fraction added on landing"],
    ["sv_staminarecoveryrate", "60", "5–200", "Bhop & Stamina", "Stamina recovery rate in units/sec"],
    ["sv_walk_ratio", "0.52", "0.2–1.0", "Air & Ground", "Slow-walk speed multiplier (0.52 = 130 u/s)"],
    ["sv_duck_ratio", "0.34", "0.2–1.0", "Air & Ground", "Crouch speed multiplier (0.34 = 85 u/s)"],
    ["sv_wateraccelerate", "10", "1–30", "Surf & Tech", "Swimming acceleration multiplier"],
    ["sv_waterfriction", "1", "0–10", "Surf & Tech", "Water drag coefficient"],
    ["sv_ladder_scale_speed", "0.78", "0.3–1.5", "Surf & Tech", "Ladder climb speed ratio (~sqrt(2) with fast ladder)"],
    ["sv_surf_enabled", "1", "0 / 1", "Surf & Tech", "Enable ClipVelocity surfing on ramps, stairs, slabs, snow"],
    ["sv_surf_overbounce", "1", "1–1.25", "Surf & Tech", "ClipVelocity plane bounce multiplier (1.0 standard surf)"],
    ["sv_weapon_weight", "0", "0 / 1", "Surf & Tech", "Scale maxspeed by held item weight"],
    ["sv_crouch_jump", "1", "0 / 1", "Surf & Tech", "Air crouch-jump foot-pull & 54 HU crouch hull"],
    ["sv_edgebug_enabled", "1", "0 / 1", "Surf & Tech", "Cancel fall damage when skimming a block edge"],
    ["sv_survival_fall_damage", "0", "0 / 1", "Surf & Tech", "Apply survival fall damage on hard landings"],
    ["sv_tickrate", "100", "20–128", "Scale & Timing", "Internal physics sub-step frequency in Hz"],
    ["sv_hu_per_block", "40", "32–48", "Scale & Timing", "Hammer Units per Minecraft block (40 = 1.80m 72 HU)"],
    ["sv_jump_buffer_ticks", "2", "0–6", "Scale & Timing", "Pre-landing scroll/jump input buffer in MC ticks"]
  ];

  // Active Playground State
  let currentPreset = PRESETS[0];
  let params = {
    airAccelerate: 100,
    tickrate: 100,
    airMaxWishspeed: 30,
    turnRateDegPerSec: 260,
    maxspeed: 250,
    enableBhop: true
  };

  let interactiveMode = false;
  let showGhost20 = true;
  let activeCategory = "All";
  let keys = { a: false, d: false, pointerDown: false, lastMouseX: null };

  // Simulation Runners (Source Sub-stepped vs 20-TPS Naive)
  const sim = {
    x: 0, z: 0, vx: 0, vz: 250, yawDeg: 0,
    trail: [], speedHist: [], peak: 250, syncGood: 0, syncTotal: 0, optimalDeg: 6.9,
    t: 0
  };
  const ghost = {
    x: 0, z: 0, vx: 0, vz: 250, yawDeg: 0, trail: [], peak: 250
  };

  function resetRunners() {
    sim.x = 0; sim.z = 0; sim.vx = 0; sim.vz = params.maxspeed; sim.yawDeg = 0;
    sim.trail = []; sim.speedHist = []; sim.peak = params.maxspeed; sim.syncGood = 0; sim.syncTotal = 0; sim.t = 0;
    ghost.x = 0; ghost.z = 0; ghost.vx = 0; ghost.vz = params.maxspeed; ghost.yawDeg = 0;
    ghost.trail = []; ghost.peak = params.maxspeed;
  }

  function airAccelerate(vx, vz, wishX, wishZ, wishSpeed, aa, wishCap, dt) {
    const wishSpd = Math.min(wishSpeed, wishCap);
    const cur = vx * wishX + vz * wishZ;
    const add = wishSpd - cur;
    if (add <= 0) return [vx, vz, 0];
    let acc = aa * wishSpeed * dt;
    if (acc > add) acc = add;
    return [vx + acc * wishX, vz + acc * wishZ, acc];
  }

  function stepRunner(r, dt, side, turnDeltaDeg, aa, wishCap) {
    r.yawDeg += turnDeltaDeg;
    const rad = (r.yawDeg * Math.PI) / 180;
    const rightX = -Math.cos(rad);
    const rightZ = -Math.sin(rad);
    const wishX = side * rightX;
    const wishZ = side * rightZ;
    const [nvx, nvz, acc] = airAccelerate(r.vx, r.vz, wishX, wishZ, params.maxspeed, aa, wishCap, dt);
    r.vx = nvx;
    r.vz = nvz;
    r.x += r.vx * dt;
    r.z += r.vz * dt;
    const spd = Math.hypot(r.vx, r.vz);
    if (spd > r.peak) r.peak = spd;
    return { spd, wishX, wishZ, gained: acc > 0.01 };
  }

  function advanceFrame(dtSec) {
    // Sub-stepped CS:S simulation
    const subDt = 1 / Math.max(20, params.tickrate);
    const steps = Math.max(1, Math.round(dtSec / subDt));
    const stepDt = dtSec / steps;

    for (let i = 0; i < steps; i++) {
      sim.t += stepDt;
      let side = 0;
      let turnRate = 0;
      if (!interactiveMode) {
        const phase = Math.floor(sim.t / 0.72) % 2;
        side = phase === 0 ? 1 : -1;
        turnRate = side * params.turnRateDegPerSec;
      } else {
        if (keys.d && !keys.a) {
          side = 1;
          turnRate = params.turnRateDegPerSec;
        } else if (keys.a && !keys.d) {
          side = -1;
          turnRate = -params.turnRateDegPerSec;
        }
      }
      if (side !== 0) {
        const res = stepRunner(sim, stepDt, side, turnRate * stepDt, params.airAccelerate, params.airMaxWishspeed);
        sim.syncTotal++;
        if (res.gained) sim.syncGood++;
        sim.optimalDeg = (Math.asin(Math.min(1, params.airMaxWishspeed / Math.max(params.airMaxWishspeed, res.spd))) * 180) / Math.PI;
      } else {
        sim.x += sim.vx * stepDt;
        sim.z += sim.vz * stepDt;
      }
    }

    // 20-TPS quantized comparison ghost (single discrete 50ms step + turn loss)
    if (showGhost20 && !interactiveMode) {
      const ghostDt = 0.05;
      const gSteps = Math.max(1, Math.round(dtSec / ghostDt));
      for (let i = 0; i < gSteps; i++) {
        const phase = Math.floor((sim.t - dtSec + i * ghostDt) / 0.72) % 2;
        const side = phase === 0 ? 1 : -1;
        // 20 TPS quantizes yaw updates once per 50ms and loses high-turn alignment
        stepRunner(ghost, ghostDt, side, side * params.turnRateDegPerSec * ghostDt, Math.min(params.airAccelerate, 24), params.airMaxWishspeed);
      }
    }

    sim.trail.push({ x: sim.x, z: sim.z, spd: Math.hypot(sim.vx, sim.vz) });
    if (sim.trail.length > 260) sim.trail.shift();

    sim.speedHist.push(Math.hypot(sim.vx, sim.vz));
    if (sim.speedHist.length > 140) sim.speedHist.shift();

    ghost.trail.push({ x: ghost.x, z: ghost.z });
    if (ghost.trail.length > 260) ghost.trail.shift();

    // Soft wrap if speed runs extreme during auto-bhop 1000 demo
    if (Math.hypot(sim.vx, sim.vz) > 1650 && !interactiveMode) {
      resetRunners();
    }
  }

  function renderCanvas() {
    const canvas = document.getElementById("simCanvas");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    const W = canvas.width;
    const H = canvas.height;

    ctx.fillStyle = "#070b14";
    ctx.fillRect(0, 0, W, H);

    // Camera follows runner smoothly with scrolling world grid
    const scale = 0.52;
    const camX = sim.x;
    const camZ = sim.z;

    const gridStep = 100;
    const startGX = Math.floor((camX - W / (2 * scale)) / gridStep) * gridStep;
    const endGX = camX + W / (2 * scale) + gridStep;
    const startGZ = Math.floor((camZ - H / (2 * scale)) / gridStep) * gridStep;
    const endGZ = camZ + H / (2 * scale) + gridStep;

    ctx.lineWidth = 1;
    ctx.strokeStyle = "#141d31";
    ctx.beginPath();
    for (let gx = startGX; gx <= endGX; gx += gridStep) {
      const sx = W / 2 + (gx - camX) * scale;
      ctx.moveTo(sx, 0);
      ctx.lineTo(sx, H);
    }
    for (let gz = startGZ; gz <= endGZ; gz += gridStep) {
      const sy = H / 2 + (gz - camZ) * scale;
      ctx.moveTo(0, sy);
      ctx.lineTo(W, sy);
    }
    ctx.stroke();

    // Ghost trail
    if (showGhost20 && !interactiveMode && ghost.trail.length > 1) {
      ctx.strokeStyle = "rgba(248, 113, 113, 0.55)";
      ctx.lineWidth = 2;
      ctx.beginPath();
      ghost.trail.forEach((pt, idx) => {
        const sx = W / 2 + (pt.x - camX) * scale;
        const sy = H / 2 + (pt.z - camZ) * scale;
        if (idx === 0) ctx.moveTo(sx, sy);
        else ctx.lineTo(sx, sy);
      });
      ctx.stroke();

      const gx = W / 2 + (ghost.x - camX) * scale;
      const gz = H / 2 + (ghost.z - camZ) * scale;
      ctx.fillStyle = "#f87171";
      ctx.beginPath();
      ctx.arc(gx, gz, 5, 0, Math.PI * 2);
      ctx.fill();
    }

    // SourceCraft sub-stepped trail
    if (sim.trail.length > 1) {
      for (let i = 1; i < sim.trail.length; i++) {
        const a = sim.trail[i - 1];
        const b = sim.trail[i];
        ctx.strokeStyle = b.spd > 280 ? "#34d399" : "#22d3ee";
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(W / 2 + (a.x - camX) * scale, H / 2 + (a.z - camZ) * scale);
        ctx.lineTo(W / 2 + (b.x - camX) * scale, H / 2 + (b.z - camZ) * scale);
        ctx.stroke();
      }
    }

    // Velocity vector & look yaw cone on centered runner
    const spd = Math.hypot(sim.vx, sim.vz);
    const velAngle = Math.atan2(sim.vz, sim.vx);
    ctx.strokeStyle = "rgba(52, 211, 153, 0.45)";
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(W / 2, H / 2);
    ctx.lineTo(W / 2 + Math.cos(velAngle) * 56, H / 2 + Math.sin(velAngle) * 56);
    ctx.stroke();

    const yawRad = (sim.yawDeg * Math.PI) / 180;
    const lookVx = -Math.sin(yawRad);
    const lookVz = Math.cos(yawRad);
    ctx.strokeStyle = "#22d3ee";
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(W / 2, H / 2);
    ctx.lineTo(W / 2 + lookVx * 42, H / 2 + lookVz * 42);
    ctx.stroke();

    ctx.fillStyle = "#f1f5f9";
    ctx.beginPath();
    ctx.arc(W / 2, H / 2, 6.5, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = "#22d3ee";
    ctx.lineWidth = 2;
    ctx.stroke();

    // Bottom sparkline inside canvas
    const gx = 16, gy = H - 56, gw = 220, gh = 42;
    ctx.fillStyle = "rgba(11, 16, 27, 0.85)";
    ctx.fillRect(gx, gy, gw, gh);
    ctx.strokeStyle = "#283652";
    ctx.lineWidth = 1;
    ctx.strokeRect(gx, gy, gw, gh);
    const maxS = Math.max(400, sim.peak);
    const baseY = gy + gh - (250 / maxS) * (gh - 6);
    ctx.strokeStyle = "#334155";
    ctx.beginPath();
    ctx.moveTo(gx, baseY);
    ctx.lineTo(gx + gw, baseY);
    ctx.stroke();

    if (sim.speedHist.length > 1) {
      ctx.strokeStyle = "#34d399";
      ctx.lineWidth = 2;
      ctx.beginPath();
      sim.speedHist.forEach((v, idx) => {
        const px = gx + (idx / (sim.speedHist.length - 1)) * gw;
        const py = gy + gh - 3 - (v / maxS) * (gh - 8);
        if (idx === 0) ctx.moveTo(px, py);
        else ctx.lineTo(px, py);
      });
      ctx.stroke();
    }

    // Legend overlay
    ctx.fillStyle = "#94a3b8";
    ctx.font = "12px Consolas, monospace";
    ctx.fillText(
      `Source ${Math.round(params.tickrate)}Hz (cyan/mint) vs 20-TPS Ghost (coral)`,
      16,
      24
    );

    // Update metric cards
    document.getElementById("hudSpeed").textContent = `${Math.round(spd)} u/s`;
    document.getElementById("hudPeak").textContent = `${Math.round(sim.peak)} / ${Math.round(ghost.peak)}`;
    const syncPct = sim.syncTotal > 0 ? Math.round((100 * sim.syncGood) / sim.syncTotal) : 100;
    document.getElementById("hudSync").textContent = `${syncPct}%`;
    document.getElementById("hudAngle").textContent = `${sim.optimalDeg.toFixed(1)}°`;
  }

  function applyPreset(preset) {
    currentPreset = preset;
    params.airAccelerate = preset.airAccelerate;
    params.tickrate = preset.tickrate;
    params.airMaxWishspeed = preset.airMaxWishspeed;
    params.maxspeed = preset.maxspeed;
    params.enableBhop = preset.enableBhop;

    document.getElementById("slAirAccel").value = params.airAccelerate;
    document.getElementById("slTickrate").value = params.tickrate;
    document.getElementById("slWish").value = params.airMaxWishspeed;
    syncLabels();
    renderPresetButtons();
    renderExports();
    resetRunners();
  }

  function syncLabels() {
    document.getElementById("valAirAccel").textContent = String(Math.round(params.airAccelerate));
    document.getElementById("valTickrate").textContent = `${Math.round(params.tickrate)} Hz`;
    document.getElementById("valWish").textContent = String(Math.round(params.airMaxWishspeed));
    document.getElementById("valTurn").textContent = `${Math.round(params.turnRateDegPerSec)}°/s`;
  }

  function renderPresetButtons() {
    const host = document.getElementById("presetButtons");
    host.innerHTML = "";
    PRESETS.forEach((p) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "btn btn-sm" + (p.id === currentPreset.id ? " btn-active" : "");
      btn.textContent = p.name;
      btn.onclick = () => applyPreset(p);
      host.appendChild(btn);
    });
  }

  function renderCvars() {
    const query = (document.getElementById("cvarSearch").value || "").toLowerCase().trim();
    const body = document.getElementById("cvarTableBody");
    body.innerHTML = "";

    CVARS.filter(([name, , , cat, desc]) => {
      if (activeCategory !== "All" && cat !== activeCategory) return false;
      if (!query) return true;
      return name.includes(query) || desc.toLowerCase().includes(query) || cat.toLowerCase().includes(query);
    }).forEach(([name, defVal, range, , desc]) => {
      const tr = document.createElement("tr");
      const cmd = `/css set ${name} ${defVal}`;
      tr.innerHTML = `
        <td class="mono" style="color:var(--accent);font-weight:600;">${name}</td>
        <td class="mono">${defVal}</td>
        <td class="mono" style="color:var(--text-secondary);">${range}</td>
        <td style="color:var(--text-secondary);">${desc}</td>
        <td><button class="btn btn-sm copy-row-btn" type="button" data-cmd="${cmd}">Copy</button></td>
      `;
      body.appendChild(tr);
    });

    body.querySelectorAll(".copy-row-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        const text = btn.getAttribute("data-cmd");
        if (navigator.clipboard) navigator.clipboard.writeText(text).catch(() => {});
        btn.textContent = "Copied";
        setTimeout(() => { btn.textContent = "Copy"; }, 1100);
      });
    });
  }

  function renderCategoryButtons() {
    const cats = ["All", "Air & Ground", "Bhop & Stamina", "Surf & Tech", "Scale & Timing"];
    const host = document.getElementById("catButtons");
    host.innerHTML = "";
    cats.forEach((c) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "btn btn-sm" + (activeCategory === c ? " btn-active" : "");
      btn.textContent = c;
      btn.onclick = () => {
        activeCategory = c;
        renderCategoryButtons();
        renderCvars();
      };
      host.appendChild(btn);
    });
  }

  function renderExports() {
    const batch = [
      `/css preset ${currentPreset.id}`,
      `/css set sv_airaccelerate ${Math.round(params.airAccelerate)}`,
      `/css set sv_tickrate ${Math.round(params.tickrate)}`,
      `/css set sv_air_max_wishspeed ${Math.round(params.airMaxWishspeed)}`
    ].join("\n");
    document.getElementById("cmdBatchOut").textContent = batch;

    const jsonObj = {
      enabled: true,
      serverEnforced: false,
      presetId: currentPreset.id,
      cvars: {
        svTickrate: params.tickrate,
        svAirAccelerate: params.airAccelerate,
        svAccelerate: currentPreset.accelerate,
        svFriction: currentPreset.friction,
        svStopspeed: currentPreset.stopspeed,
        svAirMaxWishspeed: params.airMaxWishspeed,
        svAutoBunnyhopping: currentPreset.autobhop,
        svEnableBunnyhopping: currentPreset.enableBhop,
        svStaminaMax: currentPreset.staminaMax
      }
    };
    document.getElementById("jsonCfgOut").textContent = JSON.stringify(jsonObj, null, 2);
  }

  function wireControls() {
    document.getElementById("slAirAccel").addEventListener("input", (e) => {
      params.airAccelerate = Number(e.target.value);
      syncLabels();
      renderExports();
    });
    document.getElementById("slTickrate").addEventListener("input", (e) => {
      params.tickrate = Number(e.target.value);
      syncLabels();
      renderExports();
    });
    document.getElementById("slWish").addEventListener("input", (e) => {
      params.airMaxWishspeed = Number(e.target.value);
      syncLabels();
      renderExports();
    });
    document.getElementById("slTurn").addEventListener("input", (e) => {
      params.turnRateDegPerSec = Number(e.target.value);
      syncLabels();
    });

    const modeBtn = document.getElementById("modeBtn");
    modeBtn.addEventListener("click", () => {
      interactiveMode = !interactiveMode;
      modeBtn.textContent = interactiveMode ? "Mode: Playable (A/D or Drag)" : "Mode: Auto-Strafe Demo";
      resetRunners();
    });

    const ghostBtn = document.getElementById("ghostBtn");
    ghostBtn.addEventListener("click", () => {
      showGhost20 = !showGhost20;
      ghostBtn.textContent = `20-TPS Ghost: ${showGhost20 ? "ON" : "OFF"}`;
      ghostBtn.classList.toggle("btn-active", showGhost20);
    });

    document.getElementById("resetSimBtn").addEventListener("click", resetRunners);
    document.getElementById("cvarSearch").addEventListener("input", renderCvars);

    document.getElementById("copyCmdBatch").addEventListener("click", (e) => {
      const txt = document.getElementById("cmdBatchOut").textContent;
      if (navigator.clipboard) navigator.clipboard.writeText(txt).catch(() => {});
      e.target.textContent = "Copied";
      setTimeout(() => { e.target.textContent = "Copy Commands"; }, 1100);
    });
    document.getElementById("copyJsonCfg").addEventListener("click", (e) => {
      const txt = document.getElementById("jsonCfgOut").textContent;
      if (navigator.clipboard) navigator.clipboard.writeText(txt).catch(() => {});
      e.target.textContent = "Copied";
      setTimeout(() => { e.target.textContent = "Copy JSON"; }, 1100);
    });

    window.addEventListener("keydown", (e) => {
      if (e.key === "a" || e.key === "A") keys.a = true;
      if (e.key === "d" || e.key === "D") keys.d = true;
    });
    window.addEventListener("keyup", (e) => {
      if (e.key === "a" || e.key === "A") keys.a = false;
      if (e.key === "d" || e.key === "D") keys.d = false;
    });

    const canvas = document.getElementById("simCanvas");
    canvas.addEventListener("pointerdown", (e) => {
      keys.pointerDown = true;
      keys.lastMouseX = e.clientX;
    });
    window.addEventListener("pointerup", () => {
      keys.pointerDown = false;
      keys.lastMouseX = null;
      if (interactiveMode) { keys.a = false; keys.d = false; }
    });
    canvas.addEventListener("pointermove", (e) => {
      if (!interactiveMode || !keys.pointerDown || keys.lastMouseX === null) return;
      const dx = e.clientX - keys.lastMouseX;
      keys.lastMouseX = e.clientX;
      if (dx > 1) { keys.d = true; keys.a = false; }
      else if (dx < -1) { keys.a = true; keys.d = false; }
    });
  }

  let lastTs = performance.now();
  function loop(ts) {
    const dt = Math.min(0.05, Math.max(0.005, (ts - lastTs) / 1000));
    lastTs = ts;
    advanceFrame(dt);
    renderCanvas();
    requestAnimationFrame(loop);
  }

  renderPresetButtons();
  renderCategoryButtons();
  renderCvars();
  renderExports();
  syncLabels();
  wireControls();
  resetRunners();
  // Pre-seed a clean initial curve on frame 0 so static captures immediately show rich motion
  for (let i = 0; i < 48; i++) advanceFrame(0.025);
  renderCanvas();
  requestAnimationFrame(loop);
})();
