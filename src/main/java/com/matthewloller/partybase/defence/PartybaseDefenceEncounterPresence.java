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

// Encounter heartbeat used to validate standard party specs.
@ToString(doNotUseGetters = true)
public class PartybaseDefenceEncounterPresence extends PartyMemberMessage
{
	public static final int PROTOCOL_VERSION = 1;

	private int protocolVersion;
	private int world;
	private int scopeType;
	private int scopeId;
	private long sentAtMillis;
	// Sender party session; zero is legacy.
	@ToString.Exclude
	private long partyId;

	// Gson.
	public PartybaseDefenceEncounterPresence()
	{
	}

	public PartybaseDefenceEncounterPresence(int world, int scopeType, int scopeId)
	{
		this.protocolVersion = PROTOCOL_VERSION;
		this.world = world;
		this.scopeType = scopeType;
		this.scopeId = scopeId;
		this.sentAtMillis = System.currentTimeMillis();
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
	public long getSentAtMillis()
	{
		return sentAtMillis;
	}
	public long getPartyId()
	{
		return partyId;
	}
	public PartybaseDefenceEncounterPresence withPartyId(long partyId)
	{
		this.partyId = partyId;
		return this;
	}
}
