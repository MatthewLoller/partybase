/*
 * Copyright (c) 2026, Matthew Loller <matthewloller@gmail.com>
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/**
 * The NPCs the party leader marked (shift right-click, "Mark target"), each with an icon. Marked NPCs count as
 * bosses, so the boss meter follows them and the fight ends (and the leader's boss report goes out) once all of
 * them are dead. A mark is removed when its NPC dies or despawns; all marks go when the party changes.
 * Written on the client thread; the overlay and panel read snapshots.
 */
@Singleton
class MarkedTarget
{
	static final class Mark
	{
		final String key;
		final int npcId;
		final String name;
		final TargetIcon icon;

		Mark(String key, int npcId, String name, TargetIcon icon)
		{
			this.key = key;
			this.npcId = npcId;
			this.name = name;
			this.icon = icon;
		}
	}

	/** Copy on write, in marking order; replaced as a whole so readers need no lock. */
	private volatile Map<String, Mark> marks = Collections.emptyMap();

	/** Marks the NPC, or changes its icon when it is marked already. */
	void set(String key, int npcId, String name, TargetIcon icon)
	{
		Map<String, Mark> next = new LinkedHashMap<>(marks);
		next.put(key, new Mark(key, npcId, name == null ? "Target" : name, icon));
		marks = Collections.unmodifiableMap(next);
	}

	/** @return the removed mark, or null */
	Mark remove(String key)
	{
		Mark m = marks.get(key);
		if (m != null)
		{
			Map<String, Mark> next = new LinkedHashMap<>(marks);
			next.remove(key);
			marks = Collections.unmodifiableMap(next);
		}
		return m;
	}

	void clear()
	{
		marks = Collections.emptyMap();
	}

	boolean isEmpty()
	{
		return marks.isEmpty();
	}

	/** @param key world:npcIndex, see TargetMatcher.key */
	boolean is(String key)
	{
		return marks.containsKey(key);
	}

	/** The mark on this NPC, or null. */
	Mark get(String key)
	{
		return marks.get(key);
	}

	/** Every mark, in marking order. */
	List<Mark> all()
	{
		return new ArrayList<>(marks.values());
	}

	/** An icon no current mark uses, for the next one. */
	TargetIcon nextIcon()
	{
		List<TargetIcon> used = new ArrayList<>();
		for (Mark m : marks.values())
		{
			used.add(m.icon);
		}
		return TargetIcon.firstUnused(used);
	}
}
