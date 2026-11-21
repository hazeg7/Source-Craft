package com.hazeg7.sourcecraft;

import static org.junit.jupiter.api.Assertions.*;

import com.hazeg7.sourcecraft.client.config.SourceClientConfig;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.FovMode;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.HudMode;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.ScrollJumpMode;
import com.hazeg7.sourcecraft.physics.SourceCvars;
import com.hazeg7.sourcecraft.physics.SourcePreset;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceConfigAndPresetTest {
	@Test
	void allSixPresetsDetectCleanlyAndClampCvars() {
		for (SourcePreset p : SourcePreset.all()) {
			SourceCvars cv = p.buildCvars();
			assertEquals(p.id(), SourcePreset.detectPresetId(cv),
				"Preset " + p.id() + " should round-trip through detectPresetId");
		}
		assertEquals(6, SourcePreset.all().size());

		SourceCvars custom = SourceCvars.createDefault();
		assertTrue(custom.setByName("sv_airaccelerate", 99999.0));
		assertEquals(2000.0, custom.getByName("sv_airaccelerate"), 1e-6);
		assertTrue(custom.setByName("sv_friction", -50.0));
		assertEquals(0.0, custom.getByName("sv_friction"), 1e-6);
		assertFalse(custom.setByName("sv_nonexistent", 1.0));
		assertEquals("custom", SourcePreset.detectPresetId(custom));
		assertTrue(custom.formatSourceConsole("sv_airaccelerate").contains("2000"));
	}

	@Test
	void wireArrayAndClientConfigPersistCleanly(@TempDir Path tempDir) {
		SourceCvars original = SourcePreset.CSS_SURF.buildCvars();
		SourceCvars decoded = SourceCvars.fromWireArray(original.toWireArray());
		assertTrue(original.matches(decoded));

		Path cfgPath = tempDir.resolve("sourcecraft-client.json");
		SourceClientConfig cfg = SourceClientConfig.loadFrom(cfgPath);
		cfg.allowOnVanillaServers = true;
		cfg.scrollJumpMode = ScrollJumpMode.BOTH;
		cfg.fovMode = FovMode.DYNAMIC;
		cfg.hudMode = HudMode.COMPACT;
		cfg.hudScale = 1.35f;
		cfg.localCvars = SourcePreset.SURVIVAL_HYBRID.buildCvars();
		cfg.activePresetId = SourcePreset.SURVIVAL_HYBRID.id();
		cfg.saveTo(cfgPath);

		SourceClientConfig reloaded = SourceClientConfig.loadFrom(cfgPath);
		assertTrue(reloaded.allowOnVanillaServers);
		assertEquals(ScrollJumpMode.BOTH, reloaded.scrollJumpMode);
		assertEquals(FovMode.DYNAMIC, reloaded.fovMode);
		assertEquals(HudMode.COMPACT, reloaded.hudMode);
		assertEquals(1.35f, reloaded.hudScale, 1e-4f);
		assertTrue(cfg.localCvars.matches(reloaded.localCvars));
	}
}
