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
