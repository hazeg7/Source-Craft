package com.hazeg7.sourcecraft.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hazeg7.sourcecraft.SourceCraftMod;
import com.hazeg7.sourcecraft.physics.SourceCvars;
import com.hazeg7.sourcecraft.physics.SourcePreset;
import com.hazeg7.sourcecraft.timer.TimerManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side authoritative CVar state, player activation flags, fall-damage tech exemptions, and sync.
 */
public final class SourceServerManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final class ServerConfigData {
		public boolean enabled = true;
		public boolean serverEnforced = false;
		public String presetId = SourcePreset.CSS_CLASSIC.id();
		public SourceCvars cvars = SourcePreset.CSS_CLASSIC.buildCvars();
	}

	private static final Map<UUID, Boolean> ACTIVE_PLAYERS = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> IMPULSE_GRACE_UNTIL_MS = new ConcurrentHashMap<>();
	private static final Map<UUID, Boolean> FALL_SAVE_FLAGS = new ConcurrentHashMap<>();

	private static ServerConfigData config = new ServerConfigData();
	private static Path configPath;

	private SourceServerManager() {
	}

	public static void init() {
		try {
			configPath = FabricLoader.getInstance().getConfigDir().resolve("sourcecraft-server.json");
		} catch (Throwable ignored) {
			configPath = Path.of("config", "sourcecraft-server.json");
		}
		loadConfig();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			ACTIVE_PLAYERS.put(player.getUUID(), config.enabled);
			if (ServerPlayNetworking.canSend(handler, ServerConfigS2CPayload.TYPE)) {
				sender.sendPacket(buildConfigPayload());
			}
			TimerManager.get().pushTimerState(player, System.currentTimeMillis());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			ACTIVE_PLAYERS.remove(id);
			IMPULSE_GRACE_UNTIL_MS.remove(id);
			FALL_SAVE_FLAGS.remove(id);
			TimerManager.get().removeSession(id);
		});

		ServerPlayNetworking.registerGlobalReceiver(ClientConfigRequestC2SPayload.TYPE, (payload, context) -> {
			ServerPlayer sender = context.player();
			MinecraftServer server = sender.level().getServer();
			if (!canModifyServerConfig(server, sender)) {
				// Re-send authoritative config to client if unauthorized
				if (ServerPlayNetworking.canSend(sender, ServerConfigS2CPayload.TYPE)) {
					ServerPlayNetworking.send(sender, buildConfigPayload());
				}
				return;
			}
			applyServerConfig(payload.enabled(), payload.serverEnforced(), payload.presetId(), payload.cvars(), server);
		});

		ServerPlayNetworking.registerGlobalReceiver(TimerActionC2SPayload.TYPE, (payload, context) -> {
			TimerManager.get().handleAction(context.player(), payload.action(), payload.indexArg());
		});

		ServerPlayNetworking.registerGlobalReceiver(PlayerModeC2SPayload.TYPE, (payload, context) -> {
			UUID id = context.player().getUUID();
			ACTIVE_PLAYERS.put(id, payload.movementActive() && config.enabled);
			if (payload.grantFallSave()) {
				FALL_SAVE_FLAGS.put(id, Boolean.TRUE);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> TimerManager.get().onServerTick(server));
	}

	public static boolean canModifyServerConfig(MinecraftServer server, ServerPlayer player) {
		if (server == null || player == null) {
			return false;
		}
		if (server.isSingleplayerOwner(player.nameAndId())) {
			return true;
		}
		return Commands.LEVEL_GAMEMASTERS.check(player.permissions());
	}

	public static synchronized void applyServerConfig(
		boolean enabled,
		boolean enforced,
		String presetId,
		SourceCvars newCvars,
		MinecraftServer server
	) {
		config.enabled = enabled;
		config.serverEnforced = enforced;
		config.cvars = (newCvars != null ? newCvars.copy() : SourceCvars.createDefault());
		config.cvars.sanitize();
		config.presetId = presetId != null && !presetId.isBlank()
			? presetId
			: SourcePreset.detectPresetId(config.cvars);
		saveConfig();
		broadcastConfig(server);
	}

	public static synchronized void broadcastConfig(MinecraftServer server) {
		if (server == null) {
			return;
		}
		ServerConfigS2CPayload packet = buildConfigPayload();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!config.enabled) {
				ACTIVE_PLAYERS.put(player.getUUID(), false);
			}
			if (ServerPlayNetworking.canSend(player, ServerConfigS2CPayload.TYPE)) {
				ServerPlayNetworking.send(player, packet);
			}
		}
	}

	public static ServerConfigS2CPayload buildConfigPayload() {
		return new ServerConfigS2CPayload(
			SourceCraftMod.PROTOCOL_VERSION,
			config.enabled,
			config.serverEnforced,
			config.presetId,
			config.cvars.copy()
		);
	}

	public static SourceCvars getCvars() {
		return config.cvars;
	}

	public static String getPresetId() {
		return config.presetId;
	}

	public static boolean isEnabled() {
		return config.enabled;
	}

	public static boolean isServerEnforced() {
		return config.serverEnforced;
	}

	public static boolean isActiveFor(ServerPlayer player) {
		if (player == null || !config.enabled) {
			return false;
		}
		if (ACTIVE_PLAYERS.getOrDefault(player.getUUID(), false)) {
			return true;
		}
		Long grace = IMPULSE_GRACE_UNTIL_MS.get(player.getUUID());
		return grace != null && System.currentTimeMillis() < grace;
	}

	public static void recordPlayerImpulse(UUID playerId) {
		IMPULSE_GRACE_UNTIL_MS.put(playerId, System.currentTimeMillis() + 1500L);
	}

	public static boolean consumeFallDamageExemption(ServerPlayer player) {
		if (player == null) {
			return false;
		}
		if (Boolean.TRUE.equals(FALL_SAVE_FLAGS.remove(player.getUUID()))) {
			return true;
		}
		return isActiveFor(player) && !config.cvars.svSurvivalFallDamage;
	}

	private static void loadConfig() {
		if (configPath == null || !Files.exists(configPath)) {
			config = new ServerConfigData();
			saveConfig();
			return;
		}
		try {
			String raw = Files.readString(configPath, StandardCharsets.UTF_8);
			ServerConfigData parsed = GSON.fromJson(raw, ServerConfigData.class);
			if (parsed != null) {
				if (parsed.cvars == null) {
					parsed.cvars = SourcePreset.CSS_CLASSIC.buildCvars();
				}
				parsed.cvars.sanitize();
				config = parsed;
			}
		} catch (Exception ignored) {
			config = new ServerConfigData();
		}
	}

	private static void saveConfig() {
		if (configPath == null) {
			return;
		}
		try {
			Path p = configPath.getParent();
			if (p != null) {
				Files.createDirectories(p);
			}
			Files.writeString(configPath, GSON.toJson(config), StandardCharsets.UTF_8);
		} catch (Exception ignored) {
		}
	}
}
