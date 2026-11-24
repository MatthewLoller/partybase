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
import java.util.Set;
import net.runelite.api.Item;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.client.party.PartyMember;

/** A member of RuneLite's party; reads through to the live PartyMember, which RuneLite updates in place. */
final class RuneLiteMember implements PartybaseMember
{
	private final PartyMember member;

	RuneLiteMember(PartyMember member)
	{
		this.member = member;
	}

	PartyMember unwrap()
	{
		return member;
	}

	@Override
	public long getMemberId()
	{
		return member.getMemberId();
	}

	@Override
	public String getDisplayName()
	{
		return member.getDisplayName();
	}

	@Override
	public boolean isLoggedIn()
	{
		return member.isLoggedIn();
	}

	@Override
	public BufferedImage getAvatar()
	{
		return member.getAvatar();
	}

	@Override
	public int getRunEnergy()
	{
		return member.getRunEnergy();
	}

	@Override
	public int getSpecEnergy()
	{
		return member.getSpecEnergy();
	}

	@Override
	public int getStatBase(Skill skill)
	{
		return member.getStatBase(skill);
	}

	@Override
	public int getStatBoost(Skill skill)
	{
		return member.getStatBoost(skill);
	}

	@Override
	public Item[] getInv()
	{
		return member.getInv();
	}

	@Override
	public Item[] getWorn()
	{
		return member.getWorn();
	}

	@Override
	public Set<Prayer> getPrayers()
	{
		return member.getPrayers();
	}

	@Override
	public Item[] getRunepouch()
	{
		return member.getRunepouch();
	}

	@Override
	public Item getQuiver()
	{
		return member.getQuiver();
	}
}
