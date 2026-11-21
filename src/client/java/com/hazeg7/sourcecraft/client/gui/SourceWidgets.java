package com.hazeg7.sourcecraft.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Custom dark-slate + electric cyan widgets matching SourceCraft Studio styling.
 */
public final class SourceWidgets {
	private SourceWidgets() {
	}

	private static boolean isConfirmKey(int key) {
		return key == InputConstants.KEY_RETURN
			|| key == InputConstants.KEY_NUMPADENTER
			|| key == InputConstants.KEY_SPACE;
	}

	private static double clamp01(double v) {
		return Double.isFinite(v) ? Math.max(0.0, Math.min(1.0, v)) : 0.0;
	}

	public static class StudioButton extends AbstractWidget {
		private final Runnable onPress;
		private final BooleanSupplier highlighted;
		private final boolean accentFill;

		public StudioButton(
			int x,
			int y,
			int width,
			int height,
			Component label,
			Runnable onPress,
			BooleanSupplier highlighted,
			boolean accentFill
		) {
			super(x, y, width, height, label);
			this.onPress = onPress;
			this.highlighted = highlighted;
			this.accentFill = accentFill;
		}

		public StudioButton(int x, int y, int width, int height, Component label, Runnable onPress) {
			this(x, y, width, height, label, onPress, () -> false, false);
		}

		public StudioButton withTooltip(String tip) {
			if (tip != null && !tip.isEmpty()) {
				this.setTooltip(Tooltip.create(Component.literal(tip)));
			}
			return this;
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			if (this.active && this.visible) {
				this.playDownSound(Minecraft.getInstance().getSoundManager());
				this.onPress.run();
			}
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (this.active && this.visible && isConfirmKey(event.key())) {
				this.playDownSound(Minecraft.getInstance().getSoundManager());
				this.onPress.run();
				return true;
			}
			return false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
			boolean selected = highlighted != null && highlighted.getAsBoolean();
			boolean hover = this.active && this.isHoveredOrFocused();
			int bg;
			int border;
			int textCol;
			if (!this.active) {
				bg = SourceTheme.SURFACE_ELEVATED;
				border = SourceTheme.BORDER_SUBTLE;
				textCol = SourceTheme.TEXT_MUTED;
			} else if (accentFill && selected) {
				bg = hover ? SourceTheme.ACCENT_HOVER : SourceTheme.ACCENT;
				border = SourceTheme.ACCENT;
				textCol = SourceTheme.ACCENT_TEXT;
			} else if (selected) {
				bg = hover ? SourceTheme.SURFACE_CONTROL_HOVER : SourceTheme.ACCENT_FILL;
				border = SourceTheme.ACCENT;
				textCol = SourceTheme.TEXT_PRIMARY;
			} else {
				bg = hover ? SourceTheme.SURFACE_CONTROL_HOVER : SourceTheme.SURFACE_CONTROL;
				border = hover ? SourceTheme.ACCENT_HOVER : SourceTheme.BORDER_SUBTLE;
				textCol = SourceTheme.TEXT_PRIMARY;
			}

			g.fill(getX(), getY(), getRight(), getBottom(), bg);
			g.outline(getX(), getY(), getWidth(), getHeight(), border);
			if (selected && !accentFill) {
				g.fill(getX() + 1, getBottom() - 2, getRight() - 1, getBottom() - 1, SourceTheme.ACCENT);
			}

			Font font = Minecraft.getInstance().font;
			String label = this.getMessage().getString();
			int textW = font.width(label);
			int maxW = Math.max(8, getWidth() - 8);
			if (textW > maxW) {
				label = font.plainSubstrByWidth(label, maxW - 6) + "..";
				textW = font.width(label);
			}
			int tx = getX() + (getWidth() - textW) / 2;
			int ty = getY() + (getHeight() - font.lineHeight) / 2 + 1;
			g.text(font, label, tx, ty, textCol, false);
			this.handleCursor(g);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput narration) {
			narration.add(NarratedElementType.TITLE, this.getMessage());
		}
	}

	public static class StudioToggle extends AbstractWidget {
		private final BooleanSupplier getter;
		private final Consumer<Boolean> setter;
		private final String label;

		public StudioToggle(
			int x,
			int y,
			int width,
			int height,
			String label,
			BooleanSupplier getter,
			Consumer<Boolean> setter
		) {
			super(x, y, width, height, Component.literal(label));
			this.label = label;
			this.getter = getter;
			this.setter = setter;
		}

		public StudioToggle withTooltip(String tip) {
			if (tip != null && !tip.isEmpty()) {
				this.setTooltip(Tooltip.create(Component.literal(tip)));
			}
			return this;
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			if (this.active && this.visible) {
				this.playDownSound(Minecraft.getInstance().getSoundManager());
				this.setter.accept(!this.getter.getAsBoolean());
			}
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (this.active && this.visible && isConfirmKey(event.key())) {
				this.playDownSound(Minecraft.getInstance().getSoundManager());
				this.setter.accept(!this.getter.getAsBoolean());
				return true;
			}
			return false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
			boolean on = getter.getAsBoolean();
			boolean hover = this.active && this.isHoveredOrFocused();
			int bg = hover ? SourceTheme.SURFACE_CONTROL_HOVER : SourceTheme.SURFACE_CONTROL;
			int border = hover ? SourceTheme.ACCENT : SourceTheme.BORDER_SUBTLE;
			g.fill(getX(), getY(), getRight(), getBottom(), bg);
			g.outline(getX(), getY(), getWidth(), getHeight(), border);

			Font font = Minecraft.getInstance().font;
			int ty = getY() + (getHeight() - font.lineHeight) / 2 + 1;
			g.text(font, label, getX() + 8, ty, this.active ? SourceTheme.TEXT_PRIMARY : SourceTheme.TEXT_MUTED, false);

			int pillW = 28;
			int pillH = 12;
			int px = getRight() - pillW - 7;
			int py = getY() + (getHeight() - pillH) / 2;
			int trackCol = on ? SourceTheme.ACCENT_FILL : SourceTheme.SURFACE_PRIMARY;
			int pillBorder = on ? SourceTheme.ACCENT : SourceTheme.BORDER_SUBTLE;
			g.fill(px, py, px + pillW, py + pillH, trackCol);
			g.outline(px, py, pillW, pillH, pillBorder);

			int knobSize = 8;
			int kx = on ? (px + pillW - knobSize - 2) : (px + 2);
			int ky = py + 2;
			g.fill(kx, ky, kx + knobSize, ky + knobSize, on ? SourceTheme.ACCENT : SourceTheme.TEXT_MUTED);
			this.handleCursor(g);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput narration) {
			narration.add(NarratedElementType.TITLE, Component.literal(label));
		}
	}

	public static class StudioCycleButton<T> extends AbstractWidget {
		private final String label;
		private final T[] values;
		private final Supplier<T> getter;
		private final Consumer<T> setter;
		private final java.util.function.Function<T, String> formatter;

		public StudioCycleButton(
			int x,
			int y,
			int width,
			int height,
			String label,
			T[] values,
			Supplier<T> getter,
			Consumer<T> setter,
			java.util.function.Function<T, String> formatter
		) {
			super(x, y, width, height, Component.literal(label));
			this.label = label;
			this.values = values;
			this.getter = getter;
			this.setter = setter;
			this.formatter = formatter;
		}

		public StudioCycleButton<T> withTooltip(String tip) {
			if (tip != null && !tip.isEmpty()) {
				this.setTooltip(Tooltip.create(Component.literal(tip)));
			}
			return this;
		}

		private void step(int delta) {
			T current = getter.get();
			int idx = 0;
			for (int i = 0; i < values.length; i++) {
				if (values[i].equals(current)) {
					idx = i;
					break;
				}
			}
			setter.accept(values[Math.floorMod(idx + delta, values.length)]);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			if (this.active && this.visible) {
				this.playDownSound(Minecraft.getInstance().getSoundManager());
				step(event.button() == 1 ? -1 : 1);
			}
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (this.active && this.visible && isConfirmKey(event.key())) {
				this.playDownSound(Minecraft.getInstance().getSoundManager());
				step(1);
				return true;
			}
			return false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
			boolean hover = this.active && this.isHoveredOrFocused();
			int bg = hover ? SourceTheme.SURFACE_CONTROL_HOVER : SourceTheme.SURFACE_CONTROL;
			int border = hover ? SourceTheme.ACCENT : SourceTheme.BORDER_SUBTLE;
			g.fill(getX(), getY(), getRight(), getBottom(), bg);
			g.outline(getX(), getY(), getWidth(), getHeight(), border);

			Font font = Minecraft.getInstance().font;
			int ty = getY() + (getHeight() - font.lineHeight) / 2 + 1;
			g.text(font, label, getX() + 8, ty, SourceTheme.TEXT_SECONDARY, false);

			String valStr = formatter.apply(getter.get());
			int vw = font.width(valStr);
			g.text(font, valStr, getRight() - vw - 8, ty, SourceTheme.ACCENT, false);
			this.handleCursor(g);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput narration) {
			narration.add(NarratedElementType.TITLE, Component.literal(label));
		}
	}

	public static class StudioSlider extends AbstractWidget {
		private final String label;
		private final Supplier<Double> getter;
		private final DoubleConsumer setter;
		private final Supplier<String> valueLabel;
		private boolean dragging = false;

		public StudioSlider(
			int x,
			int y,
			int width,
			int height,
			String label,
			Supplier<Double> getter,
			DoubleConsumer setter,
			Supplier<String> valueLabel
		) {
			super(x, y, width, height, Component.literal(label));
			this.label = label;
			this.getter = getter;
			this.setter = setter;
			this.valueLabel = valueLabel;
		}

		public StudioSlider withTooltip(String tip) {
			if (tip != null && !tip.isEmpty()) {
				this.setTooltip(Tooltip.create(Component.literal(tip)));
			}
			return this;
		}

		private void updateFromMouse(double mouseX) {
			double t = (mouseX - (getX() + 4)) / Math.max(1.0, getWidth() - 8.0);
			setter.accept(clamp01(t));
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			if (this.active && this.visible) {
				this.dragging = true;
				updateFromMouse(event.x());
			}
		}

		@Override
		protected void onDrag(MouseButtonEvent event, double deltaX, double deltaY) {
			if (this.active && this.visible && this.dragging) {
				updateFromMouse(event.x());
			}
		}

		@Override
		public void onRelease(MouseButtonEvent event) {
			if (this.dragging) {
				this.dragging = false;
				this.playDownSound(Minecraft.getInstance().getSoundManager());
			}
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (this.active && this.visible && this.isMouseOver(mouseX, mouseY) && scrollY != 0.0) {
				setter.accept(clamp01(getter.get() + Math.signum(scrollY) * 0.025));
				return true;
			}
			return false;
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (!this.active || !this.visible) {
				return false;
			}
			if (event.key() == InputConstants.KEY_LEFT || event.key() == InputConstants.KEY_RIGHT) {
				double delta = event.key() == InputConstants.KEY_RIGHT ? 0.03 : -0.03;
				setter.accept(clamp01(getter.get() + delta));
				return true;
			}
			return false;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
			double norm = clamp01(getter.get());
			boolean hover = this.active && (this.isHoveredOrFocused() || this.dragging);

			int bg = hover ? SourceTheme.SURFACE_CONTROL_HOVER : SourceTheme.SURFACE_CONTROL;
			int border = this.dragging ? SourceTheme.ACCENT : (hover ? SourceTheme.ACCENT_HOVER : SourceTheme.BORDER_SUBTLE);
			g.fill(getX(), getY(), getRight(), getBottom(), bg);
			g.outline(getX(), getY(), getWidth(), getHeight(), border);

			int fillW = (int) Math.round((getWidth() - 2) * norm);
			if (fillW > 0) {
				g.fill(getX() + 1, getY() + 1, getX() + 1 + fillW, getBottom() - 1, SourceTheme.ACCENT_FILL);
			}

			int handleX = getX() + 2 + (int) Math.round((getWidth() - 8) * norm);
			g.fill(handleX, getY() + 2, handleX + 3, getBottom() - 2, this.dragging ? 0xCC38BDF8 : 0x9922D3EE);
			g.fill(handleX, getBottom() - 3, handleX + 3, getBottom() - 1, SourceTheme.ACCENT);

			Font font = Minecraft.getInstance().font;
			int ty = getY() + (getHeight() - font.lineHeight) / 2 + 1;
			g.text(font, label, getX() + 8, ty, SourceTheme.TEXT_PRIMARY, false);

			String valStr = valueLabel.get();
			int vw = font.width(valStr);
			g.text(font, valStr, getRight() - vw - 8, ty, SourceTheme.TEXT_PRIMARY, false);
			this.handleCursor(g);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput narration) {
			narration.add(NarratedElementType.TITLE, Component.literal(label));
		}
	}
}
