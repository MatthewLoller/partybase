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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.Random;
import org.junit.Test;

public class FunModelTest
{
	@Test
	public void hungryTalkShowsALineThenPauses()
	{
		HungryTalk talk = new HungryTalk(new Random(1));
		String first = talk.line(0);
		assertNotNull(first);
		assertEquals(first, talk.line(HungryTalk.SHOW_MS - 1));
		assertNull(talk.line(HungryTalk.SHOW_MS + 1));
		String next = talk.line(HungryTalk.SHOW_MS + HungryTalk.MAX_PAUSE_MS + 1);
		assertNotNull(next);
		assertNotEquals("never the same line twice in a row", first, next);
	}

	@Test
	public void topPlayersOfAFight()
	{
		FightTracker t = new FightTracker();
		t.playerHit(1000, "Kurt", PlayerKind.LOCAL, "1:1", 1, "Vorkath", true, 50, false);
		t.playerHit(1000, "Zezima", PlayerKind.PARTY, "1:1", 1, "Vorkath", true, 30, false);
		t.playerHit(2000, "Zezima", PlayerKind.PARTY, "1:1", 1, "Vorkath", true, 40, false);
		t.heal(2000, "Zezima", PlayerKind.PARTY, HealType.FOOD, 20);
		FightReport r = FightReport.of(t.getCurrent(), false, 3000);
		assertEquals(2, r.playerCount());
		assertEquals("Zezima", r.topDamage().name);
		assertEquals("Kurt", r.topHit().name);
		assertEquals("Zezima", r.topHealer().name);
	}
}
