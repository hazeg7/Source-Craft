package com.hazeg7.sourcecraft.physics;

/**
 * Persistent CS: Source movement simulation state for one player.
 * Velocities are stored natively in Hammer Units per second (HU/s).
 */
public final class SourceMoveState {
	public double vx;
	public double vy;
	public double vz;

	public double stamina;
	public double subStepAccumulator;
	public int jumpBufferTicksRemaining;

	public boolean wasOnGround;
	public boolean airCrouched;
	public double crouchFraction; // 0 = standing, 1 = full crouch

	public int jumpCount;
	public double preSpeedHu;
	public double peakSpeedHu;
	public double lastJumpTakeoffSpeedHu;

	public int airTicks;
	public int syncGoodSteps;
	public int syncTotalSteps;
	public double lastStrafeErrorDeg;
	public double lastOptimalAngleDeg;
	public boolean currentlySurfing;

	public double cameraRollDeg;
	public double landingPunchPitchDeg;

	public String lastTechCallout = "";
	public int calloutTicksRemaining;

	public boolean cancelNextFallDamage;
	public float prevYawDeg;
	public boolean hasPrevYaw;

	public void resetAll() {
		vx = 0.0;
		vy = 0.0;
		vz = 0.0;
		stamina = 0.0;
		subStepAccumulator = 0.0;
		jumpBufferTicksRemaining = 0;
		wasOnGround = false;
		airCrouched = false;
		crouchFraction = 0.0;
		jumpCount = 0;
		preSpeedHu = 0.0;
		peakSpeedHu = 0.0;
		lastJumpTakeoffSpeedHu = 0.0;
		airTicks = 0;
		syncGoodSteps = 0;
		syncTotalSteps = 0;
		lastStrafeErrorDeg = 0.0;
		lastOptimalAngleDeg = 0.0;
		currentlySurfing = false;
		cameraRollDeg = 0.0;
		landingPunchPitchDeg = 0.0;
		lastTechCallout = "";
		calloutTicksRemaining = 0;
		cancelNextFallDamage = false;
		hasPrevYaw = false;
	}

	public void setVelocityBlocksPerTick(double bx, double by, double bz, double huPerBlock) {
		double scale = huPerBlock * 20.0;
		this.vx = bx * scale;
		this.vy = by * scale;
		this.vz = bz * scale;
	}

	public double horizontalSpeedHu() {
		return Math.hypot(vx, vz);
	}

	public double speed3dHu() {
		return Math.sqrt(vx * vx + vy * vy + vz * vz);
	}

	public double syncPercent() {
		if (syncTotalSteps <= 0) {
			return 100.0;
		}
		return 100.0 * syncGoodSteps / syncTotalSteps;
	}

	public void triggerCallout(String text, int ticks) {
		this.lastTechCallout = text;
		this.calloutTicksRemaining = ticks;
	}
}
