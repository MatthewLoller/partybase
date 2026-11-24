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

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Everything one player did in a game tick, sent to the party. Sent every tick, so the JSON
 * field names are kept short. The party server identifies messages by class name, which must
 * therefore never change.
 */
public class PartybaseDpsUpdate extends PartyMemberMessage
{
	public static class Hit
	{
		/** The same on every client in the same world or instance. */
		@SerializedName("i")
		public int npcIndex;
		@SerializedName("id")
		public int npcId;
		@SerializedName("nm")
		public String npcName;
		@SerializedName("a")
		public int amount;
		public boolean miss;
		public boolean boss;
		/** NPC position; instances have their own coordinates, which tells instances apart. */
		public int x;
		public int y;
		@SerializedName("p")
		public int plane;
		/** Weapon, special attack or spell; null from older versions. */
		@SerializedName("s")
		public String source;

		public Hit()
		{
		}

		Hit(int npcIndex, int npcId, String npcName, int amount, boolean miss, boolean boss, WorldPoint pos)
		{
			this.npcIndex = npcIndex;
			this.npcId = npcId;
			this.npcName = npcName;
			this.amount = amount;
			this.miss = miss;
			this.boss = boss;
			if (pos != null)
			{
				x = pos.getX();
				y = pos.getY();
				plane = pos.getPlane();
			}
		}
	}

	@SerializedName("v")
	public int version = 3;
	@SerializedName("w")
	public int world;
	@SerializedName("n")
	public String name;
	@SerializedName("h")
	public List<Hit> hits = new ArrayList<>();
	public int taken;
	public int food;
	@SerializedName("life")
	public int lifesteal;
	public int regen;
	@SerializedName("other")
	public int healOther;
	@SerializedName("of")
	public int foodOverheal;
	@SerializedName("ol")
	public int lifestealOverheal;

	boolean isEmpty()
	{
		return hits.isEmpty() && taken == 0 && food == 0 && lifesteal == 0 && regen == 0 && healOther == 0
			&& foodOverheal == 0 && lifestealOverheal == 0;
	}
}
