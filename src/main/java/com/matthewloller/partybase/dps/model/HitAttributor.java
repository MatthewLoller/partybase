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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Guesses who did an "other" hitsplat on an NPC. The game does not tell us,
 * so we remember when other players start an attack animation on that NPC,
 * and give each hitsplat to the oldest attack that has not landed yet
 * (melee lands fast, ranged and magic a few ticks later).
 *
 * <p>It is a guess: when two players hit the same NPC in the same ticks it can
 * pick the wrong one. Hits we can't match stay "Others (unknown)".</p>
 */
public class HitAttributor
{
	/** Longest time from attack animation to hitsplat (long-range magic / ranged). */
	static final int MAX_DELAY_TICKS = 6;
	/** No weapon attacks faster than every 2 ticks: closer animations are not new attacks. */
	static final int MIN_ATTACK_GAP = 2;

	private static class Attack
	{
		final String player;
		final String target;
		final int tick;
		int matchedTick = Integer.MIN_VALUE;

		Attack(String player, String target, int tick)
		{
			this.player = player;
			this.target = target;
			this.tick = tick;
		}
	}

	/** Oldest first. */
	private final LinkedList<Attack> attacks = new LinkedList<>();
	private final Map<String, Integer> lastAttack = new HashMap<>();

	/** Another player started an attack animation while targeting this NPC. */
	public void attack(int tick, String player, String targetKey)
	{
		Integer last = lastAttack.get(player);
		if (last != null && tick - last < MIN_ATTACK_GAP && tick >= last)
		{
			return;
		}
		lastAttack.put(player, tick);
		attacks.add(new Attack(player, targetKey, tick));
	}

	/** Who probably did this hitsplat? Null if nobody fits. */
	public String assign(int tick, String targetKey)
	{
		expire(tick);

		// 1) the oldest attack on this NPC that has not landed yet
		for (Attack a : attacks)
		{
			if (a.matchedTick == Integer.MIN_VALUE && a.tick <= tick && a.target.equals(targetKey))
			{
				a.matchedTick = tick;
				return a.player;
			}
		}

		// 2) more hitsplats in the same tick (scythe, multi-hit weapons):
		//    only if one player landed a hit on this NPC this tick
		Set<String> landed = new LinkedHashSet<>();
		for (Attack a : attacks)
		{
			if (a.matchedTick == tick && a.target.equals(targetKey))
			{
				landed.add(a.player);
			}
		}
		return landed.size() == 1 ? landed.iterator().next() : null;
	}

	public void clear()
	{
		attacks.clear();
		lastAttack.clear();
	}

	private void expire(int tick)
	{
		Iterator<Attack> it = attacks.iterator();
		while (it.hasNext())
		{
			Attack a = it.next();
			if (tick - a.tick > MAX_DELAY_TICKS || a.tick > tick)
			{
				it.remove();
			}
		}
		if (lastAttack.size() > 200)
		{
			lastAttack.values().removeIf(t -> tick - t > MAX_DELAY_TICKS);
		}
	}
}
