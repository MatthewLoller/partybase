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
import org.junit.Test;

public class KillCountsTest
{
	@Test
	public void readsTheGameMessages()
	{
		KillCounts k = new KillCounts();
		k.message(1000, "Your Vorkath kill count is: <col=ff0000>1,234</col>.");
		k.message(1000, "Your Barrows chest count is: <col=ff0000>19</col>.");
		k.message(1000, "Your completed Chambers of Xeric count is: <col=ff0000>21</col>.");
		k.message(1000, "Your chest is worth around 36,190 coins.");
		assertEquals(1234, k.near("Vorkath", 3000));
		assertEquals(19, k.near("Barrows", 3000));
		assertEquals(21, k.near("Chambers of Xeric", 3000));
		assertEquals(0, k.near("Zulrah", 3000));
	}

	@Test
	public void raidModesCountForTheirRaid()
	{
		KillCounts k = new KillCounts();
		k.message(1000, "Your completed Tombs of Amascut: Expert Mode count is: <col=ff0000>12</col>.");
		k.message(1000, "Your completed Chambers of Xeric Challenge Mode count is: 3.");
		assertEquals(12, k.near("Tombs of Amascut", 2000));
		assertEquals(3, k.near("Chambers of Xeric", 2000));
	}

	@Test
	public void bossesCountedUnderAnotherName()
	{
		KillCounts k = new KillCounts();
		k.message(1000, "Your Grotesque Guardians kill count is: 40.");
		k.message(1000, "Your Lunar Chest count is: 7.");
		k.message(1000, "Your Corrupted Gauntlet completion count is: 15.");
		k.message(1000, "Your Nightmare kill count is: 9.");
		k.message(1000, "Your Royal Titans kill count is: 22.");
		assertEquals(40, k.near("Dusk", 2000));
		assertEquals(7, k.near("Blood Moon", 2000));
		assertEquals(15, k.near("Corrupted Hunllef", 2000));
		assertEquals(9, k.near("The Nightmare", 2000));
		assertEquals(22, k.near("Eldric the Ice King", 2000));
	}

	@Test
	public void anOldCountIsNotUsed()
	{
		KillCounts k = new KillCounts();
		k.message(1000, "Your Vorkath kill count is: 5.");
		assertEquals(0, k.near("Vorkath", 1000 + KillCounts.NEAR_MS + 1));
	}
}
