/*
 * Copyright (c) 2026, tjeerocka <https://github.com/tjeerocka>
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

import com.google.gson.annotations.SerializedName;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Party control, since RuneLite parties have no leader or kick:
 * <ul>
 * <li>{@code hi}: a member runs Partybase and names the leader it knows (0 if none)</li>
 * <li>{@code leader}: the leader announces itself</li>
 * <li>{@code kick}: the leader asks the {@link #target} member to leave</li>
 * <li>{@code bye}: the member leaves now (the party server keeps listing it for minutes)</li>
 * <li>{@code here}: answer to {@code hi} from members who don't lead, so the new member learns join times</li>
 * <li>{@code mark}: the leader marks the NPC {@link #npcKey} as a target with {@link #icon}, or changes its icon</li>
 * <li>{@code unmark}: the leader removes the mark on {@link #npcKey}, or all marks when it is null</li>
 * </ul>
 * Every message carries {@link #since}, when the sender joined the party: the earliest one is the party's age,
 * and the member who joined first leads when nobody says who does.
 * The party server identifies messages by class name, which must therefore never change.
 */
public class PartybaseDpsPartyMessage extends PartyMemberMessage
{
	static final String HI = "hi";
	static final String LEADER = "leader";
	static final String KICK = "kick";
	static final String BYE = "bye";
	static final String HERE = "here";
	static final String MARK = "mark";
	static final String UNMARK = "unmark";

	@SerializedName("v")
	public int version = 1;
	public String type;
	public long leader;
	public long target;
	/** When the sender joined the party, epoch ms on its clock; 0 = unknown. */
	public long since;
	/** Marked target: world:npcIndex, see TargetMatcher.key. */
	public String npcKey;
	public int npcId;
	public String npcName;
	/** TargetIcon name of the mark. */
	public String icon;

	public PartybaseDpsPartyMessage()
	{
	}

	PartybaseDpsPartyMessage(String type, long leader, long target)
	{
		this.type = type;
		this.leader = leader;
		this.target = target;
	}
}
