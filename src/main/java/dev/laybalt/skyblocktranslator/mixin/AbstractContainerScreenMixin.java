package dev.laybalt.skyblocktranslator.mixin;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import dev.laybalt.skyblocktranslator.config.ModConfig;
import dev.laybalt.skyblocktranslator.detect.HypixelDetector;
import dev.laybalt.skyblocktranslator.pipeline.TranslationEngine;

/**
 * Render-level translation of container screens — which is where all of
 * SkyBlock's UI lives (its menus are chest GUIs).
 *
 * <p>Item tooltips: the line list returned by {@code getTooltipFromContainerItem}
 * is rebuilt every frame, so replacing entries here is a pure display change —
 * the underlying ItemStack stays untouched and other mods keep seeing English.
 *
 * <p>Screen titles: {@code extractLabels} draws the container title and the
 * inventory label through {@code GuiGraphicsExtractor.text(...)}; swapping the
 * component argument translates exactly what is drawn and nothing else.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
	private void skyblockTranslator$translateTooltip(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
		if (!ModConfig.get().translateItems || !HypixelDetector.isTranslationActive()) {
			return;
		}
		List<Component> lines = cir.getReturnValue();
		List<Component> translated = TranslationEngine.get().translateLines(lines);
		if (translated != lines) {
			cir.setReturnValue(translated);
		}
	}

	@ModifyArg(
			method = "extractLabels",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
			),
			index = 1
	)
	private Component skyblockTranslator$translateLabel(Component original) {
		if (!ModConfig.get().translateMenus || !HypixelDetector.isTranslationActive()) {
			return original;
		}
		return TranslationEngine.get().translate(original);
	}
}
