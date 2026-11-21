package com.hazeg7.sourcecraft;

import static org.junit.jupiter.api.Assertions.*;

import com.hazeg7.sourcecraft.network.ClientConfigRequestC2SPayload;
import com.hazeg7.sourcecraft.network.PlayerModeC2SPayload;
import com.hazeg7.sourcecraft.network.ServerConfigS2CPayload;
import com.hazeg7.sourcecraft.network.TimerActionC2SPayload;
import com.hazeg7.sourcecraft.network.TimerActionC2SPayload.Action;
import com.hazeg7.sourcecraft.network.TimerStateS2CPayload;
import com.hazeg7.sourcecraft.network.VelocitySyncS2CPayload;
import com.hazeg7.sourcecraft.physics.SourcePreset;
import com.hazeg7.sourcecraft.timer.TimerManager;
import com.hazeg7.sourcecraft.timer.TimerRecord;
import com.hazeg7.sourcecraft.timer.TimerStorage;
import com.hazeg7.sourcecraft.timer.TimerZone;
import com.hazeg7.sourcecraft.timer.TimerZone.ZoneType;
import io.netty.buffer.Unpooled;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceNetworkAndTimerTest {
	@Test
	void allSixNetworkPayloadsRoundTrip() {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

		ServerConfigS2CPayload s2cCfg = new ServerConfigS2CPayload(1, true, true, "css_surf", SourcePreset.CSS_SURF.buildCvars());
		ServerConfigS2CPayload.CODEC.encode(buf, s2cCfg);
		ServerConfigS2CPayload s2cOut = ServerConfigS2CPayload.CODEC.decode(buf);
		assertEquals(s2cCfg.enabled(), s2cOut.enabled());
		assertEquals(s2cCfg.serverEnforced(), s2cOut.serverEnforced());
		assertEquals(s2cCfg.presetId(), s2cOut.presetId());
		assertTrue(s2cCfg.cvars().matches(s2cOut.cvars()));

		ClientConfigRequestC2SPayload c2sCfg = new ClientConfigRequestC2SPayload(true, false, "css_kz", SourcePreset.CSS_KZ.buildCvars());
		ClientConfigRequestC2SPayload.CODEC.encode(buf, c2sCfg);
		ClientConfigRequestC2SPayload c2sOut = ClientConfigRequestC2SPayload.CODEC.decode(buf);
		assertEquals("css_kz", c2sOut.presetId());
		assertTrue(c2sCfg.cvars().matches(c2sOut.cvars()));

		VelocitySyncS2CPayload velIn = new VelocitySyncS2CPayload(1.25, 0.55, -0.75, "BOOST PAD!");
		VelocitySyncS2CPayload.CODEC.encode(buf, velIn);
		assertEquals(velIn, VelocitySyncS2CPayload.CODEC.decode(buf));

		TimerActionC2SPayload actIn = new TimerActionC2SPayload(Action.TP_CP, 3);
		TimerActionC2SPayload.CODEC.encode(buf, actIn);
		assertEquals(actIn, TimerActionC2SPayload.CODEC.decode(buf));

		TimerStateS2CPayload tsIn = new TimerStateS2CPayload(true, false, 14285L, 14010L, 275L, true, 2, 5, 4, "STAGE 2");
		TimerStateS2CPayload.CODEC.encode(buf, tsIn);
		assertEquals(tsIn, TimerStateS2CPayload.CODEC.decode(buf));

		PlayerModeC2SPayload pmIn = new PlayerModeC2SPayload(true, true);
		PlayerModeC2SPayload.CODEC.encode(buf, pmIn);
		assertEquals(pmIn, PlayerModeC2SPayload.CODEC.decode(buf));

		buf.release();
	}

	@Test
	void timerZonesStorageAndFormattingWork(@TempDir Path tempDir) {
		TimerZone start = TimerZone.fromCorners(1, "minecraft:overworld", ZoneType.START, 0,
			new BlockPos(0, 64, 0), new BlockPos(4, 66, 4));
		assertTrue(start.contains("minecraft:overworld", 2.5, 64.0, 2.5));
		assertFalse(start.contains("minecraft:the_nether", 2.5, 64.0, 2.5));
		assertFalse(start.contains("minecraft:overworld", 10.0, 64.0, 2.5));

		Path storeFile = tempDir.resolve("courses.json");
		TimerStorage.StoredCourseData data = new TimerStorage.StoredCourseData();
		data.zones.add(start);
		data.personalBestMs = 12345L;
		data.personalBestProMs = 12345L;
		data.records.add(new TimerRecord("hazeg7", "css_classic", 12345L, true, 0, 560.0, 94.5, List.of(4200L, 8100L), 999L));
		TimerStorage.save(storeFile, data);

		TimerStorage.StoredCourseData loaded = TimerStorage.load(storeFile);
		assertEquals(1, loaded.zones.size());
		assertEquals(12345L, loaded.personalBestMs);
		assertEquals("hazeg7", loaded.records.get(0).playerName());

		assertEquals("00:12.345", TimerManager.formatMillis(12345L));
		assertEquals("--:--.---", TimerManager.formatMillis(-1L));
		assertEquals("-0.250s", TimerManager.formatSignedDelta(-250L));
		assertEquals("+1.125s", TimerManager.formatSignedDelta(1125L));
	}
}
