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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which ended fights are reported to the party by themselves, and when: every boss kill, every
 * finished raid and every Barrows run (not each brother), a few ticks later so the last party hits are in.
 */
public class AutoReports
{
	/** Wait this many ticks for the last party hits to arrive. */
	public static final int DELAY_TICKS = 5;

	private final Map<Fight, Integer> pending = new LinkedHashMap<>();

	/** A fight ended at this tick. */
	public void fightEnded(Fight fight, int tick)
	{
		BossRecords.Kill kill = BossRecords.kill(fight);
		if (fight.isCompleteRaid() || (kill != null && !BarrowsRun.isBrother(kill.boss)))
		{
			pending.put(fight, tick + DELAY_TICKS);
		}
	}

	/** The fights to report now; each one only once. */
	public List<Fight> due(int tick)
	{
		List<Fight> out = new ArrayList<>();
		Iterator<Map.Entry<Fight, Integer>> it = pending.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<Fight, Integer> e = it.next();
			if (tick >= e.getValue())
			{
				out.add(e.getKey());
				it.remove();
			}
		}
		return out;
	}

	public void clear()
	{
		pending.clear();
	}
}
