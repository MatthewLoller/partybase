/*
 * Copyright (c) 2026, tjeerocka <https://github.com/tjeerocka>
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
package com.matthewloller.partybase.dps.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class HealTableTest
{
	@Test
	public void fixedFood()
	{
		assertEquals(20, HealTable.foodHeal("Shark", 99));
		assertEquals(18, HealTable.foodHeal("Cooked karambwan", 99));
		assertEquals(22, HealTable.foodHeal("Manta ray", 99));
		assertEquals(0, HealTable.foodBoost("Shark", 99));
		assertEquals(HealTable.UNKNOWN, HealTable.foodHeal("Bread", 99));
		assertEquals(HealTable.UNKNOWN, HealTable.foodHeal(null, 99));
	}

	@Test
	public void saradominBrew()
	{
		assertEquals(16, HealTable.foodHeal("Saradomin brew(4)", 99));
		assertEquals(16, HealTable.foodHeal("Saradomin brew(1)", 99));
		assertEquals(12, HealTable.foodHeal("Saradomin brew(3)", 70));
		assertEquals(16, HealTable.foodBoost("Saradomin brew(2)", 99));
	}

	@Test
	public void anglerfishByLevel()
	{
		assertEquals(3, HealTable.foodHeal("Anglerfish", 10));
		assertEquals(4, HealTable.foodHeal("Anglerfish", 20));
		assertEquals(22, HealTable.foodHeal("Anglerfish", 93));
		assertEquals(22, HealTable.foodHeal("Anglerfish", 99));
		assertEquals(22, HealTable.foodBoost("Blighted anglerfish", 99));
	}

	@Test
	public void lifesteal()
	{
		assertEquals(23, HealTable.sanguinesti(47));
		assertEquals(25, HealTable.bloodSpell(100, 0, "Kodai wand"));
		assertEquals(35, HealTable.bloodSpell(100, 5, "Kodai wand"));
		assertEquals(38, HealTable.bloodSpell(100, 5, "Ancient sceptre"));
		assertEquals(52, HealTable.bloodSpell(100, 5, "Zuriel's staff"));
		assertEquals(9, HealTable.bloodBoost("Blood ancient sceptre", 99));
		assertEquals(0, HealTable.bloodBoost("Kodai wand", 99));
		assertTrue(HealTable.isMagicWeapon("Kodai wand"));
		assertTrue(HealTable.isMagicWeapon("Blood ancient sceptre"));
		assertFalse(HealTable.isMagicWeapon("Scythe of vitur"));
	}
}
