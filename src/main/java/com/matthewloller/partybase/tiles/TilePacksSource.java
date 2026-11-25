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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/**
 * Reads the packs of the Tile Packs plugin (TrevorMDev). Built-in packs come from the tilePacks.jsonc inside the
 * installed plugin; edited and custom packs, and the list of packs turned on, from its settings (group
 * "tilePacks"). The plugin is only read, never changed, except by saveCustomPack.
 */
@Slf4j
@Singleton
class TilePacksSource
{
	static final String CONFIG_GROUP = "tilePacks";
	private static final String PACK_PREFIX = "pack_";
	private static final String ENABLED_KEY = "packs";
	private static final String CUSTOM_ID_KEY = "customId";
	private static final String PLUGIN_CLASS = "com.tilepacks.TilePacksPlugin";
	/** Tile Packs gives custom packs ids from 10000. */
	private static final int FIRST_CUSTOM_ID = 10000;

	/** The fields Partybase reads from a Tile Packs pack record. */
	static final class Record
	{
		Integer id;
		String packName;
		String packTiles;
		Color defaultColor;
	}

	private final PluginManager pluginManager;
	private final ConfigManager configManager;
	private final Gson gson;

	@Inject
	TilePacksSource(PluginManager pluginManager, ConfigManager configManager, Gson gson)
	{
		this.pluginManager = pluginManager;
		this.configManager = configManager;
		this.gson = gson;
	}

	/** The Tile Packs plugin is installed (on or off). */
	boolean isInstalled()
	{
		return tilePacksPlugin() != null;
	}

	/** Every pack: turned-on packs first, then by name. */
	List<TilePackInfo> packs()
	{
		Map<Integer, Record> all = new LinkedHashMap<>(builtInPacks());
		Set<Integer> builtIn = new HashSet<>(all.keySet());
		for (Record saved : savedPacks())
		{
			Record base = all.get(saved.id);
			if (saved.packName != null || saved.packTiles != null || saved.defaultColor != null || base == null)
			{
				// an edited built-in pack or a custom one: the saved record is complete
				all.put(saved.id, saved);
			}
		}
		Set<Integer> enabled = enabledIds();
		List<TilePackInfo> out = new ArrayList<>();
		for (Record r : all.values())
		{
			if (r.id == null || r.packTiles == null)
			{
				continue;
			}
			String name = Strings.isNullOrEmpty(r.packName) ? "Pack " + r.id : r.packName;
			out.add(new TilePackInfo(r.id, name, r.packTiles, r.defaultColor, enabled.contains(r.id),
				builtIn.contains(r.id) && r.id < FIRST_CUSTOM_ID));
		}
		out.sort((a, b) -> a.enabled != b.enabled ? (a.enabled ? -1 : 1) : a.name.compareToIgnoreCase(b.name));
		return out;
	}

	/**
	 * Adds a pack to Tile Packs as a custom pack, turned on. Tile Packs reads its settings when it starts, so the
	 * pack shows there after Tile Packs (or the client) restarts.
	 *
	 * @return the new pack id
	 */
	int saveCustomPack(String name, String tilesJson, Color defaultColor)
	{
		String last = configManager.getConfiguration(CONFIG_GROUP, CUSTOM_ID_KEY);
		int id = (Strings.isNullOrEmpty(last) ? FIRST_CUSTOM_ID - 1 : gson.fromJson(last, Integer.class)) + 1;
		Record r = new Record();
		r.id = id;
		r.packName = name;
		r.packTiles = tilesJson;
		r.defaultColor = defaultColor;
		configManager.setConfiguration(CONFIG_GROUP, PACK_PREFIX + id, gson.toJson(r));
		configManager.setConfiguration(CONFIG_GROUP, CUSTOM_ID_KEY, id);
		List<Integer> enabled = new ArrayList<>(enabledIds());
		enabled.add(id);
		configManager.setConfiguration(CONFIG_GROUP, ENABLED_KEY, gson.toJson(enabled));
		return id;
	}

	private Plugin tilePacksPlugin()
	{
		for (Plugin p : pluginManager.getPlugins())
		{
			if (PLUGIN_CLASS.equals(p.getClass().getName()))
			{
				return p;
			}
		}
		return null;
	}

	private Map<Integer, Record> builtInPacks()
	{
		Plugin plugin = tilePacksPlugin();
		if (plugin == null)
		{
			return Collections.emptyMap();
		}
		// the file sits next to the plugin class; Gson reads its // comments
		try (InputStream in = plugin.getClass().getResourceAsStream("tilePacks.jsonc"))
		{
			if (in == null)
			{
				return Collections.emptyMap();
			}
			Map<Integer, Record> parsed = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
				new TypeToken<Map<Integer, Record>>()
			{
			} .getType());
			return parsed == null ? Collections.emptyMap() : parsed;
		}
		catch (Exception e)
		{
			log.warn("Partybase: could not read the Tile Packs built-in packs", e);
			return Collections.emptyMap();
		}
	}

	private List<Record> savedPacks()
	{
		List<Record> out = new ArrayList<>();
		String prefix = ConfigManager.getWholeKey(CONFIG_GROUP, null, PACK_PREFIX);
		for (String key : configManager.getConfigurationKeys(prefix))
		{
			String id = key.substring(prefix.length());
			try
			{
				Record r = gson.fromJson(configManager.getConfiguration(CONFIG_GROUP, PACK_PREFIX + id), Record.class);
				if (r != null)
				{
					r.id = r.id == null ? Integer.valueOf(id) : r.id;
					out.add(r);
				}
			}
			catch (RuntimeException e)
			{
				log.debug("Partybase: skipping Tile Packs record {}", key, e);
			}
		}
		return out;
	}

	private Set<Integer> enabledIds()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, ENABLED_KEY);
		if (Strings.isNullOrEmpty(json))
		{
			return Collections.emptySet();
		}
		try
		{
			List<Integer> ids = gson.fromJson(json, new TypeToken<List<Integer>>()
			{
			} .getType());
			return ids == null ? Collections.emptySet() : new HashSet<>(ids);
		}
		catch (RuntimeException e)
		{
			return Collections.emptySet();
		}
	}
}
