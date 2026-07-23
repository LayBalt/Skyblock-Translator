package dev.laybalt.skyblocktranslator.pipeline;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NormalizerTest {
	@Test
	void plainStringWithoutNumbersIsUntouched() {
		Normalizer.Template t = Normalizer.normalize("SkyBlock Menu");
		assertEquals("SkyBlock Menu", t.key());
		assertTrue(t.args().isEmpty());
	}

	@Test
	void extractsSimpleNumber() {
		Normalizer.Template t = Normalizer.normalize("Damage: +100");
		assertEquals("Damage: +{0}", t.key());
		assertEquals(List.of("100"), t.args());
	}

	@Test
	void extractsNumbersWithSeparatorsDecimalsAndPercent() {
		Normalizer.Template t = Normalizer.normalize("Crit Chance: +12.5% Coins: 1,234,567");
		assertEquals("Crit Chance: +{0} Coins: {1}", t.key());
		assertEquals(List.of("12.5%", "1,234,567"), t.args());
	}

	@Test
	void restoreSubstitutesPlaceholdersBack() {
		Normalizer.Template t = Normalizer.normalize("Damage: +1,024");
		String restored = Normalizer.restore("Урон: +{0}", t.args());
		assertEquals("Урон: +1,024", restored);
	}

	@Test
	void restoreKeepsUnknownBracesLiteral() {
		assertEquals("Урон: +{9} x", Normalizer.restore("Урон: +{9} x", List.of("100")));
		assertEquals("{нет} 100", Normalizer.restore("{нет} {0}", List.of("100")));
	}

	@Test
	void roundTripNormalizeRestoreIsIdentity() {
		String original = "Requires Combat Skill 25!";
		Normalizer.Template t = Normalizer.normalize(original);
		assertEquals(original, Normalizer.restore(t.key(), t.args()));
	}
}
