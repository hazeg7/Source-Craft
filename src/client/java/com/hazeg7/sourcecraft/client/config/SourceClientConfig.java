package com.hazeg7.sourcecraft.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hazeg7.sourcecraft.physics.SourceCvars;
import com.hazeg7.sourcecraft.physics.SourcePreset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Persistent client-side configuration for HUD modules, controls feel, scroll-jump, and local CVars.
 */
public final class SourceClientConfig {
	public enum HudMode {
		OFF("Off"),
		COMPACT("Compact"),
		FULL("Full Studio");

		public final String label;

		HudMode(String label) {
			this.label = label;
		}

		public HudMode next() {
			HudMode[] v = values();
			return v[(ordinal() + 1) % v.length];
		}
	}

	public enum SpeedUnit {
		HU_PER_SEC("u/s"),
		BLOCKS_PER_SEC("b/s");

		public final String suffix;

		SpeedUnit(String suffix) {
			this.suffix = suffix;
		}

		public SpeedUnit next() {
			return this == HU_PER_SEC ? BLOCKS_PER_SEC : HU_PER_SEC;
		}
	}

	public enum ScrollJumpMode {
		OFF("Off"),
		DOWN("Scroll Down"),
		UP("Scroll Up"),
		BOTH("Up + Down");

		public final String label;

		ScrollJumpMode(String label) {
			this.label = label;
		}

		public ScrollJumpMode next() {
			ScrollJumpMode[] v = values();
			return v[(ordinal() + 1) % v.length];
		}
	}

	public enum FovMode {
		OFF("Locked (CS:S)"),
		SUBTLE("Subtle Speed"),
		DYNAMIC("Dynamic Surf");

		public final String label;

		FovMode(String label) {
			this.label = label;
		}

		public FovMode next() {
			FovMode[] v = values();
			return v[(ordinal() + 1) % v.length];
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static SourceClientConfig instance;
	private static Path overridePathForTests;

	public boolean modEnabled = true;
	public boolean allowOnVanillaServers = false;
	public String activePresetId = SourcePreset.CSS_CLASSIC.id();
	public SourceCvars localCvars = SourcePreset.CSS_CLASSIC.buildCvars();

	// Controls & camera feel
	public ScrollJumpMode scrollJumpMode = ScrollJumpMode.DOWN;
	public double strafeRollDegrees = 1.35;
	public double landingKickScale = 1.0;
	public FovMode fovMode = FovMode.SUBTLE;
	public boolean audioCues = true;
	public boolean restoreVelocityOnTp = true;

	// HUD modules & normalized positioning
	public HudMode hudMode = HudMode.FULL;
	public SpeedUnit speedUnit = SpeedUnit.HU_PER_SEC;
	public boolean showSpeedometer = true;
	public boolean showStrafeTrainer = true;
	public boolean showKeystrokes = true;
	public boolean showVelocityGraph = true;
	public boolean showTimerBanner = true;

	public float hudScale = 1.0f;
	public float hudOpacity = 0.90f;

	public float speedNormX = 0.50f;
	public float speedNormY = 0.62f;
	public float strafeNormX = 0.50f;
	public float strafeNormY = 0.70f;
	public float keysNormX = 0.98f;
	public float keysNormY = 0.88f;
	public float graphNormX = 0.02f;
	public float graphNormY = 0.88f;
	public float timerNormX = 0.50f;
	public float timerNormY = 0.04f;

	public static synchronized SourceClientConfig get() {
		if (instance == null) {
			instance = loadFrom(resolvePath());
		}
		return instance;
	}

	public static synchronized void setOverridePathForTests(Path p) {
		overridePathForTests = p;
		instance = null;
	}

	private static Path resolvePath() {
		if (overridePathForTests != null) {
			return overridePathForTests;
		}
		try {
			return FabricLoader.getInstance().getConfigDir().resolve("sourcecraft-client.json");
		} catch (Throwable ignored) {
			return Path.of("config", "sourcecraft-client.json");
		}
	}

	public static SourceClientConfig loadFrom(Path path) {
		if (path == null || !Files.exists(path)) {
			SourceClientConfig cfg = new SourceClientConfig();
			cfg.sanitize();
			cfg.saveTo(path);
			return cfg;
		}
		try {
			String raw = Files.readString(path, StandardCharsets.UTF_8);
			SourceClientConfig cfg = GSON.fromJson(raw, SourceClientConfig.class);
			if (cfg == null) {
				cfg = new SourceClientConfig();
			}
			cfg.sanitize();
			return cfg;
		} catch (Exception ignored) {
			SourceClientConfig cfg = new SourceClientConfig();
			cfg.sanitize();
			return cfg;
		}
	}

	public void save() {
		sanitize();
		saveTo(resolvePath());
	}

	public void saveTo(Path path) {
		if (path == null) {
			return;
		}
		try {
			Path p = path.getParent();
			if (p != null) {
				Files.createDirectories(p);
			}
			Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (Exception ignored) {
		}
	}

	public SourceClientConfig copy() {
		SourceClientConfig c = new SourceClientConfig();
		c.modEnabled = this.modEnabled;
		c.allowOnVanillaServers = this.allowOnVanillaServers;
		c.activePresetId = this.activePresetId;
		c.localCvars = this.localCvars != null ? this.localCvars.copy() : SourcePreset.CSS_CLASSIC.buildCvars();
		c.scrollJumpMode = this.scrollJumpMode;
		c.strafeRollDegrees = this.strafeRollDegrees;
		c.landingKickScale = this.landingKickScale;
		c.fovMode = this.fovMode;
		c.audioCues = this.audioCues;
		c.restoreVelocityOnTp = this.restoreVelocityOnTp;
		c.hudMode = this.hudMode;
		c.speedUnit = this.speedUnit;
		c.showSpeedometer = this.showSpeedometer;
		c.showStrafeTrainer = this.showStrafeTrainer;
		c.showKeystrokes = this.showKeystrokes;
		c.showVelocityGraph = this.showVelocityGraph;
		c.showTimerBanner = this.showTimerBanner;
		c.hudScale = this.hudScale;
		c.hudOpacity = this.hudOpacity;
		c.speedNormX = this.speedNormX;
		c.speedNormY = this.speedNormY;
		c.strafeNormX = this.strafeNormX;
		c.strafeNormY = this.strafeNormY;
		c.keysNormX = this.keysNormX;
		c.keysNormY = this.keysNormY;
		c.graphNormX = this.graphNormX;
		c.graphNormY = this.graphNormY;
		c.timerNormX = this.timerNormX;
		c.timerNormY = this.timerNormY;
		c.sanitize();
		return c;
	}

	public void resetHudPositions() {
		hudScale = 1.0f;
		hudOpacity = 0.90f;
		speedNormX = 0.50f;
		speedNormY = 0.62f;
		strafeNormX = 0.50f;
		strafeNormY = 0.70f;
		keysNormX = 0.98f;
		keysNormY = 0.88f;
		graphNormX = 0.02f;
		graphNormY = 0.88f;
		timerNormX = 0.50f;
		timerNormY = 0.04f;
	}

	public void sanitize() {
		if (localCvars == null) {
			localCvars = SourcePreset.CSS_CLASSIC.buildCvars();
		}
		localCvars.sanitize();
		if (activePresetId == null || activePresetId.isBlank()) {
			activePresetId = SourcePreset.detectPresetId(localCvars);
		}
		if (scrollJumpMode == null) {
			scrollJumpMode = ScrollJumpMode.DOWN;
		}
		if (fovMode == null) {
			fovMode = FovMode.SUBTLE;
		}
		if (hudMode == null) {
			hudMode = HudMode.FULL;
		}
		if (speedUnit == null) {
			speedUnit = SpeedUnit.HU_PER_SEC;
		}
		strafeRollDegrees = Math.max(0.0, Math.min(3.5, strafeRollDegrees));
		landingKickScale = Math.max(0.0, Math.min(2.5, landingKickScale));
		hudScale = Math.max(0.65f, Math.min(1.65f, hudScale));
		hudOpacity = Math.max(0.25f, Math.min(1.0f, hudOpacity));
		speedNormX = clamp01(speedNormX);
		speedNormY = clamp01(speedNormY);
		strafeNormX = clamp01(strafeNormX);
		strafeNormY = clamp01(strafeNormY);
		keysNormX = clamp01(keysNormX);
		keysNormY = clamp01(keysNormY);
		graphNormX = clamp01(graphNormX);
		graphNormY = clamp01(graphNormY);
		timerNormX = clamp01(timerNormX);
		timerNormY = clamp01(timerNormY);
	}

	private static float clamp01(float v) {
		return Float.isFinite(v) ? Math.max(0.0f, Math.min(1.0f, v)) : 0.5f;
	}
}
