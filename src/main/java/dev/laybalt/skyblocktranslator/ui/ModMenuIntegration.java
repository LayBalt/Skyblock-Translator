package dev.laybalt.skyblocktranslator.ui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import net.minecraft.network.chat.Component;

import dev.laybalt.skyblocktranslator.config.ConfigHolder;

/** Puts the MoulConfig settings screen behind Mod Menu's "Configure" button. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new MoulConfigScreenComponent(
				Component.literal("SkyBlock Translator"),
				new GuiContext(new GuiElementComponent(ConfigHolder.managed().getEditor())),
				parent);
	}
}
