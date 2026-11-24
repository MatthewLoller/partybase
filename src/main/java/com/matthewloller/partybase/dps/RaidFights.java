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
package com.matthewloller.partybase.dps;

import com.matthewloller.partybase.dps.model.FightTracker;
import com.matthewloller.partybase.dps.model.RaidZone;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

/** One fight per raid: starts when you enter the raid instance and ends when you leave it. Client thread only. */
class RaidFights
{
	private static final String RAID_BEGAN = "The raid has begun!";

	private final Client client;
	private final ChatNotifier chat;
	private final FightTracker tracker;
	private final RaidZone raidZone = new RaidZone();

	RaidFights(Client client, ChatNotifier chat, FightTracker tracker)
	{
		this.client = client;
		this.chat = chat;
		this.tracker = tracker;
	}

	void tick(long now)
	{
		RaidZone.Change change = raidZone.tick(currentRaid());
		if (change.start != null)
		{
			tracker.startRaid(now, change.start);
			chat.info(change.start + " started. The meter counts the whole raid until you leave.");
		}
		else if (change.end)
		{
			tracker.endRaid(now, "Left the raid");
		}
	}

	/** CoX starts its timer with this message; ToB and ToA start theirs at the first room. */
	void gameMessage(long now, String message)
	{
		if (message.startsWith(RAID_BEGAN))
		{
			tracker.raidBegan(now);
		}
	}

	boolean isInsideRaid()
	{
		return raidZone.getInside() != null;
	}

	void reset()
	{
		raidZone.reset();
	}

	/** The raid whose instance you stand in, or null. */
	private String currentRaid()
	{
		Player me = client.getLocalPlayer();
		WorldView wv = client.getTopLevelWorldView();
		if (me == null || me.getLocalLocation() == null || wv == null)
		{
			return null;
		}
		WorldPoint pos = WorldPoint.fromLocalInstance(client, me.getLocalLocation());
		return pos == null ? null : RaidZone.raidFor(pos.getRegionID(), wv.isInstance());
	}
}
