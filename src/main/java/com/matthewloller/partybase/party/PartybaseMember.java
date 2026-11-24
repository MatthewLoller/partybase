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

/**
 * A party member as Partybase sees it, from RuneLite's party or from a Partybase relay server. Values that were
 * never received are 0 or null.
 */
public interface PartybaseMember
{
	long getMemberId();

	/** The character name, or "<unknown>" before it is known. */
	String getDisplayName();

	boolean isLoggedIn();

	/** Discord avatar on RuneLite's party; always null on a relay server. */
	BufferedImage getAvatar();

	int getRunEnergy();

	int getSpecEnergy();

	int getStatBase(Skill skill);

	int getStatBoost(Skill skill);

	Item[] getInv();

	Item[] getWorn();

	Set<Prayer> getPrayers();

	Item[] getRunepouch();

	Item getQuiver();
}
