package dev.laybalt.skyblocktranslator.mixin;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import dev.laybalt.skyblocktranslator.config.ModConfig;
import dev.laybalt.skyblocktranslator.detect.HypixelDetector;
import dev.laybalt.skyblocktranslator.pipeline.TranslationEngine;

/** Boss bar titles (SkyBlock uses them for event/objective banners). */
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
	@ModifyArg(
			method = "extractRenderState",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
			),
			index = 1
	)
	private Component skyblockTranslator$translateBossBar(Component original) {
		if (!ModConfig.get().surfaces.bossBar || !HypixelDetector.isTranslationActive()) {
			return original;
		}
		return TranslationEngine.get().translate(original);
	}
}
