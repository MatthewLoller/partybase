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
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class RecentTargetTest
{
	private final RecentTarget<String> target = new RecentTarget<>();

	@Test
	public void keptFor15SecondsAfterYouStopAttacking()
	{
		assertEquals("Blue dragon", target.get(100, "Blue dragon", false, true));
		assertEquals("walking away", "Blue dragon", target.get(105, null, false, true));
		assertEquals("Blue dragon", target.get(100 + RecentTarget.KEEP_TICKS, null, false, true));
		assertNull(target.get(101 + RecentTarget.KEEP_TICKS, null, false, true));
	}

	@Test
	public void bossesAreKeptAMinute()
	{
		target.get(100, "Vorkath", true, true);
		assertEquals("acid phase", "Vorkath", target.get(100 + RecentTarget.KEEP_BOSS_TICKS, null, false, true));
		assertNull(target.get(101 + RecentTarget.KEEP_BOSS_TICKS, null, false, true));
	}

	@Test
	public void attackingAgainStartsTheTimeAgain()
	{
		target.get(100, "Blue dragon", false, true);
		target.get(110, "Blue dragon", false, true);
		assertEquals("Blue dragon", target.get(110 + RecentTarget.KEEP_TICKS, null, false, true));
	}

	@Test
	public void aNewTargetReplacesTheOld()
	{
		target.get(100, "Vorkath", true, true);
		assertEquals("Zulrah", target.get(101, "Zulrah", true, true));
	}

	@Test
	public void aDeadOrGoneMonsterIsDroppedAtOnce()
	{
		target.get(100, "Vorkath", true, true);
		assertNull(target.get(101, null, false, false));
	}
}
