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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.dps.model.BarrowsRun;
import com.matthewloller.partybase.dps.model.FightTracker;
import com.matthewloller.partybase.dps.model.Format;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Barrows runs in the game: brother hits and deaths, the chest message, and saving the run to the
 * RS profile so it survives a client restart. The run rules are in {@link BarrowsRun}.
 * Client thread, except {@link #clientClosed}.
 */
@Slf4j
@Singleton
class BarrowsTracker
{
	/** Game message when you open the Barrows chest. */
	private static final String CHEST_MESSAGE = "Your Barrows chest count is";
	/** RS profile key of the run that is going on. */
	private static final String SAVE_KEY = "barrowsRun";
	/** Save a running run once a minute (and on every start, kill and logout), so a crash loses little. */
	private static final int SAVE_TICKS = 100;

	private final ConfigManager configManager;
	private final Gson gson;
	private final BarrowsRun run = new BarrowsRun();
	private boolean loadAfterLogin;
	/** The live run time for the overlay and the panel, or null without a run. */
	private volatile String liveTime;
	private volatile boolean paused;

	@Inject
	BarrowsTracker(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	/** You hit an NPC. */
	void hit(long now, String npcName)
	{
		if (run.hit(now, npcName))
		{
			save(now);
		}
	}

	/** An NPC died. */
	void died(long now, String npcName)
	{
		if (BarrowsRun.isBrother(npcName))
		{
			run.died(npcName);
			save(now);
		}
	}

	/** A game message: the chest adds the run to the fights. */
	void gameMessage(long now, String message, FightTracker tracker)
	{
		if (!message.startsWith(CHEST_MESSAGE))
		{
			return;
		}
		BarrowsRun.Result r = run.chestOpened(now);
		if (r != null)
		{
			tracker.addRun(BarrowsRun.NAME, r.startMs, r.endMs, r.activeMs, t -> BarrowsRun.isBrother(t.name),
				"Chest opened");
			save(now);
		}
	}

	/** The saved run is loaded on the next tick: the RS profile is set by then. */
	void loggedIn()
	{
		loadAfterLogin = true;
	}

	/** Logout or world hop: the clock stops and the run is saved for this account. */
	void loggedOut(long now)
	{
		run.loggedOut(now);
		save(now);
		run.clear();
		liveTime = null;
	}

	/** Not on the client thread, but the game loop has stopped. After a logout the run was saved already. */
	void clientClosed(long now)
	{
		if (run.isRunning())
		{
			run.loggedOut(now);
			save(now);
		}
	}

	void tick(long now, int tick)
	{
		if (loadAfterLogin)
		{
			loadAfterLogin = false;
			load();
		}
		if (run.isRunning() && tick % SAVE_TICKS == 0)
		{
			save(now);
		}
		paused = run.isPaused(now);
		liveTime = run.isRunning() ? Format.duration(run.activeMs(now)) : null;
	}

	String liveTime()
	{
		return liveTime;
	}

	boolean isPaused()
	{
		return paused;
	}

	private void save(long now)
	{
		BarrowsRun.Saved s = run.save(now);
		if (s == null)
		{
			configManager.unsetRSProfileConfiguration(PartybaseConfig.STATE_GROUP, SAVE_KEY);
		}
		else
		{
			configManager.setRSProfileConfiguration(PartybaseConfig.STATE_GROUP, SAVE_KEY, gson.toJson(s));
		}
	}

	/** Takes over the run saved for this account; its clock starts again at the next brother hit. */
	private void load()
	{
		if (!run.isRunning())
		{
			String json = configManager.getRSProfileConfiguration(PartybaseConfig.STATE_GROUP, SAVE_KEY);
			try
			{
				run.restore(json == null ? null : gson.fromJson(json, BarrowsRun.Saved.class));
			}
			catch (JsonParseException ex)
			{
				log.warn("Could not read the saved Barrows run", ex);
			}
		}
	}
}
