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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Injector;
import com.matthewloller.partybase.PartybaseModule;
import com.matthewloller.partybase.PartybasePlugin;
import com.matthewloller.partybase.dps.DpsModule;
import com.matthewloller.partybase.party.PartyLink;
import com.matthewloller.partybase.party.PartybaseMember;
import com.matthewloller.partybase.party.events.PartybasePartyChanged;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Tile pack sharing. Members share packs from their Tile Packs plugin; Partybase draws received packs itself, so
 * receivers don't need Tile Packs. Packs from the leader show straight away, packs from others once turned on.
 * Received packs last until you leave or switch parties; "Save to Tile Packs" keeps one for good.
 */
@Slf4j
@Singleton
public class TileModule implements PartybaseModule
{
	/** Tiles JSON per message part; leaves room for JSON escaping under the 30 KB relay frame limit. */
	static final int PART_CHARS = 12_000;
	/** Parts sent per game tick, so sharing many packs doesn't flood the party server. */
	private static final int PARTS_PER_TICK = 2;
	/** Most packs kept from one member, and parts kept for one pack. */
	private static final int MAX_PACKS_PER_MEMBER = 100;
	private static final int MAX_PARTS = 20;

	/** A pack someone shared (you included). Fields are only changed on the Swing thread. */
	public static final class SharedPack
	{
		final String key;
		final long fromMemberId;
		final String fromName;
		final String name;
		final Color defaultColor;
		final String tilesJson;
		final List<TilePoint> points;
		volatile boolean visible;

		SharedPack(String key, long fromMemberId, String fromName, String name, Color defaultColor, String tilesJson,
			List<TilePoint> points, boolean visible)
		{
			this.key = key;
			this.fromMemberId = fromMemberId;
			this.fromName = fromName;
			this.name = name;
			this.defaultColor = defaultColor;
			this.tilesJson = tilesJson;
			this.points = points;
			this.visible = visible;
		}
	}

	/** A tile ready to draw: where in the loaded scene, its colour and label. */
	static final class DrawTile
	{
		final WorldView worldView;
		final WorldPoint point;
		final Color color;
		final String label;

		DrawTile(WorldView worldView, WorldPoint point, Color color, String label)
		{
			this.worldView = worldView;
			this.point = point;
			this.color = color;
			this.label = label;
		}
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PartyLink partyLink;

	@Inject
	private DpsModule dps;

	@Inject
	private Gson gson;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TilePacksSource source;

	/** Makes the overlay in startUp: it needs this module, so it can't be injected into it. */
	@Inject
	private Injector injector;

	private SharedTilesOverlay overlay;

	/** Received packs by sender and share id, in arrival order. */
	private final Map<String, SharedPack> shared = Collections.synchronizedMap(new LinkedHashMap<>());
	/** Parts still missing, by sender and share id. */
	private final Map<String, String[]> pending = new HashMap<>();
	private final Deque<PartybaseTilePack> outbox = new ArrayDeque<>();
	private volatile List<DrawTile> drawTiles = Collections.emptyList();
	private TilesPanel panel;

	@Override
	public void startUp(PartybasePlugin plugin)
	{
		partyLink.registerMessage(PartybaseTilePack.class);
		overlay = injector.getInstance(SharedTilesOverlay.class);
		overlayManager.add(overlay);
		panel = new TilesPanel(this);
		if (log.isDebugEnabled())
		{
			List<TilePackInfo> packs = source.packs();
			log.debug("Partybase tiles: Tile Packs installed={}, {} packs readable, {} turned on", source.isInstalled(),
				packs.size(), packs.stream().filter(TilePackInfo::isEnabled).count());
		}
	}

	@Override
	public void shutDown()
	{
		overlayManager.remove(overlay);
		partyLink.unregisterMessage(PartybaseTilePack.class);
		synchronized (outbox)
		{
			outbox.clear();
		}
		synchronized (pending)
		{
			pending.clear();
		}
		shared.clear();
		drawTiles = Collections.emptyList();
		panel = null;
	}

	/** The Tiles tab; null while stopped. Swing thread. */
	public JComponent getPanel()
	{
		return panel;
	}

	/** Called when the Tiles tab comes on screen, to reload your packs. Swing thread. */
	public void onTabShown()
	{
		if (panel != null)
		{
			panel.reload();
		}
	}

	// ---- your packs ----

	boolean isTilePacksInstalled()
	{
		return source.isInstalled();
	}

	List<TilePackInfo> yourPacks()
	{
		return source.packs();
	}

	boolean canShare()
	{
		return partyLink.isInParty();
	}

	/** Queues a pack for sending; it goes out over the next few ticks. */
	void share(TilePackInfo pack)
	{
		if (!partyLink.isInParty())
		{
			return;
		}
		List<String> parts = split(pack.tilesJson, PART_CHARS);
		synchronized (outbox)
		{
			for (int i = 0; i < parts.size(); i++)
			{
				PartybaseTilePack m = new PartybaseTilePack();
				m.shareId = String.valueOf(pack.id);
				m.name = pack.name;
				m.defaultColor = pack.defaultColor == null ? 0 : pack.defaultColor.getRGB();
				m.part = i;
				m.parts = parts.size();
				m.tiles = parts.get(i);
				outbox.add(m);
			}
		}
	}

	static List<String> split(String text, int size)
	{
		List<String> out = new ArrayList<>();
		for (int i = 0; i < text.length(); i += size)
		{
			out.add(text.substring(i, Math.min(text.length(), i + size)));
		}
		if (out.isEmpty())
		{
			out.add("");
		}
		return out;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		synchronized (outbox)
		{
			for (int i = 0; i < PARTS_PER_TICK && !outbox.isEmpty(); i++)
			{
				partyLink.send(outbox.poll());
			}
		}
	}

	// ---- shared packs ----

	List<SharedPack> sharedPacks()
	{
		synchronized (shared)
		{
			return new ArrayList<>(shared.values());
		}
	}

	void setVisible(SharedPack pack, boolean visible)
	{
		pack.visible = visible;
		clientThread.invokeLater(this::rebuildTiles);
	}

	void remove(SharedPack pack)
	{
		shared.remove(pack.key);
		clientThread.invokeLater(this::rebuildTiles);
		refreshPanel();
	}

	/** @return the Tile Packs id it was saved as */
	int saveToTilePacks(SharedPack pack)
	{
		return source.saveCustomPack(pack.name, pack.tilesJson, pack.defaultColor);
	}

	@Subscribe
	public void onPartybaseTilePack(PartybaseTilePack m)
	{
		if (m.shareId == null || m.tiles == null || m.parts < 1 || m.parts > MAX_PARTS || m.part < 0 || m.part >= m.parts)
		{
			return;
		}
		String key = m.getMemberId() + ":" + m.shareId;
		String[] parts;
		synchronized (pending)
		{
			parts = pending.get(key);
			if (parts == null || parts.length != m.parts)
			{
				parts = new String[m.parts];
				pending.put(key, parts);
			}
			parts[m.part] = m.tiles;
			for (String p : parts)
			{
				if (p == null)
				{
					return;
				}
			}
			pending.remove(key);
		}
		String json = String.join("", parts);
		clientThread.invokeLater(() -> received(key, m, json));
	}

	/** Client thread: the leader is known here. */
	private void received(String key, PartybaseTilePack m, String json)
	{
		List<TilePoint> points = parseTiles(json);
		if (points == null)
		{
			log.debug("Partybase: shared pack {} has unreadable tiles", m.name);
			return;
		}
		long from = m.getMemberId();
		long count = sharedPacks().stream().filter(p -> p.fromMemberId == from).count();
		SharedPack old = shared.get(key);
		if (old == null && count >= MAX_PACKS_PER_MEMBER)
		{
			return;
		}
		PartybaseMember member = partyLink.getMemberById(from);
		PartybaseMember local = partyLink.getLocalMember();
		boolean fromYou = local != null && local.getMemberId() == from;
		String fromName = fromYou ? "you" : member == null ? "party member" : member.getDisplayName();
		// keep the choice when a pack is shared again. Packs from the leader start visible; your own echo starts
		// hidden, since Tile Packs already draws your packs
		boolean visible = old != null ? old.visible : !fromYou && from == dps.leaderId();
		Color color = m.defaultColor == 0 ? null : new Color(m.defaultColor, true);
		shared.put(key, new SharedPack(key, from, fromName, m.name == null ? "Tile pack" : m.name, color, json, points,
			visible));
		rebuildTiles();
		refreshPanel();
	}

	List<TilePoint> parseTiles(String json)
	{
		try
		{
			List<TilePoint> points = gson.fromJson(json, new TypeToken<List<TilePoint>>()
			{
			} .getType());
			if (points == null)
			{
				return null;
			}
			points.removeIf(p -> p == null);
			return points;
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	@Subscribe
	public void onPartybasePartyChanged(PartybasePartyChanged e)
	{
		// shared packs belong to the party
		shared.clear();
		synchronized (pending)
		{
			pending.clear();
		}
		synchronized (outbox)
		{
			outbox.clear();
		}
		drawTiles = Collections.emptyList();
		refreshPanel();
	}

	// ---- drawing ----

	List<DrawTile> drawTiles()
	{
		return drawTiles;
	}

	@Subscribe
	public void onWorldViewLoaded(WorldViewLoaded e)
	{
		rebuildTiles();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			rebuildTiles();
		}
	}

	/** Client thread: the visible packs' tiles in the loaded regions, as Tile Packs does it. */
	void rebuildTiles()
	{
		WorldView top = client.getTopLevelWorldView();
		List<SharedPack> visible = new ArrayList<>();
		for (SharedPack p : sharedPacks())
		{
			if (p.visible)
			{
				visible.add(p);
			}
		}
		if (top == null || visible.isEmpty())
		{
			drawTiles = Collections.emptyList();
			return;
		}
		List<DrawTile> out = new ArrayList<>();
		addTiles(out, top, visible);
		for (WorldEntity we : top.worldEntities())
		{
			addTiles(out, we.getWorldView(), visible);
		}
		drawTiles = out;
	}

	private static void addTiles(List<DrawTile> out, WorldView wv, List<SharedPack> packs)
	{
		int[] regions = wv.getMapRegions();
		if (regions == null)
		{
			return;
		}
		Set<Integer> loaded = new HashSet<>();
		for (int r : regions)
		{
			loaded.add(r);
		}
		for (SharedPack pack : packs)
		{
			for (TilePoint t : pack.points)
			{
				if (!loaded.contains(t.regionId))
				{
					continue;
				}
				Color color = t.color != null ? t.color : pack.defaultColor != null ? pack.defaultColor : Color.YELLOW;
				WorldPoint wp = WorldPoint.fromRegion(t.regionId, t.regionX, t.regionY, t.z);
				for (WorldPoint local : WorldPoint.toLocalInstance(wv, wp))
				{
					out.add(new DrawTile(wv, local, color, t.label));
				}
			}
		}
	}

	private void refreshPanel()
	{
		TilesPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(p::refreshShared);
		}
	}
}
