# Changelog

## v1.0.0 — Initial Release (Minecraft 26.3)

- **CS: Source Physics Engine:** Sub-stepped 100 Hz / 66 Hz engine (`SourceMovementEngine`) with sub-tick yaw interpolation, authentic `sv_air_max_wishspeed = 30` `AirAccelerate`, `sv_friction = 4` / `sv_stopspeed = 75` friction, bhop friction-skip, Valve soft cap (`sv_enablebunnyhopping 0`), jump/land stamina, fast ladder strafing, water jump-out, air crouch-jump (`54 HU` crouch hull), and edgebug/jumpbug detection.
- **6 Presets & 29 CVars:** `css_classic` (default), `css_comp`, `css_surf`, `css_autobhop`, `css_kz`, and `survival_hybrid` configurable via `/css` commands or the SourceCraft Studio GUI.
- **Custom Surf & Map Blocks:** `surf_ramp_30`, `surf_ramp_45`, `surf_ramp_60`, `boost_pad`, `timer_start_pad`, `timer_end_pad`, `checkpoint_pad`, and `map_wand` plus vanilla stair/slab/snow slope surfing.
- **KZ / Surf Practice & Timer:** `/cp`, `/tpcp`, `/prevcp`, `/nextcp`, `/r`, cuboid timer zones, `PRO` vs `NUB` run records, stage splits, and JSON PB persistence.
- **HUD & Studio UI:** Dark-slate + electric cyan Studio GUI with live 2D Air-Strafe Simulator (`M` / Mod Menu), draggable 5-module telemetry HUD (`/csshud`), and Mod Menu integration.
- **GitHub & Pages Ready:** Automated Gradle build workflow, GitHub Pages deployment workflow, and interactive physics simulator in `docs/`.
