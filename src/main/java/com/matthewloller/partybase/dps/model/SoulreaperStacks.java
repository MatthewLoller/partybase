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

import java.util.Locale;

/**
 * The Soulreaper axe's soul stacks: one per attack (hit or miss), at most five; one is lost every 50 ticks
 * without an attack; its special attack (Behead) uses them all, for 12% more accuracy each. Client thread only.
 */
public class SoulreaperStacks
{
	static final int MAX_STACKS = 5;
	static final int DECAY_TICKS = 50;

	private int stacks;
	private int lastChangeTick;

	/**
	 * You attacked.
	 * @param spec it was the special attack, which uses all stacks
	 */
	public void attack(int tick, String weapon, boolean spec)
	{
		decay(tick);
		if (weapon == null || !weapon.toLowerCase(Locale.ROOT).startsWith("soulreaper axe"))
		{
			return;
		}
		stacks = spec ? 0 : Math.min(MAX_STACKS, stacks + 1);
		lastChangeTick = tick;
	}

	public int stacks(int tick)
	{
		decay(tick);
		return stacks;
	}

	private void decay(int tick)
	{
		while (stacks > 0 && tick - lastChangeTick >= DECAY_TICKS)
		{
			stacks--;
			lastChangeTick += DECAY_TICKS;
		}
	}
}
