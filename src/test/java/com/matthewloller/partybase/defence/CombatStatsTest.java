/*
 * Copyright (c) 2026, Heisty <https://github.com/Falsary>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.matthewloller.partybase.defence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

public class CombatStatsTest
{
	@Test
	public void challengeModeTektonUsesMagicDefensiveScaling()
	{
		CombatStats stats = CombatStats.initial(BossDefence.TEKTON, 5, true, true);

		assertEquals(287, stats.defence);
		assertEquals(287, stats.startDefence);
		assertEquals(287, stats.magicLevel);
		assertEquals(287, stats.startMagicLevel);
	}

	@Test
	public void scalingOutsideCoxLeavesTheCatalogueBaselineUntouched()
	{
		CombatStats stats = CombatStats.initial(BossDefence.GREAT_OLM, 5, false, true);

		assertEquals(BossDefence.GREAT_OLM.getBaseDef(), stats.defence);
		assertEquals(BossDefence.GREAT_OLM.getBaseMagic(), stats.magicLevel);
	}

	@Test
	public void copiesAreIndependentAndBaselineResetsOnlyCombatState()
	{
		CombatStats stats = CombatStats.initial(BossDefence.CORPOREAL_BEAST, 1, false, false);
		stats.drained = true;
		stats.accursedApplied = true;
		stats.defence = 200;
		stats.magicLevel = 300;

		CombatStats copy = stats.copy();
		assertNotSame(stats, copy);
		assertEquals(200, copy.defence);
		assertEquals(300, copy.magicLevel);

		CombatStats baseline = stats.undrainedBaseline(BossDefence.CORPOREAL_BEAST);
		assertEquals(BossDefence.CORPOREAL_BEAST.getBaseDef(), baseline.defence);
		assertEquals(BossDefence.CORPOREAL_BEAST.getBaseMagic(), baseline.magicLevel);
		assertTrue(stats.drained);
		assertTrue(stats.accursedApplied);
		assertFalse(baseline.drained);
		assertFalse(baseline.accursedApplied);
	}

	@Test
	public void hammerAndMaulUseCurrentDefenceWithDistinctPercentages()
	{
		CombatStats hammer = CombatStats.initial(BossDefence.CORPOREAL_BEAST, 1, false, false);
		CombatStats maul = hammer.copy();
		hammer.defence = 199;
		maul.defence = 199;
		hammer.applyWeapon(SpecialWeapon.DRAGON_WARHAMMER, 1, null, "Corporeal Beast");
		maul.applyWeapon(SpecialWeapon.ELDER_MAUL, 1, null, "Corporeal Beast");
		assertEquals(140, hammer.defence);
		assertEquals(130, maul.defence);
	}

	@Test
	public void ralosTruncatesEachGlaiveBeforeMultiplying()
	{
		CombatStats stats = CombatStats.initial(BossDefence.CORPOREAL_BEAST, 1, false, false);
		stats.magicLevel = 15;
		stats.applyWeapon(SpecialWeapon.TONALZTICS_OF_RALOS, 2, null, "Corporeal Beast");
		assertEquals(308, stats.defence);
		stats.applyWeapon(SpecialWeapon.TONALZTICS_OF_RALOS, 0, null, "Corporeal Beast");
		assertEquals(308, stats.defence);
	}

	@Test
	public void lightWeaponsUseBaseDefenceAndKeepTheirDemonBonuses()
	{
		CombatStats arclight = CombatStats.initial(BossDefence.ABYSSAL_SIRE, 1, false, false);
		CombatStats emberlight = arclight.copy();
		arclight.defence = 200;
		emberlight.defence = 200;
		arclight.applyWeapon(SpecialWeapon.ARCLIGHT, 1, null, "Abyssal Sire");
		emberlight.applyWeapon(SpecialWeapon.EMBERLIGHT, 1, null, "Abyssal Sire");
		assertEquals(174, arclight.defence);
		assertEquals(162, emberlight.defence);
	}
}
