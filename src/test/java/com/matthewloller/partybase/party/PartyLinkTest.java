/*
 * Copyright (c) 2026, Matthew Loller <matthewloller@gmail.com>
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
package com.matthewloller.partybase.party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import net.runelite.api.Item;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.client.party.messages.PartyData;
import org.junit.Test;

public class PartyLinkTest
{
	@Test
	public void onlySecureOrLocalServerUrlsAreUsed()
	{
		assertNull(PartyLink.validServerUrl(null));
		assertNull(PartyLink.validServerUrl("  "));
		assertNull("plain ws to another host", PartyLink.validServerUrl("ws://example.com/prod"));
		assertNull(PartyLink.validServerUrl("https://example.com"));
		assertEquals("wss://x.example.com/prod", PartyLink.validServerUrl(" wss://x.example.com/prod "));
		assertEquals("ws://localhost:8080", PartyLink.validServerUrl("ws://localhost:8080"));
	}

	@Test
	public void passphrasesBecomeRelayPartyNames()
	{
		assertEquals("rune-scimitar-lobster-cape", PartyLink.relayParty("rune-scimitar-lobster-cape"));
		assertEquals("my-party-1", PartyLink.relayParty(" my party_1 "));
		assertEquals(64, PartyLink.relayParty(new String(new char[100]).replace('\0', 'a')).length());
	}

	@Test
	public void partyIdMatchesRuneLite()
	{
		String p = "rune-scimitar-lobster-cape";
		long runelite = Hashing.sha256().hashBytes(p.getBytes(StandardCharsets.UTF_8)).asLong() & Long.MAX_VALUE;
		assertEquals(runelite, PartyLink.passphraseToId(p));
	}

	@Test
	public void relayMembersReadPartyDataLikeRuneLite()
	{
		RelayMember m = new RelayMember(5, "");
		assertEquals("<unknown>", m.getDisplayName());
		assertFalse(m.isLoggedIn());

		PartyData d = new PartyData();
		d.setCharacterName("Zezima");
		d.setRunEnergy(70);
		d.setSpecEnergy(50);
		d.setInv(new int[] {995, 100, -1, 0, 4151, 1});
		d.setPrayers((1L << Prayer.PIETY.ordinal()) | (1L << Prayer.PROTECT_FROM_MELEE.ordinal()));
		int[] base = new int[Skill.values().length];
		base[Skill.ATTACK.ordinal()] = 99;
		d.setStatBase(base);
		m.apply(d);

		assertEquals("Zezima", m.getDisplayName());
		assertTrue(m.isLoggedIn());
		assertEquals(70, m.getRunEnergy());
		assertEquals(50, m.getSpecEnergy());
		assertEquals(new Item(995, 100), m.getInv()[0]);
		assertNull("empty slot", m.getInv()[1]);
		assertEquals(new Item(4151, 1), m.getInv()[2]);
		assertTrue(m.getPrayers().contains(Prayer.PIETY));
		assertTrue(m.getPrayers().contains(Prayer.PROTECT_FROM_MELEE));
		assertEquals(99, m.getStatBase(Skill.ATTACK));

		// fields not in the next update are kept; an empty name means logged out
		PartyData next = new PartyData();
		next.setCharacterName("");
		m.apply(next);
		assertFalse(m.isLoggedIn());
		assertEquals("Zezima", m.getDisplayName());
		assertEquals(70, m.getRunEnergy());
	}
}
