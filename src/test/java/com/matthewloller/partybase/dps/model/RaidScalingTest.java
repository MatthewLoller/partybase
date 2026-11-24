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
import static org.junit.Assert.assertSame;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.BeforeClass;
import org.junit.Test;

public class RaidScalingTest
{
	private static NpcStats npcs;

	@BeforeClass
	public static void load() throws Exception
	{
		npcs = NpcStats.load();
	}

	private static NpcStats.Npc cox(String name, int partySize, boolean challengeMode)
	{
		RaidScaling raid = new RaidScaling();
		raid.update(RaidZone.COX, partySize, challengeMode, 99, 0);
		return raid.apply(npcs.get(-1, name));
	}

	@Test
	public void tektonInChallengeModeLikeTheCalculator()
	{
		// the OSRS Wiki DPS calculator's own test values
		assertEquals(246, cox("Tekton", 1, true).defence);
		assertEquals(248, cox("Tekton", 2, true).defence);
		assertEquals(250, cox("Tekton", 3, true).defence);
		assertEquals(284, cox("Tekton", 4, true).defence);
		assertEquals(287, cox("Tekton", 5, true).defence);
		assertEquals("magic is a defence stat for Tekton", 287, cox("Tekton", 5, true).magicLevel);
	}

	@Test
	public void soloWith99HitpointsChangesNothingInTheChambers() throws Exception
	{
		RaidScaling raid = new RaidScaling();
		raid.update(RaidZone.COX, 1, false, 99, 0);
		int checked = 0;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				NpcStats.class.getResourceAsStream("/com/matthewloller/partybase/dps/npc_stats.tsv"), StandardCharsets.UTF_8)))
		{
			for (String line; (line = reader.readLine()) != null; )
			{
				String[] cells = line.split("\t", -1);
				NpcStats.Npc npc = line.startsWith("#") ? null : npcs.get(Integer.parseInt(cells[0]), cells[1]);
				if (npc != null && npc.is("xerician"))
				{
					NpcStats.Npc scaled = raid.apply(npc);
					assertEquals(npc.name, npc.defence, scaled.defence);
					// only Olm's mage hand changes: its magic is halved
					int magic = npc.hasRule("olm-mage-hand") ? npc.magicLevel / 2 : npc.magicLevel;
					assertEquals(npc.name, magic, scaled.magicLevel);
					checked++;
				}
			}
		}
		assertEquals("all Chambers monsters", true, checked > 10);
	}

	@Test
	public void outsideRaidsNothingChanges()
	{
		NpcStats.Npc tekton = npcs.get(-1, "Tekton");
		assertSame(tekton, new RaidScaling().apply(tekton));
	}

	@Test
	public void theTombsMakeTheDefenceRollBigger()
	{
		RaidScaling raid = new RaidScaling();
		raid.update(RaidZone.TOA, 1, false, 99, 300);
		NpcStats.Npc zebak = npcs.get(-1, "Zebak");
		NpcStats.Npc scaled = raid.apply(zebak);
		assertEquals(zebak.defence, scaled.defence);
		long normal = HitChance.defenceRoll(WeaponStyles.Type.STAB, zebak, zebak.defence, zebak.magicLevel);
		assertEquals(normal * 550 / 250, HitChance.defenceRoll(WeaponStyles.Type.STAB, scaled, zebak.defence,
			zebak.magicLevel));
	}

	@Test
	public void someBossesHaveALowestDefence()
	{
		assertEquals(250, npcs.get(-1, "Nex").defenceFloor());
		NpcStats.Npc vardorvis = npcs.get(-1, "Vardorvis");
		assertEquals("cannot be lowered", vardorvis.defence, vardorvis.defenceFloor());
		assertEquals(0, npcs.get(-1, "Vorkath").defenceFloor());
	}
}
