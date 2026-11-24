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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kills per hour in one session: every monster, every boss, and completions of every raid.
 * A rate runs from the start of the first kill until now, so it drops while you do something else.
 */
public final class KillRates
{
	public enum Kind
	{
		RAID,
		/** Barrows chests. */
		RUN,
		BOSS,
		MONSTER
	}

	/** One raid, boss or monster. */
	public static final class Rate
	{
		public final String name;
		public final Kind kind;
		public int kills;
		/** Kills per hour. */
		public double perHour;
		/** Average kill (or raid) time. */
		public long averageMs;
		long firstStartMs = Long.MAX_VALUE;
		long totalMs;

		Rate(String name, Kind kind)
		{
			this.name = name;
			this.kind = kind;
		}
	}

	private KillRates()
	{
	}

	/** Rates for the fights of one session: raids first, then bosses, then monsters, most kills first. */
	public static List<Rate> of(List<Fight> fights, long nowMs)
	{
		Map<String, Rate> rates = new LinkedHashMap<>();
		for (Fight f : fights)
		{
			if (f.mergedFights > 0)
			{
				continue;
			}
			if (f.isCompleteRaid())
			{
				add(rates, f.raid, f.run ? Kind.RUN : Kind.RAID, f.startMs, 1, f.durationMs(f.endMs));
			}
			BossRecords.Kill kill = BossRecords.kill(f);
			// Barrows brothers die once per chest: chests per hour says it
			if (kill != null && !BarrowsRun.isBrother(kill.boss))
			{
				add(rates, kill.boss, Kind.BOSS, f.startMs, 1, kill.timeMs);
			}
			if (f.partOf == null && !f.run)
			{
				addMonsters(rates, f);
			}
		}
		List<Rate> out = new ArrayList<>(rates.values());
		for (Rate r : out)
		{
			r.averageMs = r.totalMs / r.kills;
			r.perHour = r.kills * 3_600_000.0 / Math.max(1, nowMs - r.firstStartMs);
		}
		out.sort((a, b) -> a.kind != b.kind ? a.kind.compareTo(b.kind)
			: b.kills != a.kills ? Integer.compare(b.kills, a.kills) : a.name.compareTo(b.name));
		return out;
	}

	/** Non-boss NPCs you or your party fought that died. Raid boss fights are skipped: their raid has them. */
	private static void addMonsters(Map<String, Rate> rates, Fight f)
	{
		for (TargetStats t : f.targets.values())
		{
			if (!t.boss && t.engaged && t.killCount() > 0 && t.name != null && t.firstMs > 0)
			{
				add(rates, t.name, Kind.MONSTER, t.firstMs, t.killCount(), t.kills > 0 ? t.killTimeMs : t.lastKillTimeMs());
			}
		}
	}

	/** @param timeMs the time of all these kills together */
	private static void add(Map<String, Rate> rates, String name, Kind kind, long startMs, int kills, long timeMs)
	{
		Rate r = rates.computeIfAbsent(kind + "|" + name, k -> new Rate(name, kind));
		r.kills += kills;
		r.firstStartMs = Math.min(r.firstStartMs, startMs);
		r.totalMs += timeMs;
	}
}
