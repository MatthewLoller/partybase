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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Your cannon's hits look like your own hits, so this remembers when each of your cannonballs lands on a
 * monster: a hit of yours on that monster at that moment is the cannon's. Client thread only.
 */
public class CannonHits
{
	public static final String CANNON = "Dwarf multicannon";
	/** A cannonball and its hitsplat can be a tick apart. */
	static final int TICKS_EARLY_OR_LATE = 1;

	/** Target key to the ticks your cannonballs land on it. */
	private final Map<String, List<Integer>> landing = new HashMap<>();

	/** Your cannon fired at this monster; the ball lands at this tick. */
	public void fired(String targetKey, int landTick)
	{
		landing.computeIfAbsent(targetKey, k -> new ArrayList<>()).add(landTick);
	}

	/** A hit of yours on this monster now: true when it is a cannonball (each ball counts once). */
	public boolean isCannonHit(String targetKey, int tick)
	{
		List<Integer> ticks = landing.get(targetKey);
		if (ticks == null)
		{
			return false;
		}
		boolean found = false;
		for (Iterator<Integer> it = ticks.iterator(); it.hasNext(); )
		{
			int land = it.next();
			if (land < tick - TICKS_EARLY_OR_LATE)
			{
				// missed its hitsplat, for example when the monster died first
				it.remove();
			}
			else if (!found && land <= tick + TICKS_EARLY_OR_LATE)
			{
				it.remove();
				found = true;
			}
		}
		if (ticks.isEmpty())
		{
			landing.remove(targetKey);
		}
		return found;
	}

	public void clear()
	{
		landing.clear();
	}
}
