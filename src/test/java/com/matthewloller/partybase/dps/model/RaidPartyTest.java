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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class RaidPartyTest
{
	private static final String TOB = "ToB";
	private static final String P1 = RaidParty.PREFIX + "tob 111";
	private static final String P2 = RaidParty.PREFIX + "tob 222";

	private static String tickUntilStable(RaidParty r, String name, String current)
	{
		RaidParty.Action action = RaidParty.Action.NONE;
		for (int i = 0; i < RaidParty.STABLE_TICKS; i++)
		{
			action = r.tick(name, current, false);
		}
		return action.join;
	}

	@Test
	public void everyoneGetsTheSameName()
	{
		String a = RaidParty.partyName(TOB, 302, Arrays.asList("Kurt Yoghurt", "Zezima", "", null));
		String b = RaidParty.partyName(TOB, 302, Arrays.asList("zezima", "Kurt Yoghurt"));
		assertEquals(a, b);
		assertTrue(a.startsWith("partybase tob "));
	}

	@Test
	public void otherRaidWorldOrPeopleGiveAnotherName()
	{
		String base = RaidParty.partyName(TOB, 302, Arrays.asList("A", "B"));
		assertNotEquals(base, RaidParty.partyName("ToA", 302, Arrays.asList("A", "B")));
		assertNotEquals(base, RaidParty.partyName(TOB, 303, Arrays.asList("A", "B")));
		assertNotEquals(base, RaidParty.partyName(TOB, 302, Arrays.asList("A", "B", "C")));
	}

	@Test
	public void aloneIsNoParty()
	{
		assertNull(RaidParty.partyName(TOB, 302, Collections.singletonList("Kurt")));
		assertNull(RaidParty.partyName(TOB, 302, Collections.emptyList()));
	}

	@Test
	public void joinsOnceTheNamesAreStable()
	{
		RaidParty r = new RaidParty();
		for (int i = 1; i < RaidParty.STABLE_TICKS; i++)
		{
			assertNull(r.tick(P1, null, false).join);
		}
		assertEquals(P1, r.tick(P1, null, false).join);
		assertNull(r.tick(P1, P1, false).join);
	}

	@Test
	public void movesWhenTheRaidPartyChanges()
	{
		RaidParty r = new RaidParty();
		tickUntilStable(r, P1, null);
		assertEquals(P2, tickUntilStable(r, P2, P1));
	}

	@Test
	public void neverTouchesAManualParty()
	{
		RaidParty r = new RaidParty();
		for (int i = 0; i < 20; i++)
		{
			assertNull(r.tick(P1, "my friends", false).join);
			assertFalse(r.tick(null, "my friends", false).leave);
		}
	}

	@Test
	public void leavesOnceAfterTheRaid()
	{
		RaidParty r = new RaidParty();
		tickUntilStable(r, P1, null);
		for (int i = 1; i < RaidParty.LEAVE_TICKS; i++)
		{
			assertFalse(r.tick(null, P1, false).leave);
		}
		assertTrue(r.tick(null, P1, false).leave);
		assertFalse("only once", r.tick(null, P1, false).leave);
	}

	@Test
	public void staysInsideTheRaidWithoutAPartyId()
	{
		RaidParty r = new RaidParty();
		tickUntilStable(r, P1, null);
		for (int i = 0; i < 100; i++)
		{
			assertFalse(r.tick(null, P1, true).leave);
		}
		for (int i = 1; i < RaidParty.LEAVE_TICKS; i++)
		{
			assertFalse(r.tick(null, P1, false).leave);
		}
		assertTrue("leaves after the raid", r.tick(null, P1, false).leave);
	}

	@Test
	public void yourOwnPartyIsNeverLeftButARaidPartyReplacesIt()
	{
		String own = RaidParty.OWN_PREFIX + "blue cat";
		assertTrue(RaidParty.isAuto(own));
		assertTrue(RaidParty.isOwn(own));
		assertFalse(RaidParty.isOwn(P1));
		RaidParty r = new RaidParty();
		for (int i = 0; i < 3 * RaidParty.LEAVE_TICKS; i++)
		{
			assertFalse(r.tick(null, own, false).leave);
		}
		assertEquals("a raid party replaces your own party", P1, tickUntilStable(r, P1, own));
	}

	@Test
	public void insideCoxYouJoinTheRememberedParty()
	{
		RaidParty r = new RaidParty();
		tickUntilStable(r, P1, "my friends");    // in the lobby, still in your own party
		RaidParty.Action action = RaidParty.Action.NONE;
		for (int i = 0; i < RaidParty.STABLE_TICKS; i++)
		{
			// inside the raid the game shows no party id; you have left your party
			action = r.tick(null, null, true);
		}
		assertEquals(P1, action.join);
	}

	@Test
	public void aRememberedPartyIsUsedAfterARestartAndForgottenAfterTheRaid()
	{
		RaidParty r = new RaidParty();
		r.remember(P1);
		assertEquals(P1, tickUntilStableInRaid(r));
		for (int i = 0; i < RaidParty.LEAVE_TICKS; i++)
		{
			r.tick(null, P1, false);
		}
		assertNull(r.getRemembered());
		assertNull("outside a raid memory is not used", tickUntilStable(r, null, null));
	}

	private static String tickUntilStableInRaid(RaidParty r)
	{
		RaidParty.Action action = RaidParty.Action.NONE;
		for (int i = 0; i < RaidParty.STABLE_TICKS; i++)
		{
			action = r.tick(null, null, true);
		}
		return action.join;
	}

	@Test
	public void shortGapsWhileLoadingDoNotLeave()
	{
		RaidParty r = new RaidParty();
		tickUntilStable(r, P1, null);
		for (int i = 0; i < 30; i++)
		{
			assertFalse(r.tick(i % 5 == 0 ? P1 : null, P1, false).leave);
		}
		assertTrue(RaidParty.isAuto(P1));
		assertFalse(RaidParty.isAuto("my friends"));
	}

	@Test
	public void coxUsesTheRaidPartyId()
	{
		String a = RaidParty.coxPartyName(302, 12345, 0);
		assertEquals(a, RaidParty.coxPartyName(302, 12345, 3));
		assertTrue(a.startsWith(RaidParty.PREFIX + "cox "));
		assertNotEquals(a, RaidParty.coxPartyName(303, 12345, 3));
		assertNotEquals(a, RaidParty.coxPartyName(302, 999, 3));
		assertNull(RaidParty.coxPartyName(302, -1, 0));
		assertNull(RaidParty.coxPartyName(302, 12345, 1));
	}
}
