# SourceCraft

**Authentic Counter-Strike: Source (`CGameMovement` / `CCSGameMovement`) movement engine for Minecraft 26.3 (Fabric).**

> Sub-stepped 100 Hz / 66 Hz Source physics inside Minecraft's 20-TPS loop — complete with sub-tick mouse yaw air-strafing, authentic `sv_air_max_wishspeed 30` speed gain, `ClipVelocity` ramp & stair surfing, bhop friction-skip, Valve stamina, air crouch-jump, fast ladder strafing, edgebugs/jumpbugs, a KZ/Surf timer & checkpoint system, custom surf blocks, a dark Studio GUI, and a draggable telemetry HUD.

- **Interactive Web Simulator & CVar Docs:** [`https://hazeg7.github.io/Source-Craft/`](https://hazeg7.github.io/Source-Craft/) *(served from [`docs/`](docs/index.html))*
- **Target Version:** Minecraft `26.3` · Fabric Loader `>=0.19.5` · Fabric API `0.166.2+26.3` · Java `25`

---

## Core Physics & Features

### 1. Sub-Stepped CS: Source Engine (`100 Hz` / `66 Hz`)
- Runs 5 internal physics sub-steps (`100 tick`, `dt = 0.01s`) or `66 tick` per Minecraft `50ms` tick using a residual time accumulator.
- **Sub-Tick Mouse Yaw Capture:** Samples raw camera yaw deltas inside `MouseHandler.turnPlayer` so fast air-strafes accelerate smoothly across every sub-step rather than quantizing to 20 TPS.
- **Authentic Hammer Unit Scale:** Default `40 HU = 1 block` aligns CS:S standing height (`72 HU = 1.80m`), eye height (`64 HU = 1.60m`), crouch hull (`54 HU = 1.35m`), and knife speed (`250 u/s = 6.25 b/s`).
- **True `AirAccelerate` Asymmetry:** Caps `addspeed` using `sv_air_max_wishspeed` (`30 u/s`) while multiplying acceleration by unclamped `wishspeed` (`250 u/s`) — reproducing Source's exact curved air-strafe speed gain.
- **Bhop & Stamina Modes:** Skips ground friction on landing-jump frames, supports `sv_autobunnyhopping` and scroll-wheel jump (`mwheeldown`/`mwheelup`/`both`), and optionally enforces Valve's `1.1x` `PreventBunnyJumping` cap + jump/land stamina drain.

### 2. Custom Surf Blocks & Vanilla World Surfing
- **Custom Blocks** (in the **SourceCraft Surf & KZ** Creative tab):
  - `surf_ramp_30`, `surf_ramp_45`, `surf_ramp_60` — 8-slice collision ramps with analytical `ClipVelocity` surface normals.
  - `boost_pad` — Directional launch pad (`950 u/s` forward, `420 u/s` vertical) with velocity sync and spark cues.
  - `timer_start_pad`, `timer_end_pad`, `checkpoint_pad` — Walk-over KZ/Surf timer pads.
  - `map_wand` — Left-click/Right-click cuboid zone selection tool for `START`, `END`, and `STAGE` course zones.
- **Vanilla Slope Surfing:** Seamlessly surf down upright Minecraft stairs, sloped snow-layer stacks, and slab gradients using `ClipVelocity` without catching ledges.

### 3. CS:S Tech & Camera Feel
- **Air Crouch-Jump Foot-Pull:** Crouching in mid-air shrinks the hull to `54 HU` (`1.35` blocks) and lifts your feet `+0.45` blocks when overhead is clear.
- **Fast Ladder Strafing:** Combines `W` + strafe along ladder normals for `√2` climb speed and jump push-off launches.
- **Edgebug & Jumpbug:** Skimming a solid block edge or timing an uncrouch-jump right above ground cancels fall damage and fires HUD/audio callouts.
- **Camera Feel:** Optional subtle strafe roll tilt, landing view-kick punch, and smooth speed-based (or classic locked) FOV.

### 4. Telemetry HUD, Studio GUI & CVar Console
- **Telemetry HUD:** Speedometer (`u/s` or `b/s` color-coded mint/coral/ice for gain/loss), `PRE` / `PEAK` speed, Strafe Sync % coach bar, `WASD + JMP/DCK/WLK + < >` keystrokes, 120-sample velocity sparkline, and Run Timer banner — all drag-and-drop repositionable with snap guides (`/csshud`).
- **SourceCraft Studio (`M` or Mod Menu):** Live 2D Air-Strafe Simulation stage alongside 5 configuration tabs and 6 one-click presets.

---

## Built-In Presets

| Preset ID | Name | Tickrate | `sv_airaccelerate` | `sv_autobunnyhopping` | `sv_enablebunnyhopping` | `sv_staminamax` | Fall Damage |
|---|---|---:|---:|:---:|:---:|---:|:---:|
| `css_classic` *(default)* | CS:S Bhop 100t | `100` | `100` | `0` | `1` | `0` | Off |
| `css_comp` | CS:S Match 66t | `66` | `10` | `0` | `0` | `80` | On |
| `css_surf` | CS:S Surf 66t | `66` | `150` | `1` | `1` | `0` | Off |
| `css_autobhop` | Auto-Bhop 100t | `100` | `1000` | `1` | `1` | `0` | Off |
| `css_kz` | KZ / Climb 100t | `100` | `100` | `0` | `1` | `0` | On |
| `survival_hybrid` | Survival Hybrid | `100` | `60` | `0` | `1` | `40` | On |

---

## Controls & Commands

### Default Keybinds
- `M` — Open SourceCraft Studio
- `G` — Toggle CS:S Physics ON/OFF
- `B` — Toggle Auto-Bhop
- `Left Alt` — Slow Walk (`130 u/s`)
- `C` — Save Checkpoint
- `R` — Teleport to Checkpoint
- `Scroll Down` — Bhop Jump (configurable: Down / Up / Both / Off)

### Commands
- `/css info` — Show active engine preset & core CVars
- `/css preset <id>` — Switch between all 6 presets
- `/css set <cvar> <value>` & `/css get [cvar]` — Adjust or inspect any of the 29 CVars
- `/css enforce` & `/css toggle` — Lock server CVars / toggle physics (OP / singleplayer owner)
- `/css zone <start|end|stage|list|clear>` — Define cuboid course timer zones
- `/cp`, `/tpcp [index]`, `/prevcp`, `/nextcp`, `/r` — KZ checkpoints & restart run
- `/cssgui`, `/csshud` — Client shortcuts to Studio & HUD Position Editor

---

## Multiplayer Behavior
- **Modded Servers:** Full CVar sync (`ServerConfigS2CPayload`), boost/checkpoint velocity sync (`VelocitySyncS2CPayload`), server-side movement check bypass for active players, and OP enforcement option.
- **Unmodded Servers:** Automatically enters `VANILLA` safe fallback (disables CS:S travel override by default to protect against server anti-cheat rubberbanding; can be opted into per-client under `Keys & Sync`).

---

## Building From Source

Requires **Java 25** (Eclipse Temurin 25 recommended):

```bash
./gradlew clean test build
```

The built mod JAR is emitted to `build/libs/sourcecraft-1.0.0.jar`.

---

## License

Released under the [MIT License](LICENSE).
