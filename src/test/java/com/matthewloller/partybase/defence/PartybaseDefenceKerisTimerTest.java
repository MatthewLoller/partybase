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
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class PartybaseDefenceKerisTimerTest
{
	private final PartybaseDefenceKerisTimer timer = new PartybaseDefenceKerisTimer(1, 301, BossDefence.CORPOREAL_BEAST,
		PartybaseDefenceSync.SCOPE_INSTANCE, 42, 123, new WorldPoint(100, 100, 0),
		10_000, "c42", 90);

	@Test
	public void raidClockUsesOriginalHitTick()
	{
		assertEquals(7, timer.remainingTicks(10_100, "c42", 93));
		assertEquals(1, timer.remainingTicks(15_000, "c42", 99));
		assertEquals(0, timer.remainingTicks(15_001, "c42", 100));
	}

	@Test
	public void fallbackExpiresFromImpactRatherThanPacketReceipt()
	{
		assertEquals(8, timer.remainingTicks(11_200, "c43", 90));
		assertEquals(1, timer.remainingTicks(15_999, null, RaidTimerClock.NO_TICK));
		assertEquals(0, timer.remainingTicks(16_000, null, RaidTimerClock.NO_TICK));
		assertEquals(0, timer.remainingTicks(9_399, null, RaidTimerClock.NO_TICK));
	}
}
