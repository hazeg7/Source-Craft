package com.hazeg7.sourcecraft.client.gui;

import com.hazeg7.sourcecraft.client.SourceClientController;
import com.hazeg7.sourcecraft.client.SourceClientController.NetSyncState;
import com.hazeg7.sourcecraft.client.SourceKeybinds;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.FovMode;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.HudMode;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.ScrollJumpMode;
import com.hazeg7.sourcecraft.client.config.SourceClientConfig.SpeedUnit;
import com.hazeg7.sourcecraft.network.TimerActionC2SPayload.Action;
import com.hazeg7.sourcecraft.physics.SourceCvars;
import com.hazeg7.sourcecraft.physics.SourcePreset;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Dark-slate + electric cyan SourceCraft Studio configuration GUI with live 2D air-strafe simulator.
 */
public class SourceStudioScreen extends Screen {
	private enum Tab {
		PHYSICS("Physics"),
		BHOP_SURF("Bhop & Surf"),
		FEEL("Feel"),
		HUD_TIMER("HUD & Timer"),
		KEYS_SYNC("Keys & Sync");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private record ScrollSection(int baseY, String title, String subtitle, int color) {
	}

	private record ScrollWidgetEntry(AbstractWidget widget, int baseY) {
	}

	private final Screen parent;
	private final SourceClientConfig clientConfig;
	private final SourceClientConfig openingSnapshot;
	private final SourceCvars workingCvars;
	private final AirStrafePreviewWidget previewWidget = new AirStrafePreviewWidget();

	private final List<ScrollSection> scrollSections = new ArrayList<>();
	private final List<ScrollWidgetEntry> scrollWidgets = new ArrayList<>();

	private SourceStudioLayout.ComputedLayout layout;
	private Tab activeTab = Tab.PHYSICS;
	private boolean worldPeek = false;
	private int scrollOffset = 0;
	private int contentHeight = 0;
	private KeyMapping listeningMapping = null;

	public SourceStudioScreen(Screen parent) {
		super(Component.literal("SourceCraft Studio"));
		this.parent = parent;
		this.clientConfig = SourceClientConfig.get();
		this.openingSnapshot = this.clientConfig.copy();
		this.workingCvars = SourceClientController.get().activeCvars().copy();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		this.clearWidgets();
		this.scrollSections.clear();
		this.scrollWidgets.clear();
		this.listeningMapping = null;

		this.layout = SourceStudioLayout.compute(this.width, this.height);
		initHeader();
		initPresetBar();
		initTabBar();
		initFooter();
		buildActiveTabContent();
	}

	private void initHeader() {
		SourceStudioLayout.Rect h = layout.header();
		int btnH = h.height() - 4;
		int by = h.y() + 2;
		int right = h.right() - 2;

		int doneW = layout.compactMode() ? 42 : 52;
		right -= doneW;
		this.addRenderableWidget(new SourceWidgets.StudioButton(
			right, by, doneW, btnH, Component.literal("Done"), this::onClose, () -> true, true
		).withTooltip("Apply CVars and return"));

		right -= 4;
		int peekW = layout.compactMode() ? 40 : 48;
		right -= peekW;
		this.addRenderableWidget(new SourceWidgets.StudioButton(
			right, by, peekW, btnH, Component.literal("Peek"), () -> worldPeek = !worldPeek, () -> worldPeek, false
		).withTooltip("Dim studio backdrop to watch live world"));

		right -= 4;
		int masterW = layout.compactMode() ? 58 : 74;
		right -= masterW;
		this.addRenderableWidget(new SourceWidgets.StudioButton(
			right, by, masterW, btnH,
			Component.literal("Engine"),
			() -> {
				clientConfig.modEnabled = !clientConfig.modEnabled;
				clientConfig.save();
			},
			() -> clientConfig.modEnabled,
			true
		) {
			@Override
			public Component getMessage() {
				return Component.literal(clientConfig.modEnabled ? "ACTIVE" : "BYPASS");
			}
		}.withTooltip("Toggle CS:S Physics Engine on/off (G)"));
	}

	private void initPresetBar() {
		SourceStudioLayout.Rect bar = layout.presetBar();
		List<SourcePreset> presets = SourcePreset.all();
		int count = presets.size() + 1;
		int gap = 3;
		int btnW = Math.max(24, (bar.width() - gap * (count - 1)) / count);
		int x = bar.x();

		for (SourcePreset preset : presets) {
			String label = btnW < 82 ? shortPresetName(preset.id()) : preset.name();
			this.addRenderableWidget(new SourceWidgets.StudioButton(
				x, bar.y(), btnW, bar.height(),
				Component.literal(label),
				() -> {
					if (!SourceClientController.get().canEditCvars()) {
						return;
					}
					preset.applyTo(workingCvars);
					SourceClientController.get().applyEditedCvars(preset.id(), workingCvars);
				},
				() -> SourcePreset.detectPresetId(workingCvars).equals(preset.id()),
				false
			).withTooltip(preset.description()));
			x += btnW + gap;
		}

		int remW = Math.max(24, bar.right() - x);
		this.addRenderableWidget(new SourceWidgets.StudioButton(
			x, bar.y(), remW, bar.height(),
			Component.literal("Reset"),
			() -> {
				if (!SourceClientController.get().canEditCvars()) {
					return;
				}
				SourcePreset.CSS_CLASSIC.applyTo(workingCvars);
				SourceClientController.get().applyEditedCvars(SourcePreset.CSS_CLASSIC.id(), workingCvars);
			}
		).withTooltip("Reset physics CVars to CS:S Bhop 100t"));
	}

	private static String shortPresetName(String id) {
		return switch (id) {
			case "css_classic" -> "Bhop 100t";
			case "css_comp" -> "Comp 66t";
			case "css_surf" -> "Surf";
			case "css_autobhop" -> "AutoBhop";
			case "css_kz" -> "KZ Climb";
			case "survival_hybrid" -> "Survival";
			default -> id;
		};
	}

	private void initTabBar() {
		SourceStudioLayout.Rect tb = layout.tabBar();
		Tab[] tabs = Tab.values();
		int gap = 3;
		int btnW = Math.max(28, (tb.width() - gap * (tabs.length - 1)) / tabs.length);
		int x = tb.x();
		for (int i = 0; i < tabs.length; i++) {
			Tab tab = tabs[i];
			int w = (i == tabs.length - 1) ? Math.max(28, tb.right() - x) : btnW;
			this.addRenderableWidget(new SourceWidgets.StudioButton(
				x, tb.y(), w, tb.height(),
				Component.literal(layout.compactMode() ? shortTab(tab) : tab.label),
				() -> {
					activeTab = tab;
					scrollOffset = 0;
					init();
				},
				() -> activeTab == tab,
				false
			));
			x += w + gap;
		}
	}

	private static String shortTab(Tab t) {
		return switch (t) {
			case PHYSICS -> "Phys";
			case BHOP_SURF -> "Bhop";
			case FEEL -> "Feel";
			case HUD_TIMER -> "HUD";
			case KEYS_SYNC -> "Keys";
		};
	}

	private void initFooter() {
		SourceStudioLayout.Rect f = layout.footer();
		int btnH = f.height() - 2;
		int y = f.y() + 1;
		int wHud = layout.compactMode() ? 58 : 74;
		int wCancel = layout.compactMode() ? 48 : 62;
		int wDone = layout.compactMode() ? 52 : 66;

		this.addRenderableWidget(new SourceWidgets.StudioButton(
			f.right() - wDone, y, wDone, btnH, Component.literal("Apply"), this::onClose, () -> true, true
		));
		this.addRenderableWidget(new SourceWidgets.StudioButton(
			f.right() - wDone - 4 - wCancel, y, wCancel, btnH, Component.literal("Cancel"), this::cancelAndClose
		));
		this.addRenderableWidget(new SourceWidgets.StudioButton(
			f.right() - wDone - 4 - wCancel - 4 - wHud, y, wHud, btnH,
			Component.literal("Edit HUD"),
			() -> this.minecraft.gui.setScreen(new SourceHudEditorScreen(this))
		).withTooltip("Drag, snap, and scale HUD modules"));
	}

	private void buildActiveTabContent() {
		SourceStudioLayout.Rect vp = layout.controlsViewport();
		int x = vp.x() + 8;
		int w = Math.max(80, vp.width() - 20);
		int y = 6;
		boolean canEdit = SourceClientController.get().canEditCvars();

		switch (activeTab) {
			case PHYSICS -> {
				y = addSection(y, "CORE SOURCE PHYSICS", "CGameMovement CVars", SourceTheme.ACCENT);
				y = addCvarSlider(x, y, w, "Tickrate (Hz)", 20, 128, () -> workingCvars.svTickrate, v -> workingCvars.svTickrate = Math.round(v), "%.0f Hz", canEdit);
				y = addCvarSlider(x, y, w, "Air Accelerate", 1, 1000, () -> workingCvars.svAirAccelerate, v -> workingCvars.svAirAccelerate = Math.round(v), "%.0f", canEdit);
				y = addCvarSlider(x, y, w, "Ground Accel", 1, 20, () -> workingCvars.svAccelerate, v -> workingCvars.svAccelerate = v, "%.1f", canEdit);
				y = addCvarSlider(x, y, w, "Ground Friction", 0, 12, () -> workingCvars.svFriction, v -> workingCvars.svFriction = v, "%.1f", canEdit);
				y = addCvarSlider(x, y, w, "Stopspeed", 10, 200, () -> workingCvars.svStopspeed, v -> workingCvars.svStopspeed = Math.round(v), "%.0f u/s", canEdit);
				y = addCvarSlider(x, y, w, "Base Maxspeed", 120, 450, () -> workingCvars.svMaxSpeed, v -> workingCvars.svMaxSpeed = Math.round(v), "%.0f u/s", canEdit);
				y = addCvarSlider(x, y, w, "Air Max Wishspeed", 10, 80, () -> workingCvars.svAirMaxWishspeed, v -> workingCvars.svAirMaxWishspeed = Math.round(v), "%.0f u/s", canEdit);
				y = addCvarSlider(x, y, w, "Gravity (HU/s²)", 200, 1600, () -> workingCvars.svGravity, v -> workingCvars.svGravity = Math.round(v), "%.0f", canEdit);
				y = addCvarSlider(x, y, w, "Jump Impulse", 180, 480, () -> workingCvars.svJumpImpulse, v -> workingCvars.svJumpImpulse = Math.round(v), "%.0f u/s", canEdit);
				y = addCvarSlider(x, y, w, "HU per Block", 32, 48, () -> workingCvars.svHuPerBlock, v -> workingCvars.svHuPerBlock = Math.round(v), "%.0f HU/b", canEdit);
			}
			case BHOP_SURF -> {
				y = addSection(y, "BUNNYHOP & STAMINA", "Timing, caps & stamina", SourceTheme.GAIN_MINT);
				y = addCvarToggle(x, y, w, "Auto-Bunnyhopping (Hold Space)", () -> workingCvars.svAutoBunnyhopping, b -> workingCvars.svAutoBunnyhopping = b, canEdit);
				y = addCvarToggle(x, y, w, "Uncapped Bhop (sv_enablebunnyhopping)", () -> workingCvars.svEnableBunnyhopping, b -> workingCvars.svEnableBunnyhopping = b, canEdit);
				y = addCvarSlider(x, y, w, "Jump Buffer Window", 0, 6, () -> (double) workingCvars.svJumpBufferTicks, v -> workingCvars.svJumpBufferTicks = (int) Math.round(v), "%.0f ticks", canEdit);
				y = addCvarSlider(x, y, w, "Stamina Max", 0, 100, () -> workingCvars.svStaminaMax, v -> workingCvars.svStaminaMax = Math.round(v), "%.0f", canEdit);

				y = addSection(y + 4, "SURF & SOURCE TECH", "Ramps, crouch-jump & edgebug", SourceTheme.ACCENT);
				y = addCvarToggle(x, y, w, "Surfing (Ramps, Stairs, Slabs, Snow)", () -> workingCvars.svSurfEnabled, b -> workingCvars.svSurfEnabled = b, canEdit);
				y = addCvarSlider(x, y, w, "Surf Overbounce", 1.0, 1.2, () -> workingCvars.svSurfOverbounce, v -> workingCvars.svSurfOverbounce = v, "%.2fx", canEdit);
				y = addCvarToggle(x, y, w, "Air Crouch-Jump & 54 HU Hull", () -> workingCvars.svCrouchJump, b -> workingCvars.svCrouchJump = b, canEdit);
				y = addCvarToggle(x, y, w, "Edgebug & Jumpbug Fall-Save", () -> workingCvars.svEdgebugEnabled, b -> workingCvars.svEdgebugEnabled = b, canEdit);
				y = addCvarToggle(x, y, w, "Item Weight Mobility (CS:S Weapons)", () -> workingCvars.svWeaponWeight, b -> workingCvars.svWeaponWeight = b, canEdit);
				y = addCvarToggle(x, y, w, "Survival Fall Damage", () -> workingCvars.svSurvivalFallDamage, b -> workingCvars.svSurvivalFallDamage = b, canEdit);
			}
			case FEEL -> {
				y = addSection(y, "INPUT & CAMERA FEEL", "Scroll jump, strafe tilt & FOV", SourceTheme.ACCENT);
				y = addScrollWidget(new SourceWidgets.StudioCycleButton<>(
					x, y, w, 20, "Scroll-Wheel Jump",
					ScrollJumpMode.values(), () -> clientConfig.scrollJumpMode,
					v -> { clientConfig.scrollJumpMode = v; clientConfig.save(); }, m -> m.label
				), y) + 24;
				y = addScrollWidget(new SourceWidgets.StudioCycleButton<>(
					x, y, w, 20, "Speed FOV Behavior",
					FovMode.values(), () -> clientConfig.fovMode,
					v -> { clientConfig.fovMode = v; clientConfig.save(); }, m -> m.label
				), y) + 24;
				y = addScrollWidget(new SourceWidgets.StudioSlider(
					x, y, w, 20, "Strafe Camera Roll",
					() -> clientConfig.strafeRollDegrees / 3.5,
					t -> { clientConfig.strafeRollDegrees = t * 3.5; clientConfig.save(); },
					() -> String.format(Locale.ROOT, "%.2f°", clientConfig.strafeRollDegrees)
				), y) + 24;
				y = addScrollWidget(new SourceWidgets.StudioSlider(
					x, y, w, 20, "Landing View Kick",
					() -> clientConfig.landingKickScale / 2.5,
					t -> { clientConfig.landingKickScale = t * 2.5; clientConfig.save(); },
					() -> String.format(Locale.ROOT, "%.2fx", clientConfig.landingKickScale)
				), y) + 24;
				y = addScrollWidget(new SourceWidgets.StudioToggle(
					x, y, w, 20, "Audio Feedback (Bhop/Edgebug Chimes)",
					() -> clientConfig.audioCues, b -> { clientConfig.audioCues = b; clientConfig.save(); }
				), y) + 24;
			}
			case HUD_TIMER -> {
				y = addSection(y, "TELEMETRY HUD MODULES", "Configure overlay widgets", SourceTheme.ACCENT);
				y = addScrollWidget(new SourceWidgets.StudioCycleButton<>(
					x, y, w, 20, "HUD Mode",
					HudMode.values(), () -> clientConfig.hudMode,
					v -> { clientConfig.hudMode = v; clientConfig.save(); }, m -> m.label
				), y) + 24;
				y = addScrollWidget(new SourceWidgets.StudioCycleButton<>(
					x, y, w, 20, "Speed Units",
					SpeedUnit.values(), () -> clientConfig.speedUnit,
					v -> { clientConfig.speedUnit = v; clientConfig.save(); }, u -> u.suffix
				), y) + 24;
				y = addScrollWidget(new SourceWidgets.StudioToggle(x, y, w, 20, "Center Speedometer & Pre-Speed", () -> clientConfig.showSpeedometer, b -> clientConfig.showSpeedometer = b), y) + 24;
				y = addScrollWidget(new SourceWidgets.StudioToggle(x, y, w, 20, "Strafe Sync Bar & Coach", () -> clientConfig.showStrafeTrainer, b -> clientConfig.showStrafeTrainer = b), y) + 24;
				y = addScrollWidget(new SourceWidgets.StudioToggle(x, y, w, 20, "WASD + Mouse Turn Keystrokes", () -> clientConfig.showKeystrokes, b -> clientConfig.showKeystrokes = b), y) + 24;
				y = addScrollWidget(new SourceWidgets.StudioToggle(x, y, w, 20, "Live Velocity Sparkline Graph", () -> clientConfig.showVelocityGraph, b -> clientConfig.showVelocityGraph = b), y) + 24;
				y = addScrollWidget(new SourceWidgets.StudioToggle(x, y, w, 20, "KZ / Surf Timer & PB Banner", () -> clientConfig.showTimerBanner, b -> clientConfig.showTimerBanner = b), y) + 24;

				y = addSection(y + 4, "CHECKPOINTS & RUN PRACTICE", "KZ teleport actions", SourceTheme.GAIN_MINT);
				int half = (w - 4) / 2;
				addScrollWidget(new SourceWidgets.StudioButton(x, y, half, 20, Component.literal("Save CP (C)"),
					() -> SourceClientController.get().sendTimerAction(Action.SAVE_CP, -1)), y);
				addScrollWidget(new SourceWidgets.StudioButton(x + half + 4, y, w - half - 4, 20, Component.literal("TP CP (R)"),
					() -> SourceClientController.get().sendTimerAction(Action.TP_CP, -1)), y);
				y += 24;
				addScrollWidget(new SourceWidgets.StudioButton(x, y, half, 20, Component.literal("Toggle CP Velocity"),
					() -> SourceClientController.get().sendTimerAction(Action.TOGGLE_RESTORE_VEL, -1)), y);
				addScrollWidget(new SourceWidgets.StudioButton(x + half + 4, y, w - half - 4, 20, Component.literal("Restart Run"),
					() -> SourceClientController.get().sendTimerAction(Action.RESTART, -1)), y);
				y += 24;
			}
			case KEYS_SYNC -> {
				NetSyncState sync = SourceClientController.get().getSyncState();
				y = addSection(y, "MULTIPLAYER SYNC", sync.detail, SourceTheme.ACCENT);
				y = addScrollWidget(new SourceWidgets.StudioToggle(
					x, y, w, 20, "Allow on Unmodded Servers (Use With Caution)",
					() -> clientConfig.allowOnVanillaServers,
					b -> { clientConfig.allowOnVanillaServers = b; clientConfig.save(); }
				).withTooltip("Keep OFF on public servers with anti-cheat"), y) + 24;
				y = addSection(y + 2, "KEYBINDINGS", "Click to rebind · ESC clears", SourceTheme.TEXT_SECONDARY);
				for (SourceKeybinds.BindingEntry entry : SourceKeybinds.allBindings()) {
					KeyMapping km = entry.mapping();
					y = addScrollWidget(new SourceWidgets.StudioButton(
						x, y, w, 20,
						Component.literal(entry.label()),
						() -> listeningMapping = (listeningMapping == km ? null : km),
						() -> listeningMapping == km,
						false
					) {
						@Override
						public Component getMessage() {
							String bound = (listeningMapping == km) ? "> PRESS KEY <" : km.getTranslatedKeyMessage().getString();
							return Component.literal(entry.label() + ": " + bound);
						}
					}, y) + 23;
				}
			}
		}

		contentHeight = y + 6;
		clampScroll();
		repositionScrollWidgets();
	}

	private int addSection(int y, String title, String subtitle, int color) {
		scrollSections.add(new ScrollSection(y, title, subtitle, color));
		return y + 16;
	}

	private int addScrollWidget(AbstractWidget w, int baseY) {
		this.addWidget(w);
		scrollWidgets.add(new ScrollWidgetEntry(w, baseY));
		return baseY;
	}

	private int addCvarSlider(
		int x,
		int y,
		int w,
		String label,
		double min,
		double max,
		java.util.function.DoubleSupplier getter,
		java.util.function.DoubleConsumer setter,
		String fmt,
		boolean active
	) {
		SourceWidgets.StudioSlider s = new SourceWidgets.StudioSlider(
			x, y, w, 20, label,
			() -> (getter.getAsDouble() - min) / Math.max(1e-6, max - min),
			t -> {
				if (!active) {
					return;
				}
				setter.accept(min + t * (max - min));
				workingCvars.sanitize();
				SourceClientController.get().applyEditedCvars(SourcePreset.detectPresetId(workingCvars), workingCvars);
			},
			() -> String.format(Locale.ROOT, fmt, getter.getAsDouble())
		);
		s.active = active;
		return addScrollWidget(s, y) + 23;
	}

	private int addCvarToggle(
		int x,
		int y,
		int w,
		String label,
		java.util.function.BooleanSupplier getter,
		java.util.function.Consumer<Boolean> setter,
		boolean active
	) {
		SourceWidgets.StudioToggle t = new SourceWidgets.StudioToggle(
			x, y, w, 20, label, getter,
			b -> {
				if (!active) {
					return;
				}
				setter.accept(b);
				workingCvars.sanitize();
				SourceClientController.get().applyEditedCvars(SourcePreset.detectPresetId(workingCvars), workingCvars);
			}
		);
		t.active = active;
		return addScrollWidget(t, y) + 23;
	}

	private void clampScroll() {
		int maxScroll = Math.max(0, contentHeight - layout.controlsViewport().height());
		scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
	}

	private void repositionScrollWidgets() {
		SourceStudioLayout.Rect vp = layout.controlsViewport();
		for (ScrollWidgetEntry e : scrollWidgets) {
			int wy = vp.y() + e.baseY() - scrollOffset;
			e.widget().setY(wy);
			e.widget().visible = (wy + e.widget().getHeight() > vp.y()) && (wy < vp.bottom());
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, this.width, this.height, worldPeek ? SourceTheme.BG_BACKDROP_PEEK : SourceTheme.BG_BACKDROP_SOLID);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		SourceStudioLayout.Rect panel = layout.panel();
		SourceTheme.drawPanel(g, panel, worldPeek ? 0xB80F172A : SourceTheme.SURFACE_PRIMARY, SourceTheme.BORDER_SUBTLE);

		SourceStudioLayout.Rect h = layout.header();
		SourceTheme.drawPanel(g, h, SourceTheme.SURFACE_ELEVATED, SourceTheme.BORDER_SUBTLE);
		int ty = h.y() + (h.height() - this.font.lineHeight) / 2 + 1;
		g.text(this.font, "SOURCECRAFT", h.x() + 8, ty, SourceTheme.ACCENT, false);

		if (!layout.compactMode()) {
			int titleEnd = h.x() + 8 + this.font.width("SOURCECRAFT") + 8;
			g.text(this.font, "CS:S Engine Studio", titleEnd, ty, SourceTheme.TEXT_MUTED, false);

			NetSyncState sync = SourceClientController.get().getSyncState();
			int badgeCol = switch (sync) {
				case SYNCED, LOCAL -> SourceTheme.GAIN_MINT;
				case ENFORCED -> SourceTheme.ACCENT;
				case VANILLA -> SourceTheme.SYNC_BLUE;
			};
			int buttonsLeft = h.right() - (52 + 4 + 48 + 4 + 74 + 8);
			int pillW = this.font.width(sync.tag) + 12;
			if (buttonsLeft - pillW > titleEnd + 80) {
				SourceTheme.drawPill(g, this.font, buttonsLeft - pillW - 4, h.y() + 4, h.height() - 8,
					sync.tag, SourceTheme.SURFACE_PRIMARY, badgeCol, badgeCol);
			}
		}

		previewWidget.render(g, this.font, layout, workingCvars);

		SourceStudioLayout.Rect vp = layout.controlsViewport();
		SourceTheme.drawPanel(g, vp, SourceTheme.SURFACE_ELEVATED, SourceTheme.BORDER_SUBTLE);

		super.extractRenderState(g, mouseX, mouseY, partialTick);

		boolean inVp = vp.contains(mouseX, mouseY);
		int emx = inVp ? mouseX : -1;
		int emy = inVp ? mouseY : -1;

		g.enableScissor(vp.x() + 1, vp.y() + 1, vp.right() - 1, vp.bottom() - 1);
		for (ScrollSection sec : scrollSections) {
			int sy = vp.y() + sec.baseY() - scrollOffset;
			if (sy + 24 < vp.y() || sy > vp.bottom()) {
				continue;
			}
			g.text(this.font, sec.title(), vp.x() + 8, sy + 2, sec.color(), false);
			if (!layout.compactMode() && !sec.subtitle().isEmpty()) {
				int tw = this.font.width(sec.title()) + 8;
				if (vp.width() - tw - 20 > 60) {
					g.text(this.font, sec.subtitle(), vp.x() + 8 + tw, sy + 2, SourceTheme.TEXT_MUTED, false);
				}
			}
			g.fill(vp.x() + 8, sy + this.font.lineHeight + 4, vp.right() - 12, sy + this.font.lineHeight + 5, SourceTheme.BORDER_SUBTLE);
		}
		for (ScrollWidgetEntry e : scrollWidgets) {
			if (e.widget().visible) {
				e.widget().extractRenderState(g, emx, emy, partialTick);
			}
		}
		int maxScroll = Math.max(0, contentHeight - vp.height());
		if (maxScroll > 0) {
			int tx = vp.right() - 6;
			int trackY = vp.y() + 4;
			int trackH = Math.max(12, vp.height() - 8);
			g.fill(tx, trackY, tx + 3, trackY + trackH, SourceTheme.SURFACE_PRIMARY);
			int thumbH = Math.max(14, Math.round(trackH * ((float) vp.height() / contentHeight)));
			int thumbY = trackY + Math.round((trackH - thumbH) * ((float) scrollOffset / maxScroll));
			g.fill(tx, thumbY, tx + 3, thumbY + thumbH, SourceTheme.ACCENT);
		}
		g.disableScissor();

		SourceStudioLayout.Rect f = layout.footer();
		String preset = SourcePreset.detectPresetId(workingCvars);
		String footerTxt = "Preset: " + ("custom".equals(preset) ? "Custom" : SourcePreset.byId(preset).name())
			+ " · /css set <cvar> <val>";
		int fy = f.y() + (f.height() - this.font.lineHeight) / 2 + 1;
		int maxW = Math.max(40, f.width() - 220);
		if (this.font.width(footerTxt) > maxW) {
			footerTxt = this.font.plainSubstrByWidth(footerTxt, Math.max(12, maxW - 6)) + "..";
		}
		g.text(this.font, footerTxt, f.x() + 4, fy, SourceTheme.TEXT_SECONDARY, false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (listeningMapping != null) {
			if (event.button() >= 2) {
				listeningMapping.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
				KeyMapping.resetMapping();
				this.minecraft.options.save();
				listeningMapping = null;
				return true;
			}
			if (!layout.controlsViewport().contains(event.x(), event.y())) {
				listeningMapping = null;
			}
		}
		if (!layout.controlsViewport().contains(event.x(), event.y())) {
			for (ScrollWidgetEntry e : scrollWidgets) {
				e.widget().visible = false;
			}
			try {
				return super.mouseClicked(event, doubleClick);
			} finally {
				repositionScrollWidgets();
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (layout.controlsViewport().contains(mouseX, mouseY) && contentHeight > layout.controlsViewport().height()) {
			scrollOffset -= (int) Math.round(scrollY * 22.0);
			clampScroll();
			repositionScrollWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listeningMapping != null) {
			if (event.key() == InputConstants.KEY_ESCAPE) {
				listeningMapping.setKey(InputConstants.UNKNOWN);
			} else {
				listeningMapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(event.key()));
			}
			KeyMapping.resetMapping();
			this.minecraft.options.save();
			listeningMapping = null;
			return true;
		}
		return super.keyPressed(event);
	}

	private void cancelAndClose() {
		SourceClientConfig restored = openingSnapshot.copy();
		clientConfig.modEnabled = restored.modEnabled;
		clientConfig.activePresetId = restored.activePresetId;
		clientConfig.localCvars = restored.localCvars.copy();
		clientConfig.scrollJumpMode = restored.scrollJumpMode;
		clientConfig.strafeRollDegrees = restored.strafeRollDegrees;
		clientConfig.landingKickScale = restored.landingKickScale;
		clientConfig.fovMode = restored.fovMode;
		clientConfig.audioCues = restored.audioCues;
		clientConfig.hudMode = restored.hudMode;
		clientConfig.speedUnit = restored.speedUnit;
		clientConfig.showSpeedometer = restored.showSpeedometer;
		clientConfig.showStrafeTrainer = restored.showStrafeTrainer;
		clientConfig.showKeystrokes = restored.showKeystrokes;
		clientConfig.showVelocityGraph = restored.showVelocityGraph;
		clientConfig.showTimerBanner = restored.showTimerBanner;
		clientConfig.save();
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void onClose() {
		if (SourceClientController.get().canEditCvars()) {
			SourceClientController.get().applyEditedCvars(SourcePreset.detectPresetId(workingCvars), workingCvars);
		} else {
			clientConfig.save();
		}
		this.minecraft.gui.setScreen(this.parent);
	}
}
