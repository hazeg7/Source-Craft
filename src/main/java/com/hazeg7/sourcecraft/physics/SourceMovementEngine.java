package com.hazeg7.sourcecraft.physics;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java recreation of Valve Source Engine / CS: Source CGameMovement & CCSGameMovement.
 * Supports sub-stepped 66/100-tick integration within Minecraft's 20-TPS loop, sub-tick mouse yaw
 * interpolation, authentic AirAccelerate (wishspd clamped to sv_air_max_wishspeed=30), ClipVelocity
 * surfing, bhop friction-skip, Valve stamina, fast ladder strafing, water-jump, and edgebug/jumpbug.
 */
public final class SourceMovementEngine {
	public static final double MC_TICK_SECONDS = 0.05; // 20 TPS

	private SourceMovementEngine() {
	}

	public static double blocksPerTickToHuPerSec(double blocksPerTick, double huPerBlock) {
		return blocksPerTick * 20.0 * huPerBlock;
	}

	public static double huPerSecToBlocksPerTick(double huPerSec, double huPerBlock) {
		return huPerSec / (20.0 * huPerBlock);
	}

	/**
	 * Executes one 50ms Minecraft tick using sub-stepped Source ticks at {@code cvars.svTickrate}.
	 */
	public static SourceStepTelemetry stepTick(SourceMoveState state, SourceMoveInput input, SourceCvars cvars) {
		double prevHoriz = state.horizontalSpeedHu();
		if (state.calloutTicksRemaining > 0) {
			state.calloutTicksRemaining--;
			if (state.calloutTicksRemaining == 0) {
				state.lastTechCallout = "";
			}
		}

		// Jump input buffer for crisp manual bhop / mwheeldown
		if (input.jumpJustPressed) {
			state.jumpBufferTicksRemaining = Math.max(1, cvars.svJumpBufferTicks + 1);
		} else if (state.jumpBufferTicksRemaining > 0) {
			state.jumpBufferTicksRemaining--;
		}

		// Crouch transition & air crouch-jump foot-pull (+0.45 block / ~18 HU hull lift)
		boolean pullFeetUp = false;
		boolean releaseFeetDown = false;
		boolean wantsCrouch = input.crouchHeld || !input.hasStandingClearance;
		if (cvars.svCrouchJump) {
			double rate = 4.5 * MC_TICK_SECONDS;
			if (wantsCrouch) {
				if (!input.onGround && !state.airCrouched && state.crouchFraction < 0.5) {
					state.airCrouched = true;
					pullFeetUp = true;
				}
				state.crouchFraction = Math.min(1.0, state.crouchFraction + rate);
			} else {
				if (state.airCrouched && !input.onGround) {
					state.airCrouched = false;
					releaseFeetDown = true;
				} else if (input.onGround) {
					state.airCrouched = false;
				}
				state.crouchFraction = Math.max(0.0, state.crouchFraction - rate);
			}
		} else {
			state.crouchFraction = wantsCrouch ? 1.0 : 0.0;
			state.airCrouched = false;
		}

		boolean landedNow = !state.wasOnGround && input.onGround;
		boolean edgebugNow = false;
		boolean jumpbugNow = false;

		if (landedNow) {
			double dropSpeed = Math.abs(state.vy);
			if (dropSpeed > 260.0) {
				state.landingPunchPitchDeg = Math.min(4.5, (dropSpeed - 260.0) / 140.0);
			}
			if (cvars.svStaminaMax > 0.0) {
				state.stamina = Math.min(cvars.svStaminaMax, state.stamina + cvars.svStaminaLandCost * cvars.svMaxSpeed);
			}
			state.airTicks = 0;
			state.airCrouched = false;
		}

		// Edgebug check: falling fast while skimming edge of a solid block with no vertical block landing
		if (cvars.svEdgebugEnabled && !input.onGround && input.edgebugCandidate && state.vy < -340.0) {
			state.vy = -10.0;
			state.cancelNextFallDamage = true;
			edgebugNow = true;
			state.triggerCallout("EDGEBUG!", 30);
		}

		// Sub-step timing accumulator
		double stepDt = 1.0 / Math.max(20.0, cvars.svTickrate);
		state.subStepAccumulator += MC_TICK_SECONDS;
		int subSteps = (int) Math.floor((state.subStepAccumulator + 1e-9) / stepDt);
		subSteps = Math.max(1, Math.min(8, subSteps));
		state.subStepAccumulator -= subSteps * stepDt;
		if (state.subStepAccumulator < -stepDt) {
			state.subStepAccumulator = 0.0;
		}

		double dispHuX = 0.0;
		double dispHuY = 0.0;
		double dispHuZ = 0.0;
		boolean jumpedNow = false;
		boolean surfingNow = false;
		boolean ladderNow = false;
		boolean waterJumpNow = false;

		boolean simOnGround = input.onGround;

		for (int s = 0; s < subSteps; s++) {
			double t = (s + 0.5) / subSteps;
			float yaw = sampleYaw(input.yawSamples, input.currentYaw, t);
			float yawDelta = state.hasPrevYaw ? wrapAngleDeg(yaw - state.prevYawDeg) : 0.0f;
			state.prevYawDeg = yaw;
			state.hasPrevYaw = true;

			// Decay stamina & landing view kick
			if (state.stamina > 0.0) {
				state.stamina = Math.max(0.0, state.stamina - cvars.svStaminaRecoveryRate * stepDt);
			}
			state.landingPunchPitchDeg *= Math.exp(-16.0 * stepDt);

			double maxSpeed = cvars.svMaxSpeed * input.speedPotionMultiplier;
			if (cvars.svWeaponWeight) {
				maxSpeed *= input.heldItemMaxSpeedRatio;
			}
			if (state.stamina > 0.0 && cvars.svStaminaMax > 0.0) {
				double ratio = Math.max(0.25, 1.0 - state.stamina / 100.0);
				maxSpeed *= ratio;
			}
			if (wantsCrouch && simOnGround) {
				maxSpeed *= cvars.svDuckRatio;
			} else if (input.walkHeld && simOnGround) {
				maxSpeed *= cvars.svWalkRatio;
			}

			// Compute Source basis vectors in Minecraft's coordinate frame
			double yawRad = Math.toRadians(yaw);
			double fwdX = -Math.sin(yawRad);
			double fwdZ = Math.cos(yawRad);
			double rightX = -Math.cos(yawRad);
			double rightZ = -Math.sin(yawRad);

			// Wish vector
			double fmove = input.forwardMove * maxSpeed;
			double smove = input.sideMove * maxSpeed;
			double wishVelX = fwdX * fmove + rightX * smove;
			double wishVelZ = fwdZ * fmove + rightZ * smove;
			double wishSpeed = Math.hypot(wishVelX, wishVelZ);
			if (wishSpeed > maxSpeed && wishSpeed > 1e-6) {
				double scale = maxSpeed / wishSpeed;
				wishVelX *= scale;
				wishVelZ *= scale;
				wishSpeed = maxSpeed;
			}
			double wishDirX = wishSpeed > 1e-6 ? wishVelX / wishSpeed : 0.0;
			double wishDirZ = wishSpeed > 1e-6 ? wishVelZ / wishSpeed : 0.0;

			// Jumpbug check (uncrouch + jump within tiny clearance above ground while falling)
			boolean wantsJump = (cvars.svAutoBunnyhopping && input.jumpHeld) || state.jumpBufferTicksRemaining > 0;
			if (!simOnGround && wantsJump && input.jumpbugCandidate && state.vy < -220.0 && !input.crouchHeld) {
				jumpbugNow = true;
				simOnGround = true;
				state.cancelNextFallDamage = true;
				state.triggerCallout("JUMPBUG!", 30);
			}

			// Mode dispatch: Ladder -> Water -> Surf / Ground / Air
			if (input.onLadder) {
				ladderNow = true;
				stepLadder(state, input, cvars, fwdX, fwdZ, rightX, rightZ, maxSpeed, stepDt);
				if (wantsJump) {
					// Ladder push-off jump in CS:S
					state.vx = input.ladderNormalX * 270.0;
					state.vz = input.ladderNormalZ * 270.0;
					state.vy = Math.max(state.vy, cvars.svJumpImpulse * 0.85);
					state.jumpBufferTicksRemaining = 0;
					jumpedNow = true;
				}
			} else if (input.inWater) {
				boolean wj = stepWater(state, input, cvars, wishDirX, wishDirZ, wishSpeed, maxSpeed, stepDt);
				if (wj) {
					waterJumpNow = true;
					jumpedNow = true;
				}
			} else {
				// Ground / Air / Surf movement
				boolean jumpedThisSubStep = false;
				if (simOnGround && wantsJump) {
					applyJump(state, input, cvars, maxSpeed, landedNow && s == 0);
					simOnGround = false;
					jumpedThisSubStep = true;
					jumpedNow = true;
					state.jumpBufferTicksRemaining = 0;
					state.syncGoodSteps = 0;
					state.syncTotalSteps = 0;
				}

				if (simOnGround) {
					// Ground move: Friction -> Ground Accelerate (zero Y)
					applyFriction(state, cvars, stepDt, input.surfaceFriction);
					accelerate(state, wishDirX, wishDirZ, wishSpeed, cvars.svAccelerate, stepDt, input.surfaceFriction);
					if (state.vy < 0.0) {
						state.vy = 0.0;
					}
					state.currentlySurfing = false;
				} else {
					// Air / Surf move: Half-Gravity -> AirAccelerate -> Surf ClipVelocity -> Half-Gravity
					state.airTicks++;
					state.vy -= 0.5 * cvars.svGravity * stepDt;

					// AirAccelerate with clamped wishspd = sv_air_max_wishspeed (30)
					airAccelerate(state, wishDirX, wishDirZ, wishSpeed, cvars.svAirAccelerate, cvars.svAirMaxWishspeed, stepDt, input.surfaceFriction);

					// Surf ramp plane clip
					if (cvars.svSurfEnabled && input.touchingSurfPlane) {
						surfingNow = true;
						state.currentlySurfing = true;
						clipVelocity(state, input.surfNormalX, input.surfNormalY, input.surfNormalZ, cvars.svSurfOverbounce);
						if (!state.lastTechCallout.equals("EDGEBUG!") && !state.lastTechCallout.equals("JUMPBUG!")) {
							state.triggerCallout("SURFING", 4);
						}
					} else {
						state.currentlySurfing = false;
					}

					state.vy -= 0.5 * cvars.svGravity * stepDt;

					// Track strafe trainer analytics when actively air-strafing
					updateStrafeAnalytics(state, wishDirX, wishDirZ, wishSpeed, yawDelta, cvars.svAirMaxWishspeed, stepDt);
				}

				if (jumpedThisSubStep && state.preSpeedHu > 265.0) {
					state.triggerCallout(String.format("BHOP %.0f u/s", state.preSpeedHu), 18);
				}
			}

			// Clamp max velocity per axis (CS:S sv_maxvelocity = 3500 u/s)
			double vmax = cvars.svMaxVelocity;
			state.vx = clampAxis(state.vx, vmax);
			state.vy = clampAxis(state.vy, vmax);
			state.vz = clampAxis(state.vz, vmax);

			// Update subtle strafe camera roll target
			double targetRoll = simOnGround || input.onLadder ? 0.0 : clampAxis(input.sideMove * 1.4, 2.5);
			state.cameraRollDeg += (targetRoll - state.cameraRollDeg) * Math.min(1.0, 12.0 * stepDt);

			// Integrate displacement for this sub-step
			dispHuX += state.vx * stepDt;
			dispHuY += state.vy * stepDt;
			dispHuZ += state.vz * stepDt;
		}

		double horizNow = state.horizontalSpeedHu();
		if (horizNow > state.peakSpeedHu) {
			state.peakSpeedHu = horizNow;
		}
		state.wasOnGround = input.onGround && !jumpedNow;

		double invHu = 1.0 / cvars.svHuPerBlock;
		return new SourceStepTelemetry(
			dispHuX * invHu,
			dispHuY * invHu,
			dispHuZ * invHu,
			horizNow,
			horizNow - prevHoriz,
			jumpedNow,
			landedNow,
			surfingNow,
			ladderNow,
			waterJumpNow,
			pullFeetUp,
			releaseFeetDown,
			edgebugNow,
			jumpbugNow,
			subSteps,
			state.syncPercent(),
			state.lastStrafeErrorDeg
		);
	}

	private static void applyJump(SourceMoveState state, SourceMoveInput input, SourceCvars cvars, double maxSpeed, boolean immediateLandingHop) {
		// PreventBunnyJumping (Valve 2010 cap when sv_enablebunnyhopping == 0)
		if (!cvars.svEnableBunnyhopping) {
			double maxScaled = maxSpeed * 1.1;
			double horiz = state.horizontalSpeedHu();
			if (horiz > maxScaled && horiz > 1e-6) {
				double scale = maxScaled / horiz;
				state.vx *= scale;
				state.vz *= scale;
			}
		}

		state.preSpeedHu = state.horizontalSpeedHu();
		state.lastJumpTakeoffSpeedHu = state.preSpeedHu;
		state.jumpCount++;

		double jumpVel = cvars.svJumpImpulse + input.jumpBoostHuBonus;
		if (state.stamina > 0.0 && cvars.svStaminaMax > 0.0) {
			double ratio = Math.max(0.55, 1.0 - (state.stamina / cvars.svStaminaMax) * 0.25);
			jumpVel *= ratio;
		}
		state.vy = Math.max(state.vy, 0.0) + jumpVel;

		if (cvars.svStaminaMax > 0.0) {
			state.stamina = Math.min(cvars.svStaminaMax, state.stamina + cvars.svStaminaJumpCost * cvars.svMaxSpeed);
		}
		if (immediateLandingHop) {
			state.cancelNextFallDamage = false;
		}
	}

	public static void applyFriction(SourceMoveState state, SourceCvars cvars, double dt, double surfaceFriction) {
		double speed = state.horizontalSpeedHu();
		if (speed < 0.1) {
			state.vx = 0.0;
			state.vz = 0.0;
			return;
		}
		double control = speed < cvars.svStopspeed ? cvars.svStopspeed : speed;
		double drop = control * cvars.svFriction * dt * surfaceFriction;
		double newSpeed = speed - drop;
		if (newSpeed < 0.0) {
			newSpeed = 0.0;
		}
		if (newSpeed != speed) {
			double ratio = newSpeed / speed;
			state.vx *= ratio;
			state.vz *= ratio;
		}
	}

	public static void accelerate(
		SourceMoveState state,
		double wishDirX,
		double wishDirZ,
		double wishSpeed,
		double accel,
		double dt,
		double surfaceFriction
	) {
		double currentSpeed = state.vx * wishDirX + state.vz * wishDirZ;
		double addSpeed = wishSpeed - currentSpeed;
		if (addSpeed <= 0.0) {
			return;
		}
		double accelSpeed = accel * dt * wishSpeed * surfaceFriction;
		if (accelSpeed > addSpeed) {
			accelSpeed = addSpeed;
		}
		state.vx += accelSpeed * wishDirX;
		state.vz += accelSpeed * wishDirZ;
	}

	public static void airAccelerate(
		SourceMoveState state,
		double wishDirX,
		double wishDirZ,
		double wishSpeed,
		double airAccel,
		double airMaxWishspeed,
		double dt,
		double surfaceFriction
	) {
		double wishSpd = Math.min(wishSpeed, airMaxWishspeed);
		double currentSpeed = state.vx * wishDirX + state.vz * wishDirZ;
		double addSpeed = wishSpd - currentSpeed;
		if (addSpeed <= 0.0) {
			return;
		}
		// Authentic Source SDK: multiply accel by UNCLAMPED wishSpeed while capping addSpeed by wishSpd
		double accelSpeed = airAccel * wishSpeed * dt * surfaceFriction;
		if (accelSpeed > addSpeed) {
			accelSpeed = addSpeed;
		}
		state.vx += accelSpeed * wishDirX;
		state.vz += accelSpeed * wishDirZ;
	}

	/**
	 * Exact Valve ClipVelocity against surface unit normal (nx, ny, nz).
	 */
	public static void clipVelocity(SourceMoveState state, double nx, double ny, double nz, double overbounce) {
		double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (len < 1e-6) {
			return;
		}
		nx /= len;
		ny /= len;
		nz /= len;

		double backoff = state.vx * nx + state.vy * ny + state.vz * nz;
		if (backoff < 0.0) {
			backoff *= overbounce;
		} else {
			backoff /= overbounce;
		}

		state.vx -= nx * backoff;
		state.vy -= ny * backoff;
		state.vz -= nz * backoff;

		// Ensure no remaining inward velocity component into the plane
		double adjust = state.vx * nx + state.vy * ny + state.vz * nz;
		if (adjust < 0.0) {
			state.vx -= nx * adjust;
			state.vy -= ny * adjust;
			state.vz -= nz * adjust;
		}
	}

	private static void stepLadder(
		SourceMoveState state,
		SourceMoveInput input,
		SourceCvars cvars,
		double fwdX,
		double fwdZ,
		double rightX,
		double rightZ,
		double maxSpeed,
		double dt
	) {
		double ladderSpeed = maxSpeed * cvars.svLadderScaleSpeed;
		double fmove = input.forwardMove * ladderSpeed;
		double smove = input.sideMove * ladderSpeed;

		// Project forward/right wish vector onto ladder plane using Source's LadderMove math
		double nx = input.ladderNormalX;
		double nz = input.ladderNormalZ;
		double nLen = Math.hypot(nx, nz);
		if (nLen < 1e-6) {
			nx = 0.0;
			nz = 1.0;
		} else {
			nx /= nLen;
			nz /= nLen;
		}

		double perpX = -nz;
		double perpZ = nx;

		double lookPitchSin = Math.sin(Math.toRadians(input.currentPitch));
		double verticalLookScale = Math.abs(lookPitchSin) > 0.2 ? -Math.signum(lookPitchSin) : 1.0;
		double rawVx = fwdX * fmove + rightX * smove;
		double rawVz = fwdZ * fmove + rightZ * smove;

		// Fast ladder strafing: combining W + strafe adds full perpendicular + upward climb (~sqrt(2))
		double normalDot = rawVx * nx + rawVz * nz;
		double tang = rawVx * perpX + rawVz * perpZ;
		double climb = (-normalDot) * verticalLookScale;

		// If looking straight up/down while holding W+A or W+D, add side speed boost
		double targetVx = perpX * tang + nx * Math.max(0.0, normalDot * 0.25);
		double targetVz = perpZ * tang + nz * Math.max(0.0, normalDot * 0.25);
		double targetVy = climb;

		state.vx += (targetVx - state.vx) * Math.min(1.0, 20.0 * dt);
		state.vy += (targetVy - state.vy) * Math.min(1.0, 20.0 * dt);
		state.vz += (targetVz - state.vz) * Math.min(1.0, 20.0 * dt);
	}

	private static boolean stepWater(
		SourceMoveState state,
		SourceMoveInput input,
		SourceCvars cvars,
		double wishDirX,
		double wishDirZ,
		double wishSpeed,
		double maxSpeed,
		double dt
	) {
		double speed = state.speed3dHu();
		if (speed > 0.1) {
			double newSpeed = speed - dt * speed * cvars.svWaterFriction * input.surfaceFriction;
			if (newSpeed < 0.0) {
				newSpeed = 0.0;
			}
			double ratio = newSpeed / speed;
			state.vx *= ratio;
			state.vy *= ratio;
			state.vz *= ratio;
		}

		double waterWish = Math.min(wishSpeed, maxSpeed * 0.8);
		double current = state.vx * wishDirX + state.vz * wishDirZ;
		double add = waterWish - current;
		if (add > 0.0) {
			double accel = cvars.svWaterAccelerate * waterWish * dt * input.surfaceFriction;
			if (accel > add) {
				accel = add;
			}
			state.vx += accel * wishDirX;
			state.vz += accel * wishDirZ;
		}

		if (input.jumpHeld) {
			state.vy += 450.0 * dt;
			if (state.vy > 210.0) {
				state.vy = 210.0;
			}
			return input.jumpJustPressed;
		}
		state.vy -= 48.0 * dt;
		return false;
	}

	private static void updateStrafeAnalytics(
		SourceMoveState state,
		double wishDirX,
		double wishDirZ,
		double wishSpeed,
		float yawDeltaDeg,
		double airMaxWishspeed,
		double dt
	) {
		double horiz = state.horizontalSpeedHu();
		if (wishSpeed < 10.0 || horiz < 40.0) {
			state.lastStrafeErrorDeg = 0.0;
			return;
		}
		// Ideal wish-to-velocity angle theta where cos(theta) = airMaxWishspeed / horiz (for small gains: ~90 - asin(w/v))
		double sinIdeal = Math.min(1.0, airMaxWishspeed / Math.max(airMaxWishspeed, horiz));
		double idealAngleFromPerpDeg = Math.toDegrees(Math.asin(sinIdeal));
		state.lastOptimalAngleDeg = 90.0 - idealAngleFromPerpDeg;

		double dot = Math.max(-1.0, Math.min(1.0, (state.vx * wishDirX + state.vz * wishDirZ) / horiz));
		double actualAngleDeg = Math.toDegrees(Math.acos(dot));
		state.lastStrafeErrorDeg = actualAngleDeg - state.lastOptimalAngleDeg;

		state.syncTotalSteps++;
		boolean gaining = dot * horiz < airMaxWishspeed;
		boolean activelyTurning = Math.abs(yawDeltaDeg / Math.max(1e-4, dt)) > 15.0;
		if (gaining && (activelyTurning || Math.abs(state.lastStrafeErrorDeg) < 22.0)) {
			state.syncGoodSteps++;
		}
	}

	private static float sampleYaw(float[] samples, float fallbackYaw, double t) {
		if (samples == null || samples.length == 0) {
			return fallbackYaw;
		}
		if (samples.length == 1) {
			return samples[0];
		}
		double scaled = Math.max(0.0, Math.min(1.0, t)) * (samples.length - 1);
		int i0 = (int) Math.floor(scaled);
		int i1 = Math.min(samples.length - 1, i0 + 1);
		double frac = scaled - i0;
		float delta = wrapAngleDeg(samples[i1] - samples[i0]);
		return samples[i0] + (float) (delta * frac);
	}

	public static float wrapAngleDeg(float deg) {
		float a = deg % 360.0f;
		if (a >= 180.0f) {
			a -= 360.0f;
		} else if (a < -180.0f) {
			a += 360.0f;
		}
		return a;
	}

	private static double clampAxis(double val, double limit) {
		if (val > limit) {
			return limit;
		}
		if (val < -limit) {
			return -limit;
		}
		return val;
	}

	/**
	 * Lightweight 2D simulation point used by tests and the in-game Air-Strafe Visualizer.
	 */
	public record PreviewPoint(double xHu, double zHu, double speedHu, double yawDeg, double syncPct) {
	}

	/**
	 * Runs a clean 2D air-strafe loop preview for {@code mcTicks} ticks with the given CVars.
	 */
	public static List<PreviewPoint> simulateAirStrafePreview(SourceCvars cvars, int mcTicks) {
		SourceMoveState st = new SourceMoveState();
		st.vx = 0.0;
		st.vz = cvars.svMaxSpeed;
		st.vy = cvars.svJumpImpulse;

		List<PreviewPoint> points = new ArrayList<>(mcTicks + 1);
		double x = 0.0;
		double z = 0.0;
		float yaw = 0.0f;
		points.add(new PreviewPoint(x, z, st.horizontalSpeedHu(), yaw, 100.0));

		for (int t = 0; t < mcTicks; t++) {
			// Alternate left/right air-strafes every 10 ticks like real bhop strafing
			boolean strafeRight = (t / 10) % 2 == 0;
			double turnRateDegPerTick = strafeRight ? 16.0 : -16.0;
			int sub = 5;
			float[] samples = new float[sub];
			for (int i = 0; i < sub; i++) {
				samples[i] = yaw + (float) (turnRateDegPerTick * (i + 1) / sub);
			}
			yaw = samples[sub - 1];

			SourceMoveInput in = new SourceMoveInput();
			in.forwardMove = 0.0;
			in.sideMove = strafeRight ? 1.0 : -1.0;
			in.yawSamples = samples;
			in.currentYaw = yaw;
			in.onGround = false;
			in.jumpHeld = true;

			SourceStepTelemetry tel = stepTick(st, in, cvars);
			// Keep preview aloft in simulated bhop cycle
			if (st.vy < -cvars.svJumpImpulse) {
				st.vy = cvars.svJumpImpulse;
			}
			x += tel.displacementXBlocks() * cvars.svHuPerBlock;
			z += tel.displacementZBlocks() * cvars.svHuPerBlock;
			points.add(new PreviewPoint(x, z, st.horizontalSpeedHu(), yaw, tel.syncPercent()));
		}
		return points;
	}
}
