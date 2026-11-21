package com.hazeg7.sourcecraft.timer;

import java.util.List;

/**
 * Completed run record persisted for personal-best tracking and split comparison.
 */
public record TimerRecord(
	String playerName,
	String presetId,
	long elapsedMs,
	boolean proRun,
	int checkpointsUsed,
	double peakSpeedHu,
	double avgSyncPct,
	List<Long> stageSplitsMs,
	long recordedAtMs
) {
}
