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
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

public class AuditScopeReturnRegressionTest
{
	private SpecHistoryEntry event(String id, RaidTimerClock clock, int tick, SpecialWeapon weapon, int hit)
	{
		return new SpecHistoryEntry(id, weapon, hit, id, clock.domain(), clock.tickAt(tick));
	}

	private void assertConservative(SpecHistoryEntry earlier, SpecHistoryEntry later)
	{
		Integer result = EncounterTimeline.replay(Arrays.asList(earlier, later), 310,
			new EncounterTimeline.Replay<Integer>()
			{
				public Integer apply(Integer defence, SpecHistoryEntry e)
				{
					return e.getWeapon() == SpecialWeapon.DRAGON_WARHAMMER
						? defence - defence * 30 / 100
						: Math.max(0, defence - e.getHit() * 2);
				}
				public Integer conservative(Integer a, Integer b)
				{
					return Math.max(a, b);
				}
			});
		assertEquals("A room change cannot certify compatibility with pre-correction timestamps",
			Integer.valueOf(189), result);
	}

	@Test
	public void returningToPreviousRoomMustNotRestorePreCorrectionDomain()
	{
		RaidTimerClock clock = new RaidTimerClock();
		clock.sample(RaidTimerClock.SOURCE_TOA, "i111", 500, false, 1000);
		SpecHistoryEntry earlier = event("bgs", clock, 1001, SpecialWeapon.BANDOS_GODSWORD, 20);
		assertFalse(clock.sample(RaidTimerClock.SOURCE_TOA, "i111", 10, false, 1004));
		assertTrue(clock.sample(RaidTimerClock.SOURCE_TOA, "i111", 11, false, 1005));
		clock.sample(RaidTimerClock.SOURCE_TOA, "i222", 12, false, 1006);
		clock.sample(RaidTimerClock.SOURCE_TOA, "i111", 13, false, 1007);
		SpecHistoryEntry later = event("dwh", clock, 1007, SpecialWeapon.DRAGON_WARHAMMER, 1);
		assertConservative(earlier, later);
	}

	@Test
	public void scopeChangeWhileInactiveMustNotRestoreOldDomainOnReturn()
	{
		RaidTimerClock clock = new RaidTimerClock();
		clock.sample(RaidTimerClock.SOURCE_TOA, "i111", 500, false, 1000);
		SpecHistoryEntry earlier = event("bgs", clock, 1001, SpecialWeapon.BANDOS_GODSWORD, 20);
		assertFalse(clock.sample(RaidTimerClock.SOURCE_TOA, "i111", 10, false, 1004));
		assertTrue(clock.sample(RaidTimerClock.SOURCE_TOA, "i222", 11, false, 1005));
		clock.sample(RaidTimerClock.SOURCE_TOA, "i111", 12, false, 1006);
		SpecHistoryEntry later = event("dwh", clock, 1006, SpecialWeapon.DRAGON_WARHAMMER, 1);
		assertConservative(earlier, later);
	}
}
