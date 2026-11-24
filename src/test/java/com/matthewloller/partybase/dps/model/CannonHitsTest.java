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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CannonHitsTest
{
	private final CannonHits cannon = new CannonHits();

	@Test
	public void aHitWhenTheBallLandsIsTheCannon()
	{
		cannon.fired("1:5", 102);
		assertFalse("too early", cannon.isCannonHit("1:5", 100));
		assertTrue(cannon.isCannonHit("1:5", 102));
		assertFalse("each ball counts once", cannon.isCannonHit("1:5", 102));
	}

	@Test
	public void aTickEarlyOrLateStillCounts()
	{
		cannon.fired("1:5", 102);
		cannon.fired("1:5", 106);
		assertTrue(cannon.isCannonHit("1:5", 103));
		assertTrue(cannon.isCannonHit("1:5", 105));
	}

	@Test
	public void otherMonstersAndOldBallsDoNotCount()
	{
		cannon.fired("1:5", 102);
		assertFalse(cannon.isCannonHit("1:6", 102));
		assertFalse("the ball's hitsplat never came", cannon.isCannonHit("1:5", 110));
		cannon.fired("1:5", 120);
		cannon.clear();
		assertFalse(cannon.isCannonHit("1:5", 120));
	}
}
