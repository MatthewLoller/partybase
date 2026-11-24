package com.matthewloller.partybase.dps;

import javax.inject.Singleton;

/**
 * The NPC the party leader marked (shift right-click, "Mark target"). Its damage counts as a boss fight, so the
 * boss meter follows it and its death ends the fight and triggers the leader's boss report. Cleared when it dies,
 * despawns, or the party changes. Written on the client thread; the overlay and panel read it.
 */
@Singleton
class MarkedTarget
{
	private static final class Mark
	{
		final String key;
		final int npcId;
		final String name;

		Mark(String key, int npcId, String name)
		{
			this.key = key;
			this.npcId = npcId;
			this.name = name;
		}
	}

	private volatile Mark mark;

	void set(String key, int npcId, String name)
	{
		mark = new Mark(key, npcId, name == null ? "Target" : name);
	}

	void clear()
	{
		mark = null;
	}

	boolean isSet()
	{
		return mark != null;
	}

	/** @param key world:npcIndex, see TargetMatcher.key */
	boolean is(String key)
	{
		Mark m = mark;
		return m != null && m.key.equals(key);
	}

	String getKey()
	{
		Mark m = mark;
		return m == null ? null : m.key;
	}

	int getNpcId()
	{
		Mark m = mark;
		return m == null ? -1 : m.npcId;
	}

	/** The marked NPC's name, or null without a mark. */
	String getName()
	{
		Mark m = mark;
		return m == null ? null : m.name;
	}
}
