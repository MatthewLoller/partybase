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

/**
 * The monster you attack, kept for a while after you stop (to walk, eat or dodge), so its hit chance stays
 * shown: 15 seconds, a minute for bosses (Vorkath's acid, Zulrah's and Hydra's phases). Client thread only.
 * @param <T> the monster
 */
public class RecentTarget<T>
{
	static final int KEEP_TICKS = 25;
	static final int KEEP_BOSS_TICKS = 100;

	private T target;
	private int keepUntilTick;

	/**
	 * @param now the monster you attack now, or null
	 * @param boss the monster you attack now is a boss
	 * @param stillThere false when the kept monster died or is gone
	 * @return the monster to show, or null
	 */
	public T get(int tick, T now, boolean boss, boolean stillThere)
	{
		if (now != null)
		{
			target = now;
			keepUntilTick = tick + (boss ? KEEP_BOSS_TICKS : KEEP_TICKS);
		}
		else if (target != null && (!stillThere || tick > keepUntilTick))
		{
			target = null;
		}
		return target;
	}

	/** The monster kept now, or null. */
	public T kept()
	{
		return target;
	}
}
