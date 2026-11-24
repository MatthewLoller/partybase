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

import com.matthewloller.partybase.dps.model.FightReport;
import com.matthewloller.partybase.dps.model.Format;
import com.matthewloller.partybase.dps.model.HungryTalk;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The fun extras for group fights: a crown over the MVP, a burger over the top healer of the
 * whole fight (or raid), fries over the top healer on the current boss, and lines from both.
 * Updated on the client thread, read by the overlays.
 */
class FunEffects
{
	/** Icons stay this long after the fight ended. */
	private static final long SHOW_AFTER_FIGHT_MS = 30_000;
	private static final long SAY_MS = 5000;

	/** Something a player "says" over their head for a while (drawn by the overlay only). */
	private static final class Speech
	{
		final String text;
		final long until;

		Speech(String text, long until)
		{
			this.text = text;
			this.until = until;
		}
	}

	private final HungryTalk burgerTalk = new HungryTalk(new Random());
	private final HungryTalk friesTalk = new HungryTalk(new Random());

	private String mvp;
	private String burger;
	private String fries;
	private final Map<String, Speech> speech = new HashMap<>();
	private String lastBossFightId;

	/**
	 * @param fight  the running or last fight (the whole raid inside a raid), or null
	 * @param boss   the current or last boss fight, or null
	 * @param bossId id of that boss fight, to announce its healer once
	 */
	void update(FightReport fight, long fightEndedMs, FightReport boss, long bossEndedMs, String bossId, long now)
	{
		FightReport shownFight = visible(fight, fightEndedMs, now) ? fight : null;
		FightReport shownBoss = visible(boss, bossEndedMs, now) ? boss : null;
		mvp = name(shownFight == null ? null : shownFight.topDamage());
		FightReport.PlayerRow fightHealer = shownFight == null ? null : shownFight.topHealer();
		FightReport.PlayerRow bossHealer = shownBoss == null ? null : shownBoss.topHealer();
		burger = name(fightHealer);
		fries = name(bossHealer);
		if (bossHealer != null && bossEndedMs > 0 && !bossId.equals(lastBossFightId))
		{
			lastBossFightId = bossId;
			announceHealers(bossHealer, fightHealer, now);
		}
		speech.values().removeIf(s -> now >= s.until);
	}

	/** After a boss kill the fries holder says the boss heal and the burger holder the total. */
	private void announceHealers(FightReport.PlayerRow bossHealer, FightReport.PlayerRow fightHealer, long now)
	{
		boolean samePlayer = fightHealer == null || fightHealer.name.equals(bossHealer.name);
		if (samePlayer)
		{
			long total = fightHealer == null ? bossHealer.healing : fightHealer.healing;
			say(bossHealer.name, healedLine(bossHealer.healing, total), now);
			return;
		}
		say(bossHealer.name, healedLine(bossHealer.healing, bossHealer.healing), now);
		say(fightHealer.name, "Total healed: " + Format.amount(fightHealer.healing) + " HP!", now);
	}

	/** "I healed 1,234 HP!", with the total when the whole fight healed more. */
	private static String healedLine(long boss, long total)
	{
		String line = "I healed " + Format.amount(boss) + " HP!";
		return total > boss ? line + " (" + Format.amount(total) + " total)" : line;
	}

	private void say(String player, String text, long now)
	{
		speech.put(player, new Speech(text, now + SAY_MS));
	}

	private static boolean visible(FightReport report, long endedMs, long now)
	{
		return report != null && report.playerCount() >= 2 && (endedMs == 0 || now - endedMs < SHOW_AFTER_FIGHT_MS);
	}

	private static String name(FightReport.PlayerRow row)
	{
		return row == null ? null : row.name;
	}

	boolean hasCrown(String player)
	{
		return player.equals(mvp);
	}

	boolean hasBurger(String player)
	{
		return player.equals(burger);
	}

	boolean hasFries(String player)
	{
		return player.equals(fries);
	}

	boolean anyone()
	{
		return mvp != null || burger != null || fries != null || !speech.isEmpty();
	}

	/** What the player says now: the heal after a boss first, otherwise a hungry line. Null when silent. */
	String line(String player, long now)
	{
		Speech s = speech.get(player);
		if (s != null && now < s.until)
		{
			return s.text;
		}
		return hungryLine(player, now);
	}

	private String hungryLine(String player, long now)
	{
		if (hasBurger(player))
		{
			return burgerTalk.line(now);
		}
		return hasFries(player) ? friesTalk.line(now) : null;
	}
}
