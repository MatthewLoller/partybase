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
package com.matthewloller.partybase.tiles;

import com.google.common.base.Strings;
import com.matthewloller.partybase.dps.DpsModule;
import com.matthewloller.partybase.party.PartyLink;
import com.matthewloller.partybase.party.PartybaseMember;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.util.ColorUtil;

/**
 * Tiles the leader marks for the whole party (shift right-click a tile, like RuneLite's Ground Markers). Solo players
 * can mark them too. Positions are stored the way Ground Markers stores them (instance template coordinates), so
 * they line up for every member in the same instance. Client thread only, except copies handed to the panel.
 */
final class PartyTiles
{
	/** Most party tiles at once. */
	static final int MAX_TILES = 500;
	/** Tiles per message: about 100 bytes each, well under a party message's size limit. */
	static final int TILES_PER_MESSAGE = 150;
	static final int MAX_LABEL = 40;

	private final Client client;
	private final PartyLink partyLink;
	private final DpsModule dps;
	private final ChatboxPanelManager chatbox;
	private final Runnable changed;
	private final Map<String, TilePoint> tiles = new LinkedHashMap<>();

	PartyTiles(Client client, PartyLink partyLink, DpsModule dps, ChatboxPanelManager chatbox, Runnable changed)
	{
		this.client = client;
		this.partyLink = partyLink;
		this.dps = dps;
		this.chatbox = chatbox;
		this.changed = changed;
	}

	synchronized List<TilePoint> all()
	{
		return new ArrayList<>(tiles.values());
	}

	synchronized int size()
	{
		return tiles.size();
	}

	static String key(int regionId, int regionX, int regionY, int z)
	{
		return regionId + ":" + regionX + ":" + regionY + ":" + z;
	}

	static String key(TilePoint t)
	{
		return key(t.regionId, t.regionX, t.regionY, t.z);
	}

	// ---- the leader's side ----

	/** Shift right-click on the ground: party tile options for whoever may mark. */
	void onMenuEntryAdded(MenuEntryAdded e)
	{
		MenuAction action = e.getMenuEntry().getType();
		if (!client.isKeyPressed(KeyCode.KC_SHIFT) || (action != MenuAction.WALK && action != MenuAction.SET_HEADING)
			|| !dps.canMark())
		{
			return;
		}
		WorldView wv = client.getWorldView(e.getMenuEntry().getWorldViewId());
		Tile selected = wv == null ? null : wv.getSelectedSceneTile();
		if (selected == null)
		{
			return;
		}
		WorldPoint wp = WorldPoint.fromLocalInstance(client, selected.getLocalLocation());
		String key = key(wp.getRegionID(), wp.getRegionX(), wp.getRegionY(), wp.getPlane());
		TilePoint existing;
		synchronized (this)
		{
			existing = tiles.get(key);
		}
		if (existing == null)
		{
			MenuEntry mark = client.createMenuEntry(-1)
				.setOption("Mark party tile")
				.setTarget("Tile")
				.setType(MenuAction.RUNELITE)
				.onClick(c -> put(point(wp, PartyTileColor.YELLOW.color, null)));
			addColours(mark.createSubMenu(), c -> put(point(wp, c, null)));
			return;
		}
		client.createMenuEntry(-1)
		.setOption("Unmark party tile")
		.setTarget("Tile")
		.setType(MenuAction.RUNELITE)
		.onClick(c -> remove(existing));
		client.createMenuEntry(-2)
		.setOption("Label party tile")
		.setTarget("Tile")
		.setType(MenuAction.RUNELITE)
		.onClick(c -> askLabel(existing));
		MenuEntry colour = client.createMenuEntry(-3)
			.setOption("Party tile colour")
			.setTarget("Tile")
			.setType(MenuAction.RUNELITE);
		addColours(colour.createSubMenu(), c -> put(point(existing, c, existing.label)));
	}

	private static void addColours(Menu menu, java.util.function.Consumer<Color> pick)
	{
		for (PartyTileColor c : PartyTileColor.values())
		{
			menu.createMenuEntry(-1)
			.setOption(ColorUtil.wrapWithColorTag(c.label, c.color))
			.setType(MenuAction.RUNELITE)
			.onClick(e -> pick.accept(c.color));
		}
	}

	private void askLabel(TilePoint existing)
	{
		chatbox.openTextInput("Party tile label")
		.value(Strings.nullToEmpty(existing.label))
		.onDone(input ->
		{
			String label = Strings.emptyToNull(input == null ? null : input.trim());
			if (label != null && label.length() > MAX_LABEL)
			{
				label = label.substring(0, MAX_LABEL);
			}
			put(point(existing, existing.color, label));
		})
		.build();
	}

	/** Marks a tile or changes it, here and (when you lead) for the party. */
	void put(TilePoint t)
	{
		synchronized (this)
		{
			if (!tiles.containsKey(key(t)) && tiles.size() >= MAX_TILES)
			{
				return;
			}
			tiles.put(key(t), t);
		}
		changed.run();
		send(PartybasePartyTiles.PUT, Collections.singletonList(t));
	}

	void remove(TilePoint t)
	{
		synchronized (this)
		{
			tiles.remove(key(t));
		}
		changed.run();
		send(PartybasePartyTiles.REMOVE, Collections.singletonList(t));
	}

	/** Removes every party tile, for the party too when you lead. */
	void clear()
	{
		synchronized (this)
		{
			tiles.clear();
		}
		changed.run();
		send(PartybasePartyTiles.CLEAR, Collections.emptyList());
	}

	/** Someone joined (they asked for everyone's state): the leader sends all party tiles. */
	void sendAllIfLeader()
	{
		if (!dps.youLead())
		{
			return;
		}
		List<TilePoint> list = all();
		if (list.isEmpty())
		{
			return;
		}
		for (int i = 0; i < list.size(); i += TILES_PER_MESSAGE)
		{
			send(i == 0 ? PartybasePartyTiles.SET : PartybasePartyTiles.PUT,
				list.subList(i, Math.min(list.size(), i + TILES_PER_MESSAGE)));
		}
	}

	private void send(String op, List<TilePoint> list)
	{
		if (!partyLink.isInParty() || !dps.youLead())
		{
			return;
		}
		PartybasePartyTiles m = new PartybasePartyTiles();
		m.op = op;
		m.tiles = new ArrayList<>(list);
		partyLink.send(m);
	}

	// ---- everyone's side ----

	/** A party tiles message, on the client thread. Only the leader's are used; your own echo is skipped. */
	void received(PartybasePartyTiles m)
	{
		PartybaseMember local = partyLink.getLocalMember();
		if (m.op == null || m.getMemberId() != dps.leaderId() || (local != null && local.getMemberId() == m.getMemberId()))
		{
			return;
		}
		List<TilePoint> list = m.tiles == null ? Collections.emptyList() : m.tiles;
		synchronized (this)
		{
			switch (m.op)
			{
				case PartybasePartyTiles.CLEAR:
					tiles.clear();
					break;
				case PartybasePartyTiles.SET:
					tiles.clear();
					putAll(list);
					break;
				case PartybasePartyTiles.PUT:
					putAll(list);
					break;
				case PartybasePartyTiles.REMOVE:
					for (TilePoint t : list)
					{
						if (t != null)
						{
							tiles.remove(key(t));
						}
					}
					break;
				default:
					return;
			}
		}
		changed.run();
	}

	private void putAll(List<TilePoint> list)
	{
		for (TilePoint t : list)
		{
			if (t != null && (tiles.size() < MAX_TILES || tiles.containsKey(key(t))))
			{
				if (t.label != null && t.label.length() > MAX_LABEL)
				{
					t.label = t.label.substring(0, MAX_LABEL);
				}
				tiles.put(key(t), t);
			}
		}
	}

	/** You left or switched parties: the tiles belonged to that party. */
	void partyChanged()
	{
		synchronized (this)
		{
			tiles.clear();
		}
		changed.run();
	}

	private static TilePoint point(WorldPoint wp, Color color, String label)
	{
		TilePoint t = new TilePoint();
		t.regionId = wp.getRegionID();
		t.regionX = wp.getRegionX();
		t.regionY = wp.getRegionY();
		t.z = wp.getPlane();
		t.color = color;
		t.label = label;
		return t;
	}

	private static TilePoint point(TilePoint at, Color color, String label)
	{
		TilePoint t = new TilePoint();
		t.regionId = at.regionId;
		t.regionX = at.regionX;
		t.regionY = at.regionY;
		t.z = at.z;
		t.color = color;
		t.label = label;
		return t;
	}
}
