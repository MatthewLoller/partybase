/*
 * Copyright (c) 2026, Heisty <https://github.com/Falsary>
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
package com.matthewloller.partybase.defence;

import lombok.ToString;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.party.messages.PartyMemberMessage;

// Red Keris effect with original impact timing.
@ToString(doNotUseGetters = true)
public class PartybaseDefenceKerisTimer extends PartyMemberMessage
{
	static final int PROTOCOL_VERSION = 1;
	private static final int TICK_MILLIS = 600;

	private int protocolVersion;
	@ToString.Exclude
	private long partyId;
	private int world;
	private BossDefence boss;
	private int scopeType;
	private int scopeId;
	private int npcId;
	private int bossX;
	private int bossY;
	private int bossPlane;
	private long hitAtMillis;
	private String clockScope;
	private long hitRaidTick;

	public PartybaseDefenceKerisTimer()
	{
	}

	PartybaseDefenceKerisTimer(long partyId, int world, BossDefence boss, int scopeType, int scopeId,
		int npcId, WorldPoint point, long hitAtMillis, String clockScope, long hitRaidTick)
	{
		protocolVersion = PROTOCOL_VERSION;
		this.partyId = partyId;
		this.world = world;
		this.boss = boss;
		this.scopeType = scopeType;
		this.scopeId = scopeId;
		this.npcId = npcId;
		bossX = point.getX();
		bossY = point.getY();
		bossPlane = point.getPlane();
		this.hitAtMillis = hitAtMillis;
		this.clockScope = clockScope;
		this.hitRaidTick = hitRaidTick;
	}

	int remainingTicks(long now, String localClockScope, long raidTick)
	{
		int duration = BossEffectTracker.KERIS_EFFECT_TICKS;
		long age = now - hitAtMillis;
		if (hitAtMillis <= 0 || age < -TICK_MILLIS || age >= duration * TICK_MILLIS)
		{
			return 0;
		}
		if (clockScope != null && clockScope.equals(localClockScope) && hitRaidTick >= 0 && raidTick >= 0)
		{
			long elapsed = raidTick - hitRaidTick;
			return elapsed < 0 || elapsed >= duration ? 0 : duration - (int) elapsed;
		}
		return duration - (int) (Math.max(0, age) / TICK_MILLIS);
	}

	int getProtocolVersion()
	{
		return protocolVersion;
	}
	long getPartyId()
	{
		return partyId;
	}
	int getWorld()
	{
		return world;
	}
	BossDefence getBoss()
	{
		return boss == null ? null : boss.trackingType();
	}
	int getScopeType()
	{
		return scopeType;
	}
	int getScopeId()
	{
		return scopeId;
	}
	int getNpcId()
	{
		return npcId;
	}
	int getBossX()
	{
		return bossX;
	}
	int getBossY()
	{
		return bossY;
	}
	int getBossPlane()
	{
		return bossPlane;
	}
	long getHitAtMillis()
	{
		return hitAtMillis;
	}
}
