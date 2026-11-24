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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RaidZoneTest
{
	@Test
	public void regionsNeedAnInstance()
	{
		assertEquals(RaidZone.TOB, RaidZone.raidFor(12613, true));
		assertEquals(RaidZone.TFA, RaidZone.raidFor(16214, true));
		assertNull("same region but not instanced", RaidZone.raidFor(12613, false));
		assertNull("ToB lobby (Ver Sinhaza)", RaidZone.raidFor(14642, true));
	}

	@Test
	public void startsAtOnceAndEndsAfterSomeTicksOutside()
	{
		RaidZone z = new RaidZone();
		assertEquals(RaidZone.TOA, z.tick(RaidZone.TOA).start);
		assertNull(z.tick(RaidZone.TOA).start);
		// loading screen between rooms
		assertFalse(z.tick(null).end);
		assertNull(z.tick(RaidZone.TOA).start);
		for (int i = 1; i < RaidZone.LEAVE_TICKS; i++)
		{
			assertFalse(z.tick(null).end);
		}
		assertTrue(z.tick(null).end);
		assertNull(z.getInside());
	}

	@Test
	public void goingStraightToAnotherRaidStartsIt()
	{
		RaidZone z = new RaidZone();
		z.tick(RaidZone.TOB);
		assertEquals(RaidZone.COX, z.tick(RaidZone.COX).start);
	}
}
