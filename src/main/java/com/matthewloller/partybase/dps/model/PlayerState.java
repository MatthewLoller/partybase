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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** What decides your hit chance: levels, prayers, gear and attack style. Filled on the client thread. */
public class PlayerState
{
	/** 0 when you hold no weapon. */
	public int weaponId;
	/** null when you hold no weapon. */
	public String weaponName;
	/** The selected style in the combat tab (0-3, 4 for autocast). */
	public int styleSlot;
	/** Boosted levels. */
	public int attack;
	public int ranged;
	public int magic;
	/** Names of the active prayers, like "PIETY". */
	public Set<String> prayers = new HashSet<>();
	/** Attack bonuses of all your gear together. */
	public int stabBonus;
	public int slashBonus;
	public int crushBonus;
	public int magicBonus;
	public int rangedBonus;
	/** Your special attack is turned on: the next attack is a spec. */
	public boolean specialAttack;
	/** A spell you cast by clicking it, like "Ice Barrage"; null when you attack with your weapon or autocast. */
	public String clickedSpell;
	/** "fire", "water", "earth" or "air" when you autocast an elemental spell, else null. */
	public String autocastElement;
	/** You autocast a demonbane spell (with the purging staff, for example). */
	public boolean autocastDemonbane;
	/** In the Wilderness: charged revenant weapons hit better. */
	public boolean inWilderness;
	/** Soul stacks of the Soulreaper axe: its spec gets 12% more accuracy for each. */
	public int soulreaperStacks;
	/** Mark of Darkness is on: demonbane spells hit demons better. */
	public boolean markOfDarkness;
	/** On the standard spellbook: smoke staves help its spells. */
	public boolean standardSpellbook;
	/** The monster is on your slayer task (from RuneLite's Slayer plugin). */
	public boolean onTask;
	/** Names of the items you wear. */
	public List<String> worn = new ArrayList<>();
}
