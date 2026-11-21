package com.hazeg7.sourcecraft.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Cool dark-slate + electric cyan + mint + coral visual token palette for SourceCraft GUI & HUD.
 */
public final class SourceTheme {
	public static final int BG_BACKDROP_SOLID = 0xCC090D14;
	public static final int BG_BACKDROP_PEEK = 0x38090D14;

	public static final int SURFACE_PRIMARY = 0xF20F172A;
	public static final int SURFACE_ELEVATED = 0xFF182235;
	public static final int SURFACE_CONTROL = 0xFF212E45;
	public static final int SURFACE_CONTROL_HOVER = 0xFF2A3B58;

	public static final int BORDER_SUBTLE = 0xFF2B3B55;
	public static final int BORDER_ACTIVE = 0xFF22D3EE;

	public static final int TEXT_PRIMARY = 0xFFF1F5F9;
	public static final int TEXT_SECONDARY = 0xFF94A3B8;
	public static final int TEXT_MUTED = 0xFF64748B;

	public static final int ACCENT = 0xFF22D3EE;
	public static final int ACCENT_HOVER = 0xFF38BDF8;
	public static final int ACCENT_FILL = 0x2622D3EE;
	public static final int ACCENT_TEXT = 0xFF06131C;

	public static final int GAIN_MINT = 0xFF34D399;
	public static final int LOSS_CORAL = 0xFFF87171;
	public static final int NEUTRAL_ICE = 0xFFBAE6FD;
	public static final int SYNC_BLUE = 0xFF60A5FA;

	private SourceTheme() {
	}

	public static void drawPanel(GuiGraphicsExtractor g, int x, int y, int width, int height, int bg, int border) {
		if (width <= 0 || height <= 0) {
			return;
		}
		g.fill(x, y, x + width, y + height, bg);
		g.outline(x, y, width, height, border);
	}

	public static void drawPanel(GuiGraphicsExtractor g, SourceStudioLayout.Rect rect, int bg, int border) {
		drawPanel(g, rect.x(), rect.y(), rect.width(), rect.height(), bg, border);
	}

	public static void drawPill(
		GuiGraphicsExtractor g,
		Font font,
		int x,
		int y,
		int height,
		String text,
		int bg,
		int border,
		int textColor
	) {
		int width = font.width(text) + 12;
		g.fill(x, y, x + width, y + height, bg);
		g.outline(x, y, width, height, border);
		int textY = y + (height - font.lineHeight) / 2 + 1;
		g.text(font, text, x + 6, textY, textColor, false);
	}

	public static void drawProgressBar(
		GuiGraphicsExtractor g,
		int x,
		int y,
		int width,
		int height,
		float progress,
		int trackColor,
		int fillColor
	) {
		if (width <= 0 || height <= 0) {
			return;
		}
		float clamped = Math.max(0.0f, Math.min(1.0f, progress));
		g.fill(x, y, x + width, y + height, trackColor);
		int fillW = Math.round(width * clamped);
		if (fillW > 0) {
			g.fill(x, y, x + fillW, y + height, fillColor);
		}
	}

	public static int withAlpha(int rgbOrArgb, int alpha) {
		int a = Math.max(0, Math.min(255, alpha));
		return (a << 24) | (rgbOrArgb & 0x00FFFFFF);
	}
}
