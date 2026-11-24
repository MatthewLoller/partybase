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
package com.matthewloller.partybase.dps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import org.junit.Test;

public class PartyMessageTest
{
	@Test
	public void messageSurvivesJson()
	{
		PartybaseDpsUpdate u = new PartybaseDpsUpdate();
		assertTrue(u.isEmpty());
		u.world = 302;
		u.name = "Zezima";
		u.hits.add(new PartybaseDpsUpdate.Hit(1234, 8061, "Vorkath", 47, false, true, new net.runelite.api.coords.WorldPoint(2272, 4065, 0)));
		u.hits.add(new PartybaseDpsUpdate.Hit(1234, 8061, "Vorkath", 0, true, true, null));
		u.food = 20;
		u.setMemberId(99);
		assertFalse(u.isEmpty());

		Gson gson = new Gson();
		String json = gson.toJson(u);
		assertFalse("member id is set by the party server, not sent", json.contains("memberId"));
		assertTrue("short field names on the wire", json.contains("\"h\":[") && json.contains("\"a\":47"));

		PartybaseDpsUpdate back = gson.fromJson(json, PartybaseDpsUpdate.class);
		assertEquals(302, back.world);
		assertEquals("Zezima", back.name);
		assertEquals(2, back.hits.size());
		assertEquals(47, back.hits.get(0).amount);
		assertTrue(back.hits.get(1).miss);
		assertTrue(back.hits.get(0).boss);
		assertEquals(20, back.food);
		assertEquals(2272, back.hits.get(0).x);
		assertEquals(4065, back.hits.get(0).y);
	}

	@Test
	public void targetKeyUsesWorldAndIndex()
	{
		assertEquals("302:1234", com.matthewloller.partybase.dps.model.TargetMatcher.key(302, 1234));
	}

	@Test
	public void bossesByIdAndName()
	{
		Bosses b = new Bosses();
		assertTrue(b.isBoss(net.runelite.api.gameval.NpcID.VORKATH, "Vorkath"));
		assertTrue(b.isBoss(-1, "Duke Sucellus"));
		assertFalse(b.isBoss(-1, "Goblin"));
		assertFalse(b.isBoss(-1, null));
	}
}
