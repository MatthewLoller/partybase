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
package com.matthewloller.partybase.dps.model;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.util.Filepath;

/** Fight history, personal bests and monster stats as JSON, one file of each per account. */
public class HistoryStore
{
	public static final int VERSION = 1;

	public static class SaveFile
	{
		public int version = VERSION;
		public long accountHash;
		public long savedAt;
		/** Newest first. */
		public List<Fight> fights = new ArrayList<>();
	}

	private final Gson gson;
	private final Filepath dir;

	/** @param dir the plugin's data folder (Plugin.getPluginDirectory) */
	public HistoryStore(Gson gson, Filepath dir)
	{
		this.gson = gson;
		this.dir = dir;
	}

	public Filepath fileFor(long accountHash)
	{
		return dir.joinSegment(accountHash + ".json");
	}

	public void save(long accountHash, List<Fight> fights, long now) throws IOException
	{
		SaveFile data = new SaveFile();
		data.accountHash = accountHash;
		data.savedAt = now;
		data.fights = new ArrayList<>(fights);
		write(fileFor(accountHash), data);
	}

	/** Personal bests have their own file, so they outlive the fights they came from. */
	public void saveRecords(long accountHash, BossRecords records) throws IOException
	{
		write(recordsFile(accountHash), records);
	}

	private Filepath recordsFile(long accountHash)
	{
		return dir.joinSegment(accountHash + ".records.json");
	}

	/** Monster stats have no limit, so they live apart from the fight history too. */
	public void saveMonsters(long accountHash, MonsterStats monsters) throws IOException
	{
		write(monstersFile(accountHash), monsters);
	}

	private Filepath monstersFile(long accountHash)
	{
		return dir.joinSegment(accountHash + ".monsters.json");
	}

	/** Writes to a temporary file first, so a crash never leaves a half-written file. */
	private void write(Filepath target, Object data) throws IOException
	{
		dir.createDirectories();
		Filepath tmp = dir.joinSegment(target.getFileName() + ".tmp");
		try (Writer w = tmp.openBufferedWriter())
		{
			gson.toJson(data, w);
		}
		try
		{
			tmp.moveTo(target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException e)
		{
			tmp.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/** Saved personal bests, or null when there are none or the file is broken. */
	public BossRecords loadRecords(long accountHash) throws IOException
	{
		BossRecords data = read(recordsFile(accountHash), BossRecords.class);
		if (data == null || data.byBoss == null)
		{
			return null;
		}
		data.byBoss.values().removeIf(x -> x == null || x.boss == null);
		return data;
	}

	/** Saved monster stats; empty when there are none yet or the file is broken. */
	public MonsterStats loadMonsters(long accountHash) throws IOException
	{
		MonsterStats data = read(monstersFile(accountHash), MonsterStats.class);
		if (data == null || data.byName == null)
		{
			return new MonsterStats();
		}
		data.byName.values().removeIf(m -> m == null || m.name == null || m.attacks == null || m.weapons == null);
		return data;
	}

	/** The file as {@code type}, or null when it is missing or broken. */
	private <T> T read(Filepath file, Class<T> type) throws IOException
	{
		if (!file.exists())
		{
			return null;
		}
		try (Reader r = file.openBufferedReader())
		{
			return gson.fromJson(r, type);
		}
		catch (JsonParseException e)
		{
			return null;
		}
	}

	/** Saved fights, newest first. A broken file is kept aside and an empty list returned. */
	public List<Fight> load(long accountHash) throws IOException
	{
		Filepath file = fileFor(accountHash);
		if (!file.exists())
		{
			return new ArrayList<>();
		}
		try (Reader r = file.openBufferedReader())
		{
			SaveFile data = gson.fromJson(r, SaveFile.class);
			if (data == null || data.fights == null)
			{
				return new ArrayList<>();
			}
			List<Fight> out = new ArrayList<>();
			for (Fight f : data.fights)
			{
				if (f != null && f.id != null && f.players != null && f.targets != null)
				{
					out.add(f);
				}
			}
			return out;
		}
		catch (JsonParseException e)
		{
			file.moveTo(dir.joinSegment(accountHash + ".broken-" + System.currentTimeMillis() + ".json"),
				StandardCopyOption.REPLACE_EXISTING);
			return new ArrayList<>();
		}
	}
}
