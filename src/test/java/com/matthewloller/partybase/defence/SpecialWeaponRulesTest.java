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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

public class SpecialWeaponRulesTest
{
	@Test
	public void supportedWeaponsUseOneClassification()
	{
		assertTrue(SpecialWeaponRules.isSupported(SpecialWeapon.DRAGON_WARHAMMER));
		assertTrue(SpecialWeaponRules.drainsDefence(SpecialWeapon.DRAGON_WARHAMMER));
		assertFalse(SpecialWeaponRules.drainsMagicDefence(SpecialWeapon.DRAGON_WARHAMMER));

		assertTrue(SpecialWeaponRules.isSupported(SpecialWeapon.SEERCULL));
		assertFalse(SpecialWeaponRules.drainsDefence(SpecialWeapon.SEERCULL));
		assertTrue(SpecialWeaponRules.drainsMagicDefence(SpecialWeapon.SEERCULL));

		assertFalse(SpecialWeaponRules.isSupported(SpecialWeapon.BULWARK));
		assertFalse(SpecialWeaponRules.drainsDefence(null));
	}

	@Test
	public void equipmentIdIndexContainsEverySupportedVariantOnly()
	{
		for (SpecialWeapon weapon : SpecialWeapon.values())
		{
			for (int itemId : weapon.getItemID())
			{
				SpecialWeapon match = SpecialWeaponRules.byItemId(itemId);
				if (SpecialWeaponRules.isSupported(weapon))
				{
					assertSame(weapon, match);
				}
				else
				{
					assertNull(match);
				}
			}
		}
		assertSame(SpecialWeapon.DRAGON_WARHAMMER,
			SpecialWeaponRules.byItemId(ItemID.BH_DRAGON_WARHAMMER_CORRUPTED));
	}

	@Test
	public void reportedValuesPreserveDamageAndCountWeapons()
	{
		assertEquals(25, SpecialWeaponRules.reportedValue(SpecialWeapon.BANDOS_GODSWORD, 25));
		assertEquals(2, SpecialWeaponRules.reportedValue(SpecialWeapon.TONALZTICS_OF_RALOS, 2));
		assertEquals(1, SpecialWeaponRules.reportedValue(SpecialWeapon.DRAGON_WARHAMMER, 42));
		assertEquals(0, SpecialWeaponRules.reportedValue(SpecialWeapon.BANDOS_GODSWORD, -1));
	}
}
