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

import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.messages.PartyData;
import net.runelite.client.util.Text;

/**
 * On a relay server, sends your member data the way RuneLite's PartyDataService does on RuneLite's party: only the
 * fields that changed, in a PartyData message, at most every second tick unless something else forces it. The
 * encoding (id/quantity pairs, prayer bits) is the same, so RelayMember reads it like PartyDataService would.
 * Client thread only.
 */
final class RelayDataSender
{
	private static final int[] RUNE_POUCH_TYPES = {VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2,
									VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6};
	private static final int[] RUNE_POUCH_QUANTITIES = {VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2,
									VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5,
									VarbitID.RUNE_POUCH_QUANTITY_6};
	private static final int SEND_EVERY_TICKS = 2;

	private final Client client;

	private boolean force = true;
	private boolean invDirty = true;
	private boolean wornDirty = true;
	private boolean pouchDirty = true;
	private boolean quiverDirty = true;
	private boolean statsDirty = true;
	private String lastName;
	private int lastRun = -1;
	private int lastSpec = -1;
	private long lastPrayers = -1;

	RelayDataSender(Client client)
	{
		this.client = client;
	}

	/** Send everything on the next tick (after joining, or when someone asks with UserSync). */
	void forceNext()
	{
		force = true;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() == InventoryID.INV)
		{
			invDirty = true;
		}
		else if (e.getContainerId() == InventoryID.WORN)
		{
			wornDirty = true;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarpId() == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO || e.getVarpId() == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT)
		{
			quiverDirty = true;
		}
		for (int i = 0; i < RUNE_POUCH_TYPES.length; i++)
		{
			if (e.getVarbitId() == RUNE_POUCH_TYPES[i] || e.getVarbitId() == RUNE_POUCH_QUANTITIES[i])
			{
				pouchDirty = true;
			}
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		statsDirty = true;
	}

	/** The update to send this tick, or null when nothing changed (or it is not a send tick). */
	PartyData tick()
	{
		boolean all = force;
		if (!all && client.getTickCount() % SEND_EVERY_TICKS != 0)
		{
			return null;
		}
		boolean loggedIn = client.getGameState().getState() >= GameState.LOADING.getState() && client.getLocalPlayer() != null;
		String name = loggedIn ? Text.removeTags(client.getLocalPlayer().getName()) : "";
		int run = (int) Math.ceil(client.getEnergy() / 1000.0) * 10;
		int spec = client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
		long prayers = prayerBits();

		PartyData d = new PartyData();
		boolean changed = false;
		if (all || !name.equals(lastName))
		{
			d.setCharacterName(name);
			lastName = name;
			changed = true;
		}
		if (!loggedIn)
		{
			force = false;
			return changed ? d : null;
		}
		if (all || run != lastRun)
		{
			d.setRunEnergy(run);
			lastRun = run;
			changed = true;
		}
		if (all || spec != lastSpec)
		{
			d.setSpecEnergy(spec);
			lastSpec = spec;
			changed = true;
		}
		if (all || prayers != lastPrayers)
		{
			d.setPrayers(prayers);
			lastPrayers = prayers;
			changed = true;
		}
		if (all || invDirty)
		{
			d.setInv(items(client.getItemContainer(InventoryID.INV)));
			invDirty = false;
			changed = true;
		}
		if (all || wornDirty)
		{
			d.setWorn(items(client.getItemContainer(InventoryID.WORN)));
			wornDirty = false;
			changed = true;
		}
		if (all || statsDirty)
		{
			d.setStatBase(stats(true));
			d.setStatBoost(stats(false));
			statsDirty = false;
			changed = true;
		}
		if (all || pouchDirty)
		{
			d.setRunePouch(runePouch());
			pouchDirty = false;
			changed = true;
		}
		if (all || quiverDirty)
		{
			d.setQuiver(quiver());
			quiverDirty = false;
			changed = true;
		}
		force = false;
		return changed ? d : null;
	}

	private long prayerBits()
	{
		long bits = 0;
		for (Prayer p : Prayer.values())
		{
			if (p.ordinal() < 64 && client.getVarbitValue(p.getVarbit()) == 1)
			{
				bits |= 1L << p.ordinal();
			}
		}
		return bits;
	}

	private int[] stats(boolean base)
	{
		Skill[] skills = Skill.values();
		int[] out = new int[skills.length];
		for (int i = 0; i < skills.length; i++)
		{
			out[i] = base ? client.getRealSkillLevel(skills[i]) : client.getBoostedSkillLevel(skills[i]);
		}
		return out;
	}

	/** id, quantity pairs; -1 for an empty slot. */
	static int[] items(ItemContainer container)
	{
		if (container == null)
		{
			return new int[0];
		}
		Item[] items = container.getItems();
		int[] out = new int[items.length * 2];
		Arrays.fill(out, -1);
		for (int i = 0; i < items.length; i++)
		{
			if (items[i] != null && items[i].getId() > -1)
			{
				out[i * 2] = items[i].getId();
				out[i * 2 + 1] = items[i].getQuantity();
			}
		}
		return out;
	}

	private int[] runePouch()
	{
		EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		int[] out = new int[RUNE_POUCH_TYPES.length * 2];
		for (int i = 0; i < RUNE_POUCH_TYPES.length; i++)
		{
			out[i * 2] = runes.getIntValue(client.getVarbitValue(RUNE_POUCH_TYPES[i]));
			out[i * 2 + 1] = client.getVarbitValue(RUNE_POUCH_QUANTITIES[i]);
		}
		return out;
	}

	private int[] quiver()
	{
		int id = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO);
		int count = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT);
		return id == -1 || count == 0 ? new int[0] : new int[] {id, count};
	}
}
