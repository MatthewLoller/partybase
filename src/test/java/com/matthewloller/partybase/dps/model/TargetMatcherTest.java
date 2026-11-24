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
import org.junit.Before;
import org.junit.Test;

public class TargetMatcherTest
{
	private TargetMatcher m;

	@Before
	public void setUp()
	{
		m = new TargetMatcher();
		// my Vorkath, NPC number 5 on world 301
		m.seen("301:5", 9000, 4000, 0);
	}

	@Test
	public void sameNpcSameKey()
	{
		assertEquals("301:5", m.partyKey(301, 301, 5, 9003, 4002, 0, "Bob"));
	}

	@Test
	public void sameNumberInAnotherInstanceIsSeparate()
	{
		// Bob's own Vorkath instance: same world, same NPC number, far away
		assertEquals("p:Bob:301:5", m.partyKey(301, 301, 5, 9640, 4000, 0, "Bob"));
	}

	@Test
	public void otherWorldIsSeparate()
	{
		assertEquals("p:Bob:302:5", m.partyKey(301, 302, 5, 9000, 4000, 0, "Bob"));
	}

	@Test
	public void otherFloorIsSeparate()
	{
		assertEquals("p:Bob:301:5", m.partyKey(301, 301, 5, 9000, 4000, 1, "Bob"));
	}

	@Test
	public void npcNeverSeenIsSeparate()
	{
		assertEquals("p:Bob:301:6", m.partyKey(301, 301, 6, 9000, 4000, 0, "Bob"));
	}

	@Test
	public void separateKeysDoNotChangeOthersUnknown()
	{
		FightTracker t = new FightTracker();
		t.playerHit(0, "Me", PlayerKind.LOCAL, "301:5", 1, "Vorkath", true, 30, false);
		t.seenHit(0, "301:5", 1, "Vorkath", true, 30);
		t.seenHit(600, "301:5", 1, "Vorkath", true, 20); // a stranger at my Vorkath
		// Bob hits his own Vorkath in another instance (same NPC number)
		String bobKey = m.partyKey(301, 301, 5, 9640, 4000, 0, "Bob");
		t.playerHit(600, "Bob", PlayerKind.PARTY, bobKey, 1, "Vorkath", true, 50, false);

		FightReport r = FightReport.of(t.getCurrent(), false, 6000);
		assertEquals(20, r.row(FightReport.UNKNOWN_NAME).damage);
		assertEquals(50, r.row("Bob").damage);
	}
}
