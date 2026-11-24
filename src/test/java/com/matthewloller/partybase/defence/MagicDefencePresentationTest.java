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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.matthewloller.partybase.defence.DefenceTracker.DefenceState;
import org.junit.Test;

/** TFA Magic Defence presentation is disabled, without changing normal Defence tracking. */
public class MagicDefencePresentationTest
{
	private static DefenceState state(boolean tfaBoss, boolean drained)
	{
		return new DefenceState(12, "Example", 100, 0, 100,
				25000, 25000, 100, 100, 290, 290, drained, tfaBoss);
	}

	@Test
	public void tfaMagicIsHiddenBeforeAndAfterSpecs()
	{
		assertFalse(DefenceReadout.magicDefenceVisible(state(true, false)));
		assertFalse(DefenceReadout.magicDefenceVisible(state(true, true)));
	}

	@Test
	public void nonTfaMagicIsUnchanged()
	{
		assertTrue(DefenceReadout.magicDefenceVisible(state(false, false)));
		assertTrue(DefenceReadout.magicDefenceVisible(state(false, true)));
	}

	@Test
	public void nullTargetNeverDisplaysMagic()
	{
		assertFalse(DefenceReadout.magicDefenceVisible(null));
	}

	@Test
	public void everySupportedTfaBossIsExcluded()
	{
		for (BossDefence boss : BossDefence.values())
		{
			if (boss.has(BossDefence.Flag.TFA))
			{
				assertFalse(boss.name(), DefenceReadout.magicDefenceVisible(state(true, true)));
			}
		}
	}
}
