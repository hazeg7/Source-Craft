package com.hazeg7.sourcecraft.physics;

/**
 * Per-tick world/input snapshot fed into the pure CS: Source movement engine.
 */
public final class SourceMoveInput {
	/** +1 = W (forward), -1 = S (backward). */
	public double forwardMove;
	/** +1 = D (right), -1 = A (left) — matches CS:S sideMove convention. */
	public double sideMove;

	public boolean jumpHeld;
	public boolean jumpJustPressed;
	public boolean crouchHeld;
	public boolean walkHeld;

	/** Sub-tick yaw timeline in degrees across this tick (at least 1 sample = end yaw). */
	public float[] yawSamples = new float[] { 0.0f };
	public float currentYaw;
	public float currentPitch;

	public boolean onGround;
	public boolean inWater;
	public boolean onLadder;

	/** Outward normal from ladder block face (XZ unit vector). */
	public double ladderNormalX;
	public double ladderNormalZ = 1.0;

	/** Surf slope contact flag + unit plane normal when touching a ramp or stair slope. */
	public boolean touchingSurfPlane;
	public double surfNormalX;
	public double surfNormalY;
	public double surfNormalZ;

	public double surfaceFriction = 1.0;
	public double speedPotionMultiplier = 1.0;
	public double jumpBoostHuBonus = 0.0;
	public double heldItemMaxSpeedRatio = 1.0;

	/** True if standing up from crouch would fit inside world collision above feet. */
	public boolean hasStandingClearance = true;
	/** True when player bbox edge overhangs solid block face right before ground contact (edgebug). */
	public boolean edgebugCandidate = false;
	/** True if player is within 4 HU of ground with active jump/uncrouch timing (jumpbug). */
	public boolean jumpbugCandidate = false;
}
