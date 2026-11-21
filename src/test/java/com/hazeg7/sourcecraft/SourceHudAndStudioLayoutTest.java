package com.hazeg7.sourcecraft;

import static org.junit.jupiter.api.Assertions.*;

import com.hazeg7.sourcecraft.client.gui.SourceStudioLayout;
import com.hazeg7.sourcecraft.client.hud.SourceHudLayout;
import com.hazeg7.sourcecraft.client.hud.SourceHudLayout.HudBox;
import com.hazeg7.sourcecraft.client.hud.SourceHudLayout.ModuleId;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceHudAndStudioLayoutTest {
	@Test
	void studioLayoutHasZeroOverlapsAcrossAllResolutions() {
		int[][] resolutions = {
			{ 427, 240 },
			{ 640, 360 },
			{ 854, 480 },
			{ 1280, 720 },
			{ 1920, 1080 }
		};

		for (int[] res : resolutions) {
			int w = res[0];
			int h = res[1];
			SourceStudioLayout.ComputedLayout lo = SourceStudioLayout.compute(w, h);

			List<SourceStudioLayout.Rect> nonOverlapping = List.of(
				lo.header(),
				lo.presetBar(),
				lo.previewViewport(),
				lo.previewTelemetry(),
				lo.tabBar(),
				lo.controlsViewport(),
				lo.footer()
			);

			for (SourceStudioLayout.Rect r : nonOverlapping) {
				assertTrue(r.width() > 0 && r.height() > 0, "Zero/negative rect at " + w + "x" + h);
				assertTrue(r.x() >= 0 && r.y() >= 0 && r.right() <= w && r.bottom() <= h,
					"Out-of-bounds rect " + r + " at " + w + "x" + h);
			}

			for (int i = 0; i < nonOverlapping.size(); i++) {
				for (int j = i + 1; j < nonOverlapping.size(); j++) {
					assertFalse(nonOverlapping.get(i).intersects(nonOverlapping.get(j)),
						"Overlap between " + nonOverlapping.get(i) + " and " + nonOverlapping.get(j) + " at " + w + "x" + h);
				}
			}
		}
	}

	@Test
	void hudBoxesClampInsideScreenAndSnapCleanly() {
		for (ModuleId id : ModuleId.values()) {
			for (float scale : new float[] { 0.65f, 1.0f, 1.65f }) {
				HudBox box = SourceHudLayout.computeBox(427, 240, id, 1.0f, 1.0f, scale);
				assertTrue(box.x() >= 4 && box.y() >= 4);
				assertTrue(box.x() + box.width() <= 423);
				assertTrue(box.y() + box.height() <= 236);
			}
		}

		float[] snappedCenter = SourceHudLayout.snapNorm(0.51f, 0.49f, 0.025f);
		assertEquals(0.5f, snappedCenter[0], 1e-6f);
		assertEquals(0.5f, snappedCenter[1], 1e-6f);

		float[] snappedEdge = SourceHudLayout.snapNorm(0.01f, 0.99f, 0.025f);
		assertEquals(0.0f, snappedEdge[0], 1e-6f);
		assertEquals(1.0f, snappedEdge[1], 1e-6f);
	}
}
