package com.hazeg7.sourcecraft.client.gui;

import com.hazeg7.sourcecraft.client.SourceClientController;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.HudMode;
import com.hazeg7.sourcecraft.client.hud.SourceHudLayout;
import com.hazeg7.sourcecraft.client.hud.SourceHudLayout.HudBox;
import com.hazeg7.sourcecraft.client.hud.SourceHudLayout.ModuleId;
import com.hazeg7.sourcecraft.client.hud.SourceHudRenderer;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Interactive drag-and-drop HUD layout editor with snap guides, scale, and opacity controls.
 */
public class SourceHudEditorScreen extends Screen {
	private final Screen parent;
	private final SourceClientConfig config;
	private final Map<ModuleId, HudBox> renderedBoxes = new EnumMap<>(ModuleId.class);

	private ModuleId draggingId = null;
	private int dragOffX = 0;
	private int dragOffY = 0;

	public SourceHudEditorScreen(Screen parent) {
		super(Component.literal("SourceCraft HUD Editor"));
		this.parent = parent;
		this.config = SourceClientConfig.get();
		if (this.config.hudMode == HudMode.OFF) {
			this.config.hudMode = HudMode.FULL;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		this.clearWidgets();
		int panelW = Math.min(380, this.width - 24);
		int panelH = 82;
		int px = (this.width - panelW) / 2;
		int py = this.height - panelH - 10;

		int thirdW = (panelW - 24) / 3;
		int y1 = py + 24;

		this.addRenderableWidget(new SourceWidgets.StudioCycleButton<>(
			px + 8, y1, thirdW, 20, "HUD",
			HudMode.values(), () -> config.hudMode, v -> config.hudMode = v, m -> m.label
		));
		this.addRenderableWidget(new SourceWidgets.StudioSlider(
			px + 12 + thirdW, y1, thirdW, 20, "Scale",
			() -> (config.hudScale - 0.65) / 1.0,
			t -> config.hudScale = (float) (0.65 + t * 1.0),
			() -> String.format(Locale.ROOT, "%.2fx", config.hudScale)
		));
		this.addRenderableWidget(new SourceWidgets.StudioSlider(
			px + 16 + thirdW * 2, y1, thirdW, 20, "Alpha",
			() -> (config.hudOpacity - 0.25) / 0.75,
			t -> config.hudOpacity = (float) (0.25 + t * 0.75),
			() -> Math.round(config.hudOpacity * 100.0f) + "%"
		));

		int halfW = (panelW - 20) / 2;
		int y2 = py + 50;
		this.addRenderableWidget(new SourceWidgets.StudioButton(
			px + 8, y2, halfW, 22, Component.literal("Reset Positions"), config::resetHudPositions
		));
		this.addRenderableWidget(new SourceWidgets.StudioButton(
			px + 12 + halfW, y2, halfW, 22, Component.literal("Done"), this::onClose, () -> true, true
		));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, this.width, this.height, 0x33090D14);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		int guide = 0x5522D3EE;
		g.fill(this.width / 2, 0, this.width / 2 + 1, this.height, guide);
		g.fill(0, this.height / 2, this.width, this.height / 2 + 1, guide);
		g.outline(10, 10, Math.max(20, this.width - 20), Math.max(20, this.height - 20), 0x3322D3EE);

		int alpha = Math.max(40, Math.min(255, Math.round(config.hudOpacity * 255.0f)));
		SourceClientController ctrl = SourceClientController.get();
		renderedBoxes.clear();

		for (ModuleId id : ModuleId.values()) {
			HudBox box = SourceHudRenderer.renderModuleForEditor(g, this.font, this.width, this.height, id, config, ctrl, alpha);
			renderedBoxes.put(id, box);
			boolean active = id == draggingId || box.contains(mouseX, mouseY);
			g.outline(box.x() - 2, box.y() - 2, box.width() + 4, box.height() + 4,
				active ? SourceTheme.ACCENT : 0x6638BDF8);
		}

		int panelW = Math.min(380, this.width - 24);
		int panelH = 82;
		int px = (this.width - panelW) / 2;
		int py = this.height - panelH - 10;
		SourceTheme.drawPanel(g, px, py, panelW, panelH, SourceTheme.SURFACE_PRIMARY, SourceTheme.BORDER_SUBTLE);
		g.centeredText(this.font, "Drag Any HUD Module (Center & Edge Snap Guides Active)", this.width / 2, py + 7, SourceTheme.TEXT_PRIMARY);

		super.extractRenderState(g, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() == 0) {
			for (ModuleId id : ModuleId.values()) {
				HudBox box = renderedBoxes.get(id);
				if (box != null && box.contains(event.x(), event.y())) {
					draggingId = id;
					dragOffX = (int) Math.round(event.x()) - box.x();
					dragOffY = (int) Math.round(event.y()) - box.y();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (draggingId != null) {
			float[] currentNorm = getNorm(draggingId);
			HudBox box = SourceHudLayout.computeBox(this.width, this.height, draggingId, currentNorm[0], currentNorm[1], config.hudScale);
			int newX = (int) Math.round(event.x()) - dragOffX;
			int newY = (int) Math.round(event.y()) - dragOffY;
			float[] norm = SourceHudLayout.boxOriginToNorm(newX, newY, this.width, this.height, box.width(), box.height());
			float[] snapped = SourceHudLayout.snapNorm(norm[0], norm[1], 0.025f);
			setNorm(draggingId, snapped[0], snapped[1]);
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingId != null) {
			draggingId = null;
			config.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	private float[] getNorm(ModuleId id) {
		return switch (id) {
			case SPEEDOMETER -> new float[] { config.speedNormX, config.speedNormY };
			case STRAFE_TRAINER -> new float[] { config.strafeNormX, config.strafeNormY };
			case KEYSTROKES -> new float[] { config.keysNormX, config.keysNormY };
			case VELOCITY_GRAPH -> new float[] { config.graphNormX, config.graphNormY };
			case TIMER_BANNER -> new float[] { config.timerNormX, config.timerNormY };
		};
	}

	private void setNorm(ModuleId id, float nx, float ny) {
		switch (id) {
			case SPEEDOMETER -> { config.speedNormX = nx; config.speedNormY = ny; }
			case STRAFE_TRAINER -> { config.strafeNormX = nx; config.strafeNormY = ny; }
			case KEYSTROKES -> { config.keysNormX = nx; config.keysNormY = ny; }
			case VELOCITY_GRAPH -> { config.graphNormX = nx; config.graphNormY = ny; }
			case TIMER_BANNER -> { config.timerNormX = nx; config.timerNormY = ny; }
		}
	}

	@Override
	public void onClose() {
		config.save();
		this.minecraft.gui.setScreen(this.parent);
	}
}
