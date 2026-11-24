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
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class PartyNoticesTest
{
	private static Map<Long, String> members(Object... idName)
	{
		Map<Long, String> m = new HashMap<>();
		for (int i = 0; i < idName.length; i += 2)
		{
			m.put(((Number) idName[i]).longValue(), (String) idName[i + 1]);
		}
		return m;
	}

	@Test
	public void peopleAlreadyInThePartyAreNotAnnounced()
	{
		PartyNotices n = new PartyNotices();
		assertTrue(n.tick(0, 1, members()).isEmpty());
		// their names arrive 2 s later
		assertTrue(n.tick(2000, 1, members(5, "Zezima")).isEmpty());
	}

	@Test
	public void joinAndLeave()
	{
		PartyNotices n = new PartyNotices();
		n.tick(0, 1, members());
		List<PartyNotices.Notice> j = n.tick(10_000, 1, members(5, "Monster NRGs"));
		assertEquals(1, j.size());
		assertEquals("Monster NRGs", j.get(0).name);
		assertTrue(j.get(0).joined);
		assertTrue("only once", n.tick(11_000, 1, members(5, "Monster NRGs")).isEmpty());

		assertTrue("not yet: maybe a world hop", n.tick(20_000, 1, members()).isEmpty());
		List<PartyNotices.Notice> l = n.tick(20_000 + PartyNotices.BACK_MS, 1, members());
		assertEquals(1, l.size());
		assertFalse(l.get(0).joined);
	}

	@Test
	public void aHopOrReconnectIsNotAnnounced()
	{
		PartyNotices n = new PartyNotices();
		n.tick(0, 1, members());
		n.tick(10_000, 1, members(5, "Jern Henrik"));
		n.tick(20_000, 1, members());
		// back with a new member id a few seconds later
		assertTrue(n.tick(23_000, 1, members(9, "Jern Henrik")).isEmpty());
		assertTrue(n.tick(60_000, 1, members(9, "Jern Henrik")).isEmpty());
	}

	@Test
	public void kickedMembersGetNoLeftNotice()
	{
		PartyNotices n = new PartyNotices();
		n.tick(0, 1, members());
		n.tick(10_000, 1, members(5, "Monster NRGs"));
		n.kicked(5);
		assertTrue(n.tick(12_000, 1, members()).isEmpty());
	}

	@Test
	public void aNewPartyStartsQuietAgain()
	{
		PartyNotices n = new PartyNotices();
		n.tick(0, 1, members());
		n.tick(10_000, 1, members(5, "A"));
		// you joined another party: new member id
		assertTrue(n.tick(11_000, 2, members(7, "B")).isEmpty());
	}

	@Test
	public void theOneWhoJoinsHearsWhoIsInThePartyOnce()
	{
		PartyNotices n = new PartyNotices();
		n.tick(0, 1, members());
		assertTrue(n.welcome(0) == null);
		n.tick(2000, 1, members(5, "Zezima", 6, "Bob"));
		n.tick(PartyNotices.QUIET_MS, 1, members(5, "Zezima", 6, "Bob"));
		List<String> w = n.welcome(PartyNotices.QUIET_MS);
		assertEquals("you are in the party \"tjeerocka\" with Bob, Zezima.", PartyNotices.welcomeText("tjeerocka", w));
		assertTrue("only once", n.welcome(9000) == null);
		assertEquals("you are in the party \"x\". Nobody else is in it yet.",
			PartyNotices.welcomeText("x", new java.util.ArrayList<>()));
	}
}
