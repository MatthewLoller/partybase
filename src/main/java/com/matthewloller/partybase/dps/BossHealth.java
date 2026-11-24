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

import com.matthewloller.partybase.dps.model.Fight;
import com.matthewloller.partybase.dps.model.TargetStats;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;

/** How far a boss is from dead, from its health bar. Client thread only. */
class BossHealth
{
	private final Client client;

	BossHealth(Client client)
	{
		this.client = client;
	}

	/**
	 * The part of the boss's health that is gone (0 to 1), or -1 when it is not known: no health bar,
	 * not in view, or several living bosses (Dagannoth Kings), where one bar says little.
	 */
	double done(Fight boss)
	{
		TargetStats alive = null;
		for (TargetStats t : boss.targets.values())
		{
			if (t.boss && t.engaged && !t.died)
			{
				if (alive != null)
				{
					return -1;
				}
				alive = t;
			}
		}
		NPC npc = alive == null ? null : npc(alive.key);
		if (npc == null || npc.getHealthScale() <= 0 || npc.getHealthRatio() < 0)
		{
			return -1;
		}
		return 1 - (double) npc.getHealthRatio() / npc.getHealthScale();
	}

	/** The NPC behind a target key ("world:index") in your scene, or null. */
	private NPC npc(String targetKey)
	{
		int split = targetKey.indexOf(':');
		WorldView view = client.getTopLevelWorldView();
		if (split < 0 || view == null || !targetKey.startsWith(client.getWorld() + ":"))
		{
			return null;
		}
		return view.npcs().byIndex(Integer.parseInt(targetKey.substring(split + 1)));
	}
}
