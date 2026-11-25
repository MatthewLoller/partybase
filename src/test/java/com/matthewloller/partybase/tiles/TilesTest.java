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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.matthewloller.partybase.dps.DpsModule;
import com.matthewloller.partybase.party.PartyLink;
import com.matthewloller.partybase.party.PartybaseMember;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

public class TilesTest
{
	private static final String TILES = "[{\"regionId\":12850,\"regionX\":10,\"regionY\":20,\"z\":0,\"color\":\"#FF00FF00\","
		+ "\"label\":\"Stand\"},{\"regionId\":12850,\"regionX\":11,\"regionY\":20,\"z\":0}]";

	@Test
	public void splitsIntoPartsAndBack()
	{
		String text = "abcdefghij";
		List<String> parts = TileModule.split(text, 3);
		assertEquals(4, parts.size());
		assertEquals("j", parts.get(3));
		assertEquals(text, String.join("", parts));
		assertEquals(Collections.singletonList(""), TileModule.split("", 3));
	}

	@Test
	public void readsCustomPacksFromTilePacksSettings()
	{
		ConfigManager config = mock(ConfigManager.class);
		PluginManager plugins = mock(PluginManager.class);
		when(plugins.getPlugins()).thenReturn(Collections.emptyList());
		String prefix = ConfigManager.getWholeKey("tilePacks", null, "pack_");
		when(config.getConfigurationKeys(prefix)).thenReturn(Collections.singletonList(prefix + "10000"));
		when(config.getConfiguration("tilePacks", "pack_10000")).thenReturn(
			"{\"id\":10000,\"packName\":\"Our Vorkath\",\"packTiles\":" + RuneLiteAPI.GSON.toJson(TILES)
			+ ",\"defaultColor\":\"#FFFF0000\"}");
		when(config.getConfiguration("tilePacks", "packs")).thenReturn("[10000]");

		TilePacksSource source = new TilePacksSource(plugins, config, RuneLiteAPI.GSON);
		assertFalse(source.isInstalled());
		List<TilePackInfo> packs = source.packs();
		assertEquals(1, packs.size());
		TilePackInfo p = packs.get(0);
		assertEquals("Our Vorkath", p.name);
		assertTrue(p.enabled);
		assertFalse(p.builtIn);
		assertEquals(new Color(255, 0, 0), p.defaultColor);
		assertEquals(TILES, p.tilesJson);
	}

	@Test
	public void savesSharedPacksAsNewCustomPacks()
	{
		ConfigManager config = mock(ConfigManager.class);
		when(config.getConfiguration("tilePacks", "customId")).thenReturn("10004");
		when(config.getConfiguration("tilePacks", "packs")).thenReturn("[3,10004]");
		TilePacksSource source = new TilePacksSource(mock(PluginManager.class), config, RuneLiteAPI.GSON);

		assertEquals(10005, source.saveCustomPack("Shared", TILES, null));
		verify(config).setConfiguration(eq("tilePacks"), eq("pack_10005"), any(String.class));
		verify(config).setConfiguration("tilePacks", "customId", 10005);
		verify(config).setConfiguration("tilePacks", "packs", "[3,10004,10005]");
	}

	@Test
	public void joinsPartsInAnyOrderAndShowsTheLeadersPacks() throws Exception
	{
		TileModule module = new TileModule();
		ClientThread clientThread = mock(ClientThread.class);
		doAnswer(i ->
		{
			((Runnable) i.getArgument(0)).run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
		PartyLink party = mock(PartyLink.class);
		PartybaseMember leader = mock(PartybaseMember.class);
		when(leader.getDisplayName()).thenReturn("Leader");
		when(party.getMemberById(1)).thenReturn(leader);
		DpsModule dps = mock(DpsModule.class);
		when(dps.leaderId()).thenReturn(1L);
		set(module, "client", mock(Client.class));
		set(module, "clientThread", clientThread);
		set(module, "partyLink", party);
		set(module, "dps", dps);
		set(module, "gson", RuneLiteAPI.GSON);

		List<String> parts = TileModule.split(TILES, 40);
		for (int i = parts.size() - 1; i >= 0; i--)
		{
			module.onPartybaseTilePack(part(1, "7", "Vorkath", i, parts));
			assertEquals("not done until every part is in", i == 0 ? 1 : 0, module.sharedPacks().size());
		}
		TileModule.SharedPack pack = module.sharedPacks().get(0);
		assertEquals("Vorkath", pack.name);
		assertEquals("Leader", pack.fromName);
		assertEquals(2, pack.points.size());
		assertEquals("Stand", pack.points.get(0).label);
		assertEquals(new Color(0, 255, 0), pack.points.get(0).color);
		assertTrue("the leader's packs show straight away", pack.visible);

		module.onPartybaseTilePack(part(2, "7", "Other", 0, Collections.singletonList(TILES)));
		assertEquals("same share id from another member is another pack", 2, module.sharedPacks().size());
		assertFalse("others' packs wait to be turned on", module.sharedPacks().get(1).visible);
	}

	private static PartybaseTilePack part(long from, String id, String name, int i, List<String> parts)
	{
		PartybaseTilePack m = new PartybaseTilePack();
		m.setMemberId(from);
		m.shareId = id;
		m.name = name;
		m.part = i;
		m.parts = parts.size();
		m.tiles = parts.get(i);
		return m;
	}

	private static void set(Object target, String name, Object value) throws Exception
	{
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}
}
