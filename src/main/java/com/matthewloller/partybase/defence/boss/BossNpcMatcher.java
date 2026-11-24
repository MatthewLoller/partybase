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
package com.matthewloller.partybase.defence.boss;

import com.matthewloller.partybase.defence.BossDefence;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.NPC;
import net.runelite.client.util.Text;

/**
 * Maps RuneLite NPC actors to their stable boss-stat definition.
 */
public final class BossNpcMatcher
{
	private static final int DAGONHAI_MONK_FIRST_ACTOR = 16645;
	private static final int DAGONHAI_MONK_SECOND_ACTOR = 16646;

	private static final Map<BossDefence, String> NORMALIZED_NAMES = buildNormalizedNames();

	private BossNpcMatcher()
	{
	}

	public static BossDefence matchingNpc(NPC npc)
	{
		if (npc == null)
		{
			return null;
		}
		if (OcellusActors.isAttackablePhaseTwo(npc))
		{
			return BossDefence.TFA_OCELLUS_P2;
		}
		if (npc.getName() == null)
		{
			return null;
		}

		String name = normalize(npc.getName());
		if ("Dagon'hai Monk".equalsIgnoreCase(name))
		{
			if (npc.getId() == DAGONHAI_MONK_FIRST_ACTOR)
			{
				return BossDefence.TFA_DAGONHAI_MONK_16645;
			}
			if (npc.getId() == DAGONHAI_MONK_SECOND_ACTOR)
			{
				return BossDefence.TFA_DAGONHAI_MONK_16646;
			}
			return null;
		}
		if (OcellusActors.isBladeName(name))
		{
			return null;
		}
		if (OcellusActors.isOcellusName(name))
		{
			return OcellusActors.matchingOcellus(npc);
		}
		return matchingNpcName(name);
	}

	public static BossDefence matchingNpcName(String name)
	{
		if (name == null || name.trim().isEmpty())
		{
			return null;
		}

		String normalized = normalize(name);
		if (OcellusActors.isBladeName(normalized))
		{
			return null;
		}

		BossDefence prefixMatch = null;
		for (Map.Entry<BossDefence, String> entry : NORMALIZED_NAMES.entrySet())
		{
			BossDefence boss = entry.getKey();
			if (boss == BossDefence.TFA_DREADNOTH && isDreadnothJellied(normalized))
			{
				return boss;
			}

			String candidate = entry.getValue();
			if (candidate.equalsIgnoreCase(normalized))
			{
				return boss.trackingType();
			}
			if (prefixMatch == null
				&& normalized.regionMatches(true, 0, candidate, 0, candidate.length())
				&& normalized.length() > candidate.length()
				&& normalized.charAt(candidate.length()) == ' ')
			{
				prefixMatch = boss;
			}
		}
		return prefixMatch == null ? null : prefixMatch.trackingType();
	}

	public static boolean isAttackableOcellusPhaseTwo(NPC npc)
	{
		return OcellusActors.isAttackablePhaseTwo(npc);
	}

	public static boolean isLiveNpc(NPC npc)
	{
		return npc != null && !npc.isDead()
			&& (npc.getHealthRatio() != 0 || isAttackableOcellusPhaseTwo(npc));
	}

	private static boolean isDreadnothJellied(String name)
	{
		String normalized = name.toLowerCase(Locale.ROOT);
		return normalized.contains("dreadnoth") && normalized.contains("jellied");
	}

	private static String normalize(String name)
	{
		return Text.removeTags(name).trim();
	}

	private static Map<BossDefence, String> buildNormalizedNames()
	{
		Map<BossDefence, String> names = new EnumMap<>(BossDefence.class);
		for (BossDefence boss : BossDefence.values())
		{
			names.put(boss, normalize(boss.getNpcName()));
		}
		return names;
	}
}
