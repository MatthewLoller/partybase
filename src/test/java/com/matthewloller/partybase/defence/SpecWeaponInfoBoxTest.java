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

import static org.junit.Assert.*;
import com.matthewloller.partybase.defence.DefenceTracker.SpecHistoryEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

/** Ensures icon values use precisely the accepted BPD hit history. */
public class SpecWeaponInfoBoxTest
{
	private SpecHistoryEntry hit(String player, SpecialWeapon weapon, int amount)
	{
		return new SpecHistoryEntry(player, weapon, amount);
	}

	@Test
	public void percentageSpecsCountHitsWhileDamageSpecsSumTheirAmounts()
	{
		Map<SpecialWeapon, SpecWeaponSummary> result = SpecWeaponSummary.fromHistory(Arrays.asList(
			hit("A", SpecialWeapon.DRAGON_WARHAMMER, 45),
			hit("B", SpecialWeapon.DRAGON_WARHAMMER, 12),
			hit("A", SpecialWeapon.BANDOS_GODSWORD, 25),
			hit("B", SpecialWeapon.BANDOS_GODSWORD, 19),
			hit("A", SpecialWeapon.EYE_OF_AYAK, 18),
			hit("B", SpecialWeapon.TONALZTICS_OF_RALOS, 2)));
		assertEquals(2, result.get(SpecialWeapon.DRAGON_WARHAMMER).value);
		assertEquals(44, result.get(SpecialWeapon.BANDOS_GODSWORD).value);
		assertEquals(18, result.get(SpecialWeapon.EYE_OF_AYAK).value);
		assertEquals(2, result.get(SpecialWeapon.TONALZTICS_OF_RALOS).value);
	}

	@Test
	public void missesNeverInflateHitsOrDamage()
	{
		SpecWeaponSummary dwh = SpecWeaponSummary.fromHistory(Arrays.asList(
			hit("A", SpecialWeapon.DRAGON_WARHAMMER, 0),
			hit("B", SpecialWeapon.DRAGON_WARHAMMER, 12),
			hit("B", SpecialWeapon.DRAGON_WARHAMMER, 0)))
			.get(SpecialWeapon.DRAGON_WARHAMMER);
		assertEquals(1, dwh.value);
		assertEquals(2, dwh.missedAttempts);
		assertEquals(1, dwh.players.get("B").misses);
		assertTrue(dwh.tooltip().contains("Misses: 2"));
	}

	@Test
	public void currentHistoryAloneControlsVisibilityAndFreshEncounterResets()
	{
		assertTrue(SpecWeaponSummary.fromHistory(Collections.emptyList()).isEmpty());
		Map<SpecialWeapon, SpecWeaponSummary> first = SpecWeaponSummary.fromHistory(Arrays.asList(
			hit("A", SpecialWeapon.BANDOS_GODSWORD, 30)));
		Map<SpecialWeapon, SpecWeaponSummary> next = SpecWeaponSummary.fromHistory(Arrays.asList(
			hit("A", SpecialWeapon.BANDOS_GODSWORD, 4)));
		assertEquals(30, first.get(SpecialWeapon.BANDOS_GODSWORD).value);
		assertEquals(4, next.get(SpecialWeapon.BANDOS_GODSWORD).value);
	}
}
