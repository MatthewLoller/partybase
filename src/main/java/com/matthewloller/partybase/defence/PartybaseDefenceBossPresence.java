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
import net.runelite.client.party.messages.PartyMemberMessage;

// Request stored defence history after a boss appears.
@ToString(doNotUseGetters = true)
public class PartybaseDefenceBossPresence extends PartyMemberMessage
{
	public static final int PROTOCOL_VERSION = 1;

	private int protocolVersion;
	private int world;
	private String bossType;
	private int scopeType;
	private int scopeId;
	private int bossX;
	private int bossY;
	private int bossPlane;
	private int healthPercent;
	private long sentAtMillis;
	private PartybaseDefenceSync offeredHistory;
	// Sender party session; zero is legacy.
	@ToString.Exclude
	private long partyId;

	// Gson.
	public PartybaseDefenceBossPresence()
	{
	}

	public PartybaseDefenceBossPresence(
		int world,
		BossDefence boss,
		int scopeType,
		int scopeId,
		int bossX,
		int bossY,
		int bossPlane,
		int healthPercent)
	{
		this.protocolVersion = PROTOCOL_VERSION;
		this.world = world;
		this.bossType = boss == null ? null : boss.name();
		this.scopeType = scopeType;
		this.scopeId = scopeId;
		this.bossX = bossX;
		this.bossY = bossY;
		this.bossPlane = bossPlane;
		this.healthPercent = healthPercent;
		this.sentAtMillis = System.currentTimeMillis();
	}

	public BossDefence toBossType()
	{
		if (bossType == null)
		{
			return null;
		}
		try
		{
			return BossDefence.valueOf(bossType).trackingType();
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	public int getProtocolVersion()
	{
		return protocolVersion;
	}
	public int getWorld()
	{
		return world;
	}
	public int getScopeType()
	{
		return scopeType;
	}
	public int getScopeId()
	{
		return scopeId;
	}
	public int getBossX()
	{
		return bossX;
	}
	public int getBossY()
	{
		return bossY;
	}
	public int getBossPlane()
	{
		return bossPlane;
	}
	public int getHealthPercent()
	{
		return healthPercent;
	}
	public long getSentAtMillis()
	{
		return sentAtMillis;
	}
	public long getPartyId()
	{
		return partyId;
	}
	public PartybaseDefenceBossPresence withPartyId(long partyId)
	{
		this.partyId = partyId;
		return this;
	}
	PartybaseDefenceSync getOfferedHistory()
	{
		return offeredHistory;
	}
	void offerHistory(PartybaseDefenceSync history)
	{
		offeredHistory = history;
	}
}
