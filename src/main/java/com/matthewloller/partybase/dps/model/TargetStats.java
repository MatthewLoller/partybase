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

/** One NPC in a fight, keyed by "world:npcIndex" (the same on every client in that world). */
public class TargetStats
{
	public String key;
	public int npcId;
	public String name;
	public boolean boss;
	/** All damage hitsplats seen on the NPC. */
	public long seenDamage;
	/** Poison, venom, burn and bleed damage. */
	public long effectDamage;
	/** Hit by you or a party member; only these count towards unknown damage. */
	public boolean engaged;
	public boolean died;
	/** Despawned without dying (a boss changing form, or out of view): it does not keep a fight going. */
	public transient boolean gone;
	public long firstMs;
	public long lastMs;
	public long diedMs;
	/**
	 * A monster that respawns at the same place gets the same key, so one target can die many times.
	 * Old saved fights have 0 here: see killCount().
	 */
	public int kills;
	/** Sum of the kill times, each from the first hit on that NPC to its death. */
	public long killTimeMs;
	/** First hit on the NPC that is at this key now. */
	public long lifeStartMs;

	public TargetStats()
	{
	}

	public TargetStats(String key, int npcId, String name, boolean boss)
	{
		this.key = key;
		this.npcId = npcId;
		this.name = name;
		this.boss = boss;
	}

	/** Hit by you or a party member. */
	public void hitAt(long now)
	{
		engaged = true;
		gone = false;
		// a party report can come a little after the death; a real respawn takes longer
		if (died && now - diedMs > FightTracker.LATE_REPORT_MS)
		{
			died = false;
			lifeStartMs = now;
		}
		if (firstMs == 0)
		{
			firstMs = now;
			lifeStartMs = now;
		}
		lastMs = Math.max(lastMs, now);
	}

	public void diedAt(long now)
	{
		if (died)
		{
			return;
		}
		died = true;
		diedMs = now;
		kills++;
		killTimeMs += lastKillTimeMs();
	}

	public int killCount()
	{
		return kills > 0 ? kills : died ? 1 : 0;
	}

	/** Time of the last kill, 0 if unknown. */
	public long lastKillTimeMs()
	{
		long start = lifeStartMs > 0 ? lifeStartMs : firstMs;
		return died && start > 0 && diedMs >= start ? diedMs - start : 0;
	}

	public void merge(TargetStats o)
	{
		seenDamage += o.seenDamage;
		effectDamage += o.effectDamage;
		engaged |= o.engaged;
		died |= o.died;
		boss |= o.boss;
		if (o.firstMs > 0 && (firstMs == 0 || o.firstMs < firstMs))
		{
			firstMs = o.firstMs;
		}
		lastMs = Math.max(lastMs, o.lastMs);
		diedMs = Math.max(diedMs, o.diedMs);
		kills += o.kills;
		killTimeMs += o.killTimeMs;
		lifeStartMs = Math.max(lifeStartMs, o.lifeStartMs);
	}

	public TargetStats copy()
	{
		TargetStats c = new TargetStats(key, npcId, name, boss);
		c.merge(this);
		return c;
	}
}
