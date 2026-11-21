package com.hazeg7.sourcecraft.client.gui;

import com.hazeg7.sourcecraft.physics.SourceCvars;
import com.hazeg7.sourcecraft.physics.SourceMovementEngine;
import com.hazeg7.sourcecraft.physics.SourceMovementEngine.PreviewPoint;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Live 2D Air-Strafe & Bhop trajectory visualizer powered directly by {@link SourceMovementEngine}.
 */
public final class AirStrafePreviewWidget {
	private List<PreviewPoint> cachedPoints = List.of();
	private SourceCvars lastCvars = null;
	private double peakPreviewSpeed = 250.0;

	public void render(GuiGraphicsExtractor g, Font font, SourceStudioLayout.ComputedLayout layout, SourceCvars cvars) {
		SourceStudioLayout.Rect stage = layout.previewStage();
		SourceTheme.drawPanel(g, stage, SourceTheme.SURFACE_ELEVATED, SourceTheme.BORDER_SUBTLE);

		if (lastCvars == null || !cvars.matches(lastCvars)) {
			lastCvars = cvars.copy();
			cachedPoints = SourceMovementEngine.simulateAirStrafePreview(lastCvars, 84);
			peakPreviewSpeed = cachedPoints.stream().mapToDouble(PreviewPoint::speedHu).max().orElse(250.0);
		}

		SourceStudioLayout.Rect vp = layout.previewViewport();
		SourceTheme.drawPanel(g, vp, SourceTheme.SURFACE_PRIMARY, SourceTheme.BORDER_SUBTLE);

		// Draw faint coordinate grid inside preview viewport
		g.enableScissor(vp.x() + 1, vp.y() + 1, vp.right() - 1, vp.bottom() - 1);
		int gridCol = 0xFF182235;
		for (int gx = vp.x() + 16; gx < vp.right(); gx += 16) {
			g.fill(gx, vp.y() + 1, gx + 1, vp.bottom() - 1, gridCol);
		}
		for (int gy = vp.y() + 16; gy < vp.bottom(); gy += 16) {
			g.fill(vp.x() + 1, gy, vp.right() - 1, gy + 1, gridCol);
		}

		g.text(font, "2D STRAFE SIM", vp.x() + 5, vp.y() + 4, SourceTheme.TEXT_MUTED, false);
		String tickTag = Math.round(cvars.svTickrate) + "Hz";
		g.text(font, tickTag, vp.right() - font.width(tickTag) - 5, vp.y() + 4, SourceTheme.ACCENT, false);

		if (!cachedPoints.isEmpty()) {
			double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
			double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
			for (PreviewPoint p : cachedPoints) {
				minX = Math.min(minX, p.xHu());
				maxX = Math.max(maxX, p.xHu());
				minZ = Math.min(minZ, p.zHu());
				maxZ = Math.max(maxZ, p.zHu());
			}
			double spanX = Math.max(80.0, maxX - minX);
			double spanZ = Math.max(80.0, maxZ - minZ);
			int drawW = Math.max(20, vp.width() - 24);
			int drawH = Math.max(20, vp.height() - 54);
			double scale = Math.min(drawW / spanX, drawH / spanZ);
			int baseOx = vp.centerX() - (int) Math.round(((minX + maxX) * 0.5) * scale);
			int baseOy = vp.y() + 16 + drawH / 2 - (int) Math.round(((minZ + maxZ) * 0.5) * scale);

			int n = cachedPoints.size();
			long ms = System.currentTimeMillis();
			int headIdx = (int) ((ms / 40L) % n);

			for (int i = 0; i < n; i++) {
				PreviewPoint p = cachedPoints.get(i);
				int sx = baseOx + (int) Math.round(p.xHu() * scale);
				int sy = baseOy + (int) Math.round(p.zHu() * scale);
				int col = p.speedHu() > cvars.svMaxSpeed + 20.0 ? SourceTheme.GAIN_MINT : SourceTheme.ACCENT;
				if (i <= headIdx) {
					g.fill(sx - 1, sy - 1, sx + 2, sy + 2, col);
				} else {
					g.fill(sx, sy, sx + 1, sy + 1, 0xFF334155);
				}
			}

			PreviewPoint head = cachedPoints.get(headIdx);
			int hx = baseOx + (int) Math.round(head.xHu() * scale);
			int hy = baseOy + (int) Math.round(head.zHu() * scale);
			g.fill(hx - 3, hy - 3, hx + 4, hy + 4, SourceTheme.TEXT_PRIMARY);
			g.outline(hx - 4, hy - 4, 9, 9, SourceTheme.ACCENT);

			// Bottom velocity curve inside viewport
			int graphTop = vp.bottom() - 28;
			int graphH = 22;
			g.fill(vp.x() + 4, graphTop, vp.right() - 4, graphTop + graphH, 0xAA0B101B);
			int gw = Math.max(10, vp.width() - 10);
			double vCap = Math.max(350.0, peakPreviewSpeed);
			for (int px = 0; px < gw; px++) {
				int idx = Math.min(n - 1, (px * n) / gw);
				double sp = cachedPoints.get(idx).speedHu();
				int bh = Math.max(1, (int) Math.round((sp / vCap) * (graphH - 2)));
				int col = idx == headIdx ? SourceTheme.TEXT_PRIMARY : (sp > 260.0 ? SourceTheme.GAIN_MINT : SourceTheme.ACCENT);
				g.fill(vp.x() + 5 + px, graphTop + graphH - 1 - bh, vp.x() + 6 + px, graphTop + graphH - 1, col);
			}
		}
		g.disableScissor();

		// Telemetry footer below preview viewport
		SourceStudioLayout.Rect tel = layout.previewTelemetry();
		SourceTheme.drawPanel(g, tel, SourceTheme.SURFACE_PRIMARY, SourceTheme.BORDER_SUBTLE);
		String line1 = String.format(Locale.ROOT, "AA: %.0f  ·  Peak: %.0f u/s", cvars.svAirAccelerate, peakPreviewSpeed);
		String line2 = String.format(
			Locale.ROOT,
			"Fric %.1f · Wish %.0f · %s",
			cvars.svFriction,
			cvars.svAirMaxWishspeed,
			cvars.svAutoBunnyhopping ? "AUTO" : "MANUAL"
		);
		g.text(font, line1, tel.x() + 6, tel.y() + 6, SourceTheme.GAIN_MINT, false);
		g.text(font, line2, tel.x() + 6, tel.y() + 18, SourceTheme.TEXT_SECONDARY, false);
		if (!layout.compactMode()) {
			String scaleTxt = String.format(Locale.ROOT, "1 block = %.0f HU  (%.2fm/s cap)",
				cvars.svHuPerBlock, cvars.svMaxSpeed / cvars.svHuPerBlock);
			g.text(font, scaleTxt, tel.x() + 6, tel.y() + 30, SourceTheme.TEXT_MUTED, false);
		}
	}
}
