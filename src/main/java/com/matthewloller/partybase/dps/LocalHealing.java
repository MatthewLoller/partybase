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

import com.matthewloller.partybase.dps.model.HealTable;
import com.matthewloller.partybase.dps.model.HealthWatcher;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpotanimID;

/**
 * Your own healing: food and potions, lifesteal (Sanguinesti staff, blood spells) and Heal Other.
 * Feeds the game events into {@link HealthWatcher}, which turns HP changes into heals and overheal.
 * Client thread only.
 */
class LocalHealing
{
	private static final int WEAPON_SLOT = 3;
	/** A food click is confirmed when the item leaves the inventory within this many ticks. */
	private static final int EAT_CONFIRM_TICKS = 3;
	private static final int[] SANGUINESTI_HEAL = {SpotanimID.SANGUINESTI_STAFF_HEAL, SpotanimID.SANGUINESTI_STAFF_HEAL_JUSTICIAR};
	private static final int[] BLOOD_SPELL_IMPACT = {SpotanimID.BLOOD_RUSH_IMPACT, SpotanimID.BLOOD_BLITZ_IMPACT,
		SpotanimID.SPELL_BLOOD_BURST_IMPACT, SpotanimID.SPELL_BLOOD_BARRAGE_IMPACT};

	private final Client client;
	private final HealthWatcher health = new HealthWatcher();
	/** Your damage per NPC this tick, for lifesteal. */
	private final Map<NPC, Integer> damageThisTick = new HashMap<>();
	/** Item id to the tick it was clicked to eat or drink. */
	private final Map<Integer, Integer> eatClicks = new HashMap<>();
	private Map<Integer, Integer> lastInventory;

	@Inject
	LocalHealing(Client client)
	{
		this.client = client;
	}

	void hpChanged(int level, int boosted)
	{
		health.setMaxHp(level);
		health.hpChanged(boosted);
	}

	void damageOnYou(int amount)
	{
		health.damageOnMe(amount);
	}

	void youHit(NPC npc, int amount, int tick)
	{
		health.myHit(tick);
		damageThisTick.merge(npc, amount, Integer::sum);
	}

	void eatClicked(int itemId, int tick)
	{
		health.consumed(tick);
		if (itemId > 0)
		{
			eatClicks.put(itemId, tick);
		}
	}

	void healOtherCast(int tick)
	{
		health.healOtherCast(tick);
	}

	void inventoryChanged(ItemContainer inventory, int tick)
	{
		Map<Integer, Integer> now = new HashMap<>();
		for (Item item : inventory.getItems())
		{
			if (item.getId() > 0)
			{
				now.merge(item.getId(), Math.max(1, item.getQuantity()), Integer::sum);
			}
		}
		Map<Integer, Integer> before = lastInventory;
		lastInventory = now;
		if (before == null || eatClicks.isEmpty())
		{
			return;
		}
		int hpLevel = client.getRealSkillLevel(Skill.HITPOINTS);
		Iterator<Map.Entry<Integer, Integer>> it = eatClicks.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<Integer, Integer> click = it.next();
			int id = click.getKey();
			if (tick - click.getValue() > EAT_CONFIRM_TICKS)
			{
				it.remove();
			}
			else if (now.getOrDefault(id, 0) < before.getOrDefault(id, 0))
			{
				String name = itemName(id);
				health.foodEaten(tick, HealTable.foodHeal(name, hpLevel), HealTable.foodBoost(name, hpLevel));
				it.remove();
			}
		}
	}

	List<HealthWatcher.Heal> endTick(int tick)
	{
		checkLifesteal(tick);
		return health.endTick(tick);
	}

	void reset()
	{
		health.reset();
		damageThisTick.clear();
		eatClicks.clear();
		lastInventory = null;
	}

	/** Weapon name, or null when unarmed. */
	String weaponName()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return null;
		}
		Item[] items = worn.getItems();
		return items.length > WEAPON_SLOT && items[WEAPON_SLOT].getId() > 0 ? itemName(items[WEAPON_SLOT].getId()) : null;
	}

	private String itemName(int itemId)
	{
		ItemComposition def = client.getItemDefinition(itemId);
		return def == null ? null : def.getName();
	}

	/** Tells the watcher how much this tick's Sanguinesti procs and blood spells would heal. */
	private void checkLifesteal(int tick)
	{
		if (damageThisTick.isEmpty())
		{
			return;
		}
		Player me = client.getLocalPlayer();
		boolean sangOnMe = me != null && hasAny(me, SANGUINESTI_HEAL);
		String weapon = null;
		int bloodbark = 0;
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn != null)
		{
			Item[] items = worn.getItems();
			for (int i = 0; i < items.length; i++)
			{
				if (items[i].getId() <= 0)
				{
					continue;
				}
				String name = itemName(items[i].getId());
				if (i == WEAPON_SLOT)
				{
					weapon = name;
				}
				if (name != null && name.startsWith("Bloodbark"))
				{
					bloodbark++;
				}
			}
		}
		int sang = 0;
		int blood = 0;
		for (Map.Entry<NPC, Integer> e : damageThisTick.entrySet())
		{
			NPC npc = e.getKey();
			if (sangOnMe || hasAny(npc, SANGUINESTI_HEAL))
			{
				sang += HealTable.sanguinesti(e.getValue());
			}
			else if (HealTable.isMagicWeapon(weapon) && hasAny(npc, BLOOD_SPELL_IMPACT))
			{
				blood += HealTable.bloodSpell(e.getValue(), bloodbark, weapon);
			}
		}
		damageThisTick.clear();
		health.lifesteal(tick, sang, 0);
		health.lifesteal(tick, blood, HealTable.bloodBoost(weapon, client.getRealSkillLevel(Skill.HITPOINTS)));
	}

	private static boolean hasAny(Actor actor, int[] spotAnims)
	{
		for (int id : spotAnims)
		{
			if (actor.hasSpotAnim(id))
			{
				return true;
			}
		}
		return false;
	}
}
