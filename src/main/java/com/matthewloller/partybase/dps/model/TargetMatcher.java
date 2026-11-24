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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decides if a party member's target is the same NPC as yours.
 *
 * <p>NPC numbers (index) are only unique inside one world or instance. Two party
 * members in different instances on the same world (for example each doing their
 * own Vorkath) can have NPCs with the same number. So we also compare where the NPC
 * is: every instance has its own map coordinates, far away from the others.</p>
 */
public class TargetMatcher
{
	/** Same NPC if it is this close (tiles) to where we last saw it. NPCs move between ticks. */
	static final int MAX_DISTANCE = 15;
	private static final int MAX_REMEMBERED = 512;

	private final Map<String, int[]> lastSeen = new LinkedHashMap<String, int[]>(64, 0.75f, true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, int[]> eldest)
		{
			return size() > MAX_REMEMBERED;
		}
	};

	public static String key(int world, int npcIndex)
	{
		return world + ":" + npcIndex;
	}

	/** Remember where an NPC near you is (call on hitsplats and when checking party hits). */
	public void seen(String key, int x, int y, int plane)
	{
		lastSeen.put(key, new int[] {x, y, plane});
	}

	/**
	 * Target key for a party member's hit. Your own key when it is the same NPC as yours,
	 * else a separate key for that member (so it never mixes with your targets).
	 */
	public String partyKey(int myWorld, int world, int npcIndex, int x, int y, int plane, String sender)
	{
		String key = key(world, npcIndex);
		if (world == myWorld)
		{
			int[] p = lastSeen.get(key);
			if (p != null && p[2] == plane && Math.abs(p[0] - x) <= MAX_DISTANCE && Math.abs(p[1] - y) <= MAX_DISTANCE)
			{
				return key;
			}
		}
		return "p:" + sender + ":" + key;
	}

	public void clear()
	{
		lastSeen.clear();
	}
}
