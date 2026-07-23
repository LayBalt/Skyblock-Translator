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
	void extractsSignedNumber() {
		Normalizer.Template t = Normalizer.normalize("Damage: +100");
		assertEquals("Damage: {0}", t.key());
		assertEquals(1, t.args().size());
		assertEquals("+100", t.args().get(0).raw());
	}

	@Test
	void extractsNumbersWithSeparatorsDecimalsAndPercent() {
		Normalizer.Template t = Normalizer.normalize("Crit Chance: +12.5% Coins: 1,234,567");
		assertEquals("Crit Chance: {0} Coins: {1}", t.key());
		assertEquals("+12.5%", t.args().get(0).raw());
		assertEquals("1,234,567", t.args().get(1).raw());
	}

	@Test
	void argumentsKeepTheirColorCodes() {
		Normalizer.Template t = Normalizer.normalize("§7Damage: §c+1,024");
		assertEquals("Damage: {0}", t.key());
		assertEquals("§c+1,024", t.args().get(0).raw());
		assertEquals("§7", t.args().get(0).after());
		assertEquals("Урон: §c+1,024§7", Normalizer.restore("Урон: {0}", t.args()));
	}

	@Test
	void surroundingColorIsRestoredAfterColoredArgument() {
		Normalizer.Template t = Normalizer.normalize("§7Gain §b+250§7 Wisdom");
		assertEquals("Gain {0} Wisdom", t.key());
		assertEquals("§b+250", t.args().get(0).raw());
		assertEquals("§7", t.args().get(0).after());
	}

	@Test
	void uncoloredArgumentAddsNothingAfter() {
		Normalizer.Template t = Normalizer.normalize("Requires Combat Skill 25!");
		assertEquals("Requires Combat Skill {0}!", t.key());
		assertEquals("25", t.args().get(0).raw());
		assertEquals("", t.args().get(0).after());
		assertEquals("Requires Combat Skill 25!", Normalizer.restore(t.key(), t.args()));
	}

	@Test
	void trailingPunctuationStaysOutsideTheNumber() {
		Normalizer.Template t = Normalizer.normalize("Reduced to 10.");
		assertEquals("Reduced to {0}.", t.key());
		assertEquals("10", t.args().get(0).raw());
	}

	@Test
	void restoreKeepsUnknownBracesLiteral() {
		List<Normalizer.Arg> args = List.of(new Normalizer.Arg("100", ""));
		assertEquals("Урон: {9} x", Normalizer.restore("Урон: {9} x", args));
		assertEquals("{нет} 100", Normalizer.restore("{нет} {0}", args));
	}

	@Test
	void placeholderCountCountsDistinctPlaceholders() {
		assertEquals(0, Normalizer.placeholderCount("no placeholders"));
		assertEquals(2, Normalizer.placeholderCount("a {0} b {1}"));
		assertEquals(1, Normalizer.placeholderCount("Урон: {0}"));
	}
}
