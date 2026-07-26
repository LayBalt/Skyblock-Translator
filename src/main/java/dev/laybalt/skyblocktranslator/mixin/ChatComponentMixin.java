package dev.laybalt.skyblocktranslator.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.laybalt.skyblocktranslator.config.ModConfig;
import dev.laybalt.skyblocktranslator.detect.HypixelDetector;
import dev.laybalt.skyblocktranslator.pipeline.TranslationEngine;

/**
 * Chat translation. NPC dialogues on SkyBlock arrive as server system messages,
 * so {@code addServerSystemMessage} covers dialogues, quest text and
 * announcements; player messages are a separate (default-off) toggle.
 *
 * <p>Messages carrying click/hover events are left untouched for now: our
 * translated component is a flat literal and would drop the interaction.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@ModifyVariable(method = "addServerSystemMessage", at = @At("HEAD"), argsOnly = true)
	private Component skyblockTranslator$translateSystem(Component message) {
		if (!ModConfig.get().surfaces.dialogs || !HypixelDetector.isTranslationActive()
				|| hasInteraction(message)) {
			return message;
		}
		return TranslationEngine.get().translate(message);
	}

	@ModifyVariable(method = "addPlayerMessage", at = @At("HEAD"), argsOnly = true)
	private Component skyblockTranslator$translatePlayer(Component message) {
		if (!ModConfig.get().surfaces.playerChat || !HypixelDetector.isTranslationActive()
				|| hasInteraction(message)) {
			return message;
		}
		return TranslationEngine.get().translate(message);
	}

	private static boolean hasInteraction(Component component) {
		Style style = component.getStyle();
		if (style.getClickEvent() != null || style.getHoverEvent() != null) {
			return true;
		}
		for (Component sibling : component.getSiblings()) {
			if (hasInteraction(sibling)) {
				return true;
			}
		}
		return false;
	}
}
