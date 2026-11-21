package com.hazeg7.sourcecraft.client.gui;

/**
 * Pure layout geometry for SourceCraft Studio Screen across all GUI scale resolutions.
 */
public final class SourceStudioLayout {
	public record Rect(int x, int y, int width, int height) {
		public int right() {
			return x + width;
		}

		public int bottom() {
			return y + height;
		}

		public int centerX() {
			return x + width / 2;
		}

		public int centerY() {
			return y + height / 2;
		}

		public boolean contains(double px, double py) {
			return px >= x && px < right() && py >= y && py < bottom();
		}

		public boolean intersects(Rect other) {
			return this.x < other.right()
				&& this.right() > other.x
				&& this.y < other.bottom()
				&& this.bottom() > other.y;
		}
	}

	public record ComputedLayout(
		Rect panel,
		Rect header,
		Rect presetBar,
		Rect previewStage,
		Rect previewViewport,
		Rect previewTelemetry,
		Rect tabBar,
		Rect controlsViewport,
		Rect footer,
		boolean compactMode
	) {
	}

	private SourceStudioLayout() {
	}

	public static ComputedLayout compute(int screenW, int screenH) {
		boolean compact = screenW < 520 || screenH < 300;
		int marginX = compact ? 6 : 16;
		int marginY = compact ? 6 : 12;

		int panelW = Math.min(780, Math.max(280, screenW - marginX * 2));
		int panelH = Math.min(468, Math.max(210, screenH - marginY * 2));
		int panelX = (screenW - panelW) / 2;
		int panelY = (screenH - panelH) / 2;
		Rect panel = new Rect(panelX, panelY, panelW, panelH);

		int pad = compact ? 6 : 10;
		int gap = compact ? 4 : 8;

		int headerH = compact ? 24 : 30;
		int presetH = compact ? 20 : 24;
		int footerH = compact ? 22 : 26;

		int innerX = panelX + pad;
		int innerW = Math.max(100, panelW - pad * 2);

		Rect header = new Rect(innerX, panelY + pad, innerW, headerH);
		Rect presetBar = new Rect(innerX, header.bottom() + gap, innerW, presetH);
		Rect footer = new Rect(innerX, panel.bottom() - pad - footerH, innerW, footerH);

		int bodyTop = presetBar.bottom() + gap;
		int bodyBottom = footer.y() - gap;
		int bodyH = Math.max(80, bodyBottom - bodyTop);

		int previewW = compact
			? Math.max(110, Math.min(165, (int) (innerW * 0.34f)))
			: Math.max(185, Math.min(260, (int) (innerW * 0.35f)));
		Rect previewStage = new Rect(innerX, bodyTop, previewW, bodyH);

		int stagePad = compact ? 4 : 6;
		int stageGap = compact ? 3 : 5;
		int telemH = compact ? 34 : 44;
		int vpY = previewStage.y() + stagePad;
		int vpBottom = previewStage.bottom() - stagePad - telemH - stageGap;
		int vpH = Math.max(40, vpBottom - vpY);
		int stageInnerW = Math.max(60, previewStage.width() - stagePad * 2);

		Rect previewViewport = new Rect(previewStage.x() + stagePad, vpY, stageInnerW, vpH);
		Rect previewTelemetry = new Rect(previewStage.x() + stagePad, previewViewport.bottom() + stageGap, stageInnerW, telemH);

		int rightX = previewStage.right() + gap;
		int rightW = Math.max(120, innerX + innerW - rightX);
		int tabH = compact ? 20 : 24;
		Rect tabBar = new Rect(rightX, bodyTop, rightW, tabH);

		int controlsY = tabBar.bottom() + gap;
		int controlsH = Math.max(48, bodyBottom - controlsY);
		Rect controlsViewport = new Rect(rightX, controlsY, rightW, controlsH);

		return new ComputedLayout(
			panel, header, presetBar, previewStage, previewViewport, previewTelemetry, tabBar, controlsViewport, footer, compact
		);
	}
}
