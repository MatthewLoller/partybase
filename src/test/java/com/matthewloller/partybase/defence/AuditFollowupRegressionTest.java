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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import org.junit.Test;

public class AuditFollowupRegressionTest
{
	private SpecHistoryEntry event(String id, String domain, long tick, SpecialWeapon weapon, int hit)
	{
		return new SpecHistoryEntry(id, weapon, hit, id, domain, tick);
	}

	private EncounterTimeline.Replay<Integer> corp()
	{
		return new EncounterTimeline.Replay<Integer>()
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
		};
	}

	@Test
	public void ignoredCorrectionMustNotForceSimultaneousClientsIntoExactOrder()
	{
		RaidTimerClock continuing = new RaidTimerClock();
		RaidTimerClock joining = new RaidTimerClock();
		continuing.sample(RaidTimerClock.SOURCE_TOA, "c77", 100, false, 1000);
		// Both now receive raid100 at the same actual game tick. One client has history,
		// the other establishes its mapping from the corrected sample.
		continuing.sample(RaidTimerClock.SOURCE_TOA, "c77", 100, false, 1002);
		joining.sample(RaidTimerClock.SOURCE_TOA, "c77", 100, false, 5000);
		assertEquals(continuing.domain(), joining.domain());
		List<SpecHistoryEntry> sameTick = Arrays.asList(
				event("bgs", continuing.domain(), continuing.tickAt(1002), SpecialWeapon.BANDOS_GODSWORD, 20),
				event("dwh", joining.domain(), joining.tickAt(5000), SpecialWeapon.DRAGON_WARHAMMER, 1));
		assertEquals("Same-tick specs must retain the smallest-drain bound after a correction",
			Integer.valueOf(189), EncounterTimeline.replay(sameTick, 310, corp()));
	}

	@Test
	public void largerCorrectionMustNotReverseEarlierImmutableEvents()
	{
		RaidTimerClock clock = new RaidTimerClock();
		clock.sample(RaidTimerClock.SOURCE_TOA, "c77", 100, false, 1000);
		SpecHistoryEntry first = event("bgs", clock.domain(), clock.tickAt(1001), SpecialWeapon.BANDOS_GODSWORD, 20);
		clock.sample(RaidTimerClock.SOURCE_TOA, "c77", 100, false, 1004);
		SpecHistoryEntry second = event("dwh", clock.domain(), clock.tickAt(1004), SpecialWeapon.DRAGON_WARHAMMER, 1);
		assertFalse("A larger backward sample cannot reuse an exact domain and reverse old events",
			EncounterTimeline.before(second, first));
	}

	@Test
	public void scopeTransitionMustNotBeLostWhenSampleIsSlightlyBehind()
	{
		RaidTimerClock continuing = new RaidTimerClock();
		RaidTimerClock joining = new RaidTimerClock();
		continuing.sample(RaidTimerClock.SOURCE_TOA, "i111", 100, false, 1000);
		continuing.sample(RaidTimerClock.SOURCE_TOA, "c77", 109, false, 1010);
		joining.sample(RaidTimerClock.SOURCE_TOA, "c77", 109, false, 5000);
		assertEquals("The old scene fallback must not stick after a controller becomes available",
			joining.domain(), continuing.domain());
	}

	@Test
	public void successorAwarePruningMatchesFullSearchAcrossClockLayouts()
	{
		Random random = new Random(627);
		for (int trial = 0; trial < 256; trial++)
		{
			List<SpecHistoryEntry> events = new ArrayList<>();
			for (int i = 0; i < 6; i++)
			{
				SpecialWeapon weapon = random.nextBoolean()
					? SpecialWeapon.DRAGON_WARHAMMER
					: SpecialWeapon.BANDOS_GODSWORD;
				events.add(event("e" + i, "clock" + random.nextInt(3), random.nextInt(3), weapon,
					weapon == SpecialWeapon.DRAGON_WARHAMMER ? 1 : 10 + 10 * random.nextInt(3)));
			}
			assertEquals("Trial " + trial, EncounterTimeline.replay(events, 310, corp()),
				EncounterTimeline.replay(events, 310, corp(), e -> e.getWeapon().name() + ":" + e.getHit()));
		}
	}
}
