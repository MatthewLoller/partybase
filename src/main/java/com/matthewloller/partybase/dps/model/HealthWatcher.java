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
import java.util.List;

/**
 * Turns your hitpoints changes into heals, with overhealing.
 *
 * <p>Effective heal = how much your HP really went up. Overheal = the full heal of the
 * source minus the effective heal, but only when you ended at your HP cap (so a wrong
 * table value never makes up overhealing when nothing was wasted).</p>
 *
 * <ul>
 * <li>{@link #foodEaten}: the plugin saw a food/potion really leave your inventory
 *     (full heal from {@link HealTable}, or {@link HealTable#UNKNOWN}).</li>
 * <li>{@link #lifesteal}: the plugin saw a Sanguinesti heal or a blood spell on an NPC you hit.</li>
 * <li>HP up the tick you hit something, without a known source: lifesteal without overheal
 *     (Soulreaper axe and others).</li>
 * <li>+1 or +2 with no reason: {@link HealType#REGEN}. Anything else: {@link HealType#RECEIVED}.</li>
 * <li>HP that goes down without a hitsplat after you cast Heal Other / Heal Group: {@link HealType#HEAL_OTHER}.</li>
 * </ul>
 *
 * <p>HP is only read once per tick, so a heal and a hit in the same tick are
 * added back together: heal = HP change + damage hitsplats on you.</p>
 */
public class HealthWatcher
{
	/** Ticks we wait for the HP to go up after eating. */
	static final int FOOD_TICKS = 2;
	/** Ticks we wait for the HP to go up after a lifesteal hit. */
	static final int LIFESTEAL_TICKS = 1;
	static final int HEAL_OTHER_TICKS = 5;
	static final int MAX_REGEN = 2;

	public static class Heal
	{
		public final HealType type;
		public final int amount;
		public final int overheal;

		Heal(HealType type, int amount, int overheal)
		{
			this.type = type;
			this.amount = amount;
			this.overheal = overheal;
		}

		@Override
		public String toString()
		{
			return type + " " + amount + " (+" + overheal + " over)";
		}
	}

	/** Heals we know are coming (food eaten, lifesteal seen) but have not been matched to HP yet. */
	private static class Pending
	{
		final int tick;
		final int full;
		/** HP above your level this source may reach. */
		final int boost;

		Pending(int tick, int full, int boost)
		{
			this.tick = tick;
			this.full = full;
			this.boost = boost;
		}
	}

	private int prevHp = -1;
	private int maxHp = 99;
	private int netChange;
	private int hitsplatDamage;
	private int lastClickTick = Integer.MIN_VALUE / 2;
	private int lastMyHitTick = Integer.MIN_VALUE / 2;
	private int healOtherCastTick = Integer.MIN_VALUE / 2;
	private final List<Pending> food = new ArrayList<>();
	private final List<Pending> life = new ArrayList<>();

	/** Forget the last HP value (login, world hop). */
	public void reset()
	{
		prevHp = -1;
		netChange = 0;
		hitsplatDamage = 0;
		food.clear();
		life.clear();
	}

	/** Your real (not boosted) Hitpoints level. */
	public void setMaxHp(int level)
	{
		if (level > 0)
		{
			maxHp = level;
		}
	}

	/** New current (boosted) hitpoints value. */
	public void hpChanged(int hp)
	{
		if (prevHp < 0)
		{
			prevHp = hp;
			return;
		}
		netChange += hp - prevHp;
		prevHp = hp;
	}

	/** You clicked Eat / Drink (backup when the item is not seen leaving the inventory). */
	public void consumed(int tick)
	{
		lastClickTick = tick;
	}

	/** Food or potion really eaten. full = {@link HealTable#UNKNOWN} when not in the table. */
	public void foodEaten(int tick, int full, int boost)
	{
		food.add(new Pending(tick, full, boost));
		lastClickTick = tick;
	}

	/** A lifesteal heal is coming (Sanguinesti heal, blood spell) worth this much if nothing is wasted. */
	public void lifesteal(int tick, int full, int boost)
	{
		if (full > 0)
		{
			life.add(new Pending(tick, full, boost));
		}
	}

	/** You hit an NPC for more than 0. */
	public void myHit(int tick)
	{
		lastMyHitTick = tick;
	}

	public void healOtherCast(int tick)
	{
		healOtherCastTick = tick;
	}

	/** A damage hitsplat on you. */
	public void damageOnMe(int amount)
	{
		hitsplatDamage += Math.max(0, amount);
	}

	/** Call once at the end of every tick. Returns the heals found this tick. */
	public List<Heal> endTick(int tick)
	{
		List<Heal> out = new ArrayList<>(2);
		int change = netChange + hitsplatDamage;
		boolean realGain = netChange > 0;
		int gain = Math.max(0, change);

		gain = settle(food, HealType.FOOD, FOOD_TICKS, tick, gain, out);
		gain = settle(life, HealType.LIFESTEAL, LIFESTEAL_TICKS, tick, gain, out);

		if (gain > 0)
		{
			HealType type;
			if (tick - lastClickTick <= FOOD_TICKS)
			{
				type = HealType.FOOD;
			}
			else if (tick - lastMyHitTick <= LIFESTEAL_TICKS)
			{
				type = HealType.LIFESTEAL;
			}
			else if (gain <= MAX_REGEN)
			{
				type = HealType.REGEN;
			}
			else
			{
				type = HealType.RECEIVED;
			}
			// HP did not really go up: only trust it when there is a clear reason
			if (realGain || type == HealType.FOOD || type == HealType.LIFESTEAL)
			{
				out.add(new Heal(type, gain, 0));
			}
		}
		else if (change < 0 && tick - healOtherCastTick <= HEAL_OTHER_TICKS)
		{
			out.add(new Heal(HealType.HEAL_OTHER, -change, 0));
			healOtherCastTick = Integer.MIN_VALUE / 2;
		}

		netChange = 0;
		hitsplatDamage = 0;
		return out;
	}

	/**
	 * Match waiting heals to the HP gain. Settles when HP went up, or when the wait is over
	 * (then everything was wasted). Returns the gain that is left for other sources.
	 */
	private int settle(List<Pending> list, HealType type, int waitTicks, int tick, int gain, List<Heal> out)
	{
		if (list.isEmpty())
		{
			return gain;
		}
		int oldest = Integer.MAX_VALUE;
		for (Pending p : list)
		{
			oldest = Math.min(oldest, p.tick);
		}
		if (gain <= 0 && tick - oldest < waitTicks)
		{
			return gain; // keep waiting
		}

		boolean unknown = false;
		int full = 0;
		int boost = 0;
		for (Pending p : list)
		{
			if (p.full < 0)
			{
				unknown = true;
			}
			else
			{
				full += p.full;
			}
			boost = Math.max(boost, p.boost);
		}
		list.clear();

		int effective = unknown ? gain : Math.min(gain, full);
		boolean capped = prevHp >= 0 && prevHp >= maxHp + boost;
		int overheal = !unknown && capped ? Math.max(0, full - effective) : 0;
		if (effective > 0 || overheal > 0)
		{
			out.add(new Heal(type, effective, overheal));
		}
		return gain - effective;
	}
}
