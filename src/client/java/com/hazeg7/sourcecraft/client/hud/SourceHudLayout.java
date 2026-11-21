package com.hazeg7.sourcecraft.client.hud;

/**
 * Pure geometry & snap calculations for all 5 SourceCraft telemetry HUD modules.
 */
public final class SourceHudLayout {
	public enum ModuleId {
		SPEEDOMETER("Speedometer", 136, 36),
		STRAFE_TRAINER("Strafe Sync", 136, 20),
		KEYSTROKES("Keystrokes", 92, 62),
		VELOCITY_GRAPH("Velocity Graph", 126, 44),
		TIMER_BANNER("Run Timer", 156, 26);

		public final String label;
		public final int baseW;
		public final int baseH;

		ModuleId(String label, int baseW, int baseH) {
			this.label = label;
			this.baseW = baseW;
			this.baseH = baseH;
		}
	}

	public record HudBox(int x, int y, int width, int height, int baseW, int baseH, float scale) {
		public boolean contains(double px, double py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	private SourceHudLayout() {
	}

	public static HudBox computeBox(int screenW, int screenH, ModuleId id, float normX, float normY, float scale) {
		float safeScale = Math.max(0.65f, Math.min(1.65f, Float.isFinite(scale) ? scale : 1.0f));
		int maxAllowedW = Math.max(48, screenW - 12);
		int w = Math.min(maxAllowedW, Math.round(id.baseW * safeScale));
		float effectiveScale = (float) w / id.baseW;
		int h = Math.max(12, Math.round(id.baseH * effectiveScale));

		int availX = Math.max(0, screenW - w - 8);
		int availY = Math.max(0, screenH - h - 8);
		int x = 4 + Math.round(clamp01(normX) * availX);
		int y = 4 + Math.round(clamp01(normY) * availY);
		return new HudBox(x, y, w, h, id.baseW, id.baseH, effectiveScale);
	}

	public static float[] boxOriginToNorm(int x, int y, int screenW, int screenH, int w, int h) {
		int availX = Math.max(1, screenW - w - 8);
		int availY = Math.max(1, screenH - h - 8);
		float nx = clamp01((float) (x - 4) / availX);
		float ny = clamp01((float) (y - 4) / availY);
		return new float[] { nx, ny };
	}

	public static float[] snapNorm(float nx, float ny, float threshold) {
		float outX = nx;
		float outY = ny;
		if (Math.abs(nx - 0.5f) < threshold) {
			outX = 0.5f;
		} else if (nx < threshold) {
			outX = 0.0f;
		} else if (nx > 1.0f - threshold) {
			outX = 1.0f;
		}
		if (Math.abs(ny - 0.5f) < threshold) {
			outY = 0.5f;
		} else if (ny < threshold) {
			outY = 0.0f;
		} else if (ny > 1.0f - threshold) {
			outY = 1.0f;
		}
		return new float[] { outX, outY };
	}

	private static float clamp01(float v) {
		return Float.isFinite(v) ? Math.max(0.0f, Math.min(1.0f, v)) : 0.5f;
	}
}
