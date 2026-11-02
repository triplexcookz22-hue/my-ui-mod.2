package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputConstants;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ExampleModClient implements ClientModInitializer {
	private static boolean wasDown = false;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean down = InputConstants.isKeyDown(client.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
			if (down && !wasDown) {
				if (client.screen instanceof ClickGuiScreen gui) {
					gui.requestClose();
				} else if (client.screen == null) {
					client.setScreen(new ClickGuiScreen());
				}
			}
			wasDown = down;
		});
	}

	public static class ClickGuiScreen extends Screen {
		private record Category(String name, List<String> buttons) {}

		private final List<Category> categories = List.of(
			new Category("Combat", List.of("Aim Assist", "Auto Clicker", "Reach")),
			new Category("Movement", List.of("Sprint", "Speed", "Flight")),
			new Category("Render", List.of("Fullbright", "ESP", "Tracers")),
			new Category("Misc", List.of("Auto Reconnect", "Chat Tweaks"))
		);

		private static final int PANEL_WIDTH = 110;
		private static final int HEADER_HEIGHT = 18;
		private static final int BUTTON_HEIGHT = 16;
		private static final int GAP = 6;
		private static final float SPEED = 9f;

		private float progress = 0f;
		private boolean closing = false;
		private long lastFrame = System.nanoTime();

		public ClickGuiScreen() {
			super(Component.literal("UI"));
		}

		public void requestClose() {
			closing = true;
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		public void onClose() {
			requestClose();
		}

		@Override
		public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
			long now = System.nanoTime();
			float dt = (now - lastFrame) / 1_000_000_000f;
			lastFrame = now;

			float target = closing ? 0f : 1f;
			progress += (target - progress) * (1f - (float) Math.exp(-SPEED * dt));

			if (closing && progress < 0.01f) {
				Minecraft.getInstance().setScreen(null);
				return;
			}

			float eased = smooth(progress);
			int dimAlpha = (int) (eased * 120);
			graphics.fill(0, 0, width, height, dimAlpha << 24);

			int totalWidth = categories.size() * PANEL_WIDTH + (categories.size() - 1) * GAP;
			int startX = (width - totalWidth) / 2;
			int startY = 40;

			for (int i = 0; i < categories.size(); i++) {
				Category category = categories.get(i);

				float local = smooth(clamp(progress - i * 0.06f, 0f, 1f));
				int alpha = (int) (local * 255);
				if (alpha <= 0) continue;

				int x = startX + i * (PANEL_WIDTH + GAP);
				int y = startY + (int) ((1f - local) * 20f);

				graphics.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, argb(alpha, 0x7A4FD6));
				graphics.drawString(font, Component.literal(category.name()),
					x + 6, y + 5, argb(alpha, 0xFFFFFF));

				int by = y + HEADER_HEIGHT;
				for (String label : category.buttons()) {
					boolean hovered = mouseX >= x && mouseX < x + PANEL_WIDTH
						&& mouseY >= by && mouseY < by + BUTTON_HEIGHT;
					int bg = hovered ? 0x3E4A63 : 0x2B3242;
					graphics.fill(x, by, x + PANEL_WIDTH, by + BUTTON_HEIGHT, argb(alpha, bg));
					graphics.drawString(font, Component.literal(label),
						x + 8, by + 4, argb(alpha, 0xDDDDDD));
					by += BUTTON_HEIGHT;
				}
			}
		}

		private static float smooth(float t) {
			return t * t * (3f - 2f * t);
		}

		private static float clamp(float v, float lo, float hi) {
			return Math.max(lo, Math.min(hi, v));
		}

		private static int argb(int alpha, int rgb) {
			return (alpha << 24) | (rgb & 0xFFFFFF);
		}
	}
}
