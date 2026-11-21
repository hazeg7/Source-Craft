# SourceCraft — Project Agent Rules

## Scope & Palette
- Target: Minecraft `26.3`, Fabric Loader `0.20.1`, Fabric API `0.166.2+26.3`, Java `25`, Mod Menu `21.0.1` (`compileOnly`).
- Use `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot` in command invocations rather than hardcoding `org.gradle.java.home` in `gradle.properties`.
- Visual design (GUI, HUD, textures, icon, and `docs/` GitHub Pages site): cool dark-slate (`#0B101B` / `#111827` / `#182235`) with electric cyan (`#22D3EE`), mint (`#34D399`), and coral (`#F87171`). Do not use warm yellow, gold, amber, beige, cream, or serif display fonts.

## Architecture
- Keep `com.hazeg7.sourcecraft.physics` pure Java (zero Minecraft classes) so every CS:S movement law runs directly in JUnit 5 tests and the in-game preview.
- Keep all mixins surgical and safe for Creative flight, Spectator, Elytra, swimming, vehicles, and unmodded servers.
- Do not commit, push, or launch visible client/browser windows without explicit user authorization.
