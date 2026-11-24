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
import org.junit.Test;

public class AuditEpochIsolationRegressionTest
{
	private RaidTimerClock isolated()
	{
		RaidTimerClock clock = new RaidTimerClock();
		assertTrue(clock.sample(RaidTimerClock.SOURCE_TOA, "c77", 500, false, 1000));
		assertFalse(clock.sample(RaidTimerClock.SOURCE_TOA, "c77", 10, false, 1004));
		assertFalse(clock.isActive());
		assertTrue(clock.sample(RaidTimerClock.SOURCE_TOA, "c77", 11, false, 1005));
		return clock;
	}

	@Test
	public void ordinaryResampleMustNotRejoinPreCorrectionDomain()
	{
		RaidTimerClock clock = isolated();
		String isolatedDomain = clock.domain();
		assertTrue(isolatedDomain.endsWith(":g1"));
		assertTrue(clock.sample(RaidTimerClock.SOURCE_TOA, "c77", 12, false, 1006));
		assertEquals("Normal timer updates must preserve the isolated epoch", isolatedDomain, clock.domain());
	}

	@Test
	public void isolatedEpochMustRetainAdvertisedUncertainty()
	{
		RaidTimerClock clock = isolated();
		assertEquals("Appending generation must not turn an uncertain timer into an exact clock",
			RaidTimerClock.TIMER_UNCERTAINTY_TICKS, EncounterTimeline.tickUncertainty(clock.domain()));
	}

	@Test
	public void separateAmbiguousResetsMustNotReuseTheSameEpoch()
	{
		RaidTimerClock clock = isolated();
		String firstEpoch = clock.domain();
		assertFalse(clock.sample(RaidTimerClock.SOURCE_TOA, "c77", 3, false, 1010));
		assertTrue(clock.sample(RaidTimerClock.SOURCE_TOA, "c77", 4, false, 1011));
		assertNotEquals("Two incompatible mappings cannot reuse the same epoch domain", firstEpoch, clock.domain());
	}
}
