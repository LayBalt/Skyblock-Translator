package dev.laybalt.skyblocktranslator.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.laybalt.skyblocktranslator.config.ModConfig;
import dev.laybalt.skyblocktranslator.detect.HypixelDetector;
import dev.laybalt.skyblocktranslator.pipeline.TranslationEngine;

/**
 * Tab list translation. SkyBlock fills the tab with fake entries whose display
 * names are info lines, but real player names appear there too — so entries are
 * translated with MT disabled (dictionary/cache/overrides only, names never get
 * sent to an online translator). Header and footer are pure info text and may
 * use the full pipeline.
 */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void skyblockTranslator$translateEntry(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		if (!ModConfig.get().translateTabList || !HypixelDetector.isTranslationActive()) {
			return;
		}
		Component original = cir.getReturnValue();
		Component translated = TranslationEngine.get().translate(original, false);
		if (translated != original) {
			cir.setReturnValue(translated);
		}
	}

	@ModifyVariable(method = "setHeader", at = @At("HEAD"), argsOnly = true)
	private Component skyblockTranslator$translateHeader(Component header) {
		return translateInfo(header);
	}

	@ModifyVariable(method = "setFooter", at = @At("HEAD"), argsOnly = true)
	private Component skyblockTranslator$translateFooter(Component footer) {
		return translateInfo(footer);
	}

	private static Component translateInfo(Component component) {
		if (component == null || !ModConfig.get().translateTabList || !HypixelDetector.isTranslationActive()) {
			return component;
		}
		return TranslationEngine.get().translate(component);
	}
}
