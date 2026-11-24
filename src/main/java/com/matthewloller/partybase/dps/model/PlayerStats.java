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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/** Everything one player did in one fight. Plain fields for Gson. */
public class PlayerStats
{
	public String name;
	public PlayerKind kind;
	/** Damage per target key. */
	public Map<String, TargetDamage> byTarget = new LinkedHashMap<>();
	/** Guessed damage per target, used for {@link PlayerKind#ESTIMATE} players. */
	public Map<String, TargetDamage> estByTarget = new LinkedHashMap<>();
	/** Damage per source (weapon, special attack or spell), then per target key. */
	public Map<String, Map<String, TargetDamage>> bySource = new LinkedHashMap<>();
	public long damageTaken;
	/** Hitsplats seen on this player, used for {@link PlayerKind#ESTIMATE} players. */
	public long seenTaken;
	public long healFood;
	public long healLifesteal;
	public long healOther;
	/** Wasted food and potion healing. Lifesteal past full health is not counted: you cannot avoid it. */
	public long overFood;

	public PlayerStats()
	{
	}

	public PlayerStats(String name, PlayerKind kind)
	{
		this.name = name;
		this.kind = kind;
	}

	public void addHit(String targetKey, int amount, boolean miss)
	{
		addHit(targetKey, amount, miss, null);
	}

	public void addHit(String targetKey, int amount, boolean miss, String source)
	{
		byTarget.computeIfAbsent(targetKey, k -> new TargetDamage()).add(amount, miss);
		if (source != null && !source.isEmpty())
		{
			sources().computeIfAbsent(source, k -> new LinkedHashMap<>())
			.computeIfAbsent(targetKey, k -> new TargetDamage()).add(amount, miss);
		}
	}

	/** Never null; files saved by older versions have no sources. */
	public Map<String, Map<String, TargetDamage>> sources()
	{
		if (bySource == null)
		{
			bySource = new LinkedHashMap<>();
		}
		return bySource;
	}

	public void addEstimate(String targetKey, int amount, boolean miss)
	{
		estByTarget.computeIfAbsent(targetKey, k -> new TargetDamage()).add(amount, miss);
	}

	public Map<String, TargetDamage> damageByTarget()
	{
		return kind == PlayerKind.ESTIMATE ? estByTarget : byTarget;
	}

	public long takenTotal()
	{
		return kind == PlayerKind.ESTIMATE ? seenTaken : damageTaken;
	}

	public void addHeal(HealType type, int amount)
	{
		addHeal(type, amount, 0);
	}

	public void addHeal(HealType type, int amount, int overheal)
	{
		if (overheal > 0 && type == HealType.FOOD)
		{
			overFood += overheal;
		}
		if (amount <= 0)
		{
			return;
		}
		switch (type)
		{
			case FOOD:
				healFood += amount;
				break;
			case LIFESTEAL:
				healLifesteal += amount;
				break;
			case HEAL_OTHER:
				healOther += amount;
				break;
			default:
				break;
		}
	}

	public long overheal()
	{
		return overFood;
	}

	/** Healing without natural regeneration, which every player gets the same. */
	public long healing()
	{
		return healFood + healLifesteal + healOther;
	}

	public void merge(PlayerStats o)
	{
		merge(o, UnaryOperator.identity());
	}

	/** Adds {@code o} to this player, renaming target keys with {@code keyMap}. */
	public void merge(PlayerStats o, UnaryOperator<String> keyMap)
	{
		if (o.kind == PlayerKind.LOCAL || (o.kind == PlayerKind.PARTY && kind == PlayerKind.ESTIMATE))
		{
			kind = o.kind;
		}
		mergeTargets(byTarget, o.byTarget, keyMap);
		mergeTargets(estByTarget, o.estByTarget, keyMap);
		for (Map.Entry<String, Map<String, TargetDamage>> s : o.sources().entrySet())
		{
			mergeTargets(sources().computeIfAbsent(s.getKey(), k -> new LinkedHashMap<>()), s.getValue(), keyMap);
		}
		damageTaken += o.damageTaken;
		seenTaken += o.seenTaken;
		healFood += o.healFood;
		healLifesteal += o.healLifesteal;
		healOther += o.healOther;
		overFood += o.overFood;
	}

	private static void mergeTargets(Map<String, TargetDamage> into, Map<String, TargetDamage> from,
		UnaryOperator<String> keyMap)
	{
		for (Map.Entry<String, TargetDamage> e : from.entrySet())
		{
			into.computeIfAbsent(keyMap.apply(e.getKey()), k -> new TargetDamage()).merge(e.getValue());
		}
	}

	/** Drops the damage on every target that is not in {@code keys}. */
	public void keepTargets(Set<String> keys)
	{
		byTarget.keySet().retainAll(keys);
		estByTarget.keySet().retainAll(keys);
		for (Map<String, TargetDamage> s : sources().values())
		{
			s.keySet().retainAll(keys);
		}
		sources().values().removeIf(Map::isEmpty);
	}

	public PlayerStats copy()
	{
		PlayerStats c = new PlayerStats(name, kind);
		c.merge(this);
		c.kind = kind;
		return c;
	}
}
