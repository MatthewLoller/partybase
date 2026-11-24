/*
 * Copyright (c) 2026, Heisty <https://github.com/Falsary>
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
package com.matthewloller.partybase.defence;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;

// Shared policy for the special attacks that Better Party Defence accepts.
final class SpecialWeaponRules
{
	private static final Set<SpecialWeapon> SUPPORTED = Collections.unmodifiableSet(EnumSet.of(
		SpecialWeapon.DRAGON_WARHAMMER,
		SpecialWeapon.ELDER_MAUL,
		SpecialWeapon.BANDOS_GODSWORD,
		SpecialWeapon.ARCLIGHT,
		SpecialWeapon.DARKLIGHT,
		SpecialWeapon.EMBERLIGHT,
		SpecialWeapon.BARRELCHEST_ANCHOR,
		SpecialWeapon.BONE_DAGGER,
		SpecialWeapon.DORGESHUUN_CROSSBOW,
		SpecialWeapon.ACCURSED_SCEPTRE,
		SpecialWeapon.TONALZTICS_OF_RALOS,
		SpecialWeapon.SEERCULL,
		SpecialWeapon.EYE_OF_AYAK));

	private static final Map<Integer, SpecialWeapon> WEAPONS_BY_ITEM = buildWeaponIndex();

	private SpecialWeaponRules()
	{
	}

	static boolean isSupported(SpecialWeapon weapon)
	{
		return weapon != null && SUPPORTED.contains(weapon);
	}

	static boolean drainsDefence(SpecialWeapon weapon)
	{
		if (!isSupported(weapon))
		{
			return false;
		}
		return weapon != SpecialWeapon.SEERCULL && weapon != SpecialWeapon.EYE_OF_AYAK;
	}

	static boolean drainsMagicDefence(SpecialWeapon weapon)
	{
		return weapon == SpecialWeapon.ACCURSED_SCEPTRE
			|| weapon == SpecialWeapon.SEERCULL
			|| weapon == SpecialWeapon.EYE_OF_AYAK;
	}

	static SpecialWeapon byItemId(int itemId)
	{
		return WEAPONS_BY_ITEM.get(itemId);
	}

	static int correlationHit(SpecialWeapon weapon, int hit)
	{
		return weapon == SpecialWeapon.TONALZTICS_OF_RALOS ? 0 : hit;
	}

	static int reportedValue(SpecialWeapon weapon, int hit)
	{
		return weapon != null && weapon.isDamage() ? Math.max(0, hit) : 1;
	}

	static String shortName(SpecialWeapon weapon)
	{
		if (weapon == null)
		{
			return "Spec";
		}
		switch (weapon)
		{
			case DRAGON_WARHAMMER:
				return "DWH";
			case BANDOS_GODSWORD:
				return "BGS";
			case ELDER_MAUL:
				return "Elder maul";
			case TONALZTICS_OF_RALOS:
				return "Ralos";
			case BARRELCHEST_ANCHOR:
				return "Anchor";
			case BONE_DAGGER:
				return "Bone dagger";
			case DORGESHUUN_CROSSBOW:
				return "Dorgeshuun cbow";
			case ACCURSED_SCEPTRE:
				return "Accursed";
			case EYE_OF_AYAK:
				return "Eye of ayak";
			case SEERCULL:
				return "Seercull";
			default:
				return weapon.name();
		}
	}

	private static Map<Integer, SpecialWeapon> buildWeaponIndex()
	{
		Map<Integer, SpecialWeapon> weapons = new HashMap<>();
		for (SpecialWeapon weapon : SpecialWeapon.values())
		{
			if (!isSupported(weapon))
			{
				continue;
			}
			for (int itemId : weapon.getItemID())
			{
				weapons.putIfAbsent(itemId, weapon);
			}
		}
		return Collections.unmodifiableMap(weapons);
	}
}
