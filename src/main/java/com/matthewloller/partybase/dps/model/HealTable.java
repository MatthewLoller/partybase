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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * How much a heal would give if nothing was wasted ("full heal"). Used for overhealing.
 * Numbers from the OSRS Wiki. Food is matched by item name, so noted/blighted/other
 * variants with the same name work too.
 */
public final class HealTable
{
	/** Not in the table: only effective healing is counted. */
	public static final int UNKNOWN = -1;

	private static final Map<String, Integer> FIXED = new HashMap<>();

	static
	{
		FIXED.put("lobster", 12);
		FIXED.put("swordfish", 14);
		FIXED.put("monkfish", 16);
		FIXED.put("cooked karambwan", 18);
		FIXED.put("blighted karambwan", 18);
		FIXED.put("curry", 19);
		FIXED.put("shark", 20);
		FIXED.put("sea turtle", 21);
		FIXED.put("manta ray", 22);
		FIXED.put("blighted manta ray", 22);
		FIXED.put("dark crab", 22);
		FIXED.put("tuna potato", 22);
	}

	private HealTable()
	{
	}

	/** "Saradomin brew(3)" becomes "saradomin brew". */
	static String baseName(String itemName)
	{
		if (itemName == null)
		{
			return "";
		}
		String n = itemName.toLowerCase(Locale.ROOT).trim();
		int p = n.indexOf('(');
		if (p > 0)
		{
			n = n.substring(0, p).trim();
		}
		return n;
	}

	/** Full heal of one bite / dose, or {@link #UNKNOWN}. hpLevel = your real Hitpoints level. */
	public static int foodHeal(String itemName, int hpLevel)
	{
		String n = baseName(itemName);
		if (n.equals("saradomin brew"))
		{
			return hpLevel * 15 / 100 + 2;
		}
		if (n.equals("anglerfish") || n.equals("blighted anglerfish"))
		{
			return hpLevel / 10 + 2 * (hpLevel / 25) + 5 * (hpLevel / 93) + 2;
		}
		Integer v = FIXED.get(n);
		return v == null ? UNKNOWN : v;
	}

	/** How far above your real Hitpoints level this food can take you (brew, anglerfish). */
	public static int foodBoost(String itemName, int hpLevel)
	{
		String n = baseName(itemName);
		if (n.equals("saradomin brew") || n.equals("anglerfish") || n.equals("blighted anglerfish"))
		{
			return foodHeal(itemName, hpLevel);
		}
		return 0;
	}

	/** Sanguinesti staff heal when it procs: half the damage of that hit. */
	public static int sanguinesti(int damage)
	{
		return Math.max(0, damage) / 2;
	}

	/**
	 * Blood spells: 25% of damage, +2% per bloodbark piece, x1.1 with an ancient sceptre,
	 * x1.5 with Zuriel's staff. Rounded down.
	 */
	public static int bloodSpell(int damage, int bloodbarkPieces, String weaponName)
	{
		double ratio = 0.25 + 0.02 * Math.max(0, Math.min(5, bloodbarkPieces));
		String w = weaponName == null ? "" : weaponName.toLowerCase(Locale.ROOT);
		if (w.contains("ancient sceptre"))
		{
			ratio *= 1.1;
		}
		else if (w.contains("zuriel"))
		{
			ratio *= 1.5;
		}
		return (int) Math.floor(Math.max(0, damage) * ratio + 1e-9);
	}

	/** The Blood ancient sceptre lets blood magic heal 10% of your Hitpoints above your level. */
	public static int bloodBoost(String weaponName, int hpLevel)
	{
		String w = weaponName == null ? "" : weaponName.toLowerCase(Locale.ROOT);
		return w.contains("blood ancient sceptre") ? hpLevel / 10 : 0;
	}

	/** Weapons you can cast blood spells with. */
	public static boolean isMagicWeapon(String weaponName)
	{
		String w = weaponName == null ? "" : weaponName.toLowerCase(Locale.ROOT);
		return w.contains("staff") || w.contains("wand") || w.contains("sceptre") || w.contains("kodai");
	}
}
