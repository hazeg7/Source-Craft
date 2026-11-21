package com.hazeg7.sourcecraft.timer;

import com.hazeg7.sourcecraft.block.TimerPadBlock;
import com.hazeg7.sourcecraft.network.SourceServerManager;
import com.hazeg7.sourcecraft.network.TimerActionC2SPayload.Action;
import com.hazeg7.sourcecraft.network.TimerStateS2CPayload;
import com.hazeg7.sourcecraft.network.VelocitySyncS2CPayload;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Manages KZ / Bhop / Surf run timers, cuboid zones, timer pads, personal bests, and checkpoint stacks.
 */
public final class TimerManager {
	public static final class PlayerSession {
		public final List<CheckpointData> checkpoints = new ArrayList<>();
		public int checkpointCursor = -1;
		public boolean restoreVelocityOnTp = true;

		public boolean armed = false;
		public boolean running = false;
		public long startTimeMs = 0L;
		public long finishTimeMs = -1L;
		public long lastDeltaMs = 0L;
		public int teleportsThisRun = 0;
		public int currentStage = 0;
		public double peakSpeedHu = 0.0;
		public final List<Long> stageSplitsMs = new ArrayList<>();

		public CheckpointData startSpawn = null;
		public boolean wasInStartZone = false;
		public long lastPadCooldownMs = 0L;
		public String calloutText = "";
		public long calloutUntilMs = 0L;

		public void clearCheckpoints() {
			checkpoints.clear();
			checkpointCursor = -1;
		}
	}

	private static final TimerManager INSTANCE = new TimerManager();
	private final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
	private TimerStorage.StoredCourseData courseData = new TimerStorage.StoredCourseData();
	private Path storagePath;
	private int nextZoneId = 1;
	private long tickCounter = 0L;

	private TimerManager() {
		try {
			this.storagePath = FabricLoader.getInstance().getConfigDir().resolve("sourcecraft-courses.json");
		} catch (Throwable ignored) {
			this.storagePath = Path.of("config", "sourcecraft-courses.json");
		}
		reloadFromDisk();
	}

	public static TimerManager get() {
		return INSTANCE;
	}

	public void setStoragePathForTests(Path path) {
		this.storagePath = path;
		this.courseData = TimerStorage.load(path);
		this.nextZoneId = courseData.zones.stream().mapToInt(TimerZone::id).max().orElse(0) + 1;
		this.sessions.clear();
	}

	public void reloadFromDisk() {
		this.courseData = TimerStorage.load(storagePath);
		this.nextZoneId = courseData.zones.stream().mapToInt(TimerZone::id).max().orElse(0) + 1;
	}

	private void saveToDisk() {
		TimerStorage.save(storagePath, courseData);
	}

	public PlayerSession session(UUID playerId) {
		return sessions.computeIfAbsent(playerId, id -> new PlayerSession());
	}

	public void removeSession(UUID playerId) {
		sessions.remove(playerId);
	}

	public synchronized TimerZone addZone(String dimensionId, TimerZone.ZoneType type, BlockPos a, BlockPos b) {
		int stageIdx = type == TimerZone.ZoneType.STAGE
			? (int) courseData.zones.stream().filter(z -> z.type() == TimerZone.ZoneType.STAGE).count() + 1
			: 0;
		TimerZone zone = TimerZone.fromCorners(nextZoneId++, dimensionId, type, stageIdx, a, b);
		courseData.zones.add(zone);
		saveToDisk();
		return zone;
	}

	public synchronized int clearZones() {
		int count = courseData.zones.size();
		courseData.zones.clear();
		nextZoneId = 1;
		saveToDisk();
		return count;
	}

	public List<TimerZone> zones() {
		return List.copyOf(courseData.zones);
	}

	public long personalBestMs() {
		return courseData.personalBestMs;
	}

	public long personalBestProMs() {
		return courseData.personalBestProMs;
	}

	public List<TimerRecord> records() {
		return List.copyOf(courseData.records);
	}

	public void onServerTick(MinecraftServer server) {
		tickCounter++;
		long nowMs = System.currentTimeMillis();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			checkZonesForPlayer(player, nowMs);
			if (tickCounter % 2 == 0) {
				pushTimerState(player, nowMs);
			}
		}
	}

	private void checkZonesForPlayer(ServerPlayer player, long nowMs) {
		if (courseData.zones.isEmpty()) {
			return;
		}
		PlayerSession s = session(player.getUUID());
		String dim = player.level().dimension().identifier().toString();
		double px = player.getX();
		double py = player.getY();
		double pz = player.getZ();

		boolean inStart = false;
		TimerZone hitEnd = null;
		TimerZone hitStage = null;

		for (TimerZone z : courseData.zones) {
			if (!z.contains(dim, px, py, pz)) {
				continue;
			}
			switch (z.type()) {
				case START -> {
					inStart = true;
					s.startSpawn = new CheckpointData(
						dim, z.centerX(), z.spawnY() + 0.05, z.centerZ(),
						player.getYRot(), player.getXRot(), 0, 0, 0, nowMs
					);
				}
				case END -> hitEnd = z;
				case STAGE -> hitStage = z;
			}
		}

		if (inStart) {
			armRun(s, player, nowMs);
			s.wasInStartZone = true;
		} else if (s.wasInStartZone) {
			s.wasInStartZone = false;
			if (s.armed && !s.running) {
				startRun(s, player, nowMs);
			}
		}

		if (s.running && hitStage != null && hitStage.stageIndex() > s.currentStage) {
			s.currentStage = hitStage.stageIndex();
			long elapsed = nowMs - s.startTimeMs;
			s.stageSplitsMs.add(elapsed);
			s.calloutText = String.format(Locale.ROOT, "STAGE %d · %s", hitStage.stageIndex(), formatMillis(elapsed));
			s.calloutUntilMs = nowMs + 2200L;
			player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.55f, 1.3f);
		}

		if (s.running && hitEnd != null) {
			finishRun(s, player, nowMs);
		}
	}

	public void onPlayerTouchPad(ServerPlayer player, BlockPos pos, TimerPadBlock.PadKind kind) {
		long nowMs = System.currentTimeMillis();
		PlayerSession s = session(player.getUUID());
		if (nowMs - s.lastPadCooldownMs < 500L) {
			return;
		}
		s.lastPadCooldownMs = nowMs;
		String dim = player.level().dimension().identifier().toString();

		switch (kind) {
			case START -> {
				s.startSpawn = new CheckpointData(
					dim, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
					player.getYRot(), player.getXRot(), 0, 0, 0, nowMs
				);
				startRun(s, player, nowMs);
				s.calloutText = "TIMER STARTED";
				s.calloutUntilMs = nowMs + 1800L;
				player.level().playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.65f, 1.25f);
			}
			case CHECKPOINT -> {
				saveCheckpoint(player, false);
				s.calloutText = "CHECKPOINT #" + s.checkpoints.size();
				s.calloutUntilMs = nowMs + 1500L;
				player.level().playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.55f, 1.4f);
			}
			case END -> {
				if (s.running) {
					finishRun(s, player, nowMs);
				} else {
					s.calloutText = "STEP ON START PAD FIRST";
					s.calloutUntilMs = nowMs + 1800L;
				}
			}
		}
		pushTimerState(player, nowMs);
	}

	public void armRun(PlayerSession s, ServerPlayer player, long nowMs) {
		s.armed = true;
		s.running = false;
		s.teleportsThisRun = 0;
		s.currentStage = 0;
		s.peakSpeedHu = 0.0;
		s.stageSplitsMs.clear();
		s.calloutText = "IN START ZONE";
		s.calloutUntilMs = nowMs + 800L;
	}

	public void startRun(PlayerSession s, ServerPlayer player, long nowMs) {
		s.armed = false;
		s.running = true;
		s.startTimeMs = nowMs;
		s.finishTimeMs = -1L;
		s.teleportsThisRun = 0;
		s.currentStage = 0;
		s.peakSpeedHu = 0.0;
		s.stageSplitsMs.clear();
	}

	public synchronized void finishRun(PlayerSession s, ServerPlayer player, long nowMs) {
		long elapsed = Math.max(1L, nowMs - s.startTimeMs);
		s.running = false;
		s.armed = false;
		s.finishTimeMs = elapsed;
		boolean pro = s.teleportsThisRun == 0;

		long prevPb = courseData.personalBestMs;
		s.lastDeltaMs = prevPb > 0 ? elapsed - prevPb : 0L;

		boolean newPb = prevPb < 0 || elapsed < prevPb;
		if (newPb) {
			courseData.personalBestMs = elapsed;
		}
		if (pro && (courseData.personalBestProMs < 0 || elapsed < courseData.personalBestProMs)) {
			courseData.personalBestProMs = elapsed;
		}

		TimerRecord rec = new TimerRecord(
			player.getPlainTextName(),
			SourceServerManager.getPresetId(),
			elapsed,
			pro,
			s.teleportsThisRun,
			s.peakSpeedHu,
			100.0,
			List.copyOf(s.stageSplitsMs),
			nowMs
		);
		courseData.records.add(rec);
		saveToDisk();

		String tag = pro ? "PRO" : "TP(" + s.teleportsThisRun + ")";
		String pbBanner = newPb ? " NEW PB!" : (prevPb > 0 ? " (" + formatSignedDelta(s.lastDeltaMs) + ")" : "");
		s.calloutText = formatMillis(elapsed) + " [" + tag + "]" + pbBanner;
		s.calloutUntilMs = nowMs + 3500L;

		player.sendSystemMessage(Component.literal(
			"§b[SourceCraft]§f Run complete: §a" + formatMillis(elapsed)
				+ " §7[" + tag + "]" + (newPb ? " §bNEW PB!" : pbBanner)
		));
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.35f);
		pushTimerState(player, nowMs);
	}

	public void handleAction(ServerPlayer player, Action action, int indexArg) {
		long nowMs = System.currentTimeMillis();
		switch (action) {
			case SAVE_CP -> saveCheckpoint(player, true);
			case TP_CP -> teleportCheckpoint(player, indexArg);
			case PREV_CP -> stepCheckpoint(player, -1);
			case NEXT_CP -> stepCheckpoint(player, +1);
			case RESTART -> restartRun(player);
			case TOGGLE_RESTORE_VEL -> {
				PlayerSession s = session(player.getUUID());
				s.restoreVelocityOnTp = !s.restoreVelocityOnTp;
				s.calloutText = "VEL RESTORE: " + (s.restoreVelocityOnTp ? "ON" : "OFF");
				s.calloutUntilMs = nowMs + 1500L;
			}
			case CLEAR_CP -> {
				PlayerSession s = session(player.getUUID());
				s.clearCheckpoints();
				s.calloutText = "CHECKPOINTS CLEARED";
				s.calloutUntilMs = nowMs + 1500L;
			}
		}
		pushTimerState(player, nowMs);
	}

	public void saveCheckpoint(ServerPlayer player, boolean notifyChat) {
		PlayerSession s = session(player.getUUID());
		var vel = player.getDeltaMovement();
		CheckpointData cp = new CheckpointData(
			player.level().dimension().identifier().toString(),
			player.getX(),
			player.getY(),
			player.getZ(),
			player.getYRot(),
			player.getXRot(),
			vel.x,
			vel.y,
			vel.z,
			System.currentTimeMillis()
		);
		s.checkpoints.add(cp);
		if (s.checkpoints.size() > 64) {
			s.checkpoints.remove(0);
		}
		s.checkpointCursor = s.checkpoints.size() - 1;
		s.calloutText = "SAVED CP #" + s.checkpoints.size();
		s.calloutUntilMs = System.currentTimeMillis() + 1400L;
		if (notifyChat) {
			player.sendSystemMessage(Component.literal("§b[SourceCraft]§f Saved checkpoint §3#" + s.checkpoints.size()));
		}
	}

	public boolean teleportCheckpoint(ServerPlayer player, int explicitIndex) {
		PlayerSession s = session(player.getUUID());
		if (s.checkpoints.isEmpty()) {
			player.sendSystemMessage(Component.literal("§c[SourceCraft]§f No checkpoints saved. Use /cp or press C."));
			return false;
		}
		int idx = explicitIndex >= 0 && explicitIndex < s.checkpoints.size()
			? explicitIndex
			: Math.max(0, Math.min(s.checkpoints.size() - 1, s.checkpointCursor));
		s.checkpointCursor = idx;
		CheckpointData cp = s.checkpoints.get(idx);
		applyTeleport(player, cp, s.restoreVelocityOnTp);
		if (s.running) {
			s.teleportsThisRun++;
		}
		s.calloutText = "TP CP #" + (idx + 1);
		s.calloutUntilMs = System.currentTimeMillis() + 1200L;
		return true;
	}

	public void stepCheckpoint(ServerPlayer player, int delta) {
		PlayerSession s = session(player.getUUID());
		if (s.checkpoints.isEmpty()) {
			return;
		}
		s.checkpointCursor = Math.floorMod(s.checkpointCursor + delta, s.checkpoints.size());
		teleportCheckpoint(player, s.checkpointCursor);
	}

	public void restartRun(ServerPlayer player) {
		PlayerSession s = session(player.getUUID());
		long nowMs = System.currentTimeMillis();
		if (s.startSpawn != null) {
			applyTeleport(player, s.startSpawn, false);
			armRun(s, player, nowMs);
			s.calloutText = "RESTARTED";
			s.calloutUntilMs = nowMs + 1200L;
		} else if (!s.checkpoints.isEmpty()) {
			applyTeleport(player, s.checkpoints.get(0), false);
			s.running = false;
			s.teleportsThisRun = 0;
			s.calloutText = "TELEPORTED TO CP #1";
			s.calloutUntilMs = nowMs + 1200L;
		} else {
			player.sendSystemMessage(Component.literal("§e[SourceCraft]§f No start zone/pad or checkpoint set yet."));
		}
	}

	private void applyTeleport(ServerPlayer player, CheckpointData cp, boolean restoreVel) {
		ServerLevel targetLevel = player.level();
		if (!targetLevel.dimension().identifier().toString().equals(cp.dimensionId())) {
			for (ServerLevel lvl : player.level().getServer().getAllLevels()) {
				if (lvl.dimension().identifier().toString().equals(cp.dimensionId())) {
					targetLevel = lvl;
					break;
				}
			}
		}
		SourceServerManager.recordPlayerImpulse(player.getUUID());
		player.resetFallDistance();
		player.teleportTo(targetLevel, cp.x(), cp.y(), cp.z(), Set.of(), cp.yaw(), cp.pitch(), false);

		double vx = restoreVel ? cp.vxBlocks() : 0.0;
		double vy = restoreVel ? cp.vyBlocks() : 0.0;
		double vz = restoreVel ? cp.vzBlocks() : 0.0;
		player.setDeltaMovement(vx, vy, vz);
		if (ServerPlayNetworking.canSend(player, VelocitySyncS2CPayload.TYPE)) {
			ServerPlayNetworking.send(player, new VelocitySyncS2CPayload(vx, vy, vz, restoreVel ? "CP VEL RESTORED" : ""));
		}
	}

	public void pushTimerState(ServerPlayer player, long nowMs) {
		if (!ServerPlayNetworking.canSend(player, TimerStateS2CPayload.TYPE)) {
			return;
		}
		PlayerSession s = session(player.getUUID());
		long elapsed = s.running ? Math.max(0L, nowMs - s.startTimeMs) : Math.max(0L, s.finishTimeMs);
		String callout = nowMs <= s.calloutUntilMs ? s.calloutText : "";
		TimerStateS2CPayload payload = new TimerStateS2CPayload(
			s.running,
			s.armed,
			elapsed,
			courseData.personalBestMs,
			s.lastDeltaMs,
			s.teleportsThisRun == 0,
			s.currentStage,
			s.checkpoints.size(),
			s.checkpointCursor,
			callout
		);
		ServerPlayNetworking.send(player, payload);
	}

	public static String formatMillis(long ms) {
		if (ms < 0L) {
			return "--:--.---";
		}
		long mins = ms / 60000L;
		long secs = (ms % 60000L) / 1000L;
		long rem = ms % 1000L;
		return String.format(Locale.ROOT, "%02d:%02d.%03d", mins, secs, rem);
	}

	public static String formatSignedDelta(long deltaMs) {
		if (deltaMs == 0L) {
			return "±0.000s";
		}
		String sign = deltaMs > 0 ? "+" : "-";
		return sign + String.format(Locale.ROOT, "%.3fs", Math.abs(deltaMs) / 1000.0);
	}
}
