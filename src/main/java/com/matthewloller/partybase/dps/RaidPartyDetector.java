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

import com.matthewloller.partybase.dps.model.RaidParty;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/** Reads the raid party you are in from the game and turns it into a shared party name. */
class RaidPartyDetector
{
	/** The raid party you are in. */
	static final class Detected
	{
		final String raid;
		/** Null while the party name cannot be made yet (for example when you are alone). */
		final String partyName;

		Detected(String raid, String partyName)
		{
			this.raid = raid;
			this.partyName = partyName;
		}
	}

	/** A raid whose members all see the member names. */
	private static final class NamedRaid
	{
		final String name;
		final int statusVarbit;
		final int firstNameVarc;
		final int lastNameVarc;

		NamedRaid(String name, int statusVarbit, int firstNameVarc, int lastNameVarc)
		{
			this.name = name;
			this.statusVarbit = statusVarbit;
			this.firstNameVarc = firstNameVarc;
			this.lastNameVarc = lastNameVarc;
		}
	}

	private static final NamedRaid[] NAMED_RAIDS =
	{
		new NamedRaid("ToB", VarbitID.TOB_CLIENT_PARTYSTATUS, VarClientID.TOB_CLIENT_NAME0, VarClientID.TOB_CLIENT_NAME4),
		new NamedRaid("ToA", VarbitID.TOA_CLIENT_PARTYSTATUS, VarClientID.TOA_CLIENT_NAME0, VarClientID.TOA_CLIENT_NAME7),
		new NamedRaid("Archive", VarbitID.TFA_CLIENT_PARTYSTATUS, VarClientID.TFA_CLIENT_NAME0, VarClientID.TFA_CLIENT_NAME7),
	};

	private final Client client;

	@Inject
	RaidPartyDetector(Client client)
	{
		this.client = client;
	}

	/** Client thread only. */
	Detected current()
	{
		for (NamedRaid r : NAMED_RAIDS)
		{
			if (client.getVarbitValue(r.statusVarbit) > 0)
			{
				List<String> names = new ArrayList<>();
				for (int v = r.firstNameVarc; v <= r.lastNameVarc; v++)
				{
					names.add(client.getVarcStrValue(v));
				}
				return new Detected(r.name, RaidParty.partyName(r.name, client.getWorld(), names));
			}
		}
		// CoX has no member list, but every member gets the same raid party id
		return new Detected("CoX", RaidParty.coxPartyName(client.getWorld(),
			client.getVarpValue(VarPlayerID.RAIDS_PARTY_GROUPHOLDER), client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSIZE)));
	}
}
