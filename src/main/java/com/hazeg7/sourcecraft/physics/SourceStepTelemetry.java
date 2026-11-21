package com.hazeg7.sourcecraft.physics;

/**
 * Per-tick diagnostic output returned by {@link SourceMovementEngine#stepTick}.
 */
public record SourceStepTelemetry(
	double displacementXBlocks,
	double displacementYBlocks,
	double displacementZBlocks,
	double horizontalSpeedHu,
	double speedDeltaHu,
	boolean jumpedThisTick,
	boolean landedThisTick,
	boolean surfingThisTick,
	boolean ladderThisTick,
	boolean waterJumpedThisTick,
	boolean crouchPulledFeetThisTick,
	boolean crouchReleasedDropThisTick,
	boolean edgebugTriggered,
	boolean jumpbugTriggered,
	int subStepsExecuted,
	double syncPercent,
	double strafeErrorDeg
) {
}
