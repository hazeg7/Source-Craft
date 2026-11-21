package com.hazeg7.sourcecraft.client.hud;

import com.hazeg7.sourcecraft.client.SourceClientController;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.HudMode;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.SpeedUnit;
import com.hazeg7.sourcecraft.client.gui.SourceHudEditorScreen;
import com.hazeg7.sourcecraft.client.gui.SourceTheme;
import com.hazeg7.sourcecraft.client.hud.SourceHudLayout.HudBox;
import com.hazeg7.sourcecraft.client.hud.SourceHudLayout.ModuleId;
import com.hazeg7.sourcecraft.network.TimerStateS2CPayload;
import com.hazeg7.sourcecraft.physics.SourceMoveState;
import com.hazeg7.sourcecraft.timer.TimerManager;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Renders the CS:S telemetry HUD modules (Speedometer, Strafe Sync, Keystrokes, Velocity Graph, Run Timer).
 */
public final class SourceHudRenderer {
	private SourceHudRenderer() {
	}

	public static void renderInWorldHud(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		if (mc.gui.screen() instanceof SourceHudEditorScreen) {
			return;
		}
		SourceClientConfig cfg = SourceClientConfig.get();
		if (cfg.hudMode == HudMode.OFF) {
			return;
		}
		renderAllModules(
			g,
			mc.font,
			mc.getWindow().getGuiScaledWidth(),
			mc.getWindow().getGuiScaledHeight(),
			cfg,
			SourceClientController.get(),
			false
		);
	}

	public static void renderAllModules(
		GuiGraphicsExtractor g,
		Font font,
		int screenW,
		int screenH,
		SourceClientConfig cfg,
		SourceClientController ctrl,
		boolean editorPreview
	) {
		int alpha = Math.max(40, Math.min(255, Math.round(cfg.hudOpacity * 255.0f)));
		boolean compact = cfg.hudMode == HudMode.COMPACT;

		if (cfg.showSpeedometer) {
			renderSpeedometer(g, font, screenW, screenH, cfg, ctrl, alpha, editorPreview);
		}
		if (!compact && cfg.showStrafeTrainer) {
			renderStrafeTrainer(g, font, screenW, screenH, cfg, ctrl, alpha, editorPreview);
		}
		if (!compact && cfg.showKeystrokes) {
			renderKeystrokes(g, font, screenW, screenH, cfg, ctrl, alpha, editorPreview);
		}
		if (!compact && cfg.showVelocityGraph) {
			renderVelocityGraph(g, font, screenW, screenH, cfg, ctrl, alpha, editorPreview);
		}
		if (cfg.showTimerBanner && (editorPreview || shouldShowTimer(ctrl.getTimerState()))) {
			renderTimerBanner(g, font, screenW, screenH, cfg, ctrl, alpha, editorPreview);
		}
	}

	private static boolean shouldShowTimer(TimerStateS2CPayload ts) {
		return ts.running() || ts.armed() || ts.checkpointCount() > 0 || ts.personalBestMs() > 0L
			|| (ts.calloutText() != null && !ts.calloutText().isEmpty());
	}

	public static HudBox renderModuleForEditor(
		GuiGraphicsExtractor g,
		Font font,
		int screenW,
		int screenH,
		ModuleId id,
		SourceClientConfig cfg,
		SourceClientController ctrl,
		int alpha
	) {
		return switch (id) {
			case SPEEDOMETER -> renderSpeedometer(g, font, screenW, screenH, cfg, ctrl, alpha, true);
			case STRAFE_TRAINER -> renderStrafeTrainer(g, font, screenW, screenH, cfg, ctrl, alpha, true);
			case KEYSTROKES -> renderKeystrokes(g, font, screenW, screenH, cfg, ctrl, alpha, true);
			case VELOCITY_GRAPH -> renderVelocityGraph(g, font, screenW, screenH, cfg, ctrl, alpha, true);
			case TIMER_BANNER -> renderTimerBanner(g, font, screenW, screenH, cfg, ctrl, alpha, true);
		};
	}

	public static HudBox renderSpeedometer(
		GuiGraphicsExtractor g,
		Font font,
		int screenW,
		int screenH,
		SourceClientConfig cfg,
		SourceClientController ctrl,
		int alpha,
		boolean preview
	) {
		HudBox box = SourceHudLayout.computeBox(screenW, screenH, ModuleId.SPEEDOMETER, cfg.speedNormX, cfg.speedNormY, cfg.hudScale);
		SourceMoveState st = ctrl.getMoveState();
		double huPerBlock = ctrl.activeCvars().svHuPerBlock;
		double rawSpeed = preview ? 482.0 : st.horizontalSpeedHu();
		double delta = preview ? +14.0 : ctrl.getLastSpeedDeltaHu();

		String speedStr = cfg.speedUnit == SpeedUnit.HU_PER_SEC
			? String.format(Locale.ROOT, "%.0f u/s", rawSpeed)
			: String.format(Locale.ROOT, "%.2f b/s", rawSpeed / Math.max(1.0, huPerBlock));

		int speedCol;
		if (delta > 0.8) {
			speedCol = SourceTheme.GAIN_MINT;
		} else if (delta < -0.8) {
			speedCol = SourceTheme.LOSS_CORAL;
		} else {
			speedCol = SourceTheme.NEUTRAL_ICE;
		}

		beginBox(g, box);
		int bg = SourceTheme.withAlpha(SourceTheme.SURFACE_PRIMARY, Math.round(alpha * 0.78f));
		int border = SourceTheme.withAlpha(SourceTheme.BORDER_SUBTLE, Math.round(alpha * 0.85f));
		SourceTheme.drawPanel(g, 0, 0, box.baseW(), box.baseH(), bg, border);

		int sw = font.width(speedStr);
		g.text(font, speedStr, (box.baseW() - sw) / 2, 6, SourceTheme.withAlpha(speedCol, alpha), false);

		String sub = String.format(
			Locale.ROOT,
			"PRE %.0f · PK %.0f · J%d",
			preview ? 286.0 : st.preSpeedHu,
			preview ? 640.0 : st.peakSpeedHu,
			preview ? 4 : st.jumpCount
		);
		int subW = font.width(sub);
		g.text(font, sub, (box.baseW() - subW) / 2, 18, SourceTheme.withAlpha(SourceTheme.TEXT_SECONDARY, alpha), false);

		String callout = preview ? "BHOP 482 u/s" : st.lastTechCallout;
		if (callout != null && !callout.isEmpty()) {
			int cw = font.width(callout);
			g.text(font, callout, (box.baseW() - cw) / 2, -11, SourceTheme.withAlpha(SourceTheme.ACCENT, alpha), false);
		}
		endBox(g);
		return box;
	}

	public static HudBox renderStrafeTrainer(
		GuiGraphicsExtractor g,
		Font font,
		int screenW,
		int screenH,
		SourceClientConfig cfg,
		SourceClientController ctrl,
		int alpha,
		boolean preview
	) {
		HudBox box = SourceHudLayout.computeBox(screenW, screenH, ModuleId.STRAFE_TRAINER, cfg.strafeNormX, cfg.strafeNormY, cfg.hudScale);
		SourceMoveState st = ctrl.getMoveState();
		double syncPct = preview ? 92.4 : st.syncPercent();
		double errDeg = preview ? 2.5 : st.lastStrafeErrorDeg;

		beginBox(g, box);
		int bg = SourceTheme.withAlpha(SourceTheme.SURFACE_PRIMARY, Math.round(alpha * 0.76f));
		int border = SourceTheme.withAlpha(SourceTheme.BORDER_SUBTLE, Math.round(alpha * 0.85f));
		SourceTheme.drawPanel(g, 0, 0, box.baseW(), box.baseH(), bg, border);

		String syncStr = String.format(Locale.ROOT, "SYNC %.0f%%", syncPct);
		int syncCol = syncPct >= 82.0 ? SourceTheme.GAIN_MINT : (syncPct >= 55.0 ? SourceTheme.ACCENT : SourceTheme.LOSS_CORAL);
		g.text(font, syncStr, 6, 4, SourceTheme.withAlpha(syncCol, alpha), false);

		String advice = Math.abs(errDeg) < 6.0 ? "OPTIMAL" : (errDeg < 0 ? "TURN MORE" : "OVERSTRAFE");
		g.text(font, advice, box.baseW() - font.width(advice) - 6, 4, SourceTheme.withAlpha(SourceTheme.TEXT_SECONDARY, alpha), false);

		// Gauge bar with center sweet-spot target
		int barX = 6;
		int barY = 14;
		int barW = box.baseW() - 12;
		g.fill(barX, barY, barX + barW, barY + 3, SourceTheme.withAlpha(SourceTheme.SURFACE_CONTROL, alpha));
		int center = barX + barW / 2;
		g.fill(center - 10, barY, center + 10, barY + 3, SourceTheme.withAlpha(0xFF164E63, alpha));

		double clampedErr = Math.max(-35.0, Math.min(35.0, errDeg));
		int pipX = center + (int) Math.round((clampedErr / 35.0) * (barW / 2 - 3));
		int pipCol = Math.abs(errDeg) < 6.0 ? SourceTheme.GAIN_MINT : SourceTheme.ACCENT;
		g.fill(pipX - 2, barY - 1, pipX + 3, barY + 4, SourceTheme.withAlpha(pipCol, alpha));
		endBox(g);
		return box;
	}

	public static HudBox renderKeystrokes(
		GuiGraphicsExtractor g,
		Font font,
		int screenW,
		int screenH,
		SourceClientConfig cfg,
		SourceClientController ctrl,
		int alpha,
		boolean preview
	) {
		HudBox box = SourceHudLayout.computeBox(screenW, screenH, ModuleId.KEYSTROKES, cfg.keysNormX, cfg.keysNormY, cfg.hudScale);
		beginBox(g, box);

		int bg = SourceTheme.withAlpha(SourceTheme.SURFACE_PRIMARY, Math.round(alpha * 0.72f));
		int border = SourceTheme.withAlpha(SourceTheme.BORDER_SUBTLE, Math.round(alpha * 0.85f));
		SourceTheme.drawPanel(g, 0, 0, box.baseW(), box.baseH(), bg, border);

		boolean w = preview || ctrl.isHudW();
		boolean a = !preview && ctrl.isHudA();
		boolean s = !preview && ctrl.isHudS();
		boolean d = preview || ctrl.isHudD();
		boolean jmp = preview || ctrl.isHudJump();
		boolean dck = !preview && ctrl.isHudDuck();
		boolean wlk = !preview && ctrl.isHudWalk();
		double turn = preview ? 1.0 : ctrl.getLastMouseTurnDir();

		drawKeyCap(g, font, 34, 4, 24, 16, "W", w, alpha);
		drawKeyCap(g, font, 6, 22, 24, 16, "A", a, alpha);
		drawKeyCap(g, font, 34, 22, 24, 16, "S", s, alpha);
		drawKeyCap(g, font, 62, 22, 24, 16, "D", d, alpha);

		// Turn arrows flanking W
		g.text(font, "<", 14, 8, SourceTheme.withAlpha(turn < -0.2 ? SourceTheme.ACCENT : SourceTheme.TEXT_MUTED, alpha), false);
		g.text(font, ">", 72, 8, SourceTheme.withAlpha(turn > 0.2 ? SourceTheme.ACCENT : SourceTheme.TEXT_MUTED, alpha), false);

		drawKeyCap(g, font, 6, 41, 38, 16, "JMP", jmp, alpha);
		drawKeyCap(g, font, 47, 41, 19, 16, "DCK", dck, alpha);
		drawKeyCap(g, font, 68, 41, 18, 16, "WLK", wlk, alpha);

		endBox(g);
		return box;
	}

	private static void drawKeyCap(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, String label, boolean down, int alpha) {
		int bg = down ? SourceTheme.withAlpha(SourceTheme.ACCENT, Math.round(alpha * 0.90f))
			: SourceTheme.withAlpha(SourceTheme.SURFACE_CONTROL, alpha);
		int border = down ? SourceTheme.withAlpha(SourceTheme.ACCENT_HOVER, alpha)
			: SourceTheme.withAlpha(SourceTheme.BORDER_SUBTLE, alpha);
		int textCol = down ? SourceTheme.ACCENT_TEXT : SourceTheme.TEXT_PRIMARY;

		g.fill(x, y, x + w, y + h, bg);
		g.outline(x, y, w, h, border);
		int lw = font.width(label);
		g.text(font, label, x + (w - lw) / 2, y + (h - font.lineHeight) / 2 + 1, SourceTheme.withAlpha(textCol, alpha), false);
	}

	public static HudBox renderVelocityGraph(
		GuiGraphicsExtractor g,
		Font font,
		int screenW,
		int screenH,
		SourceClientConfig cfg,
		SourceClientController ctrl,
		int alpha,
		boolean preview
	) {
		HudBox box = SourceHudLayout.computeBox(screenW, screenH, ModuleId.VELOCITY_GRAPH, cfg.graphNormX, cfg.graphNormY, cfg.hudScale);
		beginBox(g, box);

		int bg = SourceTheme.withAlpha(SourceTheme.SURFACE_PRIMARY, Math.round(alpha * 0.78f));
		int border = SourceTheme.withAlpha(SourceTheme.BORDER_SUBTLE, Math.round(alpha * 0.85f));
		SourceTheme.drawPanel(g, 0, 0, box.baseW(), box.baseH(), bg, border);

		double[] hist = ctrl.getOrderedSpeedHistory();
		double maxSample = 500.0;
		for (int i = 0; i < hist.length; i++) {
			double v = preview ? (240.0 + 260.0 * Math.abs(Math.sin(i * 0.14))) : hist[i];
			if (v > maxSample) {
				maxSample = v;
			}
		}

		int gx = 4;
		int gy = 13;
		int gw = box.baseW() - 8;
		int gh = box.baseH() - 17;

		// 250 u/s knife baseline
		int baseY = gy + gh - (int) Math.round((250.0 / maxSample) * gh);
		g.fill(gx, baseY, gx + gw, baseY + 1, SourceTheme.withAlpha(0xFF334155, alpha));

		g.text(font, "VEL", 5, 3, SourceTheme.withAlpha(SourceTheme.TEXT_SECONDARY, alpha), false);
		String peakTxt = String.format(Locale.ROOT, "%.0f", maxSample);
		g.text(font, peakTxt, box.baseW() - font.width(peakTxt) - 5, 3, SourceTheme.withAlpha(SourceTheme.ACCENT, alpha), false);

		int n = hist.length;
		for (int px = 0; px < gw; px++) {
			int idx = Math.min(n - 1, (px * n) / gw);
			double v = preview ? (250.0 + 240.0 * Math.abs(Math.sin(idx * 0.14))) : hist[idx];
			int barH = Math.max(1, (int) Math.round((v / maxSample) * gh));
			int col = v >= 320.0 ? SourceTheme.GAIN_MINT : (v >= 245.0 ? SourceTheme.ACCENT : SourceTheme.TEXT_MUTED);
			g.fill(gx + px, gy + gh - barH, gx + px + 1, gy + gh, SourceTheme.withAlpha(col, alpha));
		}

		endBox(g);
		return box;
	}

	public static HudBox renderTimerBanner(
		GuiGraphicsExtractor g,
		Font font,
		int screenW,
		int screenH,
		SourceClientConfig cfg,
		SourceClientController ctrl,
		int alpha,
		boolean preview
	) {
		HudBox box = SourceHudLayout.computeBox(screenW, screenH, ModuleId.TIMER_BANNER, cfg.timerNormX, cfg.timerNormY, cfg.hudScale);
		TimerStateS2CPayload ts = ctrl.getTimerState();
		beginBox(g, box);

		int bg = SourceTheme.withAlpha(SourceTheme.SURFACE_PRIMARY, Math.round(alpha * 0.80f));
		int border = ts.running()
			? SourceTheme.withAlpha(SourceTheme.ACCENT, alpha)
			: SourceTheme.withAlpha(SourceTheme.BORDER_SUBTLE, alpha);
		SourceTheme.drawPanel(g, 0, 0, box.baseW(), box.baseH(), bg, border);

		String timeStr = preview ? "00:14.280" : TimerManager.formatMillis(ts.elapsedMs());
		String tag = preview ? "PRO" : (ts.proRun() ? "PRO" : "NUB");
		int tagCol = (preview || ts.proRun()) ? SourceTheme.GAIN_MINT : SourceTheme.ACCENT;

		g.text(font, timeStr, 7, 4, SourceTheme.withAlpha(SourceTheme.TEXT_PRIMARY, alpha), false);
		g.text(font, tag, box.baseW() - font.width(tag) - 7, 4, SourceTheme.withAlpha(tagCol, alpha), false);

		String pbStr = preview
			? "PB 00:14.010 · CP 0"
			: ("PB " + TimerManager.formatMillis(ts.personalBestMs()) + " · CP " + ts.checkpointCount());
		g.text(font, pbStr, 7, 15, SourceTheme.withAlpha(SourceTheme.TEXT_SECONDARY, alpha), false);

		endBox(g);
		return box;
	}

	private static void beginBox(GuiGraphicsExtractor g, HudBox box) {
		g.pose().pushMatrix();
		g.pose().translate((float) box.x(), (float) box.y());
		g.pose().scale(box.scale(), box.scale());
	}

	private static void endBox(GuiGraphicsExtractor g) {
		g.pose().popMatrix();
	}
}
