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
import java.util.List;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

public class AuditRegressionTest
{
	private SpecHistoryEntry event(String id, String clock, long tick, SpecialWeapon weapon, int hit)
	{
		return new SpecHistoryEntry(id, weapon, hit, id, clock, tick);
	}

	private EncounterTimeline.Replay<Integer> corp()
	{
		return new EncounterTimeline.Replay<Integer>()
		{
			public Integer apply(Integer defence, SpecHistoryEntry event)
			{
				if (event.getWeapon() == SpecialWeapon.DRAGON_WARHAMMER)
					return defence - defence * 30 / 100;
				return Math.max(0, defence - event.getHit() * 2);
			}
			public Integer conservative(Integer a, Integer b)
			{
				return Math.max(a, b);
			}
		};
	}

	@Test
	public void equivalentEffectsWithDifferentSuccessorsMustNotLoseLegalOrder()
	{
		List<SpecHistoryEntry> events = Arrays.asList(
				event("a", "clock-a", 0, SpecialWeapon.DRAGON_WARHAMMER, 1),
				event("c", "clock-a", 1, SpecialWeapon.BANDOS_GODSWORD, 20),
				event("b", "clock-b", 0, SpecialWeapon.DRAGON_WARHAMMER, 1),
				event("d", "clock-b", 1, SpecialWeapon.BANDOS_GODSWORD, 50));
		Integer exhaustive = EncounterTimeline.replay(events, 310, corp());
		assertEquals(Integer.valueOf(42), exhaustive);
		Integer optimized = EncounterTimeline.replay(events, 310, corp(),
			e -> e.getWeapon().name() + ":" + e.getHit());
		assertEquals("Optimization must preserve the highest Defence legal order", exhaustive, optimized);
	}

	@Test
	public void smallBackwardCorrectionMustNotReverseAcceptedLocalChronology()
	{
		RaidTimerClock clock = new RaidTimerClock();
		clock.sample(RaidTimerClock.SOURCE_TOA, "i1", 100, false, 1000);
		String domain = clock.domain();
		SpecHistoryEntry earlier = event("bgs", domain, clock.tickAt(1001), SpecialWeapon.BANDOS_GODSWORD, 20);
		clock.sample(RaidTimerClock.SOURCE_TOA, "i1", 100, false, 1002);
		SpecHistoryEntry later = event("dwh", clock.domain(), clock.tickAt(1002), SpecialWeapon.DRAGON_WARHAMMER, 1);
		assertFalse("A later local observation cannot precede an already accepted event",
			EncounterTimeline.before(later, earlier));
	}

	@Test
	public void lateJoinInCurrentRoomMustShareDomainWithContinuingClient()
	{
		RaidTimerClock continuing = new RaidTimerClock();
		RaidTimerClock joining = new RaidTimerClock();
		continuing.sample(RaidTimerClock.SOURCE_TOA, "i111", 100, false, 1000);
		continuing.sample(RaidTimerClock.SOURCE_TOA, "i222", 200, false, 1100);
		joining.sample(RaidTimerClock.SOURCE_TOA, "i222", 200, false, 5000);
		assertEquals(continuing.tickAt(1100), joining.tickAt(5000));
		assertEquals("Same raid/room time must use a comparable domain", continuing.domain(), joining.domain());
	}

	@Test
	public void coxOneTickSamplingUncertaintyMustNotForceSameTickAttacksIntoOrder()
	{
		RaidTimerClock alice = new RaidTimerClock();
		RaidTimerClock bob = new RaidTimerClock();
		alice.sample(RaidTimerClock.SOURCE_COX, "i1", 100, false, 1000);
		bob.sample(RaidTimerClock.SOURCE_COX, "i1", 101, false, 5000);
		// CoX docs explicitly allow a one-tick client sampling-phase difference.
		// These can be simultaneous game actions even though the samples differ by one.
		List<SpecHistoryEntry> simultaneous = Arrays.asList(
				event("dwh", alice.domain(), alice.tickAt(1000), SpecialWeapon.DRAGON_WARHAMMER, 1),
				event("bgs", bob.domain(), bob.tickAt(5000), SpecialWeapon.BANDOS_GODSWORD, 20));
		assertEquals("Unknown same-tick execution must retain the 189 conservative bound",
			Integer.valueOf(189), EncounterTimeline.replay(simultaneous, 310, corp()));
	}
}
