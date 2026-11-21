package com.hazeg7.sourcecraft.client;

import com.hazeg7.sourcecraft.SourceCraftMod;
import com.hazeg7.sourcecraft.SourceMovementBridge;
import com.hazeg7.sourcecraft.block.SurfSlopeDetector;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.FovMode;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.ScrollJumpMode;
import com.hazeg7.sourcecraft.client.gui.SourceStudioScreen;
import com.hazeg7.sourcecraft.network.ClientConfigRequestC2SPayload;
import com.hazeg7.sourcecraft.network.PlayerModeC2SPayload;
import com.hazeg7.sourcecraft.network.ServerConfigS2CPayload;
import com.hazeg7.sourcecraft.network.TimerActionC2SPayload;
import com.hazeg7.sourcecraft.network.TimerActionC2SPayload.Action;
import com.hazeg7.sourcecraft.network.TimerStateS2CPayload;
import com.hazeg7.sourcecraft.network.VelocitySyncS2CPayload;
import com.hazeg7.sourcecraft.physics.ItemWeightTable;
import com.hazeg7.sourcecraft.physics.SourceCvars;
import com.hazeg7.sourcecraft.physics.SourceMoveInput;
import com.hazeg7.sourcecraft.physics.SourceMoveState;
import com.hazeg7.sourcecraft.physics.SourceMovementEngine;
import com.hazeg7.sourcecraft.physics.SourcePreset;
import com.hazeg7.sourcecraft.physics.SourceStepTelemetry;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side orchestrator connecting Minecraft 26.3 LocalPlayer input, camera feel,
 * sub-tick yaw sampling, server sync, and the pure CS: Source physics engine.
 */
public final class SourceClientController implements SourceMovementBridge.ClientMovementHandler {
	public enum NetSyncState {
		LOCAL("LOCAL", "Singleplayer / local physics"),
		SYNCED("SYNCED", "Synced with SourceCraft server"),
		ENFORCED("LOCKED", "Server-enforced CVars active"),
		VANILLA("VANILLA", "Unmodded server — fallback guard active");

		public final String tag;
		public final String detail;

		NetSyncState(String tag, String detail) {
			this.tag = tag;
			this.detail = detail;
		}
	}

	private static final SourceClientController INSTANCE = new SourceClientController();
	private static final int YAW_RING_CAP = 64;
	private static final int SPEED_HISTORY_LEN = 120;

	private final SourceMoveState moveState = new SourceMoveState();
	private final float[] yawBuffer = new float[YAW_RING_CAP];
	private int yawCount = 0;

	private final double[] speedHistory = new double[SPEED_HISTORY_LEN];
	private int speedHistoryCursor = 0;

	private boolean serverModPresent = false;
	private boolean serverEnabled = true;
	private boolean serverEnforced = false;
	private String serverPresetId = SourcePreset.CSS_CLASSIC.id();
	private SourceCvars serverCvars = SourcePreset.CSS_CLASSIC.buildCvars();

	private TimerStateS2CPayload timerState = new TimerStateS2CPayload(false, false, 0L, -1L, 0L, true, 0, 0, -1, "");
	private int scrollJumpTicks = 0;
	private boolean wasJumpDown = false;
	private boolean lastSentActive = false;
	private double lastSpeedDeltaHu = 0.0;
	private double lastMouseTurnDir = 0.0; // -1 left, 0 still, +1 right
	private float lastTrackedYaw = 0.0f;

	// Captured keystrokes for HUD display
	private boolean hudW, hudA, hudS, hudD, hudJump, hudDuck, hudWalk;
	private java.util.function.Supplier<net.minecraft.client.gui.screens.Screen> pendingScreen = null;

	private SourceClientController() {
	}

	public static SourceClientController get() {
		return INSTANCE;
	}

	public void init() {
		SourceMovementBridge.setClientHandler(this);
		registerNetworking();
	}

	private void registerNetworking() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			moveState.resetAll();
			clearSpeedHistory();
			serverModPresent = ClientPlayNetworking.canSend(ClientConfigRequestC2SPayload.TYPE);
			if (!serverModPresent) {
				SourceCraftMod.LOGGER.info("Connected to unmodded server — keeping safe local CS:S feel bounds.");
			}
			sendPlayerMode(isPhysicallyActive(client.player), false);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			serverModPresent = false;
			serverEnabled = true;
			serverEnforced = false;
			moveState.resetAll();
		});

		ClientPlayNetworking.registerGlobalReceiver(ServerConfigS2CPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				serverModPresent = true;
				serverEnabled = payload.enabled();
				serverEnforced = payload.serverEnforced();
				serverPresetId = payload.presetId();
				serverCvars = payload.cvars().copy();
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(VelocitySyncS2CPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				LocalPlayer player = context.client().player;
				if (player != null) {
					double hu = activeCvars().svHuPerBlock;
					moveState.setVelocityBlocksPerTick(payload.vxBlocks(), payload.vyBlocks(), payload.vzBlocks(), hu);
					player.setDeltaMovement(payload.vxBlocks(), payload.vyBlocks(), payload.vzBlocks());
					if (payload.calloutTag() != null && !payload.calloutTag().isEmpty()) {
						moveState.triggerCallout(payload.calloutTag(), 22);
					}
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(TimerStateS2CPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> timerState = payload);
		});
	}

	public void requestOpenScreen(java.util.function.Supplier<net.minecraft.client.gui.screens.Screen> factory) {
		this.pendingScreen = factory;
	}

	public void onEndClientTick(Minecraft mc) {
		if (pendingScreen != null && mc.gui.screen() == null) {
			var next = pendingScreen.get();
			pendingScreen = null;
			mc.gui.setScreen(next);
			return;
		}
		handleKeybinds(mc);

		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		boolean active = isPhysicallyActive(player);
		if (active != lastSentActive && ClientPlayNetworking.canSend(PlayerModeC2SPayload.TYPE)) {
			sendPlayerMode(active, false);
		}
		if (!active) {
			yawCount = 0;
			wasJumpDown = false;
		}
	}

	private void handleKeybinds(Minecraft mc) {
		SourceClientConfig cfg = SourceClientConfig.get();
		while (SourceKeybinds.OPEN_STUDIO.consumeClick()) {
			mc.gui.setScreen(new SourceStudioScreen(mc.gui.screen()));
		}
		while (SourceKeybinds.TOGGLE_ENGINE.consumeClick()) {
			cfg.modEnabled = !cfg.modEnabled;
			cfg.save();
			if (mc.player != null) {
				mc.player.sendOverlayMessage(Component.literal(
					"§b[SourceCraft]§f CS:S Physics " + (cfg.modEnabled ? "§aENABLED" : "§cDISABLED")
				));
			}
		}
		while (SourceKeybinds.TOGGLE_AUTOBHOP.consumeClick()) {
			if (!canEditCvars()) {
				notifyServerLocked(mc);
				continue;
			}
			SourceCvars next = activeCvars().copy();
			next.svAutoBunnyhopping = !next.svAutoBunnyhopping;
			applyEditedCvars(SourcePreset.detectPresetId(next), next);
			if (mc.player != null) {
				mc.player.sendOverlayMessage(Component.literal(
					"§b[SourceCraft]§f Auto-Bhop " + (next.svAutoBunnyhopping ? "§aON" : "§cOFF")
				));
			}
		}
		while (SourceKeybinds.CYCLE_PRESET.consumeClick()) {
			if (!canEditCvars()) {
				notifyServerLocked(mc);
				continue;
			}
			List<SourcePreset> all = SourcePreset.all();
			String curId = activePresetId();
			int idx = 0;
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).id().equals(curId)) {
					idx = (i + 1) % all.size();
					break;
				}
			}
			SourcePreset next = all.get(idx);
			applyEditedCvars(next.id(), next.buildCvars());
			if (mc.player != null) {
				mc.player.sendOverlayMessage(Component.literal(
					"§b[SourceCraft]§f Preset → §b" + next.name()
				));
			}
		}
		while (SourceKeybinds.CYCLE_HUD.consumeClick()) {
			cfg.hudMode = cfg.hudMode.next();
			cfg.save();
		}
		while (SourceKeybinds.SAVE_CHECKPOINT.consumeClick()) {
			sendTimerAction(Action.SAVE_CP, -1);
		}
		while (SourceKeybinds.TP_CHECKPOINT.consumeClick()) {
			sendTimerAction(Action.TP_CP, -1);
		}
		while (SourceKeybinds.RESTART_RUN.consumeClick()) {
			sendTimerAction(Action.RESTART, -1);
		}
	}

	private void notifyServerLocked(Minecraft mc) {
		if (mc.player != null) {
			mc.player.sendOverlayMessage(Component.literal("§c[SourceCraft]§f Server enforces CVars (OP required)."));
		}
	}

	public boolean onMouseScroll(double scrollDeltaY) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.screen() != null || !mc.mouseHandler.isMouseGrabbed()) {
			return false;
		}
		SourceClientConfig cfg = SourceClientConfig.get();
		if (!isPhysicallyActive(mc.player) || cfg.scrollJumpMode == ScrollJumpMode.OFF) {
			return false;
		}
		boolean match = switch (cfg.scrollJumpMode) {
			case DOWN -> scrollDeltaY < 0.0;
			case UP -> scrollDeltaY > 0.0;
			case BOTH -> Math.abs(scrollDeltaY) > 0.0;
			case OFF -> false;
		};
		if (match) {
			scrollJumpTicks = Math.max(1, activeCvars().svJumpBufferTicks + 1);
			return true; // Swallow scroll so hotbar doesn't swap slots during mwheel bhop
		}
		return false;
	}

	public void recordSubTickYaw(float yawDeg) {
		if (yawCount < YAW_RING_CAP) {
			yawBuffer[yawCount++] = yawDeg;
		} else {
			System.arraycopy(yawBuffer, 1, yawBuffer, 0, YAW_RING_CAP - 1);
			yawBuffer[YAW_RING_CAP - 1] = yawDeg;
		}
		float d = SourceMovementEngine.wrapAngleDeg(yawDeg - lastTrackedYaw);
		lastTrackedYaw = yawDeg;
		if (Math.abs(d) > 0.08f) {
			lastMouseTurnDir = Math.signum(d);
		}
	}

	public boolean canEditCvars() {
		if (!serverModPresent) {
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.hasSingleplayerServer()) {
			return true;
		}
		if (!serverEnforced) {
			return true;
		}
		return mc.player != null && mc.player.permissions() != null
			&& net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(mc.player.permissions());
	}

	public void applyEditedCvars(String presetId, SourceCvars cvars) {
		SourceClientConfig cfg = SourceClientConfig.get();
		cfg.activePresetId = presetId;
		cfg.localCvars = cvars.copy();
		cfg.save();

		if (serverModPresent && ClientPlayNetworking.canSend(ClientConfigRequestC2SPayload.TYPE)) {
			ClientPlayNetworking.send(new ClientConfigRequestC2SPayload(cfg.modEnabled, serverEnforced, presetId, cvars.copy()));
		}
	}

	public void sendTimerAction(Action action, int idx) {
		if (ClientPlayNetworking.canSend(TimerActionC2SPayload.TYPE)) {
			ClientPlayNetworking.send(new TimerActionC2SPayload(action, idx));
		}
	}

	private void sendPlayerMode(boolean active, boolean fallSave) {
		lastSentActive = active;
		if (ClientPlayNetworking.canSend(PlayerModeC2SPayload.TYPE)) {
			ClientPlayNetworking.send(new PlayerModeC2SPayload(active, fallSave));
		}
	}

	public SourceCvars activeCvars() {
		if (serverModPresent && (serverEnforced || !canEditCvars())) {
			return serverCvars;
		}
		return SourceClientConfig.get().localCvars;
	}

	public String activePresetId() {
		if (serverModPresent && serverEnforced) {
			return serverPresetId;
		}
		return SourceClientConfig.get().activePresetId;
	}

	public NetSyncState getSyncState() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null || mc.hasSingleplayerServer()) {
			return NetSyncState.LOCAL;
		}
		if (!serverModPresent) {
			return NetSyncState.VANILLA;
		}
		return serverEnforced ? NetSyncState.ENFORCED : NetSyncState.SYNCED;
	}

	public boolean isPhysicallyActive(Player player) {
		if (!(player instanceof LocalPlayer lp) || !lp.isLocalPlayer()) {
			return false;
		}
		SourceClientConfig cfg = SourceClientConfig.get();
		if (!cfg.modEnabled || (serverModPresent && !serverEnabled)) {
			return false;
		}
		if (getSyncState() == NetSyncState.VANILLA && !cfg.allowOnVanillaServers) {
			return false;
		}
		if (lp.isSpectator() || lp.getAbilities().flying || lp.isFallFlying() || lp.isPassenger() || lp.isInLava() || lp.isSwimming()) {
			return false;
		}
		return true;
	}

	@Override
	public boolean shouldSuppressVanillaJump(LivingEntity entity) {
		return entity instanceof Player p && isPhysicallyActive(p);
	}

	@Override
	public boolean isCrouchHullActive(Player player) {
		return isPhysicallyActive(player) && activeCvars().svCrouchJump;
	}

	@Override
	public boolean shouldCancelClientFallDamage(Player player) {
		if (!isPhysicallyActive(player)) {
			return false;
		}
		if (moveState.cancelNextFallDamage) {
			moveState.cancelNextFallDamage = false;
			return true;
		}
		return !activeCvars().svSurvivalFallDamage;
	}

	@Override
	public boolean handleTravel(Player player, Vec3 travelVector) {
		if (!(player instanceof LocalPlayer lp) || !isPhysicallyActive(lp)) {
			if (player instanceof LocalPlayer) {
				syncStateFromEntity(player);
			}
			return false;
		}

		SourceCvars cvars = activeCvars();
		double huPerBlock = cvars.svHuPerBlock;

		// Keep engine velocity synchronized if an external impulse (explosion, knockback, etc.) altered entity motion
		Vec3 entityVel = lp.getDeltaMovement();
		double expectedBx = SourceMovementEngine.huPerSecToBlocksPerTick(moveState.vx, huPerBlock);
		double expectedBy = SourceMovementEngine.huPerSecToBlocksPerTick(moveState.vy, huPerBlock);
		double expectedBz = SourceMovementEngine.huPerSecToBlocksPerTick(moveState.vz, huPerBlock);
		if (Math.abs(entityVel.x - expectedBx) > 0.14 || Math.abs(entityVel.z - expectedBz) > 0.14) {
			moveState.setVelocityBlocksPerTick(entityVel.x, entityVel.y, entityVel.z, huPerBlock);
		}

		SourceMoveInput in = buildMoveInput(lp, cvars);
		SourceStepTelemetry tel = SourceMovementEngine.stepTick(moveState, in, cvars);

		// Air crouch-jump foot-pull (raises feet +0.45 blocks if overhead is clear)
		if (tel.crouchPulledFeetThisTick()) {
			AABB lifted = lp.getBoundingBox().move(0.0, 0.45, 0.0);
			if (lp.level().noCollision(lp, lifted)) {
				lp.setPos(lp.getX(), lp.getY() + 0.45, lp.getZ());
			}
		} else if (tel.crouchReleasedDropThisTick()) {
			lp.move(MoverType.SELF, new Vec3(0.0, -0.45, 0.0));
		}

		// Execute world collision step with integrated sub-tick displacement
		Vec3 prevPos = lp.position();
		Vec3 desiredDisp = new Vec3(tel.displacementXBlocks(), tel.displacementYBlocks(), tel.displacementZBlocks());
		lp.setDeltaMovement(desiredDisp);
		lp.move(MoverType.SELF, desiredDisp);
		Vec3 actualDisp = lp.position().subtract(prevPos);

		// Reconcile state velocity against Minecraft block collisions
		if (lp.horizontalCollision) {
			if (in.touchingSurfPlane) {
				SourceMovementEngine.clipVelocity(moveState, in.surfNormalX, in.surfNormalY, in.surfNormalZ, cvars.svSurfOverbounce);
			} else {
				if (Math.abs(actualDisp.x - desiredDisp.x) > 1e-4 && Math.abs(desiredDisp.x) > 1e-5) {
					moveState.vx = 0.0;
				}
				if (Math.abs(actualDisp.z - desiredDisp.z) > 1e-4 && Math.abs(desiredDisp.z) > 1e-5) {
					moveState.vz = 0.0;
				}
			}
		}
		if (lp.verticalCollision) {
			if (lp.onGround() && moveState.vy < 0.0) {
				moveState.vy = 0.0;
			} else if (!lp.onGround() && desiredDisp.y > 0.0 && actualDisp.y < desiredDisp.y - 1e-4) {
				moveState.vy = 0.0; // Headbonk ceiling
			}
		}

		// If vanilla unmodded server safety fallback is needed, clamp displacement responsibly
		if (getSyncState() == NetSyncState.VANILLA) {
			double maxHorizPerTick = 1.9;
			double horiz = moveState.horizontalSpeedHu() / (20.0 * huPerBlock);
			if (horiz > maxHorizPerTick) {
				double scale = maxHorizPerTick / horiz;
				moveState.vx *= scale;
				moveState.vz *= scale;
			}
		}

		// Sync entity deltaMovement for vanilla interpolators & sound/fall distance
		double finalBx = SourceMovementEngine.huPerSecToBlocksPerTick(moveState.vx, huPerBlock);
		double finalBy = SourceMovementEngine.huPerSecToBlocksPerTick(moveState.vy, huPerBlock);
		double finalBz = SourceMovementEngine.huPerSecToBlocksPerTick(moveState.vz, huPerBlock);
		lp.setDeltaMovement(finalBx, finalBy, finalBz);

		// Reset Minecraft fallDistance during surfing / edgebugs / jumpbugs / zero-fall-damage mode
		if (tel.surfingThisTick() || tel.edgebugTriggered() || tel.jumpbugTriggered() || !cvars.svSurvivalFallDamage) {
			lp.resetFallDistance();
		}
		if (tel.edgebugTriggered() || tel.jumpbugTriggered()) {
			sendPlayerMode(true, true);
			if (SourceClientConfig.get().audioCues) {
				lp.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.45f, tel.edgebugTriggered() ? 1.65f : 1.45f);
			}
		}
		if (tel.jumpedThisTick() && moveState.preSpeedHu > 265.0 && SourceClientConfig.get().audioCues) {
			lp.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.22f, 1.35f);
		}

		// Update HUD telemetry history
		lastSpeedDeltaHu = tel.speedDeltaHu();
		pushSpeedSample(moveState.horizontalSpeedHu());
		if (Math.abs(SourceMovementEngine.wrapAngleDeg(lp.getYRot() - lastTrackedYaw)) < 0.02f) {
			lastMouseTurnDir *= 0.65;
		}
		return true;
	}

	private SourceMoveInput buildMoveInput(LocalPlayer lp, SourceCvars cvars) {
		Vec2 rawMove = lp.input.getMoveVector();
		var keys = lp.input.keyPresses;

		boolean jumpDown = keys.jump() || scrollJumpTicks > 0;
		boolean jumpJust = (keys.jump() && !wasJumpDown) || scrollJumpTicks > 0;
		wasJumpDown = keys.jump();
		if (scrollJumpTicks > 0) {
			scrollJumpTicks--;
		}

		boolean crouchHeld = keys.shift();
		boolean walkHeld = SourceKeybinds.SLOW_WALK.isDown();

		hudW = keys.forward();
		hudS = keys.backward();
		hudA = keys.left();
		hudD = keys.right();
		hudJump = jumpDown;
		hudDuck = crouchHeld;
		hudWalk = walkHeld;

		SourceMoveInput in = new SourceMoveInput();
		in.forwardMove = rawMove.y;
		in.sideMove = -rawMove.x; // MC left=+1, Source sideMove right=+1
		in.jumpHeld = jumpDown;
		in.jumpJustPressed = jumpJust;
		in.crouchHeld = crouchHeld;
		in.walkHeld = walkHeld;
		in.currentYaw = lp.getYRot();
		in.currentPitch = lp.getXRot();

		if (yawCount > 0) {
			float[] samples = new float[yawCount];
			System.arraycopy(yawBuffer, 0, samples, 0, yawCount);
			in.yawSamples = samples;
			yawCount = 0;
		} else {
			in.yawSamples = new float[] { lp.getYRot() };
		}

		in.inWater = lp.isInWater();
		in.onLadder = lp.onClimbable();
		if (in.onLadder) {
			Vec3 ln = SurfSlopeDetector.detectLadderNormal(lp);
			in.ladderNormalX = ln.x;
			in.ladderNormalZ = ln.z;
		}

		SurfSlopeDetector.SlopeProbe probe = SurfSlopeDetector.detectSurfSlope(lp, cvars.svSurfEnabled, moveState.horizontalSpeedHu());
		in.touchingSurfPlane = probe.touchingSurf();
		in.surfNormalX = probe.surfNormal().x;
		in.surfNormalY = probe.surfNormal().y;
		in.surfNormalZ = probe.surfNormal().z;

		// Treat active surf contacts (custom ramps or high-speed stair surf) as airborne slope contacts
		boolean surfingSteep = probe.touchingSurf() && (probe.isCustomRamp() || !lp.onGround() || moveState.horizontalSpeedHu() > 265.0);
		in.onGround = lp.onGround() && !surfingSteep;

		// Surface friction from block directly beneath feet
		BlockPos under = BlockPos.containing(lp.getX(), lp.getBoundingBox().minY - 0.08, lp.getZ());
		BlockState underState = lp.level().getBlockState(under);
		float blockFric = underState.getBlock().getFriction();
		in.surfaceFriction = blockFric >= 0.89f ? 0.25 : (blockFric >= 0.75f ? 0.65 : 1.0);

		// Potion & held-item modifiers
		MobEffectInstance speedEff = lp.getEffect(MobEffects.SPEED);
		in.speedPotionMultiplier = speedEff != null ? (1.0 + 0.12 * (speedEff.getAmplifier() + 1)) : 1.0;
		MobEffectInstance jumpEff = lp.getEffect(MobEffects.JUMP_BOOST);
		in.jumpBoostHuBonus = jumpEff != null ? (60.0 * (jumpEff.getAmplifier() + 1)) : 0.0;

		var itemKey = BuiltInRegistries.ITEM.getKey(lp.getMainHandItem().getItem());
		double weighted = ItemWeightTable.maxSpeedForItemKey(itemKey.getPath(), cvars.svMaxSpeed);
		in.heldItemMaxSpeedRatio = weighted / Math.max(1.0, cvars.svMaxSpeed);

		// Standing clearance check when crouched
		AABB standingHull = Pose.STANDING.equals(lp.getPose())
			? lp.getBoundingBox()
			: new AABB(lp.getX() - 0.3, lp.getY(), lp.getZ() - 0.3, lp.getX() + 0.3, lp.getY() + 1.80, lp.getZ() + 0.3);
		in.hasStandingClearance = lp.level().noCollision(lp, standingHull);

		double velBy = SourceMovementEngine.huPerSecToBlocksPerTick(moveState.vy, cvars.svHuPerBlock);
		in.edgebugCandidate = SurfSlopeDetector.isEdgebugCandidate(lp, velBy);
		in.jumpbugCandidate = SurfSlopeDetector.isJumpbugCandidate(lp, velBy);
		return in;
	}

	private void syncStateFromEntity(Player player) {
		Vec3 v = player.getDeltaMovement();
		moveState.setVelocityBlocksPerTick(v.x, v.y, v.z, activeCvars().svHuPerBlock);
		moveState.wasOnGround = player.onGround();
	}

	private void pushSpeedSample(double horizHu) {
		speedHistory[speedHistoryCursor] = horizHu;
		speedHistoryCursor = (speedHistoryCursor + 1) % SPEED_HISTORY_LEN;
	}

	private void clearSpeedHistory() {
		for (int i = 0; i < SPEED_HISTORY_LEN; i++) {
			speedHistory[i] = 0.0;
		}
		speedHistoryCursor = 0;
	}

	public float getStrafeRollRadians() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !isPhysicallyActive(mc.player)) {
			return 0.0f;
		}
		double maxDeg = SourceClientConfig.get().strafeRollDegrees;
		if (maxDeg <= 0.01) {
			return 0.0f;
		}
		double scaled = moveState.cameraRollDeg * (maxDeg / 1.4);
		return (float) Math.toRadians(scaled);
	}

	public float getLandingKickPitchDeg() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !isPhysicallyActive(mc.player)) {
			return 0.0f;
		}
		return (float) (moveState.landingPunchPitchDeg * SourceClientConfig.get().landingKickScale);
	}

	public boolean shouldOverrideFov(AbstractClientPlayer player, boolean firstPerson) {
		return firstPerson && player instanceof LocalPlayer lp && isPhysicallyActive(lp);
	}

	public float computeFovModifier(float vanillaValue, boolean firstPerson, float fovScale) {
		FovMode mode = SourceClientConfig.get().fovMode;
		if (mode == FovMode.OFF) {
			return 1.0f;
		}
		double speed = moveState.horizontalSpeedHu();
		double over = Math.max(0.0, speed - 250.0);
		double maxExtra = mode == FovMode.DYNAMIC ? 0.16 : 0.075;
		double frac = Math.min(1.0, over / 900.0);
		return (float) (1.0 + maxExtra * frac * Math.max(0.25f, fovScale));
	}

	public SourceMoveState getMoveState() {
		return moveState;
	}

	public TimerStateS2CPayload getTimerState() {
		return timerState;
	}

	public double getLastSpeedDeltaHu() {
		return lastSpeedDeltaHu;
	}

	public double getLastMouseTurnDir() {
		return lastMouseTurnDir;
	}

	public boolean isHudW() { return hudW; }
	public boolean isHudA() { return hudA; }
	public boolean isHudS() { return hudS; }
	public boolean isHudD() { return hudD; }
	public boolean isHudJump() { return hudJump; }
	public boolean isHudDuck() { return hudDuck; }
	public boolean isHudWalk() { return hudWalk; }

	public double[] getOrderedSpeedHistory() {
		double[] out = new double[SPEED_HISTORY_LEN];
		for (int i = 0; i < SPEED_HISTORY_LEN; i++) {
			out[i] = speedHistory[(speedHistoryCursor + i) % SPEED_HISTORY_LEN];
		}
		return out;
	}
}
