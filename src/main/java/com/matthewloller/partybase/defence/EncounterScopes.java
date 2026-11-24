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

import net.runelite.api.WorldView;

/**
 * Static encounter classification shared by tracking, transport, and UI code.
 */
final class EncounterScopes
{
	static final int POSITION_TOLERANCE = 16;
	static final int HEALTH_PERCENT_TOLERANCE = 20;

	private EncounterScopes()
	{
	}

	static boolean isRaidEncounterBoss(BossDefence boss)
	{
		return raidScopeType(boss) != -1 || isToaBoss(boss) || isTfaBoss(boss);
	}

	static boolean isTfaBoss(BossDefence boss)
	{
		return boss != null && boss.has(BossDefence.Flag.TFA);
	}

	static boolean isToaBoss(BossDefence boss)
	{
		if (boss == null)
		{
			return false;
		}
		switch (boss)
		{
			case AKKHA:
			case AKKHAS_SHADOW:
			case BA_BA:
			case ELIDINIS_WARDEN:
			case KEPHRI:
			case OBELISK:
			case TUMEKENS_WARDEN:
			case ZEBAK:
				return true;
			default:
				return false;
		}
	}

	static int raidScopeType(BossDefence boss)
	{
		if (boss == null)
		{
			return -1;
		}
		if (boss.has(BossDefence.Flag.COX_SCALED))
		{
			return PartybaseDefenceSync.SCOPE_COX;
		}
		switch (boss)
		{
			case THE_MAIDEN_OF_SUGADINTI:
			case PESTILENT_BLOAT:
			case NYLOCAS_VASILIAS:
			case SOTETSEG:
			case XARPUS:
				return PartybaseDefenceSync.SCOPE_TOB;
			default:
				return -1;
		}
	}

	static int instanceFingerprint(WorldView worldView)
	{
		int hash = 17;
		hash = 31 * hash + worldView.getBaseX();
		hash = 31 * hash + worldView.getBaseY();
		int[][][] chunks = worldView.getInstanceTemplateChunks();
		if (chunks == null)
		{
			return hash;
		}
		for (int[][] plane : chunks)
		{
			if (plane == null)
			{
				continue;
			}
			for (int[] row : plane)
			{
				if (row == null)
				{
					continue;
				}
				for (int chunk : row)
				{
					hash = 31 * hash + chunk;
				}
			}
		}
		return hash;
	}
}
