package dev.laybalt.skyblocktranslator.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import dev.laybalt.skyblocktranslator.config.ModConfig;
import dev.laybalt.skyblocktranslator.detect.HypixelDetector;
import dev.laybalt.skyblocktranslator.pipeline.TranslationEngine;

/**
 * Scoreboard sidebar translation. Every text draw inside
 * {@code displayScoreboardSidebar} (title, line names, score values) goes
 * through {@code GuiGraphicsExtractor.text(Font, Component, IIIZ)}; swapping the
 * component translates exactly what is on screen. Score values are numbers and
 * pass through the engine untouched.
 */
@Mixin(Hud.class)
public abstract class HudMixin {
	@ModifyArg(
			method = "displayScoreboardSidebar",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
			),
			index = 1
	)
	private Component skyblockTranslator$translateSidebar(Component original) {
		if (!ModConfig.get().surfaces.scoreboard || !HypixelDetector.isTranslationActive()) {
			return original;
		}
		return TranslationEngine.get().translate(original);
	}
}
