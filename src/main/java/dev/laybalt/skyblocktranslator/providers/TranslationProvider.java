package dev.laybalt.skyblocktranslator.providers;

import org.jetbrains.annotations.Nullable;

/**
 * A source of translations for normalized templates.
 *
 * <p>Providers are queried in order (dictionary → local cache → remote); the
 * first non-null answer wins. Keys and values are placeholder templates, e.g.
 * {@code "Damage: +{0}"} → {@code "Урон: +{0}"}.
 */
public interface TranslationProvider {
	@Nullable
	String lookup(String templateKey);
}
