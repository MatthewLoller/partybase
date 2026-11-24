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
package com.matthewloller.partybase.party;

import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.Item;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.client.party.messages.PartyData;
import net.runelite.client.util.Text;

/**
 * A member of a relay server party. The relay only knows the member id and the name given on join; the rest comes
 * from PartyData messages, applied the same way RuneLite's PartyDataService does. Written on the websocket
 * thread, read on the client and Swing threads: fields are volatile and arrays are replaced, never changed.
 */
final class RelayMember implements PartybaseMember
{
	private final long memberId;
	private volatile String displayName = "<unknown>";
	private volatile boolean loggedIn;
	private volatile int runEnergy;
	private volatile int specEnergy;
	private volatile int[] statBase = new int[Skill.values().length];
	private volatile int[] statBoost = new int[Skill.values().length];
	private volatile Item[] inv;
	private volatile Item[] worn;
	private volatile Set<Prayer> prayers;
	private volatile Item[] runepouch;
	private volatile Item quiver;

	RelayMember(long memberId, String name)
	{
		this.memberId = memberId;
		if (name != null && !name.isEmpty())
		{
			this.displayName = name;
			this.loggedIn = true;
		}
	}

	void apply(PartyData d)
	{
		if (d.getCharacterName() != null)
		{
			String name = Text.removeTags(Text.toJagexName(d.getCharacterName()));
			loggedIn = !name.isEmpty();
			if (loggedIn)
			{
				displayName = name;
			}
		}
		if (d.getRunEnergy() != null)
		{
			runEnergy = d.getRunEnergy();
		}
		if (d.getSpecEnergy() != null)
		{
			specEnergy = d.getSpecEnergy();
		}
		if (d.getInv() != null)
		{
			inv = items(d.getInv());
		}
		if (d.getWorn() != null)
		{
			worn = items(d.getWorn());
		}
		if (d.getPrayers() != null)
		{
			prayers = prayers(d.getPrayers());
		}
		if (d.getStatBase() != null)
		{
			statBase = stats(statBase, d.getStatBase());
		}
		if (d.getStatBoost() != null)
		{
			statBoost = stats(statBoost, d.getStatBoost());
		}
		if (d.getRunePouch() != null)
		{
			runepouch = items(d.getRunePouch());
		}
		if (d.getQuiver() != null)
		{
			Item[] q = items(d.getQuiver());
			quiver = q.length > 0 ? q[0] : null;
		}
	}

	/** id, quantity pairs; -1 or 0 quantity is an empty slot. */
	static Item[] items(int[] s)
	{
		Item[] items = new Item[s.length / 2];
		for (int i = 0; i < items.length; i++)
		{
			int id = s[i * 2];
			int quantity = s[i * 2 + 1];
			if (id > -1 && quantity > 0)
			{
				items[i] = new Item(id, quantity);
			}
		}
		return items;
	}

	static Set<Prayer> prayers(long bits)
	{
		Set<Prayer> out = EnumSet.noneOf(Prayer.class);
		for (Prayer p : Prayer.values())
		{
			if (p.ordinal() < 64 && (bits & (1L << p.ordinal())) != 0)
			{
				out.add(p);
			}
		}
		return out;
	}

	private static int[] stats(int[] old, int[] update)
	{
		int[] next = old.clone();
		System.arraycopy(update, 0, next, 0, Math.min(update.length, next.length));
		return next;
	}

	@Override
	public long getMemberId()
	{
		return memberId;
	}

	@Override
	public String getDisplayName()
	{
		return displayName;
	}

	@Override
	public boolean isLoggedIn()
	{
		return loggedIn;
	}

	@Override
	public BufferedImage getAvatar()
	{
		return null;
	}

	@Override
	public int getRunEnergy()
	{
		return runEnergy;
	}

	@Override
	public int getSpecEnergy()
	{
		return specEnergy;
	}

	@Override
	public int getStatBase(Skill skill)
	{
		return statBase[skill.ordinal()];
	}

	@Override
	public int getStatBoost(Skill skill)
	{
		return statBoost[skill.ordinal()];
	}

	@Override
	public Item[] getInv()
	{
		return inv;
	}

	@Override
	public Item[] getWorn()
	{
		return worn;
	}

	@Override
	public Set<Prayer> getPrayers()
	{
		return prayers;
	}

	@Override
	public Item[] getRunepouch()
	{
		return runepouch;
	}

	@Override
	public Item getQuiver()
	{
		return quiver;
	}
}
