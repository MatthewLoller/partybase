package com.matthewloller.partybase.dps.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Who leads the party. RuneLite parties have no leader and no kick, so Partybase keeps its own:
 * <ul>
 * <li>Alone in a new party (nobody else after {@link #WAIT_MS}): you are the leader.</li>
 * <li>Joining a party: the leader tells you ({@link #announce}).</li>
 * <li>Nobody says who leads (old party): the Partybase member who joined first leads (lowest id on a
 *     tie); every Partybase client works out the same one.</li>
 * <li>The leader leaves: the same rule picks the next one.</li>
 * <li>The leader can hand over to another Partybase member ({@link #handOver}).</li>
 * </ul>
 * Only the leader can kick, and only players who run Partybase can be kicked
 * (their Partybase leaves the party).
 */
public class PartyLeader
{
	/** How long to wait for the leader to answer after you joined. */
	public static final long WAIT_MS = 3000;

	private long local;
	private long leader;
	private long joinedMs;
	private final Set<Long> withPlugin = new HashSet<>();
	/** When each Partybase member joined the party (their own clock), as they told us. */
	private final Map<Long, Long> joinedAt = new HashMap<>();

	/** You joined a party (your member id is known). */
	public void joined(long localId, long now)
	{
		local = localId;
		leader = 0;
		joinedMs = now;
		withPlugin.clear();
		withPlugin.add(localId);
		joinedAt.clear();
		joinedAt.put(localId, now);
	}

	/** When you joined this party; 0 when in none. */
	public long getJoinedMs()
	{
		return local == 0 ? 0 : joinedMs;
	}

	/** A Partybase member told when they joined (0 = unknown). */
	public void sawJoin(long memberId, long since)
	{
		sawPlugin(memberId);
		if (memberId != 0 && since > 0)
		{
			joinedAt.put(memberId, since);
		}
	}

	/**
	 * When the party became active as far as its members know: the earliest join time of the members still
	 * there. 0 when in no party.
	 */
	public long partyStartMs(Collection<Long> members)
	{
		if (local == 0)
		{
			return 0;
		}
		long start = joinedMs;
		for (long id : members)
		{
			Long t = joinedAt.get(id);
			if (t != null && t < start)
			{
				start = t;
			}
		}
		return start;
	}

	/**
	 * You lead and give it to another Partybase member. Send the leader message right after.
	 *
	 * @return false when you don't lead or they don't run Partybase
	 */
	public boolean handOver(long memberId)
	{
		if (!isLeader() || memberId == local || !withPlugin.contains(memberId))
		{
			return false;
		}
		leader = memberId;
		return true;
	}

	/** You left the party. */
	public void left()
	{
		local = 0;
		leader = 0;
		withPlugin.clear();
		joinedAt.clear();
	}

	public boolean isJoined()
	{
		return local != 0;
	}

	/** Someone sent a Partybase message: they run the plugin. */
	public void sawPlugin(long memberId)
	{
		if (memberId != 0)
		{
			withPlugin.add(memberId);
		}
	}

	public boolean hasPlugin(long memberId)
	{
		return withPlugin.contains(memberId);
	}

	/**
	 * A Partybase member says who leads (0 = "I don't know yet").
	 * Taken when we don't know yet, or when the current leader hands it over.
	 */
	public void announce(long sender, long claimedLeader)
	{
		sawPlugin(sender);
		if (claimedLeader == 0)
		{
			return;
		}
		if (leader == 0 || sender == leader)
		{
			leader = claimedLeader;
		}
	}

	/** A member left. If it was the leader, the next one is picked. */
	public void memberLeft(long memberId, Collection<Long> stillThere)
	{
		withPlugin.remove(memberId);
		joinedAt.remove(memberId);
		if (memberId == leader)
		{
			leader = firstJoinedWithPlugin(stillThere);
		}
	}

	/**
	 * Call every game tick. When nobody said who leads after {@link #WAIT_MS}: alone = you lead,
	 * else the Partybase member who joined first.
	 *
	 * @return true when the leader changed (then the new leader should announce it)
	 */
	public boolean tick(long now, Collection<Long> members)
	{
		if (local == 0 || leader != 0 || now - joinedMs < WAIT_MS)
		{
			return false;
		}
		leader = members.size() <= 1 ? local : firstJoinedWithPlugin(members);
		return leader != 0;
	}

	/** The Partybase member who joined first; unknown join times count as last, ties go to the lowest id. */
	private long firstJoinedWithPlugin(Collection<Long> members)
	{
		long best = 0;
		long bestTime = Long.MAX_VALUE;
		for (long id : members)
		{
			if (!withPlugin.contains(id))
			{
				continue;
			}
			long t = joinedAt.getOrDefault(id, Long.MAX_VALUE);
			if (best == 0 || t < bestTime || (t == bestTime && id < best))
			{
				best = id;
				bestTime = t;
			}
		}
		return best;
	}

	/** Should you leave because of this kick? Only when the leader kicks you. */
	public boolean acceptKick(long sender, long target)
	{
		return local != 0 && target == local && sender == leader && sender != local;
	}

	public long getLeader()
	{
		return leader;
	}

	public boolean isLeader()
	{
		return local != 0 && leader == local;
	}
}
